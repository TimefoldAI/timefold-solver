package ai.timefold.solver.constraint.drl;

import java.util.Map;
import java.util.function.Function;

import ai.timefold.solver.constraint.drl.holder.AbstractScoreHolder;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;

import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.event.rule.RuleEventManager;

/**
 * Drools implementation of {@link ScoreDirector}, which directs the Rule Engine to calculate the {@link Score}
 * of the {@link PlanningSolution working solution}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see ScoreDirector
 */
public class DrlScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends AbstractScoreDirector<Solution_, Score_, DrlScoreDirectorFactory<Solution_, Score_>> {

    public static final String GLOBAL_SCORE_HOLDER_KEY = "scoreHolder";

    protected KieSession kieSession;
    protected AbstractScoreHolder<Score_> scoreHolder;

    public DrlScoreDirector(DrlScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory, boolean lookUpEnabled,
            boolean constraintMatchEnabledPreference, boolean expectShadowVariablesInCorrectState) {
        super(scoreDirectorFactory, lookUpEnabled, constraintMatchEnabledPreference, expectShadowVariablesInCorrectState);
    }

    public KieSession getKieSession() {
        return kieSession;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        super.setWorkingSolution(workingSolution);
        resetKieSession();
    }

    private void resetKieSession() {
        if (kieSession != null) {
            kieSession.dispose();
        }
        kieSession = scoreDirectorFactory.newKieSession();
        ((RuleEventManager) kieSession).addEventListener(new TimefoldRuleEventListener());
        resetScoreHolder();
        // TODO Adjust when uninitialized entities from getWorkingFacts get added automatically too (and call afterEntityAdded)
        getSolutionDescriptor().visitAll(workingSolution, kieSession::insert);
    }

    private void resetScoreHolder() {
        scoreHolder = AbstractScoreHolder.buildScoreHolder(getScoreDefinition(), constraintMatchEnabledPreference);
        scoreDirectorFactory.getRuleToConstraintWeightExtractorMap().forEach(
                (Rule rule, Function<Solution_, Score_> extractor) -> {
                    Score_ constraintWeight = extractor.apply(workingSolution);
                    getSolutionDescriptor().validateConstraintWeight(rule.getPackageName(), rule.getName(), constraintWeight);
                    scoreHolder.configureConstraintWeight(rule, constraintWeight);
                });
        kieSession.setGlobal(GLOBAL_SCORE_HOLDER_KEY, scoreHolder);
    }

    @Override
    public Score_ calculateScore() {
        variableListenerSupport.assertNotificationQueuesAreEmpty();
        kieSession.fireAllRules();
        Score_ score = scoreHolder.extractScore(workingInitScore);
        setCalculatedScore(score);
        return score;
    }

    @Override
    public boolean isConstraintMatchEnabled() {
        return scoreHolder.isConstraintMatchEnabled();
    }

