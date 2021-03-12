package io.quarkus.status.flaky;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import java.util.Date;

@Entity
public class TestJob extends PanacheEntity {
    public String url;
    public String name;
    public Date completedAt;
    public String sha;
}
