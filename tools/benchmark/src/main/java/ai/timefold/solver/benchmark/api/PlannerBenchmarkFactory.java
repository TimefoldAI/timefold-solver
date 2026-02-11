package ai.timefold.solver.benchmark.api;

import java.io.File;
import java.util.List;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.impl.DefaultPlannerBenchmarkFactory;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.SolverConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Builds {@link PlannerBenchmark} instances.
 * <p>
 * Supports tweaking the configuration programmatically before a {@link PlannerBenchmark} instance is build.
 */
public abstract class PlannerBenchmarkFactory {

    // ************************************************************************
    // Static creation methods: SolverConfig XML
    // ************************************************************************

    /**
     * Reads an XML solver configuration from the classpath
     * and uses that {@link SolverConfig} to build a {@link PlannerBenchmarkConfig}
     * that in turn is used to build a {@link PlannerBenchmarkFactory}.
     * The XML root element must be {@code <solver>}.
     * <p>
     * To read an XML benchmark configuration instead, use {@link #createFromXmlResource(String)}.
     *
     * @param solverConfigResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull PlannerBenchmarkFactory createFromSolverConfigXmlResource(@NonNull String solverConfigResource) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource);
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromSolverConfigXmlResource(String)}.
     *
     * @param solverConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}.
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromSolverConfigXmlResource(@NonNull String solverConfigResource,
            @Nullable ClassLoader classLoader) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource, classLoader);
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromSolverConfigXmlResource(String)}.
     *
     * @param solverConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull PlannerBenchmarkFactory createFromSolverConfigXmlResource(@NonNull String solverConfigResource,
            @NonNull File benchmarkDirectory) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource);
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig,
                benchmarkDirectory);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromSolverConfigXmlResource(String)}.
     *
     * @param solverConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromSolverConfigXmlResource(@NonNull String solverConfigResource,
            @NonNull File benchmarkDirectory, @Nullable ClassLoader classLoader) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource, classLoader);
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig,
                benchmarkDirectory);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    // ************************************************************************
    // Static creation methods: XML
    // ************************************************************************

    /**
     * Reads an XML benchmark configuration from the classpath
     * and uses that {@link PlannerBenchmarkConfig} to build a {@link PlannerBenchmarkFactory}.
     * The XML root element must be {@code <plannerBenchmark>}.
     * <p>
     * To read an XML solver configuration instead, use {@link #createFromSolverConfigXmlResource(String)}.
     *
     * @param benchmarkConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull PlannerBenchmarkFactory createFromXmlResource(@NonNull String benchmarkConfigResource) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(benchmarkConfigResource);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromXmlResource(String)}.
     *
     * @param benchmarkConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromXmlResource(@NonNull String benchmarkConfigResource,
            @Nullable ClassLoader classLoader) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(benchmarkConfigResource,
                classLoader);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * Reads an XML benchmark configuration from the file system
     * and uses that {@link PlannerBenchmarkConfig} to build a {@link PlannerBenchmarkFactory}.
     * <p>
     * Warning: this leads to platform dependent code,
     * it's recommend to use {@link #createFromXmlResource(String)} instead.
     *
     */
    public static @NonNull PlannerBenchmarkFactory createFromXmlFile(@NonNull File benchmarkConfigFile) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromXmlFile(benchmarkConfigFile);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromXmlFile(@NonNull File benchmarkConfigFile,
            @Nullable ClassLoader classLoader) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromXmlFile(benchmarkConfigFile, classLoader);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    // ************************************************************************
    // Static creation methods: Freemarker XML
    // ************************************************************************

    /**
     * Reads a Freemarker template from the classpath that generates an XML benchmark configuration
     * and uses that {@link PlannerBenchmarkConfig} to build a {@link PlannerBenchmarkFactory}.
     * The generated XML root element must be {@code <plannerBenchmark>}.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     * @see #createFromFreemarkerXmlResource(String)
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlResource(@NonNull String templateResource) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlResource(templateResource);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlResource(String)}.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlResource(@NonNull String templateResource,
            @Nullable ClassLoader classLoader) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlResource(templateResource,
                classLoader);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlResource(String)}.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlResource(@NonNull String templateResource,
            @Nullable Object model) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlResource(templateResource,
                model);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlResource(String)}.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlResource(@NonNull String templateResource,
            @Nullable Object model, @Nullable ClassLoader classLoader) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlResource(templateResource, model,
                classLoader);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * Reads a Freemarker template rom the file system that generates an XML benchmark configuration
     * and uses that {@link PlannerBenchmarkConfig} to build a {@link PlannerBenchmarkFactory}.
     * The generated XML root element must be {@code <plannerBenchmark>}.
     * <p>
     * Warning: this leads to platform dependent code,
     * it's recommend to use {@link #createFromFreemarkerXmlResource(String)} instead.
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlFile(@NonNull File templateFile) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlFile(templateFile);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlFile(@NonNull File templateFile,
            @Nullable ClassLoader classLoader) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlFile(templateFile, classLoader);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlFile(File)}.
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlFile(@NonNull File templateFile,
            @Nullable Object model) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlFile(templateFile, model);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkFactory createFromFreemarkerXmlFile(@NonNull File templateFile,
            @Nullable Object model, @Nullable ClassLoader classLoader) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlFile(templateFile, model,
                classLoader);
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    // ************************************************************************
    // Static creation methods: PlannerBenchmarkConfig and SolverConfig
    // ************************************************************************

    /**
     * Uses a {@link PlannerBenchmarkConfig} to build a {@link PlannerBenchmarkFactory}.
     * If you don't need to manipulate the {@link PlannerBenchmarkConfig} programmatically,
     * use {@link #createFromXmlResource(String)} instead.
     */
    public static @NonNull PlannerBenchmarkFactory create(@NonNull PlannerBenchmarkConfig benchmarkConfig) {
        return new DefaultPlannerBenchmarkFactory(benchmarkConfig);
    }

    public static @NonNull PlannerBenchmarkFactory createFromSolverConfig(@NonNull SolverConfig solverConfig) {
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig);
        return create(benchmarkConfig);
    }

    public static @NonNull PlannerBenchmarkFactory createFromSolverConfig(@NonNull SolverConfig solverConfig,
            @NonNull File benchmarkDirectory) {
        PlannerBenchmarkConfig benchmarkConfig =
                PlannerBenchmarkConfig.createFromSolverConfig(solverConfig, benchmarkDirectory);
        return create(benchmarkConfig);
    }

    // ************************************************************************
    // Interface methods
    // ************************************************************************

    /**
     * Creates a new {@link PlannerBenchmark} instance.
     */
    public abstract @NonNull PlannerBenchmark buildPlannerBenchmark();

    /**
     * Creates a new {@link PlannerBenchmark} instance for datasets that are already in memory.
     *
     * @param problemList can be empty
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    public <Solution_> @NonNull PlannerBenchmark buildPlannerBenchmark(@NonNull List<@NonNull Solution_> problemList) {
        return buildPlannerBenchmark(problemList.toArray());
    }

    /**
     * Creates a new {@link PlannerBenchmark} instance for datasets that are already in memory.
     *
     * @param problems can be none
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    public abstract <Solution_> @NonNull PlannerBenchmark buildPlannerBenchmark(@NonNull Solution_ @NonNull... problems);

}
