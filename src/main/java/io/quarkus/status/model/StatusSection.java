package io.quarkus.status.model;

import java.util.Objects;
import java.util.TreeSet;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StatusSection {

    public String name;

    public TreeSet<StatusLine> lines = new TreeSet<>();

    public StatusCode getStatusCode() {
        StatusCode statusCode = StatusCode.SUCCESS;
        for (StatusLine line : lines) {
            if (line.statusCode.overrides(statusCode)) {
                statusCode = line.statusCode;
            }
        }
        return statusCode;
    }

    public boolean isFailure() {
        return getStatusCode() == StatusCode.FAILURE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatusSection)) {
            return false;
        }
        StatusSection other = (StatusSection) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "StatusSection{" +
                "name='" + name + '\'' +
                ", lines=" + lines +
                '}';
    }

}
