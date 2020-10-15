package io.quarkus.status;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.status.github.GitHubService;
import io.quarkus.status.model.StatsEntry;
import io.quarkus.status.model.Stats;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class IssuesService {

    private static final String QUARKUS_REPOSITORY = "quarkusio/quarkus";
    private static final String BUG_LABEL = "kind/bug";
    private static final String ENHANCEMENT_LABEL = "kind/enhancement";
    private static final LocalDate ISSUES_STATS_START = LocalDate.of(2019, 1, 1);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Inject
    GitHubService gitHubService;

    private volatile Stats bugsStats;
    private volatile Stats enhancementsStats;

    public void initialize(@Observes StartupEvent startupEvent) throws IOException {
        bugsStats = buildBugsMonthlyStats(BUG_LABEL);
        enhancementsStats = buildBugsMonthlyStats(ENHANCEMENT_LABEL);
    }

    @Scheduled(every = "30m")
    public void updateStatus() throws IOException {
        bugsStats = buildBugsMonthlyStats(BUG_LABEL);
        enhancementsStats = buildBugsMonthlyStats(ENHANCEMENT_LABEL);
    }

    public Stats getBugsMonthlyStats() {
        return bugsStats;
    }
    public Stats getEnhancementsMonthlyStats() {
        return enhancementsStats;
    }

    private Stats buildBugsMonthlyStats(String label) throws IOException {
        Stats stats = new Stats();
        stats.updated = LocalDateTime.now();
        stats.repository = QUARKUS_REPOSITORY;
        stats.label = label;

        LocalDate start = ISSUES_STATS_START;
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