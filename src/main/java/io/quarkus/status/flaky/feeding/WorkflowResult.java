package io.quarkus.status.flaky.feeding;

import java.util.List;

public class WorkflowResult {
    private List<JobResult> jobs;
    private String sha;

    public WorkflowResult() {
    }

    public WorkflowResult(List<JobResult> jobs) {
        this.jobs = jobs;
    }

    public List<JobResult> getJobs() {
        return jobs;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }
}
