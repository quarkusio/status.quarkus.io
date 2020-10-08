package io.quarkus.status.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Status {

    public static final String MAIN_ID = "main";
    public static final String PLATFORM_ID = "platform";
    public static final String QUARKIVERSE_ID = "quarkiverse";

    public Map<String, StatusSection> sections = new LinkedHashMap<>();

    public LocalDateTime updated;

    public StatusCode getStatusCode() {
        StatusCode statusCode = StatusCode.SUCCESS;
        for (StatusSection section : sections.values()) {
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
