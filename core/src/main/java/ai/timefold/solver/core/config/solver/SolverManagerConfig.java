package ai.timefold.solver.core.config.solver;

import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlType(propOrder = {
        "parallelSolverCount",
        "threadFactoryClass"
})
public class SolverManagerConfig extends AbstractConfig<SolverManagerConfig> {

    public static final String PARALLEL_SOLVER_COUNT_AUTO = "AUTO";

    private static final Logger LOGGER = LoggerFactory.getLogger(SolverManagerConfig.class);

    protected String parallelSolverCount = null;
    protected Class<? extends ThreadFactory> threadFactoryClass = null;

    // Future features:
    // throttlingDelay
    // congestionStrategy

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public SolverManagerConfig() {
    }

    public @Nullable String getParallelSolverCount() {
        return parallelSolverCount;
    }

    public void setParallelSolverCount(@Nullable String parallelSolverCount) {
        this.parallelSolverCount = parallelSolverCount;
    }

    public @Nullable Class<? extends ThreadFactory> getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(@Nullable Class<? extends ThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SolverManagerConfig withParallelSolverCount(@NonNull String parallelSolverCount) {
        this.parallelSolverCount = parallelSolverCount;
        return this;
    }

    public @NonNull SolverManagerConfig withThreadFactoryClass(@NonNull Class<? extends ThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public @NonNull Integer resolveParallelSolverCount() {
        int availableProcessorCount = getAvailableProcessors();
        Integer resolvedParallelSolverCount;
        if (parallelSolverCount == null || parallelSolverCount.equals(PARALLEL_SOLVER_COUNT_AUTO)) {
            resolvedParallelSolverCount = resolveParallelSolverCountAutomatically(availableProcessorCount);
        } else {
            resolvedParallelSolverCount = ConfigUtils.resolvePoolSize("parallelSolverCount",
                    parallelSolverCount, PARALLEL_SOLVER_COUNT_AUTO);
        }
        if (resolvedParallelSolverCount < 1) {
            throw new IllegalArgumentException("The parallelSolverCount (" + parallelSolverCount
                    + ") resulted in a resolvedParallelSolverCount (" + resolvedParallelSolverCount
                    + ") that is lower than 1.");
        }
        if (resolvedParallelSolverCount > availableProcessorCount) {
            LOGGER.warn("The resolvedParallelSolverCount ({}) is higher "
                    + "than the availableProcessorCount ({}), which is counter-efficient.",
                    resolvedParallelSolverCount, availableProcessorCount);
            // Still allow it, to reproduce issues of a high-end server machine on a low-end developer machine
        }
        return resolvedParallelSolverCount;
    }

    protected int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    protected int resolveParallelSolverCountAutomatically(int availableProcessorCount) {
        // Tweaked based on experience
        if (availableProcessorCount < 2) {
            return 1;
        } else {
            return availableProcessorCount / 2;
        }
    }

    @Override
    public @NonNull SolverManagerConfig inherit(@NonNull SolverManagerConfig inheritedConfig) {
        parallelSolverCount = ConfigUtils.inheritOverwritableProperty(parallelSolverCount,
                inheritedConfig.getParallelSolverCount());
        threadFactoryClass = ConfigUtils.inheritOverwritableProperty(threadFactoryClass,
                inheritedConfig.getThreadFactoryClass());
        return this;
    }

    @Override
    public @NonNull SolverManagerConfig copyConfig() {
        return new SolverManagerConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(threadFactoryClass);
    }

}
