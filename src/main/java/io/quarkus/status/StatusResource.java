package io.quarkus.status;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
import io.quarkus.status.model.Status;

@Path("/")
public class StatusResource {

    @Inject
    StatusService statusService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(Status status);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() throws IOException {
        return Templates.index(statusService.getStatus());
    }
}