package io.quarkus.status.github;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.status.model.StatusLine.BuildStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.status.model.Label;
import io.quarkus.status.model.StatsEntry;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.GraphQLError;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GitHubService {

    private static final Logger LOG = Logger.getLogger(GitHubService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String STATUS_MARKER = "<!-- status.quarkus.io/status:";
    private static final String END_OF_MARKER = "-->";
    private static final Pattern STATUS_PATTERN = Pattern.compile(STATUS_MARKER + "\r?\n(.*?)\r?\n" + END_OF_MARKER,
            Pattern.DOTALL);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

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

        return new StatsEntry(
                entryName,
                timeWindow,
                data.getJsonObject("created").getInt("issueCount"),
                data.getJsonObject("createdAndClosedNow").getInt("issueCount"),
                data.getJsonObject("createdAndStillOpen").getInt("issueCount"),
                data.getJsonObject("closed").getInt("issueCount"),
                data.getJsonObject("createdAndClosed").getInt("issueCount")
        );
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

        Collections.sort(labels, Comparator.comparing(Label::name));
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
        String body = issueJson.getString("body");
        BuildStatus buildStatus = extractBuildStatus(body);

        JsonArray commentsJson = issueJson.getJsonObject("comments").getJsonArray("nodes");
        List<Comment> comments = new ArrayList<>();
        for (int j = 0; j < commentsJson.size(); j++) {
            comments.add(jsonb.fromJson(commentsJson.getJsonObject(j).toString(), Comment.class));
        }
        Collections.reverse(comments);

        Issue issue = new Issue(
                issueJson.getString("id"),
                issueJson.getInt("number"),
                issueJson.getString("title"),
                issueJson.getString("body"),
                issueJson.getString("url"),
                issueJson.getString("state"),
                issueJson.get("closedAt") != JsonValue.NULL
                ? LocalDateTime.parse(issueJson.getString("closedAt"), DATE_TIME_FORMATTER)
                        : null,
                buildStatus != null ? LocalDateTime.ofInstant(buildStatus.updatedAt(), ZoneId.systemDefault()) : null,
                buildStatus,
                comments
        );

        return issue;
    }

    private void handleErrors(Response response) throws IOException {
        if(response == null) {
            throw new RuntimeException("No response received. This is very odd...");
        }

        List<GraphQLError> errors = response.getErrors();
        if (errors != null) {
            // Checking if there are any errors different from NOT_FOUND
            for (int k = 0; k < errors.size(); k++) {
                GraphQLError error = errors.get(k);
                Map<String, Object> otherFields = error.getOtherFields();
                if(otherFields == null || otherFields.isEmpty()) {
                    throw new IOException(error.toString());
                }
                Object errorType = otherFields.getOrDefault("type", null);
                if (errorType == null || !"NOT_FOUND".equals(errorType)) {
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

    private static BuildStatus extractBuildStatus(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        Matcher matcher = STATUS_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(matcher.group(1), BuildStatus.class);
        } catch (Exception e) {
            LOG.warn("Unable to extract Status from issue body", e);
            return null;
        }
    }
}