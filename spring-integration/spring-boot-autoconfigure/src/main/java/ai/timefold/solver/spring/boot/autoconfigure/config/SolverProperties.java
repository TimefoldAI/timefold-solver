package ai.timefold.solver.spring.boot.autoconfigure.config;

import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class SolverProperties {
    public static final Set<String> VALID_FIELD_NAMES_SET =
            Set.of("solver-config-xml", "environment-mode", "daemon", "move-thread-count", "domain-access-type",
                    "constraint-stream-impl-type", "termination");

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
        properties.forEach(this::loadProperty);
    }

    private void loadProperty(String key, Object value) {
        if (value == null) {
            return;
        }
        switch (key) {
            case "solver-config-xml":
                setSolverConfigXml((String) value);
                break;
            case "environment-mode":
                setEnvironmentMode(EnvironmentMode.valueOf((String) value));
                break;
            case "daemon":
                setDaemon(Boolean.parseBoolean((String) value));
                break;
            case "move-thread-count":
                setMoveThreadCount((String) value);
                break;
            case "domain-access-type":
                setDomainAccessType(DomainAccessType.valueOf((String) value));
                break;
            case "constraint-stream-impl-type":
                setConstraintStreamImplType(ConstraintStreamImplType.valueOf((String) value));
                break;
            case "termination": {
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
