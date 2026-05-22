package ai.timefold.solver.benchmark.config.report;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.benchmark.config.ranking.SolverRankingType;
import ai.timefold.solver.benchmark.impl.ranking.SolverRankingWeightFactory;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.JaxbLocaleAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "locale",
        "solverRankingType",
        "solverRankingComparatorClass",
        "solverRankingWeightFactoryClass"
})
public class BenchmarkReportConfig extends AbstractConfig<BenchmarkReportConfig> {

    @XmlJavaTypeAdapter(JaxbLocaleAdapter.class)
    private Locale locale = null;
    private SolverRankingType solverRankingType = null;
    private String solverRankingComparatorClass = null;
    private String solverRankingWeightFactoryClass = null;

    public BenchmarkReportConfig() {
    }

    public BenchmarkReportConfig(@NonNull BenchmarkReportConfig inheritedConfig) {
        inherit(inheritedConfig);
    }

    public @Nullable Locale getLocale() {
        return locale;
    }

    public void setLocale(@Nullable Locale locale) {
        this.locale = locale;
    }

    public @Nullable SolverRankingType getSolverRankingType() {
        return solverRankingType;
    }

    public void setSolverRankingType(@Nullable SolverRankingType solverRankingType) {
        this.solverRankingType = solverRankingType;
    }

    public @Nullable Class<? extends Comparator<SolverBenchmarkResult>> getSolverRankingComparatorClass() {
        return ConfigUtils.resolveClass(solverRankingComparatorClass, "solverRankingComparatorClass", this);
    }

    public void setSolverRankingComparatorClass(
            @Nullable Class<? extends Comparator<SolverBenchmarkResult>> solverRankingComparatorClass) {
        this.solverRankingComparatorClass = solverRankingComparatorClass == null ? null
                : solverRankingComparatorClass.getName();
    }

    public @Nullable Class<? extends SolverRankingWeightFactory> getSolverRankingWeightFactoryClass() {
        return ConfigUtils.resolveClass(solverRankingWeightFactoryClass, "solverRankingWeightFactoryClass", this);
    }

    public void setSolverRankingWeightFactoryClass(
            @Nullable Class<? extends SolverRankingWeightFactory> solverRankingWeightFactoryClass) {
        this.solverRankingWeightFactoryClass = solverRankingWeightFactoryClass == null ? null
                : solverRankingWeightFactoryClass.getName();
    }

    public @Nullable Locale determineLocale() {
        return getLocale() == null ? Locale.getDefault() : getLocale();
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull BenchmarkReportConfig withLocale(@NonNull Locale locale) {
        this.setLocale(locale);
        return this;
    }

    public @NonNull BenchmarkReportConfig withSolverRankingType(@NonNull SolverRankingType solverRankingType) {
        this.setSolverRankingType(solverRankingType);
        return this;
    }

    public @NonNull BenchmarkReportConfig withSolverRankingComparatorClass(
            @NonNull Class<? extends Comparator<SolverBenchmarkResult>> solverRankingComparatorClass) {
        this.solverRankingComparatorClass = solverRankingComparatorClass.getName();
        return this;
    }

    public @NonNull BenchmarkReportConfig withSolverRankingWeightFactoryClass(
            @NonNull Class<? extends SolverRankingWeightFactory> solverRankingWeightFactoryClass) {
        this.solverRankingWeightFactoryClass = solverRankingWeightFactoryClass.getName();
        return this;
    }

    @Override
    public @NonNull BenchmarkReportConfig inherit(@NonNull BenchmarkReportConfig inheritedConfig) {
        locale = ConfigUtils.inheritOverwritableProperty(locale, inheritedConfig.getLocale());
        solverRankingType = ConfigUtils.inheritOverwritableProperty(solverRankingType,
                inheritedConfig.getSolverRankingType());
        solverRankingComparatorClass = ConfigUtils.inheritOverwritableProperty(solverRankingComparatorClass,
                inheritedConfig.solverRankingComparatorClass);
        solverRankingWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(solverRankingWeightFactoryClass,
                inheritedConfig.solverRankingWeightFactoryClass);
        return this;
    }

    @Override
    public @NonNull BenchmarkReportConfig copyConfig() {
        return new BenchmarkReportConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(getSolverRankingComparatorClass());
        classVisitor.accept(getSolverRankingWeightFactoryClass());
    }

}
