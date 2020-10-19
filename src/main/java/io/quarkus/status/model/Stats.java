package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@RegisterForReflection
public class Stats {
    public LocalDateTime updated;
    public String repository;
    public String label;

    public List<StatsEntry> entries = new LinkedList<>();

    public void add(StatsEntry statsEntry) {
        entries.add(statsEntry);
    }
}
