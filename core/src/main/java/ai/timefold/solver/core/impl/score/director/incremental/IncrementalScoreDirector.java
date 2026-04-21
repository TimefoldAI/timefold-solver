package ai.timefold.solver.core.impl.score.director.incremental;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.AnalyzableIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchRegistration;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchRegistry;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.util.MutableReference;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Incremental java implementation of {@link ScoreDirector}, which only recalculates the {@link Score}
 * of the part of the {@link PlanningSolution working solution} that changed,
 * instead of the going through the entire {@link PlanningSolution}. This is incremental calculation, which is fast.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see ScoreDirector
 */
@NullMarked
public final class IncrementalScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, IncrementalScoreDirectorFactory<Solution_, Score_>>
        implements ConstraintMatchRegistry<Score_> {

    private final IncrementalScoreCalculator<Solution_, Score_> incrementalScoreCalculator;
    private final boolean constraintMatchEnabled;
    private Score_ totalScore;
    private final SortedMap<ConstraintRef, ConstraintMatchTotal<Score_>> constraintMatchTotalMap = new TreeMap<>();

    private IncrementalScoreDirector(Builder<Solution_, Score_> builder) {
        super(builder);
        this.incrementalScoreCalculator = Objects.requireNonNull(builder.incrementalScoreCalculator,
                "The incrementalScoreCalculator must not be null.");
        this.constraintMatchEnabled = getConstraintMatchPolicy().isEnabled();
        this.totalScore = getScoreDefinition().getZeroScore();
        if (incrementalScoreCalculator instanceof AnalyzableIncrementalScoreCalculator<Solution_, Score_> analyzableIncrementalScoreCalculator
                && constraintMatchEnabled) {
            analyzableIncrementalScoreCalculator.enableConstraintMatch(this);
        }
    }

    public IncrementalScoreCalculator<Solution_, Score_> getIncrementalScoreCalculator() {
        return incrementalScoreCalculator;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public void setWorkingSolutionWithoutUpdatingShadows(Solution_ workingSolution) {
        super.setWorkingSolutionWithoutUpdatingShadows(workingSolution, null);
        resetWorkingSolutionAndMaps(workingSolution);
    }

    private void resetWorkingSolutionAndMaps(Solution_ workingSolution) {
        constraintMatchTotalMap.clear();
        totalScore = getScoreDefinition().getZeroScore();
        incrementalScoreCalculator.resetWorkingSolution(workingSolution);
    }

    @Override
    public InnerScore<Score_> calculateScore() {
        variableListenerSupport.assertNotificationQueuesAreEmpty();
        var score = Objects.requireNonNull(incrementalScoreCalculator.calculateScore(),
                () -> "The incrementalScoreCalculator (%s) must return a non-null score in the method calculateScore()."
                        .formatted(incrementalScoreCalculator));
        setCalculatedScore(score);
        return new InnerScore<>(score, -getWorkingInitScore());
    }

    @Override
    public Map<ConstraintRef, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        if (!constraintMatchPolicy.isEnabled()) {
            throw new IllegalStateException("When constraint matching (" + constraintMatchPolicy
                    + ") is disabled in the constructor, this method should not be called.");
        }
        // Notice that we don't trigger the variable listeners
        return constraintMatchTotalMap;
    }

    @Override
    public boolean requiresFlushing() {
        return true; // Incremental may decide to keep events for delayed processing.
    }

    // ************************************************************************
    // Entity/variable add/change/remove methods
    // ************************************************************************

    @Override
    public void beforeEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        super.beforeEntityAdded(entityDescriptor, entity);
    }

    @Override
    public void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        resetWorkingSolutionAndMaps(workingSolution);
        super.afterEntityAdded(entityDescriptor, entity);
    }

    @Override
    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        incrementalScoreCalculator.beforeVariableChanged(entity, variableDescriptor.getVariableName());
        super.beforeVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        incrementalScoreCalculator.afterVariableChanged(entity, variableDescriptor.getVariableName());
        super.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void beforeListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        incrementalScoreCalculator.beforeListVariableElementAssigned(variableDescriptor.getVariableName(), element);
        super.beforeListVariableElementAssigned(variableDescriptor, element);
    }

    @Override
    public void afterListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        incrementalScoreCalculator.afterListVariableElementAssigned(variableDescriptor.getVariableName(), element);
        super.afterListVariableElementAssigned(variableDescriptor, element);
    }

    @Override
    public void beforeListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        incrementalScoreCalculator.beforeListVariableElementUnassigned(variableDescriptor.getVariableName(), element);
        super.beforeListVariableElementUnassigned(variableDescriptor, element);
    }

    @Override
    public void afterListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        incrementalScoreCalculator.afterListVariableElementUnassigned(variableDescriptor.getVariableName(), element);
        super.afterListVariableElementUnassigned(variableDescriptor, element);
    }

    @Override
    public void beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        incrementalScoreCalculator.beforeListVariableChanged(entity, variableDescriptor.getVariableName(), fromIndex, toIndex);
        super.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        incrementalScoreCalculator.afterListVariableChanged(entity, variableDescriptor.getVariableName(), fromIndex, toIndex);
        super.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public void beforeEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        super.beforeEntityRemoved(entityDescriptor, entity);
    }

    @Override
    public void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        resetWorkingSolutionAndMaps(workingSolution);
        super.afterEntityRemoved(entityDescriptor, entity);
    }

    // ************************************************************************
    // Problem fact add/change/remove methods
    // ************************************************************************

    @Override
    public void beforeProblemFactAdded(Object problemFact) {
        super.beforeProblemFactAdded(problemFact);
    }

    @Override
    public void afterProblemFactAdded(Object problemFact) {
        resetWorkingSolutionAndMaps(workingSolution);
        super.afterProblemFactAdded(problemFact);
    }

    @Override
    public void beforeProblemPropertyChanged(Object problemFactOrEntity) {
        super.beforeProblemPropertyChanged(problemFactOrEntity);
    }

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {
        resetWorkingSolutionAndMaps(workingSolution);
        super.afterProblemPropertyChanged(problemFactOrEntity);
    }

    @Override
    public void beforeProblemFactRemoved(Object problemFact) {
        super.beforeProblemFactRemoved(problemFact);
    }

    @Override
    public void afterProblemFactRemoved(Object problemFact) {
        resetWorkingSolutionAndMaps(workingSolution);
        super.afterProblemFactRemoved(problemFact);
    }

    @Override
    public ConstraintMatchRegistration<Score_> registerConstraintMatch(ConstraintRef constraintRef, Score_ score,
            ConstraintJustification justification) {
        if (!constraintMatchEnabled) {
            throw new IllegalStateException(
                    "Cannot register constraint match (%s) when constraint matching (%s) is disabled in the constructor."
                            .formatted(constraintRef, constraintMatchPolicy));
        }
        var total = constraintMatchTotalMap.get(constraintRef);
        if (total == null) {
            total = new ConstraintMatchTotal<>(constraintRef, score.zero());
            constraintMatchTotalMap.put(constraintRef, total);
        }

        var match = total.addConstraintMatch(justification, score);
        totalScore = totalScore.add(score);
        var effectiveTotal = total;
        var canceled = new MutableReference<>(false);
        return new DefaultConstraintMatchRegistration<>(match, () -> {
            if (Objects.requireNonNullElse(canceled.getValue(), false)) {
                throw new IllegalStateException("Constraint match (%s) can only be canceled once."
                        .formatted(match));
            }
            canceled.setValue(true);
            totalScore = totalScore.subtract(score);
            effectiveTotal.removeConstraintMatch(match);
        });
    }

    @Override
    public Score_ totalScore() {
        return totalScore;
    }

    private record DefaultConstraintMatchRegistration<Score_ extends Score<Score_>>(
            ConstraintMatch<Score_> constraintMatch,
            Runnable undo)
            implements
                ConstraintMatchRegistration<Score_> {

        @Override
        public ConstraintRef constraintRef() {
            return constraintMatch.getConstraintRef();
        }

        @Override
        public Score_ score() {
            return constraintMatch.getScore();
        }

        @Override
        public ConstraintJustification justification() {
            return Objects.requireNonNull(constraintMatch.getJustification());
        }

        @Override
        public void cancel() {
            undo.run();
        }
    }

    @NullMarked
    public static final class Builder<Solution_, Score_ extends Score<Score_>>
            extends
            AbstractScoreDirectorBuilder<Solution_, Score_, IncrementalScoreDirectorFactory<Solution_, Score_>, Builder<Solution_, Score_>> {

        private @Nullable IncrementalScoreCalculator<Solution_, Score_> incrementalScoreCalculator;

        public Builder(IncrementalScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory) {
            super(scoreDirectorFactory);
        }

        public Builder<Solution_, Score_>
                withIncrementalScoreCalculator(IncrementalScoreCalculator<Solution_, Score_> incrementalScoreCalculator) {
            this.incrementalScoreCalculator = incrementalScoreCalculator;
            return withConstraintMatchPolicy(constraintMatchPolicy); // Ensure the policy is correct for the calculator.
        }

        @Override
        public Builder<Solution_, Score_> withConstraintMatchPolicy(ConstraintMatchPolicy constraintMatchPolicy) {
            return super.withConstraintMatchPolicy(determineCorrectPolicy(constraintMatchPolicy, incrementalScoreCalculator));
        }

        @Override
        public IncrementalScoreDirector<Solution_, Score_> build() {
            return new IncrementalScoreDirector<>(this);
        }

        private static ConstraintMatchPolicy determineCorrectPolicy(ConstraintMatchPolicy constraintMatchPolicy,
                @Nullable IncrementalScoreCalculator<?, ?> incrementalScoreCalculator) {
            if (incrementalScoreCalculator == null) {
                return ConstraintMatchPolicy.DISABLED;
            }
            return incrementalScoreCalculator instanceof AnalyzableIncrementalScoreCalculator<?, ?>
                    ? constraintMatchPolicy
                    : ConstraintMatchPolicy.DISABLED;
        }
    }

}
