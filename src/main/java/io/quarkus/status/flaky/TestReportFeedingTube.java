package io.quarkus.status.flaky;

import io.quarkus.status.flaky.TestExecution;
import io.quarkus.status.flaky.TestJob;
import io.quarkus.status.flaky.feeding.JobResult;
import io.quarkus.status.flaky.feeding.TestResultDto;
import io.quarkus.status.flaky.feeding.WorkflowResult;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/test-results")
@RolesAllowed("quarkus-bot")
public class TestReportFeedingTube {

    @Inject
    TestStatisticsResource statisticsResource;

    @POST
    @Transactional
    public Response storeTestResults(WorkflowResult results) {
        for (JobResult job : results.getJobs()) {
            var testJob = new TestJob();
            testJob.url = job.getJobUrl();
            testJob.name = job.getJobName();
            testJob.completedAt = job.getCompletedAt();
            testJob.sha = results.getSha();
            TestJob.persist(testJob);

            for (TestResultDto test : job.getTests()) {
                var testExecution = new TestExecution();
                testExecution.testName = test.getName();
                testExecution.successful = test.isSuccessful();
                testExecution.job = testJob;

                TestExecution.persist(testExecution);
            }
        }

        statisticsResource.clearCache();

        return Response.ok().build();
    }
}
