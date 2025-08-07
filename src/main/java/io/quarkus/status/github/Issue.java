package io.quarkus.status.github;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.status.github.FailureMessage.FailureMessageType;
import io.quarkus.status.model.StatusLine.BuildStatus;

@RegisterForReflection
public record Issue(String id,
        int number,
        String title,
        String body,
        String url,
        String state,
        LocalDateTime closedAt,
        LocalDateTime updatedAt,
        BuildStatus buildStatus,
        List<Comment> lastComments) implements Comparable<Issue> {

    public boolean isOpen() {
        return "OPEN".equals(state);
    }

    public FailureMessage failureMessage() {
        for (Comment comment : lastComments) {
            if (comment.body() == null || comment.bodyHTML() == null) {
                continue;
            }
            if (comment.body().contains(FailureMessage.FULL_REPORT_MARKER)) {
                return new FailureMessage(FailureMessageType.FULL_REPORT, comment.bodyHTML().replace("<table role=\"table\">",
                        "<table role=\"table\" class=\"ui celled table compact\">"));
            }
            if (comment.body().contains(FailureMessage.COMMENT_MARKER)) {
                return new FailureMessage(FailureMessageType.COMMENT, comment.bodyHTML());
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Issue))
            return false;
        Issue issue = (Issue) o;
        return id.equals(issue.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Issue o) {
        return number - o.number;
    }
}
