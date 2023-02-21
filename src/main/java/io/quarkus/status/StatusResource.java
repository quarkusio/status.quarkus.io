package io.quarkus.status;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
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
        public static native TemplateInstance issuesPerArea(Status status, boolean isBugs);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() throws Exception {
        return Templates.index(statusService.getStatus());
    }

    @GET
    @Path("bugs")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance bugs() throws Exception {
        return Templates.issues(statusService.getStatus(), issuesService.getBugsMonthlyStats(), true);
    }

    @GET
    @Path("enhancements")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance features() throws Exception {
        return Templates.issues(statusService.getStatus(), issuesService.getEnhancementsMonthlyStats(), false);
    }

    @GET
    @Path("labels/bugs")
    @Produces(MediaType.TEXT_PLAIN)
    public String bugsLabels() throws Exception {
        StringBuilder sb = new StringBuilder();
        labelsService.getBugsLabels().forEach( label -> sb.append(label).append("\n"));
        return sb.toString();
    }

    @GET
    @Path("labels/enhancements")
    @Produces(MediaType.TEXT_PLAIN)
    public String enhancementsLabels() throws Exception {
        StringBuilder sb = new StringBuilder();
        labelsService.getEnhancementsLabels().forEach( label -> sb.append(label).append("\n"));
        return sb.toString();
    }

    @GET
    @Path("bugs/per-area")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance bugsPerArea() throws Exception {
        return Templates.issuesPerArea(statusService.getStatus(), true);
    }

    @GET
    @Path("enhancements/per-area")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance enhancementsPerArea() throws Exception {
        return Templates.issuesPerArea(statusService.getStatus(), false);
    }

}
