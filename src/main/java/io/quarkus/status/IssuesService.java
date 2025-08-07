package io.quarkus.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.status.github.GitHubService;
import io.quarkus.status.model.Stats;
import io.quarkus.status.model.StatsEntry;

@ApplicationScoped
public class IssuesService {

    private static final String QUARKUS_REPOSITORY = "quarkusio/quarkus";
    private static final String BUG_NAME = "Bugs";
    public static final String BUG_LABEL = "kind/bug";
    private static final String ENHANCEMENT_NAME = "Enhancements";
    public static final String ENHANCEMENT_LABEL = "kind/enhancement";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Inject
    GitHubService gitHubService;

    @ConfigProperty(name = "status.issues.stats.start", defaultValue = "2019-01-01")
    LocalDate issuesStatsStart;

    private volatile Stats bugsStats;
    private volatile Stats enhancementsStats;

    @Scheduled(every = "6H")
    public void updateStatus() throws Exception {
        bugsStats = buildIssuesMonthlyStats(BUG_NAME, BUG_LABEL);
        enhancementsStats = buildIssuesMonthlyStats(ENHANCEMENT_NAME, ENHANCEMENT_LABEL);
    }

    public Stats getBugsMonthlyStats() throws Exception {
        Stats localStats = bugsStats;
        if (localStats == null) {
            synchronized (this) {
                localStats = bugsStats;
                if (localStats == null) {
                    bugsStats = localStats = buildIssuesMonthlyStats(BUG_NAME, BUG_LABEL);
                }
            }
        }
        return localStats;
    }

    public Stats getEnhancementsMonthlyStats() throws Exception {
        Stats localStats = enhancementsStats;
        if (localStats == null) {
            synchronized (this) {
                localStats = enhancementsStats;
                if (localStats == null) {
                    enhancementsStats = localStats = buildIssuesMonthlyStats(ENHANCEMENT_NAME, ENHANCEMENT_LABEL);
                }
            }
        }
        return localStats;
    }

    private Stats buildIssuesMonthlyStats(String name, String label) throws Exception {
        LocalDate start = issuesStatsStart;
        LocalDate stopTime = LocalDate.now().withDayOfMonth(2);

        List<StatsEntry> entries = new ArrayList<>();
        while (start.isBefore(stopTime)) {
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            String timeWindow = start + ".." + end;

            StatsEntry statsEntry = gitHubService.issuesStats(QUARKUS_REPOSITORY, label, timeWindow, FORMATTER.format(start));
            entries.add(statsEntry);

            start = start.plusMonths(1);
        }

        return new Stats(name, label, LocalDateTime.now(), QUARKUS_REPOSITORY, entries);
    }

}
