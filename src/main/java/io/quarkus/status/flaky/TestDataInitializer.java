package io.quarkus.status.flaky;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.Date;
import java.util.Random;

import static io.quarkus.status.flaky.TestStatisticsResource.RESULT_GROUP_SIZE;

@ApplicationScoped
@UnlessBuildProfile("prod")
public class TestDataInitializer {
    @Transactional
    public void setUp(@Observes StartupEvent anything) {
        TestJob testJob = new TestJob();
        testJob.name = "JVM tests on Windows";
        testJob.url = "https://example.com/test-result";
        testJob.completedAt = new Date();
        testJob.sha = "093245802938402934902341";
        TestJob.persist(testJob);

        for (int i = 0; i < 10; i++) {
            TestExecution execution = new TestExecution();
            execution.job = testJob;
            execution.testName = "com.example.restclient.it.SomeTest.alwaysFailingTest";
            execution.successful = false;
            TestExecution.persist(execution);
        }
        for (int i = 0; i < 10; i++) {
            TestExecution execution = new TestExecution();
            execution.job = testJob;
            execution.testName = "always.succeeding.test";
            execution.successful = true;
            TestExecution.persist(execution);
        }


        for (int i = 0; i < RESULT_GROUP_SIZE; i++) {
            TestExecution execution = new TestExecution();
            execution.job = testJob;
            execution.testName = "half.failing.test";
            execution.successful = (i % 2) == 0;
            TestExecution.persist(execution);
        }
        for (int i = 0; i < RESULT_GROUP_SIZE * 2; i++) {
            TestExecution execution = new TestExecution();
            execution.job = testJob;
            execution.testName = "test.failing.in.the.past";
            execution.successful = i >= RESULT_GROUP_SIZE;
            TestExecution.persist(execution);
        }
        for (int i = 0; i < RESULT_GROUP_SIZE * 2; i++) {
            TestExecution execution = new TestExecution();
            execution.job = testJob;
            execution.testName = "test.successful.in.the.past";
            execution.successful = i <= RESULT_GROUP_SIZE;
            TestExecution.persist(execution);
        }
    }

    @Inject
    PerfTestInitializer perfTestInitializer;

    public void createDataForPerfTest(@Observes StartupEvent anything) {
        if (!Boolean.TRUE.toString().equalsIgnoreCase(System.getenv("generate_perf_test_data"))) {
            return;
        }
        Random r = new Random();
        for (int i = 0; i < 600; i++) {
            String testName = "test.successful.in.the.past" + r.nextInt();
            perfTestInitializer.initTestResults(testName);
        }
        System.out.println("\ndone");
    }

    @ApplicationScoped
    public static class PerfTestInitializer {

        public static final int TEST_ROWS_FOR_TEST = 50;

        @Transactional
        void initTestResults(String testName) {
            TestJob testJob = new TestJob();
            testJob.name = "JVM tests on Linux";
            testJob.url = "https://example.com/test-result/2";
            testJob.completedAt = new Date();
            testJob.sha = "123132423412313242341";
            TestJob.persist(testJob);
            Random r = new Random();
            for (int i = 0; i < TEST_ROWS_FOR_TEST; i++) {
                TestExecution execution = new TestExecution();
                execution.job = testJob;
                execution.testName = testName;
                execution.successful = r.nextDouble() > 0.1;
                TestExecution.persist(execution);
            }
        }
    }
}
