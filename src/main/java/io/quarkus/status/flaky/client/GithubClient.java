package io.quarkus.status.flaky.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("/repos/{owner}/{repo}")
@Produces("application/vnd.github.v3+json")
@Retry(delay = 1, delayUnit = ChronoUnit.MINUTES)
public interface GithubClient {
    @GET
    @Path("/actions/runs/{runId}/artifacts")
    ArtifactList actionArtifacts(@PathParam("owner") String owner,
                                 @PathParam("repo") String repo,
                                 @PathParam("runId") long runId,
                                 @HeaderParam("Authorization") String bearerToken,
                                 @QueryParam("per_page") int per_page,
                                 @QueryParam("page") int page);

    @GET
    @Path("/actions/workflows")
    WorkflowList workflows(@PathParam("owner") String owner,
                           @PathParam("repo") String repo,
                           @HeaderParam("Authorization") String bearerToken);

    @GET
    @Path("/actions/workflows/{workflowId}/runs")
    WorkflowRunList runs(@PathParam("owner") String owner,
                         @PathParam("repo") String repo,
                         @PathParam("workflowId") int workflowId,
                         @QueryParam("per_page") int pageSize,
                         @QueryParam("page") int pageNumber,
                         @HeaderParam("Authorization") String bearerToken);

    @GET
    @Path("/actions/artifacts/{artifactId}/zip")
    InputStream artifact(@PathParam("owner") String owner,
                         @PathParam("repo") String repo,
                         @PathParam("artifactId") int id,
                         @HeaderParam("Authorization") String bearerToken);


    @JsonIgnoreProperties(ignoreUnknown = true)
    class Workflow {
        String name;
        int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class WorkflowList {
        int total_count;
        List<Workflow> workflows;

        public int getTotal_count() {
            return total_count;
        }

        public void setTotal_count(int total_count) {
            this.total_count = total_count;
        }

        public List<Workflow> getWorkflows() {
            return workflows;
        }

        public void setWorkflows(List<Workflow> workflows) {
            this.workflows = workflows;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class WorkflowRun {
        private int id;
        private String html_url;
        private String name;
        private ZonedDateTime created_at;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getHtml_url() {
            return html_url;
        }

        public void setHtml_url(String html_url) {
            this.html_url = html_url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ZonedDateTime getCreated_at() {
            return created_at;
        }

        public void setCreated_at(ZonedDateTime created_at) {
            this.created_at = created_at;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class WorkflowRunList {
        int total_count;
        List<WorkflowRun> workflow_runs;

        public int getTotal_count() {
            return total_count;
        }

        public void setTotal_count(int total_count) {
            this.total_count = total_count;
        }

        public List<WorkflowRun> getWorkflow_runs() {
            return workflow_runs;
        }

        public void setWorkflow_runs(List<WorkflowRun> workflow_runs) {
            this.workflow_runs = workflow_runs;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ArtifactList {
        int total_count;
        List<Artifact> artifacts;

        public int getTotal_count() {
            return total_count;
        }

        public void setTotal_count(int total_count) {
            this.total_count = total_count;
        }

        public List<Artifact> getArtifacts() {
            return artifacts;
        }

        public void setArtifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Artifact {
        int id;
        String archive_download_url;
        boolean expired;
        String name; // test-report

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getArchive_download_url() {
            return archive_download_url;
        }

        public void setArchive_download_url(String archive_download_url) {
            this.archive_download_url = archive_download_url;
        }

        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
