package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record StatsEntry(String entryName,
        String timeWindow,
        Integer created,
        Integer createdAndClosedNow,
        Integer createdAndOpenNow,
        Integer closed,
        Integer createdAndClosed) {
}
