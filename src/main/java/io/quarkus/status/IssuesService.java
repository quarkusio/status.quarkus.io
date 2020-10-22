package io.quarkus.status;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.status.github.GitHubService;
import io.quarkus.status.model.Stats;
import io.quarkus.status.model.StatsEntry;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
    public void updateStatus() throws IOException {
        bugsStats = buildBugsMonthlyStats(BUG_NAME, BUG_LABEL);
        enhancementsStats = buildBugsMonthlyStats(ENHANCEMENT_NAME, ENHANCEMENT_LABEL);
    }

    public Stats getBugsMonthlyStats() throws IOException {
        Stats localStats = bugsStats;
        if (localStats == null) {
            synchronized (this) {
                localStats = bugsStats;
                if (localStats == null) {
                    bugsStats = localStats = buildBugsMonthlyStats(BUG_NAME, BUG_LABEL);
                }
            }
        }
        return localStats;
    }

    public Stats getEnhancementsMonthlyStats() throws IOException {
        Stats localStats = enhancementsStats;
        if (localStats == null) {
            synchronized (this) {
                localStats = enhancementsStats;
                if (localStats == null) {
                    enhancementsStats = localStats = buildBugsMonthlyStats(ENHANCEMENT_NAME, ENHANCEMENT_LABEL);
                }
            }
        }
        return localStats;
    }

    private Stats buildBugsMonthlyStats(String name, String label) throws IOException {
        Stats stats = new Stats();
        stats.name = name;
        stats.label = label;
        stats.updated = LocalDateTime.now();
        stats.repository = QUARKUS_REPOSITORY;

        LocalDate start = issuesStatsStart;
        LocalDate stopTime = LocalDate.now().withDayOfMonth(2);

        while (start.isBefore(stopTime)) {
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            String timeWindow = start + ".." + end;

            StatsEntry statsEntry = gitHubService.issuesStats(QUARKUS_REPOSITORY, label, timeWindow, FORMATTER.format(start));
            stats.add(statsEntry);

            start = start.plusMonths(1);
        }

        return stats;
    }

}