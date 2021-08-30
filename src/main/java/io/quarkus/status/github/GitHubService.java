package io.quarkus.status.github;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.smallrye.graphql.client.Error;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.status.model.Label;
import io.quarkus.status.model.StatsEntry;

@ApplicationScoped
public class GitHubService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Inject
    @GraphQLClient("github")
    DynamicGraphQLClient graphQLClient;

    Jsonb jsonb;

    @Inject
    public GitHubService() {
        jsonb = JsonbBuilder.create();
    }

    public List<Issue> findIssuesById(String owner, String repository, List<Integer> issueNumbers) throws Exception {
        if (issueNumbers.isEmpty()) {
            return Collections.emptyList();
        }

        String query = Templates.findIssuesByIds(owner, repository, issueNumbers).render();

        Response response = graphQLClient.executeSync(query);

        // Any errors?
        handleErrors(response);

        JsonObject issuesJson = response
                .getData()
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

    public StatsEntry issuesStats(String repository, String label, String timeWindow, String entryName) throws Exception {
        String query = Templates.issuesStats(repository, label, timeWindow).render();
        Response response = graphQLClient.executeSync(query);
        handleErrors(response);

        JsonObject data = response.getData();


        StatsEntry statsEntry = new StatsEntry();
        statsEntry.entryName = entryName;
        statsEntry.created = data.getJsonObject("created").getInt("issueCount");
        statsEntry.createdAndClosedNow = data.getJsonObject("createdAndClosedNow").getInt("issueCount");
        statsEntry.createdAndOpenNow = data.getJsonObject("createdAndStillOpen").getInt("issueCount");
        statsEntry.closed = data.getJsonObject("closed").getInt("issueCount");
        statsEntry.createdAndClosed = data.getJsonObject("createdAndClosed").getInt("issueCount");

        return statsEntry;
    }

    public List<Label> labelsStats(String owner, String repository, String mainLabel, boolean subsetOnly) throws Exception {
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

    private String extractLabels(String owner, String repository, String mainLabel, int returnedElements, String cursor, List<Label> labels) throws Exception {
        String query = Templates.labelsStats(owner, repository, mainLabel, returnedElements, cursor).render();
        Response response = graphQLClient.executeSync(query);
        handleErrors(response);

        JsonObject labelsJson = response
                .getData()
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
                    String.valueOf(issueJson.getJsonObject("open").getInt("totalCount")),
                    String.valueOf(issueJson.getJsonObject("closed").getInt("totalCount"))
            ));
        }

        if (pageInfoJson.getBoolean("hasNextPage")) {
            return pageInfoJson.getString("endCursor");
        } else {
            return null;
        }
    }

    public List<Issue> findIssuesByLabel(String owner, String repository, String label) throws Exception {
        String query = Templates.findIssuesByLabel(owner, repository, label).render();

        Response response = graphQLClient.executeSync(query);

        // Any errors?
        handleErrors(response);

        JsonArray edgesJson = response
                .getData()
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
        issue.number = issueJson.getInt("number");
        issue.author = jsonb.fromJson(issueJson.getJsonObject("author").toString(), User.class);
        issue.body = issueJson.getString("body");
        issue.closedAt = issueJson.get("closedAt") != JsonValue.NULL
                ? LocalDateTime.parse(issueJson.getString("closedAt"), DATE_TIME_FORMATTER)
                : null;
        issue.state = issueJson.getString("state");
        issue.url = issueJson.getString("url");

        JsonArray commentsJson = issueJson.getJsonObject("comments").getJsonArray("nodes");
        List<Comment> comments = new ArrayList<>();
        for (int j = 0; j < commentsJson.size(); j++) {
            comments.add(jsonb.fromJson(commentsJson.getJsonObject(j).toString(), Comment.class));
        }
        Collections.reverse(comments);
        issue.lastComments = comments;
        return issue;
    }

    private void handleErrors(Response response) throws IOException {
        List<Error> errors = response.getErrors();
        if (errors != null) {
            // Checking if there are any errors different from NOT_FOUND
            for (int k = 0; k < errors.size(); k++) {
                Error error = errors.get(k);
                JsonValue errorType = error.getOtherFields().getOrDefault("type", null);
                if (errorType == null || !"NOT_FOUND".equals(((JsonString) errorType).getString())) {
                    throw new IOException(error.toString());
                }
            }
        }
        if(response.getData() == null || response.getData().equals(JsonValue.NULL)) {
            throw new RuntimeException("No data received in response. Is the auth token correct?");
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