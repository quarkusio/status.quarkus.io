package io.quarkus.status.model;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Stats(String name,
        String label,
        LocalDateTime updated,
        String repository,
        List<StatsEntry> entries) {
}
