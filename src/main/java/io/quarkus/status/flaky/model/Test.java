package io.quarkus.status.flaky.model;

import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Set;

public class Test {
    private String testName;
    private final Set<CheckRun> failures = new ConcurrentHashSet<>(); // this will hopefully be very small
    private final Set<CheckRun> errors = new ConcurrentHashSet<>(); // this will hopefully be very small

    public Test(String testName) {
        this.testName = testName;
    }

    public Test addFailure(CheckRun checkRun) {
        failures.add(checkRun);
        return this;
    }

    public Test addError(CheckRun checkRun) {
        errors.add(checkRun);
        return this;
    }

    public String getTestName() {
        return testName;
    }

    public Set<CheckRun> getFailures() {
        return failures;
    }

    public Set<CheckRun> getErrors() {
        return errors;
    }
}
