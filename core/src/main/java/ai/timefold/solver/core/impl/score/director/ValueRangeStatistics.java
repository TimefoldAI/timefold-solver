package ai.timefold.solver.core.impl.score.director;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.ProblemScaleTracker;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.impl.util.MutableLong;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class ValueRangeStatistics<Solution_> {

    private final ValueRangeManager<Solution_> valueRangeManager;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Solution_ solution;
    private @Nullable SolutionInitializationStatistics cachedInitializationStatistics = null;
    private @Nullable ProblemSizeStatistics cachedProblemSizeStatistics = null;

    private long cachedApproximateValueCount = -1L;
    // Indexed by global variable ordinal; value ranges from the solution only.
    private long @Nullable [] solutionValueCountArray;
    // Indexed by entity ordinal, then by position in the entity's genuine variable list; value ranges from the entity only.
    private long @Nullable [][] entityValueCountArray;
    private long @Nullable [] cachedEntityCountByEntityOrdinal;
    // Negative if the problem size statistics are not calculated, non-negative if cached.
    private double cachedProblemScale = -1.0;

    ValueRangeStatistics(ValueRangeManager<Solution_> valueRangeManager, SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ solution) {
        this.valueRangeManager = valueRangeManager;
        this.solutionDescriptor = solutionDescriptor;
        this.solution = Objects.requireNonNull(solution,
                "Impossible state: initialization statistics requested before the working solution is known.");
    }

    Solution_ getSolution() {
        return solution;
    }

    SolutionInitializationStatistics computeInitializationStatistics(@Nullable Consumer<Object> finisher, boolean useCache) {
        if (useCache && cachedInitializationStatistics != null) {
            return cachedInitializationStatistics;
        }
        /*
         * The score director requires all of these data points,
         * so we calculate them all in a single pass over the entities.
         * This is an important performance improvement,
         * as there are potentially thousands of entities.
         * The problem size statistics are only computed when requested,
         * as they require the value ranges of all entities.
         */
        var uninitializedEntityCount = new MutableInt();
        var uninitializedVariableCount = new MutableInt();
        var assignedListValueCount = new MutableInt();
        var genuineEntityCount = new MutableInt();
        var shadowEntityCount = new MutableInt();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        solutionDescriptor.visitAllEntities(solution, entity -> {
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                genuineEntityCount.increment();
                var uninitializedVariableCountForEntity = entityDescriptor.countUninitializedVariables(entity);
                if (uninitializedVariableCountForEntity > 0) {
                    uninitializedEntityCount.increment();
                    uninitializedVariableCount.add(uninitializedVariableCountForEntity);
                }
            } else {
                shadowEntityCount.increment();
            }
            if (finisher != null) {
                finisher.accept(entity);
            }
            if (entityDescriptor.hasAnyListVariables()
                    && Objects.requireNonNull(listVariableDescriptor).getEntityDescriptor().matchesEntity(entity)) {
                assignedListValueCount.add(listVariableDescriptor.getListSize(entity));
            }
            // TODO maybe detect duplicates and elements that are outside the value range
        });
        var unassignedValueCount = 0;
        if (listVariableDescriptor != null && !listVariableDescriptor.allowsUnassignedValues()) {
            // Every possibly unassigned element in the list variable, minus the assigned elements.
            unassignedValueCount = (int) valueRangeManager.countOnSolution(listVariableDescriptor.getValueRangeDescriptor(),
                    solution) - assignedListValueCount.intValue();
        }
        var statistics = new SolutionInitializationStatistics(genuineEntityCount.intValue(), shadowEntityCount.intValue(),
                uninitializedEntityCount.intValue(), uninitializedVariableCount.intValue(), unassignedValueCount,
                assignedListValueCount.intValue());
        if (cachedInitializationStatistics == null) {
            this.cachedInitializationStatistics = statistics;
        }
        return statistics;
    }

    private void computeProblemSizeStatistics() {
        var approximateValueCount = new MutableLong(0L);
        var maxValueRangeSize = new MutableLong(0L);

        var entityDescriptorCount = solutionDescriptor.getEntityDescriptors().size();
        var entityCountByEntityOrdinal = new long[entityDescriptorCount];
        var valueCountArray = new long[solutionDescriptor.getVariableDescriptorCount()];
        var entityValueCountMatrix = new long[entityDescriptorCount][];
        for (var entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            entityValueCountMatrix[entityDescriptor.getOrdinal()] =
                    new long[entityDescriptor.getGenuineVariableDescriptorList().size()];
        }

        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor != null) {
            var countOnSolution = valueRangeManager.countOnSolution(listVariableDescriptor.getValueRangeDescriptor(), solution);
            maxValueRangeSize.setValue(countOnSolution);
            if (listVariableDescriptor.canExtractValueRangeFromSolution()) {
                approximateValueCount.add(countOnSolution);
                valueCountArray[listVariableDescriptor.getGlobalOrdinal()] += countOnSolution;
            }
        }

        for (var entityDescriptor : solutionDescriptor.getGenuineEntityDescriptors()) {
            for (var genuineVariable : entityDescriptor.getDeclaredGenuineVariableDescriptors()) {
                if (genuineVariable instanceof BasicVariableDescriptor<Solution_> basicVariable
                        && basicVariable.canExtractValueRangeFromSolution()) {
                    var countOnSolution = valueRangeManager.countOnSolution(basicVariable.getValueRangeDescriptor(), solution);
                    approximateValueCount.add(countOnSolution);
                    valueCountArray[basicVariable.getGlobalOrdinal()] += countOnSolution;
                    if (maxValueRangeSize.longValue() < countOnSolution) {
                        maxValueRangeSize.setValue(countOnSolution);
                    }
                }
            }
        }

        var logBase = (maxValueRangeSize.longValue() < 2) ? 10 : maxValueRangeSize.longValue();
        var problemScaleTracker = new ProblemScaleTracker<>(listVariableDescriptor, valueRangeManager, logBase);

        solutionDescriptor.visitAllEntities(solution, entity -> {
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                // This count include immovable entities; problem scale ignores immovable entities
                entityCountByEntityOrdinal[entityDescriptor.getOrdinal()]++;
                processProblemScale(valueRangeManager, entityDescriptor, entity, problemScaleTracker);
            }
            var genuineVariableList = entityDescriptor.getGenuineVariableDescriptorList();
            var entityValueCountRow = entityValueCountMatrix[entityDescriptor.getOrdinal()];
            for (var i = 0; i < genuineVariableList.size(); i++) {
                var genuineVariable = genuineVariableList.get(i);
                if (!genuineVariable.canExtractValueRangeFromSolution()) {
                    var rangeValueCount = valueRangeManager.countOnEntity(genuineVariable.getValueRangeDescriptor(), entity);
                    approximateValueCount.add(rangeValueCount);
                    entityValueCountRow[i] += rangeValueCount;
                }
            }
        });
        this.cachedEntityCountByEntityOrdinal = entityCountByEntityOrdinal;
        this.solutionValueCountArray = valueCountArray;
        this.entityValueCountArray = entityValueCountMatrix;
        this.cachedApproximateValueCount = approximateValueCount.longValue();
        var problemScaleLogAsLong = problemScaleTracker.getProblemScaleLog();
        var scale = (problemScaleLogAsLong / (double) MathUtils.LOG_PRECISION) / MathUtils.getLogInBase(logBase, 10d);
        if (Double.isNaN(scale) || Double.isInfinite(scale)) {
            cachedProblemScale = 0.0;
        } else {
            cachedProblemScale = scale;
        }
    }

    private void ensureProblemSizeStatisticsInitialized() {
        if (cachedProblemScale < 0) {
            computeProblemSizeStatistics();
        }
    }

    public ProblemSizeStatistics getProblemSizeStatistics() {
        ensureProblemSizeStatisticsInitialized();
        if (cachedProblemSizeStatistics == null) {
            var result = getEntityAndValueCounts();
            cachedProblemSizeStatistics = new ProblemSizeStatistics(
                    solutionDescriptor.getGenuineEntityCount(solution),
                    result.entityClassToEntityCount(),
                    solutionDescriptor.getGenuineVariableCount(solution),
                    cachedApproximateValueCount,
                    result.entityClassToVariableToValueCount(),
                    cachedProblemScale);
        }
        return cachedProblemSizeStatistics;
    }

    private record EntityAndValueCounts(LinkedHashMap<Class<?>, Long> entityClassToEntityCount,
            LinkedHashMap<Class<?>, SequencedMap<String, Long>> entityClassToVariableToValueCount) {
    }

    private EntityAndValueCounts getEntityAndValueCounts() {
        var entityClassToEntityCount = new LinkedHashMap<Class<?>, Long>();
        var entityClassToVariableToValueCount = new LinkedHashMap<Class<?>, SequencedMap<String, Long>>();
        var entityCountByEntityOrdinal = Objects.requireNonNull(cachedEntityCountByEntityOrdinal);
        for (var entityDescriptor : solutionDescriptor.getGenuineEntityDescriptors()) {
            entityClassToEntityCount.put(entityDescriptor.getEntityClass(),
                    entityCountByEntityOrdinal[entityDescriptor.getOrdinal()]);
            entityClassToVariableToValueCount.put(entityDescriptor.getEntityClass(),
                    getVariableToValueCount(entityDescriptor));
        }
        return new EntityAndValueCounts(entityClassToEntityCount, entityClassToVariableToValueCount);
    }

    private LinkedHashMap<String, Long> getVariableToValueCount(EntityDescriptor<Solution_> entityDescriptor) {
        var valueCountArray = Objects.requireNonNull(solutionValueCountArray);
        var entityValueCountRow = Objects.requireNonNull(entityValueCountArray)[entityDescriptor.getOrdinal()];
        var variableToValueCount = new LinkedHashMap<String, Long>();
        var genuineVariableList = entityDescriptor.getGenuineVariableDescriptorList();
        for (var i = 0; i < genuineVariableList.size(); i++) {
            var genuineVariable = genuineVariableList.get(i);
            variableToValueCount.put(genuineVariable.getVariableName(),
                    genuineVariable.canExtractValueRangeFromSolution()
                            ? valueCountArray[genuineVariable.getGlobalOrdinal()]
                            : entityValueCountRow[i]);
        }
        return variableToValueCount;
    }

    long getApproximateValueCount() {
        ensureProblemSizeStatisticsInitialized();
        return cachedApproximateValueCount;
    }

    /**
     * Calculates an indication of the size of the problem instance.
     * This is approximately the base 10 logarithm of the search space size.
     *
     * <p>
     * The method uses a logarithmic scale to estimate the problem size,
     * where the base of the logarithm is determined by the maximum value range size.
     * It accounts for both basic variables and list variables in the solution,
     * considering pinned values and value ranges on both entity and solution.
     *
     * @return A non-negative double value representing the approximate base 10 logarithm of the search space size.
     *         Returns {@code 0} if the calculation results in NaN or infinity.
     */
    double getProblemScale() {
        ensureProblemSizeStatisticsInitialized();
        return cachedProblemScale;
    }

    private void processProblemScale(ValueRangeManager<Solution_> valueRangeManager,
            EntityDescriptor<Solution_> entityDescriptor, Object entity, ProblemScaleTracker<Solution_> tracker) {
        for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
            var valueCount = variableDescriptor.canExtractValueRangeFromSolution()
                    ? valueRangeManager.countOnSolution(variableDescriptor.getValueRangeDescriptor(), solution)
                    : valueRangeManager.countOnEntity(variableDescriptor.getValueRangeDescriptor(), entity);
            switch (variableDescriptor) {
                case BasicVariableDescriptor<Solution_> ignored -> {
                    if (entityDescriptor.isMovable(solution, entity)) {
                        tracker.addBasicProblemScale(valueCount);
                    }
                }
                case ListVariableDescriptor<Solution_> ignored -> {
                    // Intentionally empty, we don't need to process it here
                }
                default -> throw new IllegalStateException("Unhandled subclass of %s encountered (%s)."
                        .formatted(VariableDescriptor.class.getSimpleName(), variableDescriptor.getClass().getSimpleName()));
            }
        }
    }

}
