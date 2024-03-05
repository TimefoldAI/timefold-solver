package ai.timefold.solver.core.impl.heuristic;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadFactory;

import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
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
        return new Builder<>(environmentMode, moveThreadCount, moveThreadBufferSize, threadFactoryClass,
                nearbyDistanceMeterClass, random, initializingScoreTrend, solutionDescriptor, classInstanceCache)
                .withLogIndentation(logIndentation);
    }

    public HeuristicConfigPolicy<Solution_> createPhaseConfigPolicy() {
        return cloneBuilder().build();
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
        EntityMimicRecorder<Solution_> put = entityMimicRecorderMap.put(id, mimicRecordingEntitySelector);
        if (put != null) {
            throw new IllegalStateException("Multiple " + EntityMimicRecorder.class.getSimpleName() + "s (usually "
                    + EntitySelector.class.getSimpleName() + "s) have the same id (" + id + ").\n" +
                    "Maybe specify a variable name for the mimicking selector in situations with multiple variables on the same entity?");
        }
    }

    public EntityMimicRecorder<Solution_> getEntityMimicRecorder(String id) {
        return entityMimicRecorderMap.get(id);
    }

    public void addSubListMimicRecorder(String id, SubListMimicRecorder<Solution_> mimicRecordingSubListSelector) {
        SubListMimicRecorder<Solution_> put = subListMimicRecorderMap.put(id, mimicRecordingSubListSelector);
        if (put != null) {
            throw new IllegalStateException("Multiple " + SubListMimicRecorder.class.getSimpleName() + "s (usually "
                    + SubListSelector.class.getSimpleName() + "s) have the same id (" + id + ").\n" +
                    "Maybe specify a variable name for the mimicking selector in situations with multiple variables on the same entity?");
        }
    }

    public SubListMimicRecorder<Solution_> getSubListMimicRecorder(String id) {
        return subListMimicRecorderMap.get(id);
    }

    public void addValueMimicRecorder(String id, ValueMimicRecorder<Solution_> mimicRecordingValueSelector) {
        ValueMimicRecorder<Solution_> put = valueMimicRecorderMap.put(id, mimicRecordingValueSelector);
        if (put != null) {
            throw new IllegalStateException("Multiple " + ValueMimicRecorder.class.getSimpleName() + "s (usually "
                    + ValueSelector.class.getSimpleName() + "s) have the same id (" + id + ").\n" +
                    "Maybe specify a variable name for the mimicking selector in situations with multiple variables on the same entity?");
        }
    }

    public ValueMimicRecorder<Solution_> getValueMimicRecorder(String id) {
        return valueMimicRecorderMap.get(id);
    }

    public ThreadFactory buildThreadFactory(ChildThreadType childThreadType) {
        if (threadFactoryClass != null) {
            return ConfigUtils.newInstance(this::toString, "threadFactoryClass", threadFactoryClass);
        } else {
            String threadPrefix;
            switch (childThreadType) {
                case MOVE_THREAD:
                    threadPrefix = "MoveThread";
                    break;
                case PART_THREAD:
                    threadPrefix = "PartThread";
                    break;
                default:
                    throw new IllegalStateException("Unsupported childThreadType (" + childThreadType + ").");
            }
            return new DefaultSolverThreadFactory(threadPrefix);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + environmentMode + ")";
    }

    public static class Builder<Solution_> {

        private final EnvironmentMode environmentMode;
        private final Integer moveThreadCount;
        private final Integer moveThreadBufferSize;
        private final Class<? extends ThreadFactory> threadFactoryClass;
        private final InitializingScoreTrend initializingScoreTrend;
        private final SolutionDescriptor<Solution_> solutionDescriptor;
        private final ClassInstanceCache classInstanceCache;

        private String logIndentation = "";

        private EntitySorterManner entitySorterManner = EntitySorterManner.NONE;
        private ValueSorterManner valueSorterManner = ValueSorterManner.NONE;

        private boolean reinitializeVariableFilterEnabled = false;
        private boolean initializedChainedValueFilterEnabled = false;
        private boolean unassignedValuesAllowed = false;

        private final Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass;
        private final Random random;

        public Builder(EnvironmentMode environmentMode, Integer moveThreadCount, Integer moveThreadBufferSize,
                Class<? extends ThreadFactory> threadFactoryClass,
                Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass, Random random,
                InitializingScoreTrend initializingScoreTrend, SolutionDescriptor<Solution_> solutionDescriptor,
                ClassInstanceCache classInstanceCache) {
            this.environmentMode = environmentMode;
            this.moveThreadCount = moveThreadCount;
            this.moveThreadBufferSize = moveThreadBufferSize;
            this.threadFactoryClass = threadFactoryClass;
            this.nearbyDistanceMeterClass = nearbyDistanceMeterClass;
            this.random = random;
            this.initializingScoreTrend = initializingScoreTrend;
            this.solutionDescriptor = solutionDescriptor;
            this.classInstanceCache = classInstanceCache;
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
