package ai.timefold.solver.core.impl.score.director.stream;

import java.util.Map;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;

import org.jspecify.annotations.NullMarked;

/**
 * FP streams implementation of {@link ScoreDirector}, which only recalculates the {@link Score}
 * of the part of the {@link PlanningSolution working solution} that changed,
 * instead of the going through the entire {@link PlanningSolution}. This is incremental calculation, which is fast.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see ScoreDirector
 */
public final class BavetConstraintStreamScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, BavetConstraintStreamScoreDirectorFactory<Solution_, Score_>> {

    private final boolean derived;
    private BavetConstraintSession<Score_> session;

    private BavetConstraintStreamScoreDirector(
            BavetConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory, boolean lookUpEnabled,
            ConstraintMatchPolicy constraintMatchPolicy, boolean expectShadowVariablesInCorrectState, boolean derived) {
        super(scoreDirectorFactory, lookUpEnabled, constraintMatchPolicy,
                expectShadowVariablesInCorrectState);
        this.derived = derived;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    /**
     * The function is exclusively available for the Bavet score director, and its use must be approached with caution.
     * The primary purpose of this method is
     * to enable the {@code ConstraintVerifier}
     * to ignore events related to shadow variables when testing constraints that do not rely on them.
     *
     * @see AbstractScoreDirector#clearVariableListenerEvents()
     */
    public void clearShadowVariablesListenerQueue() {
        clearVariableListenerEvents();
    }

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        session = scoreDirectorFactory.newSession(workingSolution, constraintMatchPolicy, derived);
        session.initialize(workingSolution, this.getSupplyManager());
        super.setWorkingSolution(workingSolution, session::insert);
    }

    @Override
    public InnerScore<Score_> calculateScore() {
        variableListenerSupport.assertNotificationQueuesAreEmpty();
        var score = session.calculateScore();
        setCalculatedScore(score);
        return new InnerScore<>(score, -getWorkingInitScore());
    }

    @Override
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        if (!constraintMatchPolicy.isEnabled()) {
            throw new IllegalStateException("When constraint matching is disabled, this method should not be called.");
        } else if (workingSolution == null) {
            throw new IllegalStateException(
                    "The method setWorkingSolution() must be called before the method getConstraintMatchTotalMap().");
        }
        return session.getConstraintMatchTotalMap();
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        if (!constraintMatchPolicy.isJustificationEnabled()) {
            throw new IllegalStateException(
                    "When constraint matching with justifications is disabled, this method should not be called.");
        } else if (workingSolution == null) {
            throw new IllegalStateException(
                    "The method setWorkingSolution() must be called before the method getIndictmentMap().");
        }
        return session.getIndictmentMap();
    }

    @Override
    public boolean requiresFlushing() {
        return true; // Tuple refresh happens during score calculation.
    }

    @Override
    public void close() {
        super.close();
        if (session != null) {
            session.close();
            session = null;
        }
    }

    // ************************************************************************
    // Entity/variable add/change/remove methods
    // ************************************************************************

    // public void beforeEntityAdded(EntityDescriptor entityDescriptor, Object entity) // Do nothing

    @Override
    public void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("The entity (%s) cannot be added to the ScoreDirector.".formatted(entity));
        }
        if (!getSolutionDescriptor().hasEntityDescriptor(entity.getClass())) {
            throw new IllegalArgumentException("The entity (%s) of class (%s) is not a configured @%s.".formatted(entity,
                    entity.getClass(), PlanningEntity.class.getSimpleName()));
        }
        session.insert(entity);
        super.afterEntityAdded(entityDescriptor, entity);
    }

    // public void beforeVariableChanged(VariableDescriptor variableDescriptor, Object entity) // Do nothing

    @Override
    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        session.update(entity);
        super.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        session.update(entity);
        super.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    // public void beforeEntityRemoved(EntityDescriptor entityDescriptor, Object entity) // Do nothing

    @Override
    public void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        session.retract(entity);
        super.afterEntityRemoved(entityDescriptor, entity);
    }

    // ************************************************************************
    // Problem fact add/change/remove methods
    // ************************************************************************

    // public void beforeProblemFactAdded(Object problemFact) // Do nothing

    @Override
    public void afterProblemFactAdded(Object problemFact) {
        if (problemFact == null) {
            throw new IllegalArgumentException(
                    "The problemFact (%s) cannot be added to the ScoreDirector.".formatted(problemFact));
        }
        session.insert(problemFact);
        super.afterProblemFactAdded(problemFact);
    }

    // public void beforeProblemPropertyChanged(Object problemFactOrEntity) // Do nothing

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {
        session.update(problemFactOrEntity);
        super.afterProblemPropertyChanged(problemFactOrEntity);
    }

    // public void beforeProblemFactRemoved(Object problemFact) // Do nothing

    @Override
    public void afterProblemFactRemoved(Object problemFact) {
        session.retract(problemFact);
        super.afterProblemFactRemoved(problemFact);
    }

    /**
     * Exposed for debugging purposes, so that we can hook into it from tests and while reproducing issues.
     * 
     * @return null before first {@link #setWorkingSolution(Object)} or after {@link #close()}.
     */
    @SuppressWarnings("unused")
    public BavetConstraintSession<Score_> getSession() {
        return session;
    }

    @Override
    public boolean isDerived() {
        return derived;
    }

    @NullMarked
    public static final class Builder<Solution_, Score_ extends Score<Score_>>
            extends
            AbstractScoreDirectorBuilder<Solution_, Score_, BavetConstraintStreamScoreDirectorFactory<Solution_, Score_>, Builder<Solution_, Score_>> {

        public Builder(BavetConstraintStreamScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory) {
            super(scoreDirectorFactory);
        }

        @Override
        public BavetConstraintStreamScoreDirector<Solution_, Score_> build() {
            return new BavetConstraintStreamScoreDirector<>(scoreDirectorFactory, lookUpEnabled, constraintMatchPolicy,
                    expectShadowVariablesInCorrectState, false);
        }

        @Override
        public AbstractScoreDirector<Solution_, Score_, BavetConstraintStreamScoreDirectorFactory<Solution_, Score_>>
                buildDerived() {
            return new BavetConstraintStreamScoreDirector<>(scoreDirectorFactory, lookUpEnabled, constraintMatchPolicy,
                    expectShadowVariablesInCorrectState, true);
        }
    }

}
