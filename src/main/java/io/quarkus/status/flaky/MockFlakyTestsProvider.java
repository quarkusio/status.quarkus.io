package io.quarkus.status.flaky;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.status.flaky.model.CheckRun;
import io.quarkus.status.flaky.model.Test;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@UnlessBuildProfile("prod")
@ApplicationScoped
public class MockFlakyTestsProvider implements FlakyProvider {

    private static final Logger log = Logger.getLogger(MockFlakyTestsProvider.class);

    private static final String TEST_A = "com.example.TestA";
    private static final String TEST_B = "com.example.TestB";
    private static final String TEST_C = "com.example.TestC";

    // we only do mock update once
    private AtomicBoolean initialRun = new AtomicBoolean(true);

    @Override
    public void update(ZonedDateTime startFrom, FlakyData flakyData) {
        // three check runs, on first only com.example.TestA fails, on second
        // com.example.TestB and com.example.TestC, on third no test fail

        ZonedDateTime failureTime = ZonedDateTime.now().minusHours(10);
        if (initialRun.compareAndSet(true, false)) {

            CheckRun checkRun1 = new CheckRun(1, "http://example.com/check1", "Quarkus CI", failureTime);
            CheckRun checkRun2 = new CheckRun(2, "http://example.com/check1", "Quarkus CI", failureTime);
            CheckRun checkRun3 = new CheckRun(3, "http://example.com/check1", "Quarkus CI", failureTime);

            flakyData.getTests()
                    .put(TEST_A, new Test(TEST_A).addError(checkRun1)
                            .addFailure(checkRun2));
            flakyData.getTests()
                    .put(TEST_B, new Test(TEST_B).addFailure(checkRun2));
            flakyData.getTests()
                    .put(TEST_C, new Test(TEST_C).addError(checkRun2));

            Stream.of(checkRun1, checkRun2, checkRun3)
                    .forEach(ch -> {
                        flakyData.getScannedChecks().add(ch.getTestRunId());
                        flakyData.getAllChecks().add(ch);
                    });
            flakyData.setNewestFetched(failureTime);
            flakyData.setLastUpdateFinish(ZonedDateTime.now());
            log.info("Mock test data initialized");
        }
    }
}
