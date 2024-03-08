package ai.timefold.solver.spring.boot.autoconfigure.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

public enum SolverProperty {
    SOLVER_CONFIG_XML("solver-config-xml", SolverProperties::setSolverConfigXml, Object::toString),
    ENVIRONMENT_MODE("environment-mode", SolverProperties::setEnvironmentMode,
            value -> EnvironmentMode.valueOf(value.toString())),
    DAEMON("daemon", SolverProperties::setDaemon, value -> Boolean.valueOf(value.toString())),
    MOVE_THREAD_COUNT("move-thread-count", SolverProperties::setMoveThreadCount, Object::toString),
    DOMAIN_ACCESS_TYPE("domain-access-type", SolverProperties::setDomainAccessType,
            value -> DomainAccessType.valueOf(value.toString())),
    NEARBY_DISTANCE_METER_CLASS("nearby-distance-meter-class", SolverProperties::setNearbyDistanceMeterClass,
            value -> {
                try {
                    @SuppressWarnings("rawtypes")
                    Class nearbyClass = Class.forName(value.toString(), false,
                            Thread.currentThread().getContextClassLoader());

                    if (!NearbyDistanceMeter.class.isAssignableFrom(nearbyClass)) {
                        throw new IllegalStateException(
                                "The Nearby Selection Meter class (%s) does not implement NearbyDistanceMeter."
                                        .formatted(value.toString()));
                    }
                    return nearbyClass;
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                            "Cannot find the Nearby Selection Meter class (%s).".formatted(value.toString()));
                }
            }),
    /**
     * @deprecated No longer used.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    CONSTRAINT_STREAM_IMPL_TYPE("constraint-stream-impl-type", SolverProperties::setConstraintStreamImplType,
            value -> ConstraintStreamImplType.valueOf(value.toString())),
    CONSTRAINT_STREAM_AUTOMATIC_NODE_SHARING("constraint-stream-automatic-node-sharing",
            SolverProperties::setConstraintStreamAutomaticNodeSharing, value -> Boolean.valueOf(value.toString())),
    TERMINATION("termination", SolverProperties::setTermination, value -> {
        if (value instanceof TerminationProperties terminationProperties) {
            return terminationProperties;
        } else if (value instanceof Map<?, ?> map) {
            TerminationProperties terminationProperties = new TerminationProperties();
            terminationProperties.loadProperties((Map<String, Object>) map);
            return terminationProperties;
        } else {
            throw new IllegalStateException(
                    "The termination value (%s) is not valid. Expected an instance of %s or %s, but got an instance of %s."
                            .formatted(value, Map.class.getSimpleName(), TerminationProperties.class.getSimpleName(),
                                    value.getClass().getName()));
        }
    });

    private final String propertyName;
    private final BiConsumer<SolverProperties, Object> propertyUpdater;
    private static final Set<String> PROPERTY_NAMES = Stream.of(SolverProperty.values())
            .map(SolverProperty::getPropertyName)
            .collect(Collectors.toCollection(TreeSet::new));

    <T> SolverProperty(String propertyName, BiConsumer<SolverProperties, T> propertySetter,
            Function<Object, T> propertyConvertor) {
        this.propertyName = propertyName;
        this.propertyUpdater = (properties, object) -> propertySetter.accept(properties, propertyConvertor.apply(object));
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void update(SolverProperties properties, Object value) {
        propertyUpdater.accept(properties, value);
    }

    public static Set<String> getValidPropertyNames() {
        return Collections.unmodifiableSet(PROPERTY_NAMES);
    }

    public static SolverProperty forPropertyName(String propertyName) {
        for (var property : values()) {
            if (property.getPropertyName().equals(propertyName)) {
                return property;
            }
        }
        throw new IllegalArgumentException("No property with the name (%s). Valid properties are %s."
                .formatted(propertyName, PROPERTY_NAMES));
    }
}
