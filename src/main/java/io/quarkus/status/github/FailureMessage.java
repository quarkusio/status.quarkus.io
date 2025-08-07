package io.quarkus.status.github;

public record FailureMessage(FailureMessageType type, String message) {

    static final String FULL_REPORT_MARKER = "Quarkus-GitHub-Bot/msg-id:workflow-run-status-active";
    static final String COMMENT_MARKER = "Link to latest CI run";

    public enum FailureMessageType {
        FULL_REPORT(true),
        COMMENT(false);

        private boolean fullReport;

        FailureMessageType(boolean fullReport) {
            this.fullReport = fullReport;
        }

        public boolean isFullReport() {
            return fullReport;
        }
    }
}
