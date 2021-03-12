package io.quarkus.status.flaky;

import java.time.ZonedDateTime;

public interface FlakyProvider {

    void update(ZonedDateTime startFrom, FlakyData flakyData);
}
