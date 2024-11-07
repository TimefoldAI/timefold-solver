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
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbLocaleAdapter;

import org.jspecify.annotations.NonNull;

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
    private Class<? extends Comparator<SolverBenchmarkResult>> solverRankingComparatorClass = null;
    private Class<? extends SolverRankingWeightFactory> solverRankingWeightFactoryClass = null;

    public BenchmarkReportConfig() {
    }

    public BenchmarkReportConfig(BenchmarkReportConfig inheritedConfig) {
        inherit(inheritedConfig);
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public SolverRankingType getSolverRankingType() {
        return solverRankingType;
    }

    public void setSolverRankingType(SolverRankingType solverRankingType) {
        this.solverRankingType = solverRankingType;
    }

    public Class<? extends Comparator<SolverBenchmarkResult>> getSolverRankingComparatorClass() {
        return solverRankingComparatorClass;
    }

    public void setSolverRankingComparatorClass(
            Class<? extends Comparator<SolverBenchmarkResult>> solverRankingComparatorClass) {
        this.solverRankingComparatorClass = solverRankingComparatorClass;
    }

    public Class<? extends SolverRankingWeightFactory> getSolverRankingWeightFactoryClass() {
        return solverRankingWeightFactoryClass;
    }

    public void setSolverRankingWeightFactoryClass(
            Class<? extends SolverRankingWeightFactory> solverRankingWeightFactoryClass) {
        this.solverRankingWeightFactoryClass = solverRankingWeightFactoryClass;
    }

    public Locale determineLocale() {
        return getLocale() == null ? Locale.getDefault() : getLocale();
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public BenchmarkReportConfig withLocale(Locale locale) {
        this.setLocale(locale);
        return this;
    }

    public BenchmarkReportConfig withSolverRankingType(SolverRankingType solverRankingType) {
        this.setSolverRankingType(solverRankingType);
        return this;
    }

    public BenchmarkReportConfig withSolverRankingComparatorClass(
            Class<? extends Comparator<SolverBenchmarkResult>> solverRankingComparatorClass) {
        this.setSolverRankingComparatorClass(solverRankingComparatorClass);
        return this;
    }

    public BenchmarkReportConfig withSolverRankingWeightFactoryClass(
            Class<? extends SolverRankingWeightFactory> solverRankingWeightFactoryClass) {
        this.setSolverRankingWeightFactoryClass(solverRankingWeightFactoryClass);
        return this;
    }

    @Override
    public @NonNull BenchmarkReportConfig inherit(@NonNull BenchmarkReportConfig inheritedConfig) {
        locale = ConfigUtils.inheritOverwritableProperty(locale, inheritedConfig.getLocale());
        solverRankingType = ConfigUtils.inheritOverwritableProperty(solverRankingType,
                inheritedConfig.getSolverRankingType());
        solverRankingComparatorClass = ConfigUtils.inheritOverwritableProperty(solverRankingComparatorClass,
                inheritedConfig.getSolverRankingComparatorClass());
        solverRankingWeightFactoryClass = ConfigUtils.inheritOverwritableProperty(solverRankingWeightFactoryClass,
                inheritedConfig.getSolverRankingWeightFactoryClass());
        return this;
    }

    @Override
    public @NonNull BenchmarkReportConfig copyConfig() {
        return new BenchmarkReportConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(solverRankingComparatorClass);
        classVisitor.accept(solverRankingWeightFactoryClass);
    }

}
