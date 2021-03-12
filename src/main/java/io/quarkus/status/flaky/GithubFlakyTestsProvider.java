package io.quarkus.status.flaky;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.status.flaky.model.CheckRun;
import io.quarkus.status.flaky.model.Test;
import io.quarkus.status.flaky.client.GithubClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// TODO: artifact-> name suggests the failed job name, it may be worth showing it
@IfBuildProfile("prod")
@ApplicationScoped
public class GithubFlakyTestsProvider implements FlakyProvider {

    private static final Logger log = Logger.getLogger(GithubFlakyTestsProvider.class);

    private static final Pattern TEST_RESULT_PATTERN = Pattern.compile(".*Tests run.*in.*$");
    private static final Pattern TEST_NAME_PATTERN = Pattern.compile("(?<=in ).*$");
    private static final Pattern TEST_FAILURES_PATTERN = Pattern.compile("(?<=Failures: )\\d+");
    private static final Pattern TEST_ERRORS_PATTERN = Pattern.compile("(?<=Errors: )\\d+");
    private static final String ORG = "quarkusio";
    private static final String REPO = "quarkus";
    private static final String QUARKUS_CI = "Quarkus CI";
    private static final String REPORTS_FILE = "test-reports.tgz";

    private static final int RUN_BATCH_SIZE = 50;
    private static final String GITHUB_API_HOST = "api.github.com";

    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);

    private final String bearerToken;
    private final GithubClient client;

    @Inject
    GithubFlakyTestsProvider(
            @ConfigProperty(name = "flaky.tests.token") Optional<String> token,
            @ConfigProperty(name = "status.token") String defaultToken) {
        this.bearerToken = "Bearer " + token.orElse(defaultToken);

        ApacheHttpClient43Engine httpEngine = new ApacheHttpClient43Engine();
        httpEngine.setFollowRedirects(true);
        // good old resteasy client builder because it can do redirects:
        client = new ResteasyClientBuilderImpl().httpEngine(httpEngine).build().target("https://" + GITHUB_API_HOST)
                .proxy(GithubClient.class);
    }

    @Override
    public void update(ZonedDateTime startDate, FlakyData flakyData) {
        if (updateInProgress.compareAndSet(false, true)) {
            log.info("requested flaky test data update while previous is in progress, ignoring");
            return;
        }
        log.infof("Started gathering flaky tests starting from %s", startDate);
        // go through PR runs and collect check runs and failures
        long fetchStartTime = System.currentTimeMillis();
        GithubClient.WorkflowList workflows = client.workflows(ORG, REPO, bearerToken);

        Integer quarkusCi = null;
        for (GithubClient.Workflow workflow : workflows.getWorkflows()) {
            if (QUARKUS_CI.equals(workflow.getName())) {
                quarkusCi = workflow.getId();
                break;
            }
        }
        if (quarkusCi == null) {
            throw new IllegalStateException("Quarkus CI workflow not found. Exiting");
        }

        boolean done = false;
        for (int pageNo = 1; !done; pageNo++) {
            GithubClient.WorkflowRunList runs = client.runs(ORG, REPO, quarkusCi, RUN_BATCH_SIZE, pageNo, bearerToken);
            log.debugf("fetched %d runs", runs.getWorkflow_runs().size());
            for (GithubClient.WorkflowRun run : runs.getWorkflow_runs()) {

                if (run.getCreated_at().isBefore(startDate)) {
                    // we have at last one run that is too old, skip this one
                    // and don't fetch the next page
                    done = true;
                    continue;
                }

                if (flakyData.getScannedChecks().add(run.getId())) {

                    ZonedDateTime newestFetched = flakyData.getNewestFetched();
                    if (newestFetched == null || newestFetched.isBefore(run.getCreated_at())) {
                        flakyData.setNewestFetched(run.getCreated_at());
                    }

                    // get archived items for the run
                    int runId = run.getId();
                    List<GithubClient.Artifact> artifacts = fetchTestReports(runId);

                    addTestResults(run, artifacts, flakyData);
                }
            }
        }
        flakyData.setLastUpdateFinish(ZonedDateTime.now());
        log.infof("Flaky test gathering done in %s s", (System.currentTimeMillis() - fetchStartTime) / 1000);
        updateInProgress.set(false);
    }

    private void addTestResults(GithubClient.WorkflowRun run, List<GithubClient.Artifact> artifacts, FlakyData flakyData) {
        CheckRun checkRun = new CheckRun(run.getId(), run.getHtml_url(), run.getName(), run.getCreated_at());
        flakyData.getAllChecks().add(checkRun);
        for (GithubClient.Artifact artifact : artifacts) {
            if (artifact.isExpired()) {
                log.infof("test results archive expired for run %s", run.getHtml_url());
            } else {
                InputStream archive = client.artifact(ORG, REPO, artifact.getId(), bearerToken);
                addTestResultsFromZip(archive, checkRun, flakyData);
            }
        }
    }

    private void addTestResultsFromZip(InputStream input, CheckRun checkRun, FlakyData flakyData) {
        byte[] buffer = new byte[100_000];
        ByteArrayOutputStream tgzFile;
        try {
            tgzFile = readTgzFromZipFile(input);
        } catch (IOException e) {
            log.error("Failed to decompress tgz file", e); // TODO: more info
            return;
        }

        try (GzipCompressorInputStream gzip = new GzipCompressorInputStream(new ByteArrayInputStream(tgzFile.toByteArray()));
             TarArchiveInputStream tarGz = new TarArchiveInputStream(gzip)) {
            TarArchiveEntry entry;

            while ((entry = (TarArchiveEntry) tarGz.getNextEntry()) != null) {
                if (entry.getName().endsWith(".txt")) {
                    ByteArrayOutputStream txtFileContents = new ByteArrayOutputStream();
                    int read;
                    while ((read = tarGz.read(buffer)) != -1) {
                        txtFileContents.write(buffer, 0, read);
                    }

                    extractTestData(txtFileContents.toString(), checkRun, flakyData);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read test results tgz file", e);
        }

    }

    private void extractTestData(String text, CheckRun checkRun, FlakyData flakyData) {
        String[] lines = text.split("\n");
        Arrays.stream(lines).filter(line -> TEST_RESULT_PATTERN.matcher(line).matches())
                .forEach(line -> collectTestResults(line, checkRun, flakyData));
    }

    private void collectTestResults(String line, CheckRun checkRun, FlakyData flakyData) {
        // Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.127 s - in io.quarkus.test.common.TestResourceManagerTest
        String testName = getSingleMatch(line, TEST_NAME_PATTERN);
        int failures = getSingleInt(line, TEST_FAILURES_PATTERN);
        int errors = getSingleInt(line, TEST_ERRORS_PATTERN);

        Test test = flakyData.getTests().computeIfAbsent(testName, Test::new);
        if (errors > 0) {
            test.addError(checkRun);
        } else if (failures > 0) {
            test.addFailure(checkRun);
        }
    }

    private List<GithubClient.Artifact> fetchTestReports(int runId) {
        List<GithubClient.Artifact> result = new ArrayList<>();
        int totalCount;
        do {
            GithubClient.ArtifactList artifactList = client.actionArtifacts(ORG, REPO, runId, bearerToken, 100, 0);
            result.addAll(artifactList.getArtifacts());
            totalCount = artifactList.getTotal_count();
        } while (result.size() < totalCount);

        return result.stream()
                .filter(a -> a.getName().startsWith("test-reports"))
                .collect(Collectors.toList());
    }

    private static ByteArrayOutputStream readTgzFromZipFile(InputStream inputStream) throws IOException {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[10_000];
        try (ZipInputStream zip = new ZipInputStream(inputStream)) {
            for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip.getNextEntry()) {
                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(REPORTS_FILE)) {
                    int read;
                    while ((read = zip.read(buffer)) > 0) {
                        zipBytes.write(buffer, 0, read);
                    }
                }
                zip.closeEntry();
            }
            return zipBytes;
        }
    }

    private static int getSingleInt(String logLine, Pattern pattern) {
        String runAsString = getSingleMatch(logLine, pattern);
        return Integer.parseInt(runAsString);
    }

    private static String getSingleMatch(String logLine, Pattern pattern) {
        Matcher matcher = pattern.matcher(logLine);
        if (matcher.find()) {
            return matcher.group();
        } else {
            throw new IllegalArgumentException("failed to determine test name from " + logLine);
        }
    }

}
