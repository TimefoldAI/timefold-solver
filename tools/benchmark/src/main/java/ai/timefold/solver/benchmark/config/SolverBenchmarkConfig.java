package ai.timefold.solver.benchmark.config;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "name",
        "solverConfig",
        "problemBenchmarksConfig",
        "subSingleCount"
})
public class SolverBenchmarkConfig extends AbstractConfig<SolverBenchmarkConfig> {

    private String name = null;

    @XmlElement(name = SolverConfig.XML_ELEMENT_NAME, namespace = SolverConfig.XML_NAMESPACE)
    private SolverConfig solverConfig = null;

    @XmlElement(name = "problemBenchmarks")
    private ProblemBenchmarksConfig problemBenchmarksConfig = null;

    private Integer subSingleCount = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable SolverConfig getSolverConfig() {
        return solverConfig;
    }

    public void setSolverConfig(@Nullable SolverConfig solverConfig) {
        this.solverConfig = solverConfig;
    }

    public @Nullable ProblemBenchmarksConfig getProblemBenchmarksConfig() {
        return problemBenchmarksConfig;
    }

    public void setProblemBenchmarksConfig(@Nullable ProblemBenchmarksConfig problemBenchmarksConfig) {
        this.problemBenchmarksConfig = problemBenchmarksConfig;
    }

    public @Nullable Integer getSubSingleCount() {
        return subSingleCount;
    }

    public void setSubSingleCount(@Nullable Integer subSingleCount) {
        this.subSingleCount = subSingleCount;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SolverBenchmarkConfig withName(@NonNull String name) {
        this.setName(name);
        return this;
    }

    public @NonNull SolverBenchmarkConfig withSolverConfig(@NonNull SolverConfig solverConfig) {
        this.setSolverConfig(solverConfig);
        return this;
    }

    public @NonNull SolverBenchmarkConfig
            withProblemBenchmarksConfig(@NonNull ProblemBenchmarksConfig problemBenchmarksConfig) {
        this.setProblemBenchmarksConfig(problemBenchmarksConfig);
        return this;
    }

    public @NonNull SolverBenchmarkConfig withSubSingleCount(@NonNull Integer subSingleCount) {
        this.setSubSingleCount(subSingleCount);
        return this;
    }

    @Override
    public @NonNull SolverBenchmarkConfig inherit(@NonNull SolverBenchmarkConfig inheritedConfig) {
        solverConfig = ConfigUtils.inheritConfig(solverConfig, inheritedConfig.getSolverConfig());
        problemBenchmarksConfig = ConfigUtils.inheritConfig(problemBenchmarksConfig,
                inheritedConfig.getProblemBenchmarksConfig());
        subSingleCount = ConfigUtils.inheritOverwritableProperty(subSingleCount, inheritedConfig.getSubSingleCount());
        return this;
    }

    @Override
    public @NonNull SolverBenchmarkConfig copyConfig() {
        return new SolverBenchmarkConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (solverConfig != null) {
            solverConfig.visitReferencedClasses(classVisitor);
        }
        if (problemBenchmarksConfig != null) {
            problemBenchmarksConfig.visitReferencedClasses(classVisitor);
        }
    }

}
