package ai.timefold.solver.core.impl.testutil;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public abstract class AbstractMeterTest {

    @BeforeEach // To guard against nasty tests which do not do this.
    @AfterEach // To clean up after ourselves.
    void resetGlobalRegistry() {
        List.copyOf(Metrics.globalRegistry.getRegistries())
                .forEach(registry -> {
                    Metrics.globalRegistry.remove(registry);
                    registry.clear();
                });
        Metrics.globalRegistry.clear(); // Make absolutely sure that the global registry is empty.
    }

    protected static final class TestMeterRegistry extends SimpleMeterRegistry {

        private final Map<String, Map<String, BigDecimal>> measurementMap;

        public TestMeterRegistry() {
            super(SimpleConfig.DEFAULT, new MockClock());
            measurementMap = new LinkedHashMap<>();
        }

        public MockClock getClock() {
            return MockClock.clock(this);
        }

        public BigDecimal getMeasurement(String key, String statistic) {
            if (measurementMap.containsKey(key)) {
                Map<String, BigDecimal> meterMeasurementMap = measurementMap.get(key);
                if (meterMeasurementMap.containsKey(statistic)) {
                    return meterMeasurementMap.get(statistic);
                } else {
                    throw new IllegalArgumentException(
                            "Meter (" + key + ") does not have statistic (" + statistic + "). Available statistics are: "
                                    + meterMeasurementMap.keySet().stream().collect(Collectors.joining(", ", "[", "]")));
                }
            } else {
                throw new IllegalArgumentException("Meter (" + key + ") does not exist. Available statistics are: "
                        + measurementMap.keySet().stream().collect(Collectors.joining(", ", "[", "]")));
            }
        }

        public void publish() {
            this.getMeters().forEach(meter -> {
                final Map<String, BigDecimal> meterMeasurementMap = new LinkedHashMap<>();
                String meterTags = "";
                if (meter.getId().getTags().size() > 1) {
                    meterTags = meter.getId().getConventionTags(NamingConvention.dot).stream()
                            .filter(tag -> !tag.getKey().equals("solver.id"))
                            .map(tag -> tag.getKey() + "=" + tag.getValue())
                            .sorted()
                            .collect(Collectors.joining(",", ":", ""));
                }
                measurementMap.put(meter.getId().getConventionName(NamingConvention.dot) + meterTags,
                        meterMeasurementMap);
                meter.measure().forEach(measurement -> {
                    if (Double.isFinite(measurement.getValue())) {
                        meterMeasurementMap.put(measurement.getStatistic().name(), BigDecimal.valueOf(measurement.getValue()));
                    }
                });
            });
        }

        @Override
        protected TimeUnit getBaseTimeUnit() {
            return TimeUnit.SECONDS;
        }
    }

}
