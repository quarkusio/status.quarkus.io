package io.quarkus.status.flaky;

import io.quarkus.status.flaky.model.CheckRun;
import io.quarkus.status.flaky.model.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class FlakyData {
    private final List<CheckRun> allChecks = new CopyOnWriteArrayList<>();
    private final SortedMap<String, Test> tests = new ConcurrentSkipListMap<>();
    private final Set<Integer> scannedChecks = new ConcurrentSkipListSet<>();

    private ZonedDateTime newestFetched;

    private volatile ZonedDateTime lastUpdateFinish;

    public void setNewestFetched(ZonedDateTime newestFetched) {
        this.newestFetched = newestFetched;
    }

    public void setLastUpdateFinish(ZonedDateTime lastUpdateFinish) {
        this.lastUpdateFinish = lastUpdateFinish;
    }

    public List<CheckRun> getAllChecks() {
        return allChecks;
    }

    public SortedMap<String, Test> getTests() {
        return tests;
    }

    public Set<Integer> getScannedChecks() {
        return scannedChecks;
    }

    public ZonedDateTime getNewestFetched() {
        return newestFetched;
    }

    public ZonedDateTime getLastUpdateFinish() {
        return lastUpdateFinish;
    }
}
