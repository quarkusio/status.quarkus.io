package io.quarkus.status.flaky;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = {
        @Index(columnList = "testName"),
        @Index(columnList = "testName, id desc, successful")
})
public class TestExecution extends PanacheEntity {
    public String testName;
    public boolean successful;
    @ManyToOne
    public TestJob job;

    @Override
    public String toString() {
        return "TestExecution{" +
                "testName='" + testName + '\'' +
                ", successful=" + successful +
                ", id=" + id +
                '}';
    }
}
