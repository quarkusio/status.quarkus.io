package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Label(String name, String open, String closed) {

    public String toCsv() {
        return name + "," + open + "," + closed;
    }
}
