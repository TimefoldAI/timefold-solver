package ai.timefold.solver.core.impl.score.director;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.move.MoveRepository;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 */
public interface InnerScoreDirector<Solution_, Score_ extends Score<Score_>>
        extends VariableDescriptorAwareScoreDirector<Solution_>, AutoCloseable {

    static <Score_ extends Score<Score_>> ConstraintAnalysis<Score_> getConstraintAnalysis(
            ConstraintMatchTotal<Score_> constraintMatchTotal, ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy) {
        return switch (scoreAnalysisFetchPolicy) {
            case FETCH_ALL -> {
                // Justification can not be null here, because they are enabled by FETCH_ALL.
                var deduplicatedConstraintMatchMap = constraintMatchTotal.getConstraintMatchSet()
                        .stream()
                        .collect(groupingBy(
                                c -> (ConstraintJustification) c.getJustification(),
                                toList()));
                var matchAnalyses = sumMatchesWithSameJustification(constraintMatchTotal, deduplicatedConstraintMatchMap);
                yield new ConstraintAnalysis<>(constraintMatchTotal.getConstraintRef(),
                        constraintMatchTotal.getConstraintWeight(), constraintMatchTotal.getScore(), matchAnalyses);
            }
            case FETCH_MATCH_COUNT -> new ConstraintAnalysis<>(constraintMatchTotal.getConstraintRef(),
                    constraintMatchTotal.getConstraintWeight(), constraintMatchTotal.getScore(), null,
                    constraintMatchTotal.getConstraintMatchCount());
            case FETCH_SHALLOW ->
                new ConstraintAnalysis<>(constraintMatchTotal.getConstraintRef(), constraintMatchTotal.getConstraintWeight(),
                        constraintMatchTotal.getScore(), null);
        };
    }

    private static <Score_ extends Score<Score_>> List<MatchAnalysis<Score_>>
            sumMatchesWithSameJustification(ConstraintMatchTotal<Score_> constraintMatchTotal,
                    Map<ConstraintJustification, List<ConstraintMatch<Score_>>> deduplicatedConstraintMatchMap) {
        return deduplicatedConstraintMatchMap.entrySet().stream()
                .map(entry -> {
                    var score = entry.getValue().stream()
                            .map(ConstraintMatch::getScore)
                            .reduce(constraintMatchTotal.getScore().zero(), Score::add);
                    return new MatchAnalysis<>(constraintMatchTotal.getConstraintRef(), score, entry.getKey());
                })
                .toList();
    }

    /**
     * The {@link PlanningSolution working solution} must never be the same instance as the
     * {@link PlanningSolution best solution}, it should be a (un)changed clone.
     *
     * @param workingSolution never null
     */
    void setWorkingSolution(Solution_ workingSolution);

    /**
     * Different phases may need different move repositories,
     * as they may be based on different sets of moves.
     * Therefore move repository cannot be injected at score director construction time.
     * 
     * A phase may not need a move repository at all,
     * such as construction heuristics, which currently does not support Move Streams.
     * 
     * Each phase is responsible for calling this method to set its repository,
     * and also calling it again at the end to null it out.
     */
    void setMoveRepository(@Nullable MoveRepository<Solution_> moveRepository);

    /**
     * Calculates the {@link Score} and updates the {@link PlanningSolution working solution} accordingly.
     *
     * @return never null, the {@link Score} of the {@link PlanningSolution working solution}
     */
    InnerScore<Score_> calculateScore();

    /**
     * @return {@link ConstraintMatchPolicy#ENABLED} if {@link #getConstraintMatchTotalMap()} and {@link #getIndictmentMap()}
     *         can be called.
     *         {@link ConstraintMatchPolicy#ENABLED_WITHOUT_JUSTIFICATIONS} if only the former can be called.
     *         {@link ConstraintMatchPolicy#DISABLED} if neither can be called.
     */
    ConstraintMatchPolicy getConstraintMatchPolicy();

    /**
     * Explains the {@link Score} of {@link #calculateScore()} by splitting it up per {@link Constraint}.
     * <p>
     * The sum of {@link ConstraintMatchTotal#getScore()} equals {@link #calculateScore()}.
     * <p>
     * Call {@link #calculateScore()} before calling this method,
     * unless that method has already been called since the last {@link PlanningVariable} changes.
     *
     * @return never null, the key is the constraintId
     *         (to create one, use {@link ConstraintRef#composeConstraintId(String, String)}).
     *         If a constraint is present in the problem but resulted in no matches,
     *         it will still be in the map with a {@link ConstraintMatchTotal#getConstraintMatchSet()} size of 0.
     * @throws IllegalStateException if {@link #getConstraintMatchPolicy()} returns {@link ConstraintMatchPolicy#DISABLED}.
     * @see #getIndictmentMap()
     */
    Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap();

    /**
     * Explains the impact of each planning entity or problem fact on the {@link Score}.
     * An {@link Indictment} is basically the inverse of a {@link ConstraintMatchTotal}:
     * it is a {@link Score} total for each {@link ConstraintMatch#getJustification() constraint justification}.
     * <p>
     * The sum of {@link ConstraintMatchTotal#getScore()} differs from {@link #calculateScore()}
     * because each {@link ConstraintMatch#getScore()} is counted
     * for each {@link ConstraintMatch#getJustification() constraint justification}.
     * <p>
     * Call {@link #calculateScore()} before calling this method,
     * unless that method has already been called since the last {@link PlanningVariable} changes.
     *
     * @return never null, the key is a {@link ProblemFactCollectionProperty problem fact} or a
     *         {@link PlanningEntity planning entity}
     * @throws IllegalStateException unless {@link #getConstraintMatchPolicy()} returns {@link ConstraintMatchPolicy#ENABLED}.
     * @see #getConstraintMatchTotalMap()
     */
    Map<Object, Indictment<Score_>> getIndictmentMap();

    /**
     * @return used to check {@link #isWorkingEntityListDirty(long)} later on
     */
    long getWorkingEntityListRevision();

    int getWorkingGenuineEntityCount();

    int getWorkingInitScore();

    void executeMove(Move<Solution_> move);

    /**
     * Executes a move, finds out its score, and immediately undoes it.
     *
     * @param move never null
     * @param assertMoveScoreFromScratch true will hurt performance
     * @return never null
     */
    InnerScore<Score_> executeTemporaryMove(Move<Solution_> move, boolean assertMoveScoreFromScratch);

    /**
     * @param expectedWorkingEntityListRevision an
     * @return true if the entityList might have a different set of instances now
     */
    boolean isWorkingEntityListDirty(long expectedWorkingEntityListRevision);

    boolean isWorkingSolutionInitialized();

    /**
     * Some score directors keep a set of changes
     * that they only apply when {@link #calculateScore()} is called.
     * Until that happens, this set accumulates and could possibly act as a memory leak.
     *
     * @return true if the score director can potentially cause a memory leak due to unflushed changes.
     */
    boolean requiresFlushing();

    /**
     * Inverse shadow variables have a fail-fast for cases
     * where the shadow variable doesn't actually point to its correct inverse.
     * This is very useful to pinpoint improperly initialized solutions.
     * <p>
     * However, {@link SolutionManager#update(Object)} exists precisely for the purpose of initializing solutions.
     * And when this API is used, the fail-fast must not be triggered as it is guaranteed and expected
     * that the inverse relationships will be wrong.
     * In fact, they will be null.
     * <p>
     * For this case and this case only, this method is allowed to return false.
     * All other cases must return true, otherwise a very valuable fail-fast is lost.
     *
     * @return false if the fail-fast on shadow variables should not be triggered
     */
    boolean expectShadowVariablesInCorrectState();

    /**
     * @return never null
     */
    ScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory();

    /**
     * @return never null
     */
    SolutionDescriptor<Solution_> getSolutionDescriptor();

    /**
     * @return never null
     */
    ScoreDefinition<Score_> getScoreDefinition();

    /**
     * Returns a planning clone of the solution,
     * which is not a shallow clone nor a deep clone nor a partition clone.
     *
     * @return never null, planning clone
     */
    default Solution_ cloneWorkingSolution() {
        return cloneSolution(getWorkingSolution());
    }

    /**
     * Returns a planning clone of the solution,
     * which is not a shallow clone nor a deep clone nor a partition clone.
     *
     * @param originalSolution never null
     * @return never null, planning clone
     */
    Solution_ cloneSolution(Solution_ originalSolution);

    /**
     * @return at least 0L
     */
    long getCalculationCount();

    void resetCalculationCount();

    void incrementCalculationCount();

    /**
     * @return never null
     */
    SupplyManager getSupplyManager();

    MoveDirector<Solution_, Score_> getMoveDirector();

    ListVariableStateSupply<Solution_> getListVariableStateSupply(ListVariableDescriptor<Solution_> variableDescriptor);

    InnerScoreDirector<Solution_, Score_> createChildThreadScoreDirector(ChildThreadType childThreadType);

    /**
     * Do not waste performance by propagating changes to step (or higher) mechanisms.
     *
     * @param allChangesWillBeUndoneBeforeStepEnds true if all changes will be undone
     */
    void setAllChangesWillBeUndoneBeforeStepEnds(boolean allChangesWillBeUndoneBeforeStepEnds);

    /**
     * Asserts that if the {@link Score} is calculated for the current {@link PlanningSolution working solution}
     * in the current {@link ScoreDirector} (with possibly incremental calculation residue),
     * it is equal to the parameter {@link Score expectedWorkingScore}.
     * <p>
     * Used to assert that skipping {@link #calculateScore()} (when the score is otherwise determined) is correct.
     *
     * @param expectedWorkingScore never null
     * @param completedAction sometimes null, when assertion fails then the completedAction's {@link Object#toString()}
     *        is included in the exception message
     */
    void assertExpectedWorkingScore(InnerScore<Score_> expectedWorkingScore, Object completedAction);

    /**
     * Asserts that if all {@link VariableListener}s are forcibly triggered,
     * and therefore all shadow variables are updated if needed,
     * that none of the shadow variables of the {@link PlanningSolution working solution} change,
     * Then also asserts that the {@link Score} calculated for the {@link PlanningSolution working solution} afterwards
     * is equal to the parameter {@link Score expectedWorkingScore}.
     * <p>
     * Used to assert that the shadow variables' state is consistent with the genuine variables' state.
     *
     * @param expectedWorkingScore never null
     * @param completedAction sometimes null, when assertion fails then the completedAction's {@link Object#toString()}
     *        is included in the exception message
     */
    void assertShadowVariablesAreNotStale(InnerScore<Score_> expectedWorkingScore, Object completedAction);

    /**
     * Asserts that if the {@link Score} is calculated for the current {@link PlanningSolution working solution}
     * in a fresh {@link ScoreDirector} (with no incremental calculation residue),
     * it is equal to the parameter {@link Score workingScore}.
     * <p>
     * Furthermore, if the assert fails, a score corruption analysis might be included in the exception message.
     *
     * @param workingScore never null
     * @param completedAction sometimes null, when assertion fails then the completedAction's {@link Object#toString()}
     *        is included in the exception message
     * @see ScoreDirectorFactory#assertScoreFromScratch
     */
    void assertWorkingScoreFromScratch(InnerScore<Score_> workingScore, Object completedAction);

    /**
     * Asserts that if the {@link Score} is calculated for the current {@link PlanningSolution working solution}
     * in a fresh {@link ScoreDirector} (with no incremental calculation residue),
     * it is equal to the parameter {@link Score predictedScore}.
     * <p>
     * Furthermore, if the assert fails, a score corruption analysis might be included in the exception message.
     *
     * @param predictedScore never null
     * @param completedAction sometimes null, when assertion fails then the completedAction's {@link Object#toString()}
     *        is included in the exception message
     * @see ScoreDirectorFactory#assertScoreFromScratch
     */
    void assertPredictedScoreFromScratch(InnerScore<Score_> predictedScore, Object completedAction);

    /**
     * Asserts that if the {@link Score} is calculated for the current {@link PlanningSolution working solution}
     * in the current {@link ScoreDirector} (with incremental calculation residue),
     * it is equal to the parameter {@link Score beforeMoveScore}.
     * <p>
     * Furthermore, if the assert fails, a score corruption analysis might be included in the exception message.
     *
     * @param move never null
     * @param beforeMoveScore never null
     */
    void assertExpectedUndoMoveScore(Move<Solution_> move, InnerScore<Score_> beforeMoveScore,
            SolverLifecyclePoint executionPoint);

    /**
     * Needs to be called after use because some implementations need to clean up their resources.
     */
    @Override
    void close();

    /**
     * Unlike {@link #triggerVariableListeners()} which only triggers notifications already in the queue,
     * this triggers every variable listener on every genuine variable.
     * This is useful in {@link SolutionManager#update(Object)} to fill in shadow variable values.
     */
    void forceTriggerVariableListeners();

    /**
     * A derived score director is created from a root score director.
     * The derived score director can be used to create separate* instances for use cases like multithreaded solving.
     */
    default boolean isDerived() {
        return false;
    }

    default ScoreAnalysis<Score_> buildScoreAnalysis(ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy) {
        var state = calculateScore();
        var constraintAnalysisMap = new TreeMap<ConstraintRef, ConstraintAnalysis<Score_>>();
        for (var constraintMatchTotal : getConstraintMatchTotalMap().values()) {
            var constraintAnalysis = getConstraintAnalysis(constraintMatchTotal, scoreAnalysisFetchPolicy);
            constraintAnalysisMap.put(constraintMatchTotal.getConstraintRef(), constraintAnalysis);
        }
        return new ScoreAnalysis<>(state.raw(), constraintAnalysisMap, state.isFullyAssigned());
    }

    /*
     * The following methods are copied here from ScoreDirector because they are deprecated there for removal.
     * They will only be supported on this type, which serves for internal use only,
     * as opposed to ScoreDirector, which is a public type.
     * This way, we can ensure that these methods are used correctly and in a safe manner.
     */

    default void beforeEntityAdded(Object entity) {
        beforeEntityAdded(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    void beforeEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity);

    default void afterEntityAdded(Object entity) {
        afterEntityAdded(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity);

    default void beforeEntityRemoved(Object entity) {
        beforeEntityRemoved(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    void beforeEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity);

    default void afterEntityRemoved(Object entity) {
        afterEntityRemoved(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity);

    void beforeProblemFactAdded(Object problemFact);

    void afterProblemFactAdded(Object problemFact);

    void beforeProblemPropertyChanged(Object problemFactOrEntity);

    void afterProblemPropertyChanged(Object problemFactOrEntity);

    void beforeProblemFactRemoved(Object problemFact);

    void afterProblemFactRemoved(Object problemFact);

}
