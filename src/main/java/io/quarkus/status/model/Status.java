package io.quarkus.status.model;

import java.time.LocalDateTime;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Status(Map<String, StatusSection> sections, LocalDateTime updated) {

    public static final String MAIN_ID = "main";
    public static final String PLATFORM_ID = "platform";
    public static final String QUARKIVERSE_ID = "quarkiverse";

    public StatusCode statusCode() {
        StatusCode statusCode = StatusCode.SUCCESS;
        for (StatusSection section : sections.values()) {
            if (section.getStatusCode().overrides(statusCode)) {
                statusCode = section.getStatusCode();
            }
        }

        return statusCode;
    }

    public boolean failure() {
        return statusCode() == StatusCode.FAILURE;
    }
}
