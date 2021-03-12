package io.quarkus.status.flaky.feeding;

public class TestResultDto {
    private String name;
    private boolean successful;

    public TestResultDto(String name, boolean successful) {
        this.name = name;
        this.successful = successful;
    }

    public String getName() {
        return name;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
