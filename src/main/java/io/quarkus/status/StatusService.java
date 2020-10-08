package io.quarkus.status;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.cache.CacheResult;
import io.quarkus.status.github.GitHubService;
import io.quarkus.status.github.Issue;
import io.quarkus.status.model.Status;
import io.quarkus.status.model.StatusCode;
import io.quarkus.status.model.StatusLine;
import io.quarkus.status.model.StatusSection;

@ApplicationScoped
public class StatusService {

    private static final List<Integer> MAIN_ISSUES = Arrays.asList(
            6588,
            12111,
            11515
    );
    private static final String OWNER = "quarkusio";
    private static final String MAIN_REPOSITORY = "quarkus";

    private static final String PLATFORM_LABEL = "triage/ci-platform";

    @Inject
    GitHubService gitHubService;

    @CacheResult(cacheName = CacheNames.STATUS_CACHE_NAME)
    public Status getStatus() throws IOException {
        Status status = new Status();
        status.updated = LocalDateTime.now();

        StatusSection mainSection = new StatusSection();
        mainSection.name = "Main Builds";
        int i = 0;
        for (Issue issue : gitHubService.findIssuesById(OWNER, MAIN_REPOSITORY, MAIN_ISSUES)) {
            StatusLine statusLine = fromIssue(issue);
            // we want to respect the order
            statusLine.order = i++;
            mainSection.lines.add(statusLine);
        }
        status.sections.add(mainSection);

        StatusSection platformSection = new StatusSection();
        platformSection.name = "Platform";
        for (Issue issue : gitHubService.findIssuesByLabel(OWNER, MAIN_REPOSITORY, PLATFORM_LABEL)) {
            StatusLine statusLine = fromIssue(issue);
            platformSection.lines.add(statusLine);
        }
        status.sections.add(platformSection);

        return status;
    }

    private StatusLine fromIssue(Issue issue) {
        StatusLine statusLine = new StatusLine();
        statusLine.name = issue.title;
        statusLine.url = issue.url;
        statusLine.statusCode = issue.isOpen() ? StatusCode.FAILURE : StatusCode.SUCCESS;
        statusLine.failureMessage = issue.getFailureMessage();
        return statusLine;
    }
}