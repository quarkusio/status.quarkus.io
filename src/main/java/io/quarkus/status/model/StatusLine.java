package io.quarkus.status.model;

import java.util.Locale;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.status.github.FailureMessage;

@RegisterForReflection
public class StatusLine implements Comparable<StatusLine> {

    public String name;

    public String url;

    public FailureMessage failureMessage;

    public StatusCode statusCode;

    public int order = -1;

    public boolean isFailure() {
        return statusCode == StatusCode.FAILURE;
    }

    public boolean isWarning() {
        return statusCode == StatusCode.WARNING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatusLine)) {
            return false;
        }
        StatusLine other = (StatusLine) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "StatusLine{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", failureMessage='" + failureMessage + '\'' +
                ", statusCode='" + statusCode + '\'' +
                ", order=" + order +
                '}';
    }

    @Override
    public int compareTo(StatusLine o) {
        if (order > -1 || o.order > -1) {
            return Integer.compare(order, o.order);
        }

        return name.toLowerCase(Locale.ROOT).compareTo(o.name.toLowerCase(Locale.ROOT));
    }
}
