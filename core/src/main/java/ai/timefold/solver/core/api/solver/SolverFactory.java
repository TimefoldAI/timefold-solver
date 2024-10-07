package ai.timefold.solver.core.api.solver;

import java.io.File;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Creates {@link Solver} instances.
 * Most applications only need one SolverFactory.
 * <p>
 * To create a SolverFactory, use {@link #createFromXmlResource(String)}.
 * To change the configuration programmatically, create a {@link SolverConfig} first
 * and then use {@link #create(SolverConfig)}.
 * <p>
 * These methods are thread-safe unless explicitly stated otherwise.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface SolverFactory<Solution_> {

    // ************************************************************************
    // Static creation methods: XML
    // ************************************************************************

    /**
     * Reads an XML solver configuration from the classpath
     * and uses that {@link SolverConfig} to build a {@link SolverFactory}.
     * The XML root element must be {@code <solver>}.
     *
     * @param solverConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @return subsequent changes to the config have no effect on the returned instance
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    static <Solution_> @NonNull SolverFactory<Solution_> createFromXmlResource(@NonNull String solverConfigResource) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource);
        return new DefaultSolverFactory<>(solverConfig);
    }

    /**
     * As defined by {@link #createFromXmlResource(String)}.
     *
     * @param solverConfigResource na classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     * @return subsequent changes to the config have no effect on the returned instance
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    static <Solution_> @NonNull SolverFactory<Solution_> createFromXmlResource(@NonNull String solverConfigResource,
            @Nullable ClassLoader classLoader) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource, classLoader);
        return new DefaultSolverFactory<>(solverConfig);
    }

    /**
     * Reads an XML solver configuration from the file system
     * and uses that {@link SolverConfig} to build a {@link SolverFactory}.
     * <p>
     * Warning: this leads to platform dependent code,
     * it's recommend to use {@link #createFromXmlResource(String)} instead.
     *
     * @return subsequent changes to the config have no effect on the returned instance
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    static <Solution_> @NonNull SolverFactory<Solution_> createFromXmlFile(@NonNull File solverConfigFile) {
        SolverConfig solverConfig = SolverConfig.createFromXmlFile(solverConfigFile);
        return new DefaultSolverFactory<>(solverConfig);
    }

    /**
     * As defined by {@link #createFromXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     * @return subsequent changes to the config have no effect on the returned instance
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    static <Solution_> @NonNull SolverFactory<Solution_> createFromXmlFile(@NonNull File solverConfigFile,
            @Nullable ClassLoader classLoader) {
        SolverConfig solverConfig = SolverConfig.createFromXmlFile(solverConfigFile, classLoader);
        return new DefaultSolverFactory<>(solverConfig);
    }

    // ************************************************************************
    // Static creation methods: SolverConfig
    // ************************************************************************

    /**
     * Uses a {@link SolverConfig} to build a {@link SolverFactory}.
     * If you don't need to manipulate the {@link SolverConfig} programmatically,
     * use {@link #createFromXmlResource(String)} instead.
     *
     * @return subsequent changes to the config have no effect on the returned instance
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     */
    static <Solution_> @NonNull SolverFactory<Solution_> create(@NonNull SolverConfig solverConfig) {
        Objects.requireNonNull(solverConfig);
        // Defensive copy of solverConfig, because the DefaultSolverFactory doesn't internalize it yet
        solverConfig = new SolverConfig(solverConfig);
        return new DefaultSolverFactory<>(solverConfig);
    }

    // ************************************************************************
    // Interface methods
    // ************************************************************************

    /**
     * Creates a new {@link Solver} instance.
     */
    default @NonNull Solver<Solution_> buildSolver() {
        return this.buildSolver(new SolverConfigOverride<>());
    }

    /**
     * As defined by {@link #buildSolver()}.
     *
     * @param configOverride includes settings that override the default configuration
     */
    @NonNull
    Solver<Solution_> buildSolver(@NonNull SolverConfigOverride<Solution_> configOverride);
}
