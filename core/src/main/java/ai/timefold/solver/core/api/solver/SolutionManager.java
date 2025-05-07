package ai.timefold.solver.core.api.solver;

import static ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy.FETCH_ALL;
import static ai.timefold.solver.core.api.solver.SolutionUpdatePolicy.UPDATE_ALL;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.domain.variable.ShadowVariableUpdateHelper;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A stateless service to help calculate {@link Score}, {@link ConstraintMatchTotal},
 * {@link Indictment}, etc.
 * <p>
 * To create a {@link SolutionManager} instance, use {@link #create(SolverFactory)}.
 * <p>
 * These methods are thread-safe unless explicitly stated otherwise.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the actual score type
 */
public interface SolutionManager<Solution_, Score_ extends Score<Score_>> {

    // ************************************************************************
    // Static creation methods: SolverFactory
    // ************************************************************************

    /**
     * Uses a {@link SolverFactory} to build a {@link SolutionManager}.
     *
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     */
    static <Solution_, Score_ extends Score<Score_>> @NonNull SolutionManager<Solution_, Score_> create(
            @NonNull SolverFactory<Solution_> solverFactory) {
        return new DefaultSolutionManager<>(solverFactory);
    }

    /**
     * Uses a {@link SolverManager} to build a {@link SolutionManager}.
     *
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}
     */
    static <Solution_, Score_ extends Score<Score_>, ProblemId_> @NonNull SolutionManager<Solution_, Score_> create(
            @NonNull SolverManager<Solution_, ProblemId_> solverManager) {
        return new DefaultSolutionManager<>(solverManager);
    }

    // ************************************************************************
    // Interface methods
    // ************************************************************************

    /**
     * As defined by {@link #update(Object, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     *
     */
    default @Nullable Score_ update(@NonNull Solution_ solution) {
        return update(solution, UPDATE_ALL);
    }

    /**
     * Updates the given solution according to the {@link SolutionUpdatePolicy}.
     *
     * @param solutionUpdatePolicy if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return possibly null if already null and {@link SolutionUpdatePolicy} didn't cause its update
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     * @see #updateShadowVariables(Object) Alternative logic that does not require a solver configuration to update shadow
     *      variables
     */
    @Nullable
    Score_ update(@NonNull Solution_ solution, @NonNull SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * This method updates all shadow variables at the entity level,
     * simplifying the requirements of {@link SolutionManager#update(Object)}.
     * Unlike the latter method,
     * it does not require the complete configuration necessary to obtain an instance of {@link SolutionManager}.
     * <p>
     * However, this method requires that the entity does not define any shadow variables that rely on listeners,
     * as that would require a complete solution.
     *
     * @param solutionClass the solution class
     * @param entities all entities to be updated
     */
    static <Solution_> void updateShadowVariables(@NonNull Class<Solution_> solutionClass,
            @NonNull Object... entities) {
        Objects.requireNonNull(solutionClass);
        Objects.requireNonNull(entities);
        if (entities.length == 0) {
            throw new IllegalArgumentException("The entity array cannot be empty.");
        }
        ShadowVariableUpdateHelper.<Solution_> create().updateShadowVariables(solutionClass, entities);
    }

    /**
     * Same as {@link #updateShadowVariables(Class, Object...)},
     * this method accepts a solution rather than a list of entities.
     *
     * @param solution the solution
     */
    static <Solution_> void updateShadowVariables(@NonNull Solution_ solution) {
        Objects.requireNonNull(solution);
        ShadowVariableUpdateHelper.<Solution_> create().updateShadowVariables(solution);
    }

    /**
     * As defined by {@link #explain(Object, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     */
    default @NonNull ScoreExplanation<Solution_, Score_> explain(@NonNull Solution_ solution) {
        return explain(solution, UPDATE_ALL);
    }

    /**
     * Calculates and retrieves {@link ConstraintMatchTotal}s and {@link Indictment}s necessary for describing the
     * quality of a particular solution.
     * For a simplified, faster and JSON-friendly alternative, see {@link #analyze(Object)}}.
     *
     * @param solutionUpdatePolicy if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @throws IllegalStateException when constraint matching is disabled or not supported by the underlying score
     *         calculator, such as {@link EasyScoreCalculator}.
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    @NonNull
    ScoreExplanation<Solution_, Score_> explain(@NonNull Solution_ solution,
            @NonNull SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #analyze(Object, ScoreAnalysisFetchPolicy, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL} and {@link ScoreAnalysisFetchPolicy#FETCH_ALL}.
     */
    default @NonNull ScoreAnalysis<Score_> analyze(@NonNull Solution_ solution) {
        return analyze(solution, FETCH_ALL, UPDATE_ALL);
    }

    /**
     * As defined by {@link #analyze(Object, ScoreAnalysisFetchPolicy, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     */
    default @NonNull ScoreAnalysis<Score_> analyze(@NonNull Solution_ solution, @NonNull ScoreAnalysisFetchPolicy fetchPolicy) {
        return analyze(solution, fetchPolicy, UPDATE_ALL);
    }

    /**
     * Calculates and retrieves information about which constraints contributed to the solution's score.
     * This is a faster, JSON-friendly version of {@link #explain(Object)}.
     *
     * @param solution must be fully initialized otherwise an exception is thrown
     * @param fetchPolicy if unsure, pick {@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}
     * @param solutionUpdatePolicy if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @throws IllegalStateException when constraint matching is disabled or not supported by the underlying score
     *         calculator, such as {@link EasyScoreCalculator}.
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    @NonNull
    ScoreAnalysis<Score_> analyze(@NonNull Solution_ solution, @NonNull ScoreAnalysisFetchPolicy fetchPolicy,
            @NonNull SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * Compute a difference between two solutions.
     * The difference will contain information about which entities's variables have changed,
     * which entities were added and which were removed.
     * <p>
     * Two instances of a planning entity or a variable value are considered equal if they {@link Object#equals(Object) equal}.
     * Instances of different classes are never considered equal, even if they share a common superclass.
     * For the correct operation of this method, make sure that
     * {@link Object#equals(Object) equals} and {@link Object#equals(Object) hashCode} honor their contract
     * and are mutually consistent.
     * <p>
     * <strong>This method is only offered as a preview feature.</strong>
     * There are no guarantees for backward compatibility;
     * its signature or the types it operates on and returns may change or be removed without prior notice,
     * although we will strive to avoid this as much as possible.
     * 
     * @param oldSolution The solution to use as a base for comparison.
     * @param newSolution The solution to compare against the base.
     * @return A diff object containing information about the differences between the two solutions.
     *         Entities from the old solution that are not present in the new solution will be marked as removed.
     *         Entities from the new solution that are not present in the old solution will be marked as added.
     *         Entities that are present in both solutions will be marked as changed if their variables differ,
     *         according to the equality rules described above.
     */
    @NonNull
    PlanningSolutionDiff<Solution_> diff(@NonNull Solution_ oldSolution, @NonNull Solution_ newSolution);

    /**
     * As defined by {@link #recommendAssignment(Object, Object, Function, ScoreAnalysisFetchPolicy)},
     * with {@link ScoreAnalysisFetchPolicy#FETCH_ALL}.
     */
    default <EntityOrElement_, Proposition_> @NonNull List<RecommendedAssignment<Proposition_, Score_>> recommendAssignment(
            @NonNull Solution_ solution, EntityOrElement_ evaluatedEntityOrElement,
            @NonNull Function<EntityOrElement_, @NonNull Proposition_> propositionFunction) {
        return recommendAssignment(solution, evaluatedEntityOrElement, propositionFunction, FETCH_ALL);
    }

    /**
     * Quickly runs through all possible options of assigning a given entity or element in a given solution,
     * and returns a list of recommendations sorted by score,
     * with most favorable score first.
     * The input solution must either be fully initialized,
     * or have a single entity or element unassigned.
     *
     * <p>
     * For problems with only basic planning variables or with chained planning variables,
     * the fitted element is a planning entity of the problem.
     * Each available planning value will be tested by setting it to the planning variable in question.
     * For problems with a list variable,
     * the evaluated element may be a shadow entity,
     * and it will be tested in each position of the planning list variable.
     *
     * <p>
     * The score returned by {@link RecommendedAssignment#scoreAnalysisDiff()}
     * is the difference between the score of the solution before and after fitting.
     * Every recommendation will be in a state as if the solution was never changed;
     * if it references entities,
     * none of their genuine planning variables or shadow planning variables will be initialized.
     * The input solution will be unchanged.
     *
     * <p>
     * This method does not call local search,
     * it runs a fast greedy algorithm instead.
     * The construction heuristic configuration from the solver configuration is used.
     * If not present, the default construction heuristic configuration is used.
     * This means that the API will fail if the solver config requires custom initialization phase.
     * In this case, it will fail either directly by throwing an exception,
     * or indirectly by not providing correct data.
     *
     * <p>
     * When an element is tested,
     * a score is calculated over the entire solution with the element in place,
     * also called a placement.
     * The proposition function is also called at that time,
     * allowing the user to extract any information from the current placement;
     * the extracted information is called the proposition.
     * After the proposition is extracted,
     * the solution is returned to its original state,
     * resetting all changes made by the fitting.
     * This has a major consequence for the proposition, if it is a planning entity:
     * planning entities contain live data in their planning variables,
     * and that data will be erased when the next placement is tested for fit.
     * In this case,
     * the proposition function needs to make defensive copies of everything it wants to return,
     * such as values of shadow variables etc.
     *
     * <p>
     * Example: Consider a planning entity Shift, with a variable "employee".
     * Let's assume we have two employees to test for fit, Ann and Bob,
     * and a single Shift instance to fit them into, {@code mondayShift}.
     * The proposition function will be called twice,
     * once as {@code mondayShift@Ann} and once as {@code mondayShift@Bob}.
     * Let's assume the proposition function returns the Shift instance in its entirety.
     * This is what will happen:
     *
     * <ol>
     * <li>Calling propositionFunction on {@code mondayShift@Ann} results in proposition P1: {@code mondayShift@Ann}</li>
     * <li>Placement is cleared, {@code mondayShift@Bob} is now tested for fit.</li>
     * <li>Calling propositionFunction on {@code mondayShift@Bob} results in proposition P2: {@code mondayShift@Bob}</li>
     * <li>Proposition P1 and P2 are now both the same {@code mondayShift},
     * which means Bob is now assigned to both of them.
     * This is because both propositions operate on the same entity,
     * and therefore share the same state.
     * </li>
     * <li>The placement is then cleared again,
     * both elements have been tested,
     * and solution is returned to its original order.
     * The propositions are then returned to the user,
     * who notices that both P1 and P2 are {@code mondayShift@null}.
     * This is because they shared state,
     * and the original state of the solution was for Shift to be unassigned.
     * </li>
     * </ol>
     * <p>
     * If instead the proposition function returned Ann and Bob directly, the immutable planning variables,
     * this problem would have been avoided.
     * Alternatively, the proposition function could have returned a defensive copy of the Shift.
     *
     * @param solution for basic variable, must be fully initialized or have a single entity unassigned.
     *        For list variable, all values must be assigned to some list, with the optional exception of one.
     * @param evaluatedEntityOrElement must be part of the solution.
     *        For basic variable, it is a planning entity and may have one or more variables unassigned.
     *        For list variable, it is a shadow entity and need not be present in any list variable.
     *        {@link ScoreAnalysisFetchPolicy#FETCH_ALL} will include more data within {@link RecommendedAssignment},
     *        but will also take more time to gather that data.
     * @param <EntityOrElement_> generic type of the evaluated entity or element
     * @param <Proposition_> generic type of the user-provided proposition;
     *        if it is a planning entity, it is recommended
     *        to make a defensive copy inside the proposition function.
     * @return sorted from best to worst;
     *         designed to be JSON-friendly, see {@link RecommendedAssignment} Javadoc for more.
     * @see PlanningEntity More information about genuine and shadow planning entities.
     */
    <EntityOrElement_, Proposition_> @NonNull List<RecommendedAssignment<Proposition_, Score_>> recommendAssignment(
            @NonNull Solution_ solution, @NonNull EntityOrElement_ evaluatedEntityOrElement,
            @NonNull Function<EntityOrElement_, Proposition_> propositionFunction,
            @NonNull ScoreAnalysisFetchPolicy fetchPolicy);

    /**
     * As defined by {@link #recommendAssignment(Object, Object, Function, ScoreAnalysisFetchPolicy)},
     * with {@link ScoreAnalysisFetchPolicy#FETCH_ALL}.
     *
     * @deprecated Prefer {@link #recommendAssignment(Object, Object, Function, ScoreAnalysisFetchPolicy)}.
     */
    @Deprecated(forRemoval = true, since = "1.15.0")
    default <EntityOrElement_, Proposition_> List<RecommendedFit<Proposition_, Score_>> recommendFit(Solution_ solution,
            EntityOrElement_ fittedEntityOrElement, Function<EntityOrElement_, Proposition_> propositionFunction) {
        return recommendFit(solution, fittedEntityOrElement, propositionFunction, FETCH_ALL);
    }

    /**
     * As defined by {@link #recommendAssignment(Object, Object, Function, ScoreAnalysisFetchPolicy)}.
     *
     * @deprecated Prefer {@link #recommendAssignment(Object, Object, Function, ScoreAnalysisFetchPolicy)}.
     */
    @Deprecated(forRemoval = true, since = "1.15.0")
    <EntityOrElement_, Proposition_> List<RecommendedFit<Proposition_, Score_>> recommendFit(Solution_ solution,
            EntityOrElement_ fittedEntityOrElement, Function<EntityOrElement_, Proposition_> propositionFunction,
            ScoreAnalysisFetchPolicy fetchPolicy);

}
