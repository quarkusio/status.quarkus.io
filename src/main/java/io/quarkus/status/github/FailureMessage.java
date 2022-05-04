package io.quarkus.status.github;

public class FailureMessage {

    static final String FULL_REPORT_MARKER = "Quarkus-GitHub-Bot/msg-id:workflow-run-status-active";
    static final String COMMENT_MARKER = "Link to latest CI run";

    public final FailureMessageType type;
    public final String message;

    FailureMessage(FailureMessageType type, String message) {
        this.type = type;
        this.message = message;
    }

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
