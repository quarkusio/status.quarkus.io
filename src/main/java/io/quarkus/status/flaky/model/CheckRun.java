package io.quarkus.status.flaky.model;

import java.time.ZonedDateTime;
import java.util.Objects;

public class CheckRun {
    private int testRunId;
    private String githubActionsUrl;
    private String jobName;
    private ZonedDateTime time;

    public CheckRun(int testRunId, String githubActionsUrl, String jobName, ZonedDateTime time) {
        this.testRunId = testRunId;
        this.githubActionsUrl = githubActionsUrl;
        this.jobName = jobName;
        this.time = time;
    }

    public int getTestRunId() {
        return testRunId;
    }

    public String getGithubActionsUrl() {
        return githubActionsUrl;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckRun checkRun = (CheckRun) o;
        return Objects.equals(testRunId, checkRun.testRunId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testRunId);
    }
}
