package io.quarkus.status;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.status.flaky.TestExecution;
import io.quarkus.status.model.Stats;
import io.quarkus.status.model.Status;

@Path("/")
public class StatusResource {

    @Inject
    StatusService statusService;

    @Inject
    IssuesService issuesService;

    @Inject
    LabelsService labelsService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(Status status);
        public static native TemplateInstance issues(Status status, Stats stats, boolean isBugs);
        public static native TemplateInstance tests(Status status);
        public static native TemplateInstance testResults(Status status, List<TestExecution> executions);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() throws IOException {
        return Templates.index(statusService.getStatus());
    }

    @GET
    @Path("bugs")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance bugs() throws IOException {
        return Templates.issues(statusService.getStatus(), issuesService.getBugsMonthlyStats(), true);
    }

    @GET
    @Path("tests")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tests() throws IOException {
        return Templates.tests(statusService.getStatus());
    }

    @GET
    @Path("test-details/{testName}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance testResults(@PathParam("testName") String testName,
                                        @QueryParam("page") @DefaultValue("0") Integer page,
                                        @QueryParam("pageSize") @DefaultValue("40") Integer pageSize) throws IOException {
        List<TestExecution> executions = TestExecution.find("testName = ?1 ORDER BY id DESC", testName)
                .page(page, pageSize).list();
        return Templates.testResults(statusService.getStatus(), executions);
    }

    @GET
    @Path("enhancements")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance features() throws IOException {
        return Templates.issues(statusService.getStatus(), issuesService.getEnhancementsMonthlyStats(), false);
    }

    @GET
    @Path("labels/bugs")
    @Produces(MediaType.TEXT_PLAIN)
    public String bugsLabels() throws IOException {
        StringBuilder sb = new StringBuilder();
        labelsService.getBugsLabels().forEach( label -> sb.append(label).append("\n"));
        return sb.toString();
    }

    @GET
    @Path("labels/enhancements")
    @Produces(MediaType.TEXT_PLAIN)
    public String enhancementsLabels() throws IOException {
        StringBuilder sb = new StringBuilder();
        labelsService.getEnhancementsLabels().forEach( label -> sb.append(label).append("\n"));
        return sb.toString();
    }
}
