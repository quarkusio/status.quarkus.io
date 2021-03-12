package io.quarkus.status;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
import io.quarkus.status.flaky.FlakyService;
import io.quarkus.status.flaky.model.TestStats;
import io.quarkus.status.model.Stats;
import io.quarkus.status.model.Status;
import org.ocpsoft.prettytime.PrettyTime;

@Path("/")
public class StatusResource {

    @Inject
    StatusService statusService;

    @Inject
    IssuesService issuesService;

    @Inject
    LabelsService labelsService;

    @Inject
    FlakyService flakyService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(Status status);
        public static native TemplateInstance issues(Status status, Stats stats, boolean isBugs);
        public static native TemplateInstance testFailures(String testName,
                                                           Status status,
                                                           TestStats testStats,
                                                           ZonedDateTime lastUpdatedOn);
        public static native TemplateInstance tests(Status status,
                                                    Collection<String> tests,
                                                    ZonedDateTime lastUpdatedOn);
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
        return Templates.tests(statusService.getStatus(), flakyService.getTests(),
                flakyService.getLastUpdatedOn());
    }
    @GET
    @Path("testFailures")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance testFailures(@QueryParam("testName") String testName) throws IOException {
        // TODO: match test by substring?
        // TODO: chart of failures in time/builds
        // TODO: github-like array of red/green showing per PR statistics,
        // TODO: URL to PR
        // TODO: show the failure on click
        return Templates.testFailures(testName, statusService.getStatus(), flakyService.getTestStats(testName),
                flakyService.getLastUpdatedOn());
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

    @TemplateExtension
    static class Extensions {

        static String formatDateTime(LocalDateTime dateTime) {
            PrettyTime p = new PrettyTime();
            return p.format(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
        }
    }
}