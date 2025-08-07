package io.quarkus.status;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.status.github.GitHubService;
import io.quarkus.status.github.Issue;
import io.quarkus.status.model.Status;
import io.quarkus.status.model.StatusCode;
import io.quarkus.status.model.StatusLine;
import io.quarkus.status.model.StatusSection;

@ApplicationScoped
public class StatusService {

    private static final List<Integer> MAIN_ISSUES = Arrays.asList(
            // 15867 // JDK early access
            6588, // Quickstarts native
            12111, // Core snapshots deployment
            35716, // Platform snapshots deployment
            11515, // code.quarkus.io
            17071, // Sync main documentation to website
            13058, // Release process testing
            19500, // Quarkus QE test suite
            15417, // Quarkus Beefy test suite
            23612, // Quarkus Super Heroes testing
            26581, // Kubernetes end to end testing
            // 26582, // OpenShift end to end testing
            31837, // Knative end to end testing
            32191 // Quarkus updates recipes testing
    );
    private static final String QUARKUS_IO_ORG = "quarkusio";
    private static final String MAIN_REPOSITORY = "quarkus";

    private static final String PLATFORM_LABEL = "triage/ci-platform";

    private static final String QUARKIVERSE_ORG = "quarkiverse";
    private static final String QUARKIVERSE_REPOSITORY = "quarkiverse";
    private static final String QUARKIVERSE_LABEL = "triage/ci-quarkiverse";

    @Inject
    GitHubService gitHubService;

    private volatile Status status;

    @Scheduled(every = "10m")
    public void updateStatus() throws Exception {
        status = buildStatus();
    }

    public Status getStatus() throws Exception {
        Status localStatus = status;
        if (localStatus == null) {
            synchronized (this) {
                localStatus = status;
                if (localStatus == null) {
                    status = localStatus = buildStatus();
                }
            }
        }
        return localStatus;
    }

    private Status buildStatus() throws Exception {
        Map<String, StatusSection> sections = new LinkedHashMap<>();

        Set<StatusLine> mainSectionLines = new TreeSet<>();
        int i = 0;
        for (Issue issue : gitHubService.findIssuesById(QUARKUS_IO_ORG, MAIN_REPOSITORY, MAIN_ISSUES)) {
            StatusLine statusLine = fromIssue(issue, i++);
            mainSectionLines.add(statusLine);
        }
        StatusSection mainSection = new StatusSection("Main Builds", mainSectionLines);

        Set<StatusLine> platformStatusLines = new TreeSet<>();
        for (Issue issue : gitHubService.findIssuesByLabel(QUARKUS_IO_ORG, MAIN_REPOSITORY, PLATFORM_LABEL)) {
            StatusLine statusLine = fromIssue(issue, -1);
            platformStatusLines.add(statusLine);
        }
        StatusSection platformSection = new StatusSection("Platform", platformStatusLines);

        Set<StatusLine> quarkiverseStatusLines = new TreeSet<>();
        for (Issue issue : gitHubService.findIssuesByLabel(QUARKIVERSE_ORG, QUARKIVERSE_REPOSITORY, QUARKIVERSE_LABEL)) {
            StatusLine statusLine = fromIssue(issue, -1);
            quarkiverseStatusLines.add(statusLine);
        }
        StatusSection quarkiverseSection = new StatusSection("Quarkiverse", quarkiverseStatusLines);

        sections.put(Status.MAIN_ID, mainSection);
        sections.put(Status.PLATFORM_ID, platformSection);
        sections.put(Status.QUARKIVERSE_ID, quarkiverseSection);

        return new Status(sections, LocalDateTime.now());
    }

    private StatusLine fromIssue(Issue issue, int order) {
        return new StatusLine(
                issue.title(),
                issue.url(),
                issue.failureMessage(),
                determineStatusCode(issue),
                order,
                issue.buildStatus());
    }

    private static StatusCode determineStatusCode(Issue issue) {
        if (issue.updatedAt() == null || ChronoUnit.DAYS.between(issue.updatedAt(), LocalDateTime.now()) > 2) {
            return StatusCode.WARNING;
        }

        return issue.isOpen() ? StatusCode.FAILURE : StatusCode.SUCCESS;
    }
}
