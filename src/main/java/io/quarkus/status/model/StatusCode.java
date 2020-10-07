package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum StatusCode {

    FAILURE,
    WARNING,
    SUCCESS;

    public boolean overrides(StatusCode other) {
        if (this == FAILURE) {
            return true;
        }
        if (this == WARNING && other == SUCCESS) {
            return true;
        }
        return false;
    }
}