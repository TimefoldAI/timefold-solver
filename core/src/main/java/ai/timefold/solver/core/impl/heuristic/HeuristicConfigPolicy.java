package ai.timefold.solver.core.impl.heuristic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.EntityMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.mimic.SubListMimicRecorder;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.ValueMimicRecorder;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.impl.solver.thread.DefaultSolverThreadFactory;

public class HeuristicConfigPolicy<Solution_> {

    private final Set<PreviewFeature> previewFeatureSet;
    private final EnvironmentMode environmentMode;
    private final String logIndentation;
    private final Integer moveThreadCount;
    private final Integer moveThreadBufferSize;
    private final Class<? extends ThreadFactory> threadFactoryClass;
    private final InitializingScoreTrend initializingScoreTrend;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final EntitySorterManner entitySorterManner;
    private final ValueSorterManner valueSorterManner;
    private final ClassInstanceCache classInstanceCache;
    private final boolean reinitializeVariableFilterEnabled;
    private final boolean initializedChainedValueFilterEnabled;
    private final boolean unassignedValuesAllowed;
    private final Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass;
    private final Random random;

    private final Map<String, EntityMimicRecorder<Solution_>> entityMimicRecorderMap = new HashMap<>();
    private final Map<String, SubListMimicRecorder<Solution_>> subListMimicRecorderMap = new HashMap<>();
    private final Map<String, ValueMimicRecorder<Solution_>> valueMimicRecorderMap = new HashMap<>();

    private HeuristicConfigPolicy(Builder<Solution_> builder) {
        this.previewFeatureSet = builder.previewFeatureSet;
        this.environmentMode = builder.environmentMode;
        this.logIndentation = builder.logIndentation;
        this.moveThreadCount = builder.moveThreadCount;
        this.moveThreadBufferSize = builder.moveThreadBufferSize;
        this.threadFactoryClass = builder.threadFactoryClass;
        this.initializingScoreTrend = builder.initializingScoreTrend;
        this.solutionDescriptor = builder.solutionDescriptor;
        this.entitySorterManner = builder.entitySorterManner;
        this.valueSorterManner = builder.valueSorterManner;
        this.classInstanceCache = builder.classInstanceCache;
        this.reinitializeVariableFilterEnabled = builder.reinitializeVariableFilterEnabled;
        this.initializedChainedValueFilterEnabled = builder.initializedChainedValueFilterEnabled;
        this.unassignedValuesAllowed = builder.unassignedValuesAllowed;
        this.nearbyDistanceMeterClass = builder.nearbyDistanceMeterClass;
        this.random = builder.random;
    }

    public EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    public String getLogIndentation() {
        return logIndentation;
    }

    public Integer getMoveThreadCount() {
        return moveThreadCount;
    }

    public Integer getMoveThreadBufferSize() {
        return moveThreadBufferSize;
    }

