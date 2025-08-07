package io.quarkus.status.model;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record StatusSection(String name, Set<StatusLine> lines) {

    public StatusCode getStatusCode() {
        StatusCode statusCode = StatusCode.SUCCESS;
        for (StatusLine line : lines) {
            if (line.statusCode().overrides(statusCode)) {
                statusCode = line.statusCode();
            }
        }
        return statusCode;
    }

    public boolean isFailure() {
        return getStatusCode() == StatusCode.FAILURE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatusSection)) {
            return false;
        }
        StatusSection other = (StatusSection) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
