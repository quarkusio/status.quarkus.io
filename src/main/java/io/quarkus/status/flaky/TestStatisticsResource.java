package io.quarkus.status.flaky;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Path("/test-statistics")
@ApplicationScoped
public class TestStatisticsResource {
    private static final int TESTS_TABLE_SIZE = 20;
    static final int RESULT_GROUP_SIZE = 40;

    @ConfigProperty(name = "status.flaky.max-cache-size", defaultValue = "10000")
    Integer maxCacheSize;

    @PersistenceContext
    EntityManager entityManager;

    private Cache<String, List<TestStatistics>> queryCache; // effectively final

    @PostConstruct
    void init() {
        queryCache = Caffeine.newBuilder().maximumSize(maxCacheSize).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TestStatistics> getStats(@QueryParam("testQuery") String testQueryParam) {
        String testQuery = testQueryParam == null || testQueryParam.isBlank()
                ? "%"
                : '%' + testQueryParam + '%';
        List<TestStatistics> cachedResult = queryCache.getIfPresent(testQuery);
        if (cachedResult != null) {
            return cachedResult;
        }

        if (!testQuery.equals("%")) {
            Query countQuery = entityManager.createNativeQuery(
                    "select count(t.*) from (select distinct testname from testexecution where testname like :test) t");
            countQuery.setParameter("test", testQuery);
            BigInteger count = (BigInteger) countQuery.getSingleResult();

            if (count.intValue() > 100) {
                throw new BadRequestException(Response.status(400)
                        .entity("Too many tests matching the query found, please use a more specific query").build());
            }
        }

        Query query = entityManager.createNativeQuery("with partitioned_results as " +
                "(select id, testname, successful\\:\\:int as success, row_number() over (partition by testname order by id desc) from testexecution)" +
                "select testname, avg(success) as success_rate, sum(success), count(id) from partitioned_results " +
                "where testname like :test and row_number <= " + RESULT_GROUP_SIZE + " group by testname order by success_rate asc limit " + TESTS_TABLE_SIZE);

        query.setParameter("test", testQuery);
        List<Object[]> stats = query.getResultList();
        List<TestStatistics> result = stats.stream()
                .map(TestStatistics::new)
                .collect(Collectors.toList());

        queryCache.put(testQuery, result);
        return result;
    }

    void clearCache() {
        queryCache.invalidateAll();
    }

    public static class TestStatistics {
        public String name;
        public double failureRatio;
        public int successCount;
        public int executionCount;

        public TestStatistics(Object[] result) {
            this.name = (String) result[0];
            this.failureRatio = 1 - ((BigDecimal) result[1]).doubleValue();
            this.successCount = ((BigInteger) result[2]).intValue();
            this.executionCount = ((BigInteger) result[3]).intValue();
        }
    }
}
