package io.quarkus.status.flaky.model;

public class InvocationStatus {
    private final Status status;
    private final CheckRun checkRun;

    public InvocationStatus(Status status, CheckRun checkRun) {
        this.status = status;
        this.checkRun = checkRun;
    }

    public Status getStatus() {
        return status;
    }

    public CheckRun getCheckRun() {
        return checkRun;
    }
}
