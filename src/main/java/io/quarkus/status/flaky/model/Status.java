package io.quarkus.status.flaky.model;

public enum Status {
    NO_DATA, // in future, split to SKIPPED and SUCCESS
    ERROR, // at least one test method failed with error
    FAILURE // at least one test method failed with failure
}
