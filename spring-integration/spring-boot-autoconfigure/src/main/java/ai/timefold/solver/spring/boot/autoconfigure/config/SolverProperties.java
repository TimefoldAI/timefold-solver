package ai.timefold.solver.spring.boot.autoconfigure.config;

import static java.util.stream.Collectors.joining;

import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class SolverProperties {

    public static final String SOLVER_CONFIG_XML_PROPERTY_NAME = "solver-config-xml";
    public static final String ENVIRONMENT_MODE_PROPERTY_NAME = "environment-mode";
    public static final String DAEMON_PROPERTY_NAME = "daemon";
    public static final String MOVE_THREAD_COUNT_PROPERTY_NAME = "move-thread-count";
    public static final String DOMAIN_ACCESS_TYPE_PROPERTY_NAME = "domain-access-type";
    public static final String NEARBY_DISTANCE_METER_CLASS_PROPERTY_NAME = "nearby-distance-meter-class";
    public static final String CONSTRAINT_STREAM_IMPL_TYPE_PROPERTY_NAME = "constraint-stream-impl-type";
    public static final String TERMINATION_PROPERTY_NAME = "termination";
    public static final Set<String> VALID_FIELD_NAMES_SET =
            Set.of(SOLVER_CONFIG_XML_PROPERTY_NAME, ENVIRONMENT_MODE_PROPERTY_NAME, DAEMON_PROPERTY_NAME,
                    MOVE_THREAD_COUNT_PROPERTY_NAME, DOMAIN_ACCESS_TYPE_PROPERTY_NAME,
                    NEARBY_DISTANCE_METER_CLASS_PROPERTY_NAME, CONSTRAINT_STREAM_IMPL_TYPE_PROPERTY_NAME,
                    TERMINATION_PROPERTY_NAME);

    /**
     * A classpath resource to read the specific solver configuration XML.
     * If this property isn't specified, that solverConfig.xml is optional.
     */
    private String solverConfigXml;

    /**
     * Enable runtime assertions to detect common bugs in your implementation during development.
     * Defaults to "REPRODUCIBLE".
     */
    private EnvironmentMode environmentMode;

    /**
     * Enable daemon mode. In daemon mode, non-early termination pauses the solver instead of stopping it,
     * until the next problem fact change arrives. This is often useful for real-time planning.
     * Defaults to "false".
     */
    private Boolean daemon;
    /**
     * Enable multithreaded solving for a single problem, which increases CPU consumption.
     * Defaults to "NONE".
     * Other options include "AUTO", a number or formula based on the available processor count.
     */
    private String moveThreadCount;

    /**
     * Determines how to access the fields and methods of domain classes.
     * Defaults to REFLECTION.
     * <p>
     * To use GIZMO, io.quarkus.gizmo:gizmo must be in your classpath,
     * and all planning annotations must be on public members.
     */
    private DomainAccessType domainAccessType;

    /**
     * Enable the Nearby Selection quick configuration.
     */
    private Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass;

    /**
     * What constraint stream implementation to use. Defaults to BAVET.
     *
     * @deprecated No longer used.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    private ConstraintStreamImplType constraintStreamImplType;

    @NestedConfigurationProperty
    private TerminationProperties termination;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public String getSolverConfigXml() {
        return solverConfigXml;
    }

    public void setSolverConfigXml(String solverConfigXml) {
        this.solverConfigXml = solverConfigXml;
    }

    public EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    public void setEnvironmentMode(EnvironmentMode environmentMode) {
        this.environmentMode = environmentMode;
    }

    public Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(Boolean daemon) {
        this.daemon = daemon;
    }

    public String getMoveThreadCount() {
        return moveThreadCount;
    }

    public void setMoveThreadCount(String moveThreadCount) {
        this.moveThreadCount = moveThreadCount;
    }

    public DomainAccessType getDomainAccessType() {
        return domainAccessType;
    }

    public void setDomainAccessType(DomainAccessType domainAccessType) {
        this.domainAccessType = domainAccessType;
    }

    public Class<? extends NearbyDistanceMeter<?, ?>> getNearbyDistanceMeterClass() {
        return nearbyDistanceMeterClass;
    }

    public void setNearbyDistanceMeterClass(Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass) {
        this.nearbyDistanceMeterClass = nearbyDistanceMeterClass;
    }

    /**
     * @deprecated No longer used.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public ConstraintStreamImplType getConstraintStreamImplType() {
        return constraintStreamImplType;
    }

    /**
     * @deprecated No longer used.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public void setConstraintStreamImplType(ConstraintStreamImplType constraintStreamImplType) {
        this.constraintStreamImplType = constraintStreamImplType;
    }

    public TerminationProperties getTermination() {
        return termination;
    }

    public void setTermination(TerminationProperties termination) {
        this.termination = termination;
    }

    public void loadProperties(Map<String, Object> properties) {
        // Check if the keys are valid
        String invalidKeys = properties.entrySet().stream()
                .filter(e -> !VALID_FIELD_NAMES_SET.contains(e.getKey()))
                .map(Map.Entry::getKey)
                .collect(joining(", "));

        if (!invalidKeys.isBlank()) {
            throw new IllegalStateException("""
                    The properties [%s] are not valid.
                    Maybe try changing the property name to kebab-case.
                    Here is the list of valid properties: %s"""
                    .formatted(invalidKeys, String.join(", ", VALID_FIELD_NAMES_SET)));
        }
        properties.forEach(this::loadProperty);
    }

    private void loadProperty(String key, Object value) {
        if (value == null) {
            return;
        }
        switch (key) {
            case SOLVER_CONFIG_XML_PROPERTY_NAME:
                setSolverConfigXml(value.toString());
                break;
            case ENVIRONMENT_MODE_PROPERTY_NAME:
                setEnvironmentMode(EnvironmentMode.valueOf((String) value));
                break;
            case DAEMON_PROPERTY_NAME:
                setDaemon(Boolean.parseBoolean(value.toString()));
                break;
            case MOVE_THREAD_COUNT_PROPERTY_NAME:
                setMoveThreadCount(value.toString());
                break;
            case DOMAIN_ACCESS_TYPE_PROPERTY_NAME:
                setDomainAccessType(DomainAccessType.valueOf((String) value));
                break;
            case NEARBY_DISTANCE_METER_CLASS_PROPERTY_NAME:
                try {
                    Class<?> nearbyClass = Class.forName(value.toString(), false,
                            Thread.currentThread().getContextClassLoader());

                    if (!NearbyDistanceMeter.class.isAssignableFrom(nearbyClass)) {
                        throw new IllegalStateException(
                                "The Nearby Selection Meter class (%s) does not implement NearbyDistanceMeter."
                                        .formatted(value.toString()));
                    }
                    setNearbyDistanceMeterClass((Class<? extends NearbyDistanceMeter<?, ?>>) nearbyClass);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                            "Cannot find the Nearby Selection Meter class (%s).".formatted(value.toString()));
                }
                break;
            case CONSTRAINT_STREAM_IMPL_TYPE_PROPERTY_NAME:
                setConstraintStreamImplType(ConstraintStreamImplType.valueOf((String) value));
                break;
            case TERMINATION_PROPERTY_NAME: {
                if (value instanceof TerminationProperties terminationProperties) {
                    setTermination(terminationProperties);
                } else if (value instanceof Map<?, ?>) {
                    TerminationProperties terminationProperties = new TerminationProperties();
                    terminationProperties.loadProperties((Map<String, Object>) value);
                    setTermination(terminationProperties);
                } else {
                    throw new IllegalStateException("The termination value is not valid.");
                }
                break;
            }
            default:
                throw new IllegalStateException("The property %s is not valid.".formatted(key));
        }
    }

}
