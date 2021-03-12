package io.quarkus.status.flaky;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.status.flaky.model.CheckRun;
import io.quarkus.status.flaky.model.InvocationStatus;
import io.quarkus.status.flaky.model.Status;
import io.quarkus.status.flaky.model.Test;
import io.quarkus.status.flaky.model.TestStats;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class FlakyService {

    private static final Logger log = Logger.getLogger(FlakyService.class);
    private static final int DAYS = 10;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final FlakyData flakyData = new FlakyData();

    @Inject
    FlakyProvider flakyProvider;

    @Scheduled(every = "PT1H")
    void update() {
        dropOld();
        ZonedDateTime fetchStart = flakyData.getNewestFetched() == null
                ? ZonedDateTime.now().minusDays(DAYS)
                : flakyData.getNewestFetched().minusMinutes(5);
        fetchNewerThan(fetchStart); // to minimize the chance of missing a run
    }

    private void dropOld() {
        ZonedDateTime evictionCutOff = ZonedDateTime.now().minusDays(DAYS);

        List<CheckRun> checksToEvict = new ArrayList<>();

        for (CheckRun check : flakyData.getAllChecks()) {
            if (check.getTime().isBefore(evictionCutOff)) {
                checksToEvict.add(check);
            }
        }

        flakyData.getAllChecks().removeAll(checksToEvict);
        for (CheckRun check : checksToEvict) {
            flakyData.getScannedChecks().remove(check.getTestRunId());
            for (Test test : flakyData.getTests().values()) {
                test.getErrors().remove(check);
                test.getFailures().remove(check);
            }
        }
        log.debugf("Dropped %d old checks", checksToEvict.size());
    }

    // todo info that test data is processed until it's finished

    public void start(@Observes StartupEvent ignored) {
        fetchNewerThan(ZonedDateTime.now().minusDays(30));
    }

    private void fetchNewerThan(ZonedDateTime startDate) {
        executor.execute(() -> flakyProvider.update(startDate, flakyData));
    }



    public TestStats getTestStats(String testName) {
        if (testName == null) {
            return null;
        }
        Test test = flakyData.getTests().get(testName);
        if (test == null) {
            return null;
        }

        List<InvocationStatus> invocationStatuses = new ArrayList<>();
        for (CheckRun check : flakyData.getAllChecks()) {
            Status status;
            if (test.getErrors().contains(check)) {
                status = Status.ERROR;
            } else if (test.getFailures().contains(check)) {
                status = Status.FAILURE;
            } else {
                status = Status.NO_DATA;
            }

            invocationStatuses.add(new InvocationStatus(status, check));
        }
        return new TestStats(testName, invocationStatuses);
    }

    public ZonedDateTime getLastUpdatedOn() {
        return flakyData.getLastUpdateFinish();
    }

    public Set<String> getTests() {
        return flakyData.getTests().keySet();
    }
}
