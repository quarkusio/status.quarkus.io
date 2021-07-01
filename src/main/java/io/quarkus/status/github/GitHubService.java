package io.quarkus.status.github;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.status.graphql.GraphQLClient;
import io.quarkus.status.model.Label;
import io.quarkus.status.model.StatsEntry;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class GitHubService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Inject
    @RestClient
    GraphQLClient graphQLClient;

    private final String token;

    @Inject
    public GitHubService(
            @ConfigProperty(name = "status.token") String token) {
        this.token = "Bearer " + token;
    }

    public List<Issue> findIssuesById(String owner, String repository, List<Integer> issueNumbers) throws IOException {
        if (issueNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        String query = Templates.findIssuesByIds(owner, repository, issueNumbers).render();

        JsonObject response = graphQLClient.graphql(token, new JsonObject().put("query", query));

        // Any errors?
        handleErrors(response);

        JsonObject issuesJson = response
                .getJsonObject("data")
                .getJsonObject("repository");

        List<Issue> issues = new ArrayList<>();
        for (Integer issueNumber : issueNumbers) {
            JsonObject issueJson = issuesJson.getJsonObject("_" + issueNumber);
            if (issueJson == null || issueJson.isEmpty()) {
                continue;
            }

            issues.add(extractIssue(issueJson));
        }

        return issues;
    }

    public StatsEntry issuesStats(String repository, String label, String timeWindow, String entryName) throws IOException {
        String query = Templates.issuesStats(repository, label, timeWindow).render();
        JsonObject response = graphQLClient.graphql(token, new JsonObject().put("query", query));
        handleErrors(response);

        JsonObject data = response.getJsonObject("data");

        StatsEntry statsEntry = new StatsEntry();
        statsEntry.entryName = entryName;
        statsEntry.created = data.getJsonObject("created").getInteger("issueCount");
        statsEntry.createdAndClosedNow = data.getJsonObject("createdAndClosedNow").getInteger("issueCount");
        statsEntry.createdAndOpenNow = data.getJsonObject("createdAndStillOpen").getInteger("issueCount");
        statsEntry.closed = data.getJsonObject("closed").getInteger("issueCount");
        statsEntry.createdAndClosed = data.getJsonObject("createdAndClosed").getInteger("issueCount");

        return statsEntry;
    }

    public List<Label> labelsStats(String owner, String repository, String mainLabel, boolean subsetOnly) throws IOException {
        List<Label> labels = new ArrayList<>();
        labels.add(new Label("Label", "Open", "Closed"));

        String cursor = null;
        if (subsetOnly) {
            extractLabels(owner, repository, mainLabel, 10, cursor, labels);
        } else {
            do {
                cursor = extractLabels(owner, repository, mainLabel, 100, cursor, labels);
            } while (cursor != null);
        }

        return labels;
    }

    private String extractLabels(String owner, String repository, String mainLabel, int returnedElements, String cursor, List<Label> labels) throws IOException {
        String query = Templates.labelsStats(owner, repository, mainLabel, returnedElements, cursor).render();
        JsonObject response = graphQLClient.graphql(token, new JsonObject().put("query", query));
        handleErrors(response);

        JsonObject labelsJson = response
                .getJsonObject("data")
                .getJsonObject("organization")
                .getJsonObject("repository")
                .getJsonObject("labels");
        JsonArray edgesJson = labelsJson
                .getJsonArray("edges");
        JsonObject pageInfoJson = labelsJson
                .getJsonObject("pageInfo");

        for (Object edgeJson : edgesJson) {
            JsonObject issueJson = ((JsonObject) edgeJson).getJsonObject("node");
            labels.add(new Label(
                    issueJson.getString("name"),
                    issueJson.getJsonObject("open").getInteger("totalCount").toString(),
                    issueJson.getJsonObject("closed").getInteger("totalCount").toString()
            ));
        }

        if (pageInfoJson.getBoolean("hasNextPage")) {
            return pageInfoJson.getString("endCursor");
        } else {
            return null;
        }
    }

    public List<Issue> findIssuesByLabel(String owner, String repository, String label) throws IOException {
        String query = Templates.findIssuesByLabel(owner, repository, label).render();

        JsonObject response = graphQLClient.graphql(token, new JsonObject().put("query", query));

        // Any errors?
        handleErrors(response);

        JsonArray edgesJson = response
                .getJsonObject("data")
                .getJsonObject("repository")
                .getJsonObject("issues")
                .getJsonArray("edges");

        List<Issue> issues = new ArrayList<>();
        for (Object edgeJson : edgesJson) {
            JsonObject issueJson = ((JsonObject) edgeJson).getJsonObject("node");
            issues.add(extractIssue(issueJson));
        }

        return issues;
    }

    private Issue extractIssue(JsonObject issueJson) {
        Issue issue = new Issue();
        issue.id = issueJson.getString("id");
        issue.title = issueJson.getString("title");
        issue.number = issueJson.getInteger("number");
        issue.author = issueJson.getJsonObject("author").mapTo(User.class);
        issue.body = issueJson.getString("body");
        issue.closedAt = issueJson.getString("closedAt") != null
                ? LocalDateTime.parse(issueJson.getString("closedAt"), DATE_TIME_FORMATTER)
                : null;
        issue.state = issueJson.getString("state");
        issue.url = issueJson.getString("url");

        JsonArray commentsJson = issueJson.getJsonObject("comments").getJsonArray("nodes");
        List<Comment> comments = new ArrayList<>();
        for (int j = 0; j < commentsJson.size(); j++) {
            comments.add(commentsJson.getJsonObject(j).mapTo(Comment.class));
        }
        Collections.reverse(comments);
        issue.lastComments = comments;
        return issue;
    }

    private void handleErrors(JsonObject response) throws IOException {
        JsonArray errors = response.getJsonArray("errors");
        if (errors != null) {
            // Checking if there are any errors different from NOT_FOUND
            for (int k = 0; k < errors.size(); k++) {
                JsonObject error = errors.getJsonObject(k);
                if (!"NOT_FOUND".equals(error.getString("type"))) {
                    throw new IOException(error.toString());
                }
            }
        }
    }

    @CheckedTemplate
    private static class Templates {

        /**
         * Returns the issues given their respective numbers
         */
        public static native TemplateInstance findIssuesByIds(String owner, String repo, Collection<Integer> issues);

        /**
         * Returns the issues given a label
         */
        public static native TemplateInstance findIssuesByLabel(String owner, String repo, String label);


        /**
         * Returns the issue stats for given repository, label and time window
         */
        public static native TemplateInstance issuesStats(String repository, String label, String timeWindow);

        /**
         * Returns the labels stats for given repository and main label
         */
        public static native TemplateInstance labelsStats(String owner, String repo, String label, int count, String cursor);
    }

}