package ai.timefold.solver.core.impl.score.director.incremental;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;

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
public final class IncrementalScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, IncrementalScoreDirectorFactory<Solution_, Score_>> {

    private final IncrementalScoreCalculator<Solution_, Score_> incrementalScoreCalculator;

    private IncrementalScoreDirector(IncrementalScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            boolean lookUpEnabled, ConstraintMatchPolicy constraintMatchPolicy, boolean expectShadowVariablesInCorrectState,
            IncrementalScoreCalculator<Solution_, Score_> incrementalScoreCalculator) {
        super(scoreDirectorFactory, lookUpEnabled, determineCorrectPolicy(constraintMatchPolicy, incrementalScoreCalculator),
                expectShadowVariablesInCorrectState);
        this.incrementalScoreCalculator = Objects.requireNonNull(incrementalScoreCalculator);
    }

    private static ConstraintMatchPolicy determineCorrectPolicy(ConstraintMatchPolicy constraintMatchPolicy,
            IncrementalScoreCalculator<?, ?> incrementalScoreCalculator) {
        if (incrementalScoreCalculator instanceof ConstraintMatchAwareIncrementalScoreCalculator<?, ?>) {
            return constraintMatchPolicy;
        } else {
            return ConstraintMatchPolicy.DISABLED;
        }
    }

    public IncrementalScoreCalculator<Solution_, Score_> getIncrementalScoreCalculator() {
        return incrementalScoreCalculator;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        super.setWorkingSolution(workingSolution, null);
        if (incrementalScoreCalculator instanceof ConstraintMatchAwareIncrementalScoreCalculator) {
            ((ConstraintMatchAwareIncrementalScoreCalculator<Solution_, ?>) incrementalScoreCalculator)
                    .resetWorkingSolution(workingSolution, getConstraintMatchPolicy().isEnabled());
        } else {
            incrementalScoreCalculator.resetWorkingSolution(workingSolution);
        }
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
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        if (!constraintMatchPolicy.isEnabled()) {
            throw new IllegalStateException("When constraint matching (" + constraintMatchPolicy
                    + ") is disabled in the constructor, this method should not be called.");
        }
        // Notice that we don't trigger the variable listeners
        return ((ConstraintMatchAwareIncrementalScoreCalculator<Solution_, Score_>) incrementalScoreCalculator)
                .getConstraintMatchTotals()
                .stream()
                .collect(toMap(c -> c.getConstraintRef().constraintId(), identity()));
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        if (!constraintMatchPolicy.isJustificationEnabled()) {
            throw new IllegalStateException("When constraint matching with justifications (" + constraintMatchPolicy
                    + ") is disabled in the constructor, this method should not be called.");
        }
        Map<Object, Indictment<Score_>> incrementalIndictmentMap =
                ((ConstraintMatchAwareIncrementalScoreCalculator<Solution_, Score_>) incrementalScoreCalculator)
                        .getIndictmentMap();
        if (incrementalIndictmentMap != null) {
            return incrementalIndictmentMap;
        }
        Map<Object, Indictment<Score_>> indictmentMap = new LinkedHashMap<>();
        Score_ zeroScore = getScoreDefinition().getZeroScore();
        Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap = getConstraintMatchTotalMap();
        for (ConstraintMatchTotal<Score_> constraintMatchTotal : constraintMatchTotalMap.values()) {
            for (ConstraintMatch<Score_> constraintMatch : constraintMatchTotal.getConstraintMatchSet()) {
                constraintMatch.getIndictedObjectList()
                        .stream()
                        .filter(Objects::nonNull)
                        .distinct() // One match might have the same indictment twice.
                        .forEach(fact -> {
                            DefaultIndictment<Score_> indictment =
                                    (DefaultIndictment<Score_>) indictmentMap.computeIfAbsent(fact,
                                            k -> new DefaultIndictment<>(fact, zeroScore));
                            indictment.addConstraintMatch(constraintMatch);
                        });
            }
        }
        return indictmentMap;
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
        incrementalScoreCalculator.beforeEntityAdded(entity);
        super.beforeEntityAdded(entityDescriptor, entity);
    }

    @Override
    public void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        incrementalScoreCalculator.afterEntityAdded(entity);
        super.afterEntityAdded(entityDescriptor, entity);
    }

    @Override
    public void beforeVariableChanged(VariableDescriptor variableDescriptor, Object entity) {
        incrementalScoreCalculator.beforeVariableChanged(entity, variableDescriptor.getVariableName());
        super.beforeVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void afterVariableChanged(VariableDescriptor variableDescriptor, Object entity) {
        incrementalScoreCalculator.afterVariableChanged(entity, variableDescriptor.getVariableName());
        super.afterVariableChanged(variableDescriptor, entity);
    }

    // TODO Add support for list variable (https://issues.redhat.com/browse/PLANNER-2711).

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
        incrementalScoreCalculator.beforeEntityRemoved(entity);
        super.beforeEntityRemoved(entityDescriptor, entity);
    }

    @Override
    public void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        incrementalScoreCalculator.afterEntityRemoved(entity);
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
        incrementalScoreCalculator.resetWorkingSolution(workingSolution); // TODO do not nuke it
        super.afterProblemFactAdded(problemFact);
    }

    @Override
    public void beforeProblemPropertyChanged(Object problemFactOrEntity) {
        super.beforeProblemPropertyChanged(problemFactOrEntity);
    }

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {
        incrementalScoreCalculator.resetWorkingSolution(workingSolution); // TODO do not nuke it
        super.afterProblemPropertyChanged(problemFactOrEntity);
    }

    @Override
    public void beforeProblemFactRemoved(Object problemFact) {
        super.beforeProblemFactRemoved(problemFact);
    }

    @Override
    public void afterProblemFactRemoved(Object problemFact) {
        incrementalScoreCalculator.resetWorkingSolution(workingSolution); // TODO do not nuke it
        super.afterProblemFactRemoved(problemFact);
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
            return this;
        }

        @Override
        public IncrementalScoreDirector<Solution_, Score_> build() {
            return new IncrementalScoreDirector<>(scoreDirectorFactory, lookUpEnabled, constraintMatchPolicy,
                    expectShadowVariablesInCorrectState, incrementalScoreCalculator);
        }

    }

}
