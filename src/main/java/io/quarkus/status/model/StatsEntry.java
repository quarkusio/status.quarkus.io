package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StatsEntry {
    public String entryName;
    public String timeWindow;
    public Integer created;
    public Integer createdAndClosedNow;
    public Integer createdAndOpenNow;
    public Integer closed;
    public Integer createdAndClosed;
}
