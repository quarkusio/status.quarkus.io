package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Label {
    public String name;
    public String open;
    public String closed;

    public Label(String name, String open, String closed) {
        this.name = name;
        this.open = open;
        this.closed = closed;
    }

    @Override
    public String toString() {
        return name + "," + open + "," + closed;
    }
}
