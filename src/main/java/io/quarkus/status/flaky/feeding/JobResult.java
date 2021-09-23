package io.quarkus.status.flaky.feeding;

import java.util.Date;
import java.util.List;

public class JobResult {
    private String jobUrl;
    private String jobName;
    private Date completedAt;

    private List<TestResultDto> tests;

    public String getJobUrl() {
        return jobUrl;
    }

    public String getJobName() {
        return jobName;
    }

    public List<TestResultDto> getTests() {
        return tests;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public void setTests(List<TestResultDto> tests) {
        this.tests = tests;
    }
}
