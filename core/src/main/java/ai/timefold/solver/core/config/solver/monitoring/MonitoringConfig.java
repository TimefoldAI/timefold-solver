package ai.timefold.solver.core.config.solver.monitoring;

import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;

@XmlType(propOrder = {
        "solverMetricList",
})
public class MonitoringConfig extends AbstractConfig<MonitoringConfig> {
    @XmlElement(name = "metric")
    protected List<SolverMetric> solverMetricList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************
    public List<SolverMetric> getSolverMetricList() {
        return solverMetricList;
    }

    public void setSolverMetricList(List<SolverMetric> solverMetricList) {
        this.solverMetricList = solverMetricList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public MonitoringConfig withSolverMetricList(List<SolverMetric> solverMetricList) {
        this.solverMetricList = solverMetricList;
        return this;
    }

    @Override
    public @NonNull MonitoringConfig inherit(@NonNull MonitoringConfig inheritedConfig) {
        solverMetricList = ConfigUtils.inheritMergeableListProperty(solverMetricList, inheritedConfig.solverMetricList);
        return this;
    }

    @Override
    public @NonNull MonitoringConfig copyConfig() {
        return new MonitoringConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        // No referenced classes currently
        // If we add custom metrics here, then this should
        // register the custom metrics
    }
}
