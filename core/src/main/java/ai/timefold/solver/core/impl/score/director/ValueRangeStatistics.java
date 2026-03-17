package ai.timefold.solver.core.impl.score.director;

import java.util.Objects;
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

    // Negative if not calculated, non-negative if cached
    private long cachedApproximateValueCount = -1L;
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
         */
        var uninitializedEntityCount = new MutableInt();
        var uninitializedVariableCount = new MutableInt();
        var unassignedValueCount = new MutableInt();
        var notInAnyListValueCount = new MutableInt();
        var genuineEntityCount = new MutableInt();
        var shadowEntityCount = new MutableInt();
        var approximateValueCount = new MutableLong();
        var maxValueRangeSize = new MutableLong(0L);

        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor != null) {
            var countOnSolution =
                    (int) valueRangeManager.countOnSolution(listVariableDescriptor.getValueRangeDescriptor(), solution);
            notInAnyListValueCount.add(countOnSolution);
            maxValueRangeSize.setValue(countOnSolution);
            if (listVariableDescriptor.canExtractValueRangeFromSolution()) {
                approximateValueCount.add(countOnSolution);
            }
            if (!listVariableDescriptor.allowsUnassignedValues()) {
                // We count every possibly unassigned element in every list variable.
                // And later we subtract the assigned elements.
                unassignedValueCount.add(countOnSolution);
            }
        }

        for (var basicVariable : solutionDescriptor.getBasicVariableDescriptorList()) {
            if (basicVariable.canExtractValueRangeFromSolution()) {
                var countOnSolution = valueRangeManager.countOnSolution(basicVariable.getValueRangeDescriptor(), solution);
                approximateValueCount.add(countOnSolution);
                if (maxValueRangeSize.longValue() < countOnSolution) {
                    maxValueRangeSize.setValue(countOnSolution);
                }
            }
        }

        var logBase = (maxValueRangeSize.longValue() < 2) ? 10 : maxValueRangeSize.longValue();
        var problemScaleTracker = new ProblemScaleTracker<>(listVariableDescriptor, valueRangeManager, logBase);

        solutionDescriptor.visitAllEntities(solution, entity -> {
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                genuineEntityCount.increment();
                var uninitializedVariableCountForEntity = entityDescriptor.countUninitializedVariables(entity);
                if (uninitializedVariableCountForEntity > 0) {
                    uninitializedEntityCount.increment();
                    uninitializedVariableCount.add(uninitializedVariableCountForEntity);
                }
                processProblemScale(valueRangeManager, entityDescriptor, entity, problemScaleTracker);
            } else {
                shadowEntityCount.increment();
            }
            if (finisher != null) {
                finisher.accept(entity);
            }

            for (var genuineVariable : entityDescriptor.getGenuineVariableDescriptorList()) {
                if (genuineVariable instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                        && !basicVariableDescriptor.canExtractValueRangeFromSolution()) {
                    approximateValueCount
                            .add(valueRangeManager.countOnEntity(basicVariableDescriptor.getValueRangeDescriptor(), entity));
                }
            }
            if (!entityDescriptor.hasAnyListVariables()) {
                return;
            }
            var listVariableEntityDescriptor = listVariableDescriptor.getEntityDescriptor();
            var countOnEntity = listVariableDescriptor.getListSize(entity);
            notInAnyListValueCount.subtract(countOnEntity);
            if (!listVariableDescriptor.allowsUnassignedValues() && listVariableEntityDescriptor.matchesEntity(entity)) {
                unassignedValueCount.subtract(countOnEntity);
            }
            if (!listVariableDescriptor.canExtractValueRangeFromSolution()) {
                approximateValueCount
                        .add(valueRangeManager.countOnEntity(listVariableDescriptor.getValueRangeDescriptor(), entity));
            }
            // TODO maybe detect duplicates and elements that are outside the value range
        });
        var statistics = new SolutionInitializationStatistics(genuineEntityCount.intValue(),
                shadowEntityCount.intValue(),
                uninitializedEntityCount.intValue(), uninitializedVariableCount.intValue(), unassignedValueCount.intValue(),
                notInAnyListValueCount.intValue());
        if (cachedInitializationStatistics == null) {
            this.cachedInitializationStatistics = statistics;
        }
        cachedApproximateValueCount = approximateValueCount.longValue();
        var problemScaleLogAsLong = problemScaleTracker.getProblemScaleLog();
        var scale = (problemScaleLogAsLong / (double) MathUtils.LOG_PRECISION) / MathUtils.getLogInBase(logBase, 10d);
        if (Double.isNaN(scale) || Double.isInfinite(scale)) {
            cachedProblemScale = 0.0;
        } else {
            cachedProblemScale = scale;
        }
        return statistics;
    }

    public ProblemSizeStatistics getProblemSizeStatistics() {
        if (cachedProblemScale < 0) {
            computeInitializationStatistics(null, false);
        }
        if (cachedProblemSizeStatistics == null) {
            cachedProblemSizeStatistics = new ProblemSizeStatistics(
                    solutionDescriptor.getGenuineEntityCount(solution),
                    solutionDescriptor.getGenuineVariableCount(solution),
                    cachedApproximateValueCount,
                    cachedProblemScale);
        }
        return cachedProblemSizeStatistics;
    }

    long getApproximateValueCount() {
        if (cachedApproximateValueCount == -1) {
            computeInitializationStatistics(null, false);
        }
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
        if (cachedProblemScale < 0) {
            computeInitializationStatistics(null, false);
        }
        return cachedProblemScale;
    }

    private void processProblemScale(ValueRangeManager<Solution_> valueRangeManager,
            EntityDescriptor<Solution_> entityDescriptor, Object entity, ProblemScaleTracker<Solution_> tracker) {
        for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
            var valueCount = variableDescriptor.canExtractValueRangeFromSolution()
                    ? valueRangeManager.countOnSolution(variableDescriptor.getValueRangeDescriptor(), solution)
                    : valueRangeManager.countOnEntity(variableDescriptor.getValueRangeDescriptor(), entity);
            switch (variableDescriptor) {
                case BasicVariableDescriptor<Solution_> basicVariableDescriptor -> {
                    if (entityDescriptor.isMovable(solution, entity)) {
                        tracker.addBasicProblemScale(valueCount);
                    }
                }
                case ListVariableDescriptor<Solution_> listVariableDescriptor -> {
                    // Intentionally empty, we don't need to process it here
                }
                default -> throw new IllegalStateException("Unhandled subclass of %s encountered (%s)."
                        .formatted(VariableDescriptor.class.getSimpleName(), variableDescriptor.getClass().getSimpleName()));
            }
        }
    }

}
