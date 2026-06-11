package ai.timefold.solver.service.worker.impl.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.micrometer.runtime.MeterFilterConstraint;

/**
 * Common tags to be applied to timefold metrics when they are executed as part of the platform
 */
@Singleton
public class PlatformMetricsMeterFilter {

    @Produces
    @Singleton
    @MeterFilterConstraint(applyTo = PrometheusMeterRegistry.class)
    public MeterFilter timefoldCommonMetrics(
            @ConfigProperty(name = "ai.timefold.platform.model") Optional<String> model,
            @ConfigProperty(name = "ai.timefold.platform.tenant-id") Optional<String> tenantId) {

        List<Tag> tags = new ArrayList<>();
        if (model.isPresent()) {
            tags.add(Tag.of("model", model.get()));
        }
        if (tenantId.isPresent()) {
            tags.add(Tag.of("tenant.id", tenantId.get()));
        }

        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().startsWith("timefold")) {
                    return id.replaceTags(Tags.concat(tags, id.getTagsAsIterable()));
                } else {
                    return id;
                }
            }
        };
    }
}
