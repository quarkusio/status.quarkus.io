package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@RegisterForReflection
public record Stats(String name,
                    String label,
                    LocalDateTime updated,
                    String repository,
                    List<StatsEntry> entries) {
}