    public InitializingScoreTrend getInitializingScoreTrend() {
        return initializingScoreTrend;
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    public ScoreDefinition getScoreDefinition() {
        return solutionDescriptor.getScoreDefinition();
    }

    public EntitySorterManner getEntitySorterManner() {
        return entitySorterManner;
    }

    public ValueSorterManner getValueSorterManner() {
        return valueSorterManner;
    }

    public ClassInstanceCache getClassInstanceCache() {
        return classInstanceCache;
    }

    public boolean isReinitializeVariableFilterEnabled() {
        return reinitializeVariableFilterEnabled;
    }

    public boolean isInitializedChainedValueFilterEnabled() {
        return initializedChainedValueFilterEnabled;
    }

    public boolean isUnassignedValuesAllowed() {
        return unassignedValuesAllowed;
    }

    public Class<? extends NearbyDistanceMeter> getNearbyDistanceMeterClass() {
        return nearbyDistanceMeterClass;
    }

    public Random getRandom() {
        return random;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public Builder<Solution_> cloneBuilder() {
        return new Builder<Solution_>()
                .withPreviewFeatureSet(previewFeatureSet)
                .withEnvironmentMode(environmentMode)
                .withMoveThreadCount(moveThreadCount)
                .withMoveThreadBufferSize(moveThreadBufferSize)
                .withThreadFactoryClass(threadFactoryClass)
                .withNearbyDistanceMeterClass(nearbyDistanceMeterClass)
                .withRandom(random)
                .withInitializingScoreTrend(initializingScoreTrend)
                .withSolutionDescriptor(solutionDescriptor)
                .withClassInstanceCache(classInstanceCache)
                .withLogIndentation(logIndentation);
    }

    public HeuristicConfigPolicy<Solution_> copyConfigPolicy() {
        return cloneBuilder()
                .withEntitySorterManner(entitySorterManner)
                .withValueSorterManner(valueSorterManner)
                .withReinitializeVariableFilterEnabled(reinitializeVariableFilterEnabled)
                .withInitializedChainedValueFilterEnabled(initializedChainedValueFilterEnabled)
                .withUnassignedValuesAllowed(unassignedValuesAllowed)
                .build();
    }

    public HeuristicConfigPolicy<Solution_> createPhaseConfigPolicy() {
        return cloneBuilder().build();
    }

    public HeuristicConfigPolicy<Solution_> copyConfigPolicyWithoutNearbySetting() {
        return cloneBuilder()
                .withNearbyDistanceMeterClass(null)
                .build();
    }

    public HeuristicConfigPolicy<Solution_> createChildThreadConfigPolicy(ChildThreadType childThreadType) {
        return cloneBuilder()
                .withLogIndentation(logIndentation + "        ")
                .build();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void addEntityMimicRecorder(String id, EntityMimicRecorder<Solution_> mimicRecordingEntitySelector) {
        var put = entityMimicRecorderMap.put(id, mimicRecordingEntitySelector);
        if (put != null) {
            throw new IllegalStateException(
                    """
                            Multiple %ss (usually %ss) have the same id (%s).
                            Maybe specify a variable name for the mimicking selector in situations with multiple variables on the same entity?"""
                            .formatted(EntityMimicRecorder.class.getSimpleName(), EntitySelector.class.getSimpleName(), id));
        }
    }

    public EntityMimicRecorder<Solution_> getEntityMimicRecorder(String id) {
        return entityMimicRecorderMap.get(id);
    }

    public void addSubListMimicRecorder(String id, SubListMimicRecorder<Solution_> mimicRecordingSubListSelector) {
        var put = subListMimicRecorderMap.put(id, mimicRecordingSubListSelector);
        if (put != null) {
            throw new IllegalStateException(
                    """
                            Multiple %ss (usually %ss) have the same id (%s).
                            Maybe specify a variable name for the mimicking selector in situations with multiple variables on the same entity?"""
                            .formatted(SubListMimicRecorder.class.getSimpleName(), SubListSelector.class.getSimpleName(), id));
        }
    }

    public SubListMimicRecorder<Solution_> getSubListMimicRecorder(String id) {
        return subListMimicRecorderMap.get(id);
    }

    public void addValueMimicRecorder(String id, ValueMimicRecorder<Solution_> mimicRecordingValueSelector) {
        var put = valueMimicRecorderMap.put(id, mimicRecordingValueSelector);
        if (put != null) {
            throw new IllegalStateException(
                    """
                            Multiple %ss (usually %ss) have the same id (%s).
                            Maybe specify a variable name for the mimicking selector in situations with multiple variables on the same entity?"""
                            .formatted(ValueMimicRecorder.class.getSimpleName(), ValueSelector.class.getSimpleName(), id));
        }
    }

    public ValueMimicRecorder<Solution_> getValueMimicRecorder(String id) {
        return valueMimicRecorderMap.get(id);
    }

    public ThreadFactory buildThreadFactory(ChildThreadType childThreadType) {
        if (threadFactoryClass != null) {
            return ConfigUtils.newInstance(this::toString, "threadFactoryClass", threadFactoryClass);
        } else {
            var threadPrefix = switch (childThreadType) {
                case MOVE_THREAD -> "MoveThread";
                case PART_THREAD -> "PartThread";
            };
            return new DefaultSolverThreadFactory(threadPrefix);
        }
    }

    public void ensurePreviewFeature(PreviewFeature previewFeature) {
        ensurePreviewFeature(previewFeature, previewFeatureSet);
    }

    public static void ensurePreviewFeature(PreviewFeature previewFeature,
            Collection<PreviewFeature> previewFeatureCollection) {
        if (previewFeatureCollection == null || !previewFeatureCollection.contains(previewFeature)) {
            throw new IllegalStateException("""
                    The preview feature %s is not enabled.
                    Maybe add %s to <enablePreviewFeature> in your configuration file?"""
                    .formatted(previewFeature, previewFeature));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + environmentMode + ")";
    }

    public static class Builder<Solution_> {

        private Set<PreviewFeature> previewFeatureSet;
        private EnvironmentMode environmentMode;
        private Integer moveThreadCount;
        private Integer moveThreadBufferSize;
        private Class<? extends ThreadFactory> threadFactoryClass;
        private InitializingScoreTrend initializingScoreTrend;
        private SolutionDescriptor<Solution_> solutionDescriptor;
        private ClassInstanceCache classInstanceCache;

        private String logIndentation = "";

        private EntitySorterManner entitySorterManner = EntitySorterManner.NONE;
        private ValueSorterManner valueSorterManner = ValueSorterManner.NONE;

        private boolean reinitializeVariableFilterEnabled = false;
        private boolean initializedChainedValueFilterEnabled = false;
        private boolean unassignedValuesAllowed = false;

        private Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass;
        private Random random;

        public Builder<Solution_> withPreviewFeatureSet(Set<PreviewFeature> previewFeatureSet) {
            this.previewFeatureSet = previewFeatureSet;
            return this;
        }

        public Builder<Solution_> withEnvironmentMode(EnvironmentMode environmentMode) {
            this.environmentMode = environmentMode;
            return this;
        }

        public Builder<Solution_> withMoveThreadCount(Integer moveThreadCount) {
            this.moveThreadCount = moveThreadCount;
            return this;
        }

        public Builder<Solution_> withMoveThreadBufferSize(Integer moveThreadBufferSize) {
            this.moveThreadBufferSize = moveThreadBufferSize;
            return this;
        }

        public Builder<Solution_> withThreadFactoryClass(Class<? extends ThreadFactory> threadFactoryClass) {
            this.threadFactoryClass = threadFactoryClass;
            return this;
        }

        public Builder<Solution_>
                withNearbyDistanceMeterClass(Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass) {
            this.nearbyDistanceMeterClass = nearbyDistanceMeterClass;
            return this;
        }

        public Builder<Solution_> withRandom(Random random) {
            this.random = random;
            return this;
        }

        public Builder<Solution_> withInitializingScoreTrend(InitializingScoreTrend initializingScoreTrend) {
            this.initializingScoreTrend = initializingScoreTrend;
            return this;
        }

        public Builder<Solution_> withSolutionDescriptor(SolutionDescriptor<Solution_> solutionDescriptor) {
            this.solutionDescriptor = solutionDescriptor;
            return this;
        }

        public Builder<Solution_> withClassInstanceCache(ClassInstanceCache classInstanceCache) {
            this.classInstanceCache = classInstanceCache;
            return this;
        }

        public Builder<Solution_> withLogIndentation(String logIndentation) {
            this.logIndentation = logIndentation;
            return this;
        }

        public Builder<Solution_> withEntitySorterManner(EntitySorterManner entitySorterManner) {
            this.entitySorterManner = entitySorterManner;
            return this;
        }

        public Builder<Solution_> withValueSorterManner(ValueSorterManner valueSorterManner) {
            this.valueSorterManner = valueSorterManner;
            return this;
        }

        public Builder<Solution_> withReinitializeVariableFilterEnabled(boolean reinitializeVariableFilterEnabled) {
            this.reinitializeVariableFilterEnabled = reinitializeVariableFilterEnabled;
            return this;
        }

        public Builder<Solution_> withInitializedChainedValueFilterEnabled(boolean initializedChainedValueFilterEnabled) {
            this.initializedChainedValueFilterEnabled = initializedChainedValueFilterEnabled;
            return this;
        }

        public Builder<Solution_> withUnassignedValuesAllowed(boolean unassignedValuesAllowed) {
            this.unassignedValuesAllowed = unassignedValuesAllowed;
            return this;
        }

        public HeuristicConfigPolicy<Solution_> build() {
            return new HeuristicConfigPolicy<>(this);
        }
    }
}
