package io.quarkus.status.github;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Issue implements Comparable<Issue> {

    public String id;
    public int number;

    public String title;
    public String body;
    public String url;
    public String state;
    public LocalDateTime closedAt;

    public User author;

    public List<Comment> lastComments;

    public boolean isOpen() {
        return "OPEN".equals(state);
    }

    public String getFailureMessage() {
        for (Comment comment : lastComments) {
            if (comment.bodyText.contains("Link to latest CI run")) {
                return comment.bodyHTML;
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
