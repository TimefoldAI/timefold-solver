package ai.timefold.solver.core.impl.solver;

import static java.util.stream.Collectors.joining;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;

import org.jspecify.annotations.NullMarked;

/**
 * Sets a solver-level class (such as the neighborhood provider) found by Spring Boot or Quarkus,
 * for solvers with {@link PreviewFeature#NEIGHBORHOODS} that need it and do not set it themselves.
 * Phase-level settings are never changed.
 */
@NullMarked
@SuppressWarnings({ "rawtypes" })
public final class SolverLevelClassDetector {

    public static final SolverLevelClassDetector NEIGHBORHOOD_PROVIDER = new SolverLevelClassDetector(
            NeighborhoodProvider.class, "neighborhoodProviderClass", SolverConfig::getNeighborhoodProviderClass,
            LocalSearchPhaseConfig::getNeighborhoodProviderClass, SolverConfig::setNeighborhoodProviderClass, false);
    /**
     * A phase with move selectors rejects a strategy, so a solver with such a phase never gets one.
     */
    public static final SolverLevelClassDetector MOVE_SELECTION_STRATEGY = new SolverLevelClassDetector(
            MoveSelectionStrategy.class, "moveSelectionStrategyClass", SolverConfig::getMoveSelectionStrategyClass,
            LocalSearchPhaseConfig::getMoveSelectionStrategyClass, SolverConfig::setMoveSelectionStrategyClass, true);

    private final Class<?> type;
    private final String propertyName;
    private final Function<SolverConfig, Class<?>> solverLevelGetter;
    private final Function<LocalSearchPhaseConfig, Class<?>> phaseLevelGetter;
    private final BiConsumer<SolverConfig, Class> solverLevelSetter;
    private final boolean excludedByMoveSelectors;

    private SolverLevelClassDetector(Class<?> type, String propertyName, Function<SolverConfig, Class<?>> solverLevelGetter,
            Function<LocalSearchPhaseConfig, Class<?>> phaseLevelGetter, BiConsumer<SolverConfig, Class> solverLevelSetter,
            boolean excludedByMoveSelectors) {
        this.type = type;
        this.propertyName = propertyName;
        this.solverLevelGetter = solverLevelGetter;
        this.phaseLevelGetter = phaseLevelGetter;
        this.solverLevelSetter = solverLevelSetter;
        this.excludedByMoveSelectors = excludedByMoveSelectors;
    }

    public Class<?> getType() {
        return type;
    }

    /**
     * @param solverConfigMap solver name to its config, with the preview features already applied
     * @param classListSupplier the implementations of {@link #getType()} found by the integration;
     *        called only if a solver has {@link PreviewFeature#NEIGHBORHOODS}
     */
    public void detect(Map<String, SolverConfig> solverConfigMap, Supplier<List<Class<?>>> classListSupplier) {
        var neighborhoodsSolverConfigMap = new TreeMap<String, SolverConfig>();
        solverConfigMap.forEach((solverName, solverConfig) -> {
            var previewFeatureSet = solverConfig.getEnablePreviewFeatureSet();
            if (previewFeatureSet != null && previewFeatureSet.contains(PreviewFeature.NEIGHBORHOODS)) {
                neighborhoodsSolverConfigMap.put(solverName, solverConfig);
            }
        });
        if (neighborhoodsSolverConfigMap.isEmpty()) {
            return;
        }
        var classList = classListSupplier.get();
        if (classList.isEmpty()) {
            return;
        }
        var needingSolverNameList = neighborhoodsSolverConfigMap.entrySet().stream()
                .filter(e -> needsSolverLevelClass(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        if (classList.size() == 1) {
            needingSolverNameList.forEach(solverName -> solverLevelSetter.accept(solverConfigMap.get(solverName),
                    classList.getFirst()));
            return;
        }
        var typeName = type.getSimpleName();
        var classNames = classList.stream().map(Class::getSimpleName).collect(joining(", "));
        if (!needingSolverNameList.isEmpty()) {
            if (solverConfigMap.size() == 1) {
                throw new IllegalStateException("Multiple classes (%s) that implement %s were found in the classpath."
                        .formatted(classNames, typeName));
            }
            throw new IllegalStateException("""
                    Some solver configs (%s) don't specify a %s (%s), yet there are multiple available (%s) on the classpath.
                    Maybe set the %s in the XML config files of the related solver configs, \
                    or remove the unnecessary classes from the classpath."""
                    .formatted(String.join(", ", needingSolverNameList), typeName, propertyName, classNames, propertyName));
        }
        var usedClassNameSet = new HashSet<String>();
        for (var solverConfig : neighborhoodsSolverConfigMap.values()) {
            var solverLevelClass = solverLevelGetter.apply(solverConfig);
            if (solverLevelClass != null) {
                usedClassNameSet.add(solverLevelClass.getName());
            }
            for (var phaseConfig : localSearchPhaseConfigs(solverConfig)) {
                var phaseLevelClass = phaseLevelGetter.apply(phaseConfig);
                if (phaseLevelClass != null) {
                    usedClassNameSet.add(phaseLevelClass.getName());
                }
            }
        }
        var unusedClassNameList = classList.stream()
                .map(Class::getName)
                .filter(className -> !usedClassNameSet.contains(className))
                .toList();
        if (!unusedClassNameList.isEmpty()) {
            throw new IllegalStateException("Unused classes ([%s]) that implements %s were found."
                    .formatted(String.join(", ", unusedClassNameList), typeName));
        }
    }

    private boolean needsSolverLevelClass(SolverConfig solverConfig) {
        if (solverLevelGetter.apply(solverConfig) != null) {
            return false;
        }
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (phaseConfigList == null || phaseConfigList.isEmpty()) {
            return true; // Generated phases use the solver level.
        }
        var localSearchPhaseConfigList = localSearchPhaseConfigs(solverConfig);
        if (excludedByMoveSelectors
                && localSearchPhaseConfigList.stream().anyMatch(phaseConfig -> phaseConfig.getMoveSelectorConfig() != null)) {
            return false;
        }
        return localSearchPhaseConfigList.stream()
                .anyMatch(phaseConfig -> phaseLevelGetter.apply(phaseConfig) == null);
    }

    private static List<LocalSearchPhaseConfig> localSearchPhaseConfigs(SolverConfig solverConfig) {
        var phaseConfigList = solverConfig.getPhaseConfigList();
        if (phaseConfigList == null) {
            return List.of();
        }
        return phaseConfigList.stream()
                .filter(LocalSearchPhaseConfig.class::isInstance)
                .map(LocalSearchPhaseConfig.class::cast)
                .toList();
    }
}
