package io.quarkus.status;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
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