    @Override
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        if (workingSolution == null) {
            throw new IllegalStateException(
                    "The method setWorkingSolution() must be called before the method getConstraintMatchTotalMap().");
        }
        // Notice that we don't trigger the variable listeners
        kieSession.fireAllRules();
        return scoreHolder.getConstraintMatchTotalMap();
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        if (workingSolution == null) {
            throw new IllegalStateException(
                    "The method setWorkingSolution() must be called before the method getIndictmentMap().");
        }
        // Notice that we don't trigger the variable listeners
        kieSession.fireAllRules();
        return scoreHolder.getIndictmentMap();
    }

    @Override
    public boolean requiresFlushing() {
        return true; // Drools propagation queue is only flushed during fireAllRules().
    }

    @Override
    public void close() {
        super.close();
        if (kieSession != null) {
            kieSession.dispose();
            kieSession = null;
        }
    }

    // ************************************************************************
    // Entity/variable add/change/remove methods
    // ************************************************************************

    // public void beforeEntityAdded(EntityDescriptor entityDescriptor, Object entity) // Do nothing

    @Override
    public void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("The entity (" + entity + ") cannot be added to the ScoreDirector.");
        }
        if (!getSolutionDescriptor().hasEntityDescriptor(entity.getClass())) {
            throw new IllegalArgumentException("The entity (" + entity + ") of class (" + entity.getClass()
                    + ") is not a configured @" + PlanningEntity.class.getSimpleName() + ".");
        }
        if (kieSession.getFactHandle(entity) != null) {
            throw new IllegalArgumentException("The entity (" + entity
                    + ") was already added to this ScoreDirector."
                    + " Usually the cause is that that specific instance was already in your Solution's entities" +
                    " and you probably want to use before/afterVariableChanged() instead.");
        }
        kieSession.insert(entity);
        super.afterEntityAdded(entityDescriptor, entity);
    }

    // public void beforeVariableChanged(VariableDescriptor variableDescriptor, Object entity) // Do nothing

    @Override
    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        update(entity, variableDescriptor.getVariableName());
        super.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        update(entity, variableDescriptor.getVariableName());
        super.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    private void update(Object entity, String variableName) {
        FactHandle factHandle = kieSession.getFactHandle(entity);
        if (factHandle == null) {
            /*
             * Drools will eliminate all facts that do not match any rules.
             * Therefore fact handle can be null here even if the fact/entity was inserted before.
             * We could solve this by introducing a tracking map in the score director,
             * but in the interest of saving memory, we just re-insert the fact here.
             */
            kieSession.insert(entity);
        } else {
            kieSession.update(factHandle, entity, variableName);
        }
    }

    // public void beforeEntityRemoved(EntityDescriptor entityDescriptor, Object entity) // Do nothing

    @Override
    public void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        deleteIfExists(entity);
        super.afterEntityRemoved(entityDescriptor, entity);
    }

    private void deleteIfExists(Object factOrEntity) {
        FactHandle factHandle = kieSession.getFactHandle(factOrEntity);
        if (factHandle != null) {
            /*
             * Drools will eliminate all facts that do not match any rules.
             * Therefore fact handle can be null here even if the fact/entity was inserted before.
             * We could solve this by introducing a tracking map in the score director,
             * but in the interest of saving memory, we just ignore null fact handles.
             */
            kieSession.delete(factHandle);
        }
    }

    // ************************************************************************
    // Problem fact add/change/remove methods
    // ************************************************************************

    // public void beforeProblemFactAdded(Object problemFact) // Do nothing

    @Override
    public void afterProblemFactAdded(Object problemFact) {
        if (kieSession.getFactHandle(problemFact) != null) {
            throw new IllegalArgumentException("The problemFact (" + problemFact
                    + ") was already added to this ScoreDirector.\n"
                    + "Maybe that specific instance is already in the "
                    + PlanningSolution.class.getSimpleName() + "'s problem fact members ("
                    + getSolutionDescriptor().getProblemFactMemberAndProblemFactCollectionMemberNames() + ").\n"
                    + "Maybe use before/afterProblemPropertyChanged() instead of before/afterProblemFactAdded().");
        }
        kieSession.insert(problemFact);
        super.afterProblemFactAdded(problemFact);
    }

    // public void beforeProblemPropertyChanged(Object problemFactOrEntity) // Do nothing

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {
        FactHandle factHandle = kieSession.getFactHandle(problemFactOrEntity);
        if (factHandle == null) {
            /*
             * Drools will eliminate all facts that do not match any rules.
             * Therefore fact handle can be null here even if the fact/entity was inserted before.
             * We could solve this by introducing a tracking map in the score director,
             * but in the interest of saving memory, we just re-insert it here.
             */
            kieSession.insert(problemFactOrEntity);
        } else {
            kieSession.update(factHandle, problemFactOrEntity);
        }
        super.afterProblemPropertyChanged(problemFactOrEntity);
    }

    // public void beforeProblemFactRemoved(Object problemFact) // Do nothing

    @Override
    public void afterProblemFactRemoved(Object problemFact) {
        deleteIfExists(problemFact);
        super.afterProblemFactRemoved(problemFact);
    }

}
