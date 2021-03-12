package io.quarkus.status.flaky.model;

import java.util.List;

public class TestStats {
    private final String testName;
    private final List<InvocationStatus> invocations;

    public TestStats(String testName,
                      List<InvocationStatus> invocations) {
        this.testName = testName;
        this.invocations = invocations;
    }

    public String getTestName() {
        return testName;
    }

    public List<InvocationStatus> getInvocations() {
        return invocations;
    }
}
