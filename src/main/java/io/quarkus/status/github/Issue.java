package io.quarkus.status.github;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.status.github.FailureMessage.FailureMessageType;

@RegisterForReflection
public class Issue implements Comparable<Issue> {

    public String id;
    public int number;

    public String title;
    public String body;
    public String url;
    public String state;
    public LocalDateTime closedAt;
    public LocalDateTime updatedAt;

    public User author;

    public List<Comment> lastComments;

    public boolean isOpen() {
        return "OPEN".equals(state);
    }

    public FailureMessage getFailureMessage() {
        for (Comment comment : lastComments) {
            if (comment.body == null || comment.bodyHTML == null) {
                continue;
            }
            if (comment.body.contains(FailureMessage.FULL_REPORT_MARKER)) {
                return new FailureMessage(FailureMessageType.FULL_REPORT, comment.bodyHTML.replace("<table role=\"table\">",
                        "<table role=\"table\" class=\"ui celled table compact\">"));
            }
            if (comment.body.contains(FailureMessage.COMMENT_MARKER)) {
                return new FailureMessage(FailureMessageType.COMMENT, comment.bodyHTML);
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

    @Override
    public String toString() {
        return "Issue{" +
                "number=" + number +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
