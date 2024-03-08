package ai.timefold.solver.spring.boot.autoconfigure.config;

import java.util.Map;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class SolverProperties {
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
     * Note: this setting is only available
     * for <a href="https://timefold.ai/docs/timefold-solver/latest/enterprise-edition/enterprise-edition">Timefold Solver
     * Enterprise Edition</a>.
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

    /**
     * Note: this setting is only available
     * for <a href="https://timefold.ai/docs/timefold-solver/latest/enterprise-edition/enterprise-edition">Timefold Solver
     * Enterprise Edition</a>.
     * Enable rewriting the {@link ai.timefold.solver.core.api.score.stream.ConstraintProvider} class
     * so nodes share lambdas when possible, improving performance.
     * When enabled, breakpoints placed in the {@link ai.timefold.solver.core.api.score.stream.ConstraintProvider}
     * will no longer be triggered.
     * Defaults to "false".
     */
    private Boolean constraintStreamAutomaticNodeSharing;

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

    public Boolean getConstraintStreamAutomaticNodeSharing() {
        return constraintStreamAutomaticNodeSharing;
    }

    public void setConstraintStreamAutomaticNodeSharing(Boolean constraintStreamAutomaticNodeSharing) {
        this.constraintStreamAutomaticNodeSharing = constraintStreamAutomaticNodeSharing;
    }

    public TerminationProperties getTermination() {
        return termination;
    }

    public void setTermination(TerminationProperties termination) {
        this.termination = termination;
    }

    public void loadProperties(Map<String, Object> properties) {
        // Check if the keys are valid
        var invalidKeySet = new TreeSet<>(properties.keySet());
        invalidKeySet.removeAll(SolverProperty.getValidPropertyNames());

        if (!invalidKeySet.isEmpty()) {
            throw new IllegalStateException("""
                    The properties [%s] are not valid.
                    Maybe try changing the property name to kebab-case.
                    Here is the list of valid properties: %s"""
                    .formatted(invalidKeySet, String.join(", ", SolverProperty.getValidPropertyNames())));
        }
        properties.forEach(this::loadProperty);
    }

    private void loadProperty(String key, Object value) {
        if (value == null) {
            return;
        }
        SolverProperty property = SolverProperty.forPropertyName(key);
        property.update(this, value);
    }

}
