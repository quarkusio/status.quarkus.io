package io.quarkus.status;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(classNames = {
        "com.github.benmanes.caffeine.cache.SSW",
        "com.github.benmanes.caffeine.cache.PSW"
})
public interface CacheNames {

    String STATUS_CACHE_NAME = "status";
}
