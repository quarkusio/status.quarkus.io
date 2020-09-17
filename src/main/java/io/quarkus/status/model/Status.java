package io.quarkus.status.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Status {

    public List<StatusSection> sections = new ArrayList<>();

    public LocalDateTime updated;

    public StatusCode getStatusCode() {
        StatusCode statusCode = StatusCode.SUCCESS;
        for (StatusSection section : sections) {
            if (section.getStatusCode().overrides(statusCode)) {
                statusCode = section.getStatusCode();
            }
        }

        return statusCode;
    }

    public boolean isFailure() {
        return getStatusCode() == StatusCode.FAILURE;
    }

}
