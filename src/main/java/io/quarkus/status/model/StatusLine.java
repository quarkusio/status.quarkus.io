package io.quarkus.status.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.status.github.FailureMessage;

@RegisterForReflection
public record StatusLine(String name,
        String url,
        FailureMessage failureMessage,
        StatusCode statusCode,
        int order,
        BuildStatus buildStatus) implements Comparable<StatusLine> {

    public boolean isFailure() {
        return statusCode == StatusCode.FAILURE;
    }

    public boolean isWarning() {
        return statusCode == StatusCode.WARNING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatusLine)) {
            return false;
        }
        StatusLine other = (StatusLine) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(StatusLine o) {
        if (order > -1 || o.order > -1) {
            return Integer.compare(order, o.order);
        }

        return name.toLowerCase(Locale.ROOT).compareTo(o.name.toLowerCase(Locale.ROOT));
    }

    @RegisterForReflection
    public record BuildStatus(Instant updatedAt, boolean failure, String repository, Long runId,
            String quarkusSha, String projectSha, BuildState firstFailure, BuildState lastFailure, BuildState lastSuccess) {
    }

    @RegisterForReflection
    public record BuildState(ZonedDateTime date, String quarkusSha, String projectSha) {
    }
}
