package ai.timefold.solver.core.api.solver;

import static ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy.FETCH_ALL;
import static ai.timefold.solver.core.api.solver.SolutionUpdatePolicy.UPDATE_ALL;

import java.util.List;
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
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;

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
     * @param solverFactory never null
     * @return never null
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     */
    static <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_> create(
            SolverFactory<Solution_> solverFactory) {
        return new DefaultSolutionManager<>(solverFactory);
    }

    /**
     * Uses a {@link SolverManager} to build a {@link SolutionManager}.
     *
     * @param solverManager never null
     * @return never null
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}
     */
    static <Solution_, Score_ extends Score<Score_>, ProblemId_> SolutionManager<Solution_, Score_> create(
            SolverManager<Solution_, ProblemId_> solverManager) {
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
    default Score_ update(Solution_ solution) {
        return update(solution, UPDATE_ALL);
    }

    /**
     * Updates the given solution according to the {@link SolutionUpdatePolicy}.
     *
     * @param solution never null
     * @param solutionUpdatePolicy never null; if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return possibly null if already null and {@link SolutionUpdatePolicy} didn't cause its update
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    Score_ update(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #explain(Object, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     */
    default ScoreExplanation<Solution_, Score_> explain(Solution_ solution) {
        return explain(solution, UPDATE_ALL);
    }

    /**
     * Calculates and retrieves {@link ConstraintMatchTotal}s and {@link Indictment}s necessary for describing the
     * quality of a particular solution.
     * For a simplified, faster and JSON-friendly alternative, see {@link #analyze(Object)}}.
     *
     * @param solution never null
     * @param solutionUpdatePolicy never null; if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return never null
     * @throws IllegalStateException when constraint matching is disabled or not supported by the underlying score
     *         calculator, such as {@link EasyScoreCalculator}.
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    ScoreExplanation<Solution_, Score_> explain(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #analyze(Object, ScoreAnalysisFetchPolicy, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL} and {@link ScoreAnalysisFetchPolicy#FETCH_ALL}.
     */
    default ScoreAnalysis<Score_> analyze(Solution_ solution) {
        return analyze(solution, FETCH_ALL, UPDATE_ALL);
    }

    /**
     * As defined by {@link #analyze(Object, ScoreAnalysisFetchPolicy, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     */
    default ScoreAnalysis<Score_> analyze(Solution_ solution, ScoreAnalysisFetchPolicy fetchPolicy) {
        return analyze(solution, fetchPolicy, UPDATE_ALL);
    }

    /**
     * Calculates and retrieves information about which constraints contributed to the solution's score.
     * This is a faster, JSON-friendly version of {@link #explain(Object)}.
     *
     * @param solution never null, must be fully initialized otherwise an exception is thrown
     * @param fetchPolicy never null; if unsure, pick {@link ScoreAnalysisFetchPolicy#FETCH_ALL}
     * @param solutionUpdatePolicy never null; if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return never null
     * @throws IllegalStateException when constraint matching is disabled or not supported by the underlying score
     *         calculator, such as {@link EasyScoreCalculator}.
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    ScoreAnalysis<Score_> analyze(Solution_ solution, ScoreAnalysisFetchPolicy fetchPolicy,
            SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #recommendFit(Object, Object, Function, ScoreAnalysisFetchPolicy)},
     * with {@link ScoreAnalysisFetchPolicy#FETCH_ALL}.
     */
    default <EntityOrElement_, Proposition_> List<RecommendedFit<Proposition_, Score_>> recommendFit(Solution_ solution,
            EntityOrElement_ fittedEntityOrElement, Function<EntityOrElement_, Proposition_> propositionFunction) {
        return recommendFit(solution, fittedEntityOrElement, propositionFunction, FETCH_ALL);
    }

    /**
     * Quickly runs through all possible options of fitting a given entity or element in a given solution,
     * and returns a list of recommendations sorted by score,
     * with most favorable score first.
     * Think of this method as a construction heuristic
     * which shows you all the options to initialize the solution.
     * The input solution must be fully initialized
     * except for one entity or element, the one to be fitted.
     *
     * <p>
     * For problems with only basic planning variables or with chained planning variables,
     * the fitted element is a planning entity of the problem.
     * Each available planning value will be tested for fit
     * by setting it to the planning variable in question.
     * For problems with a list variable,
     * the fitted element may be a shadow entity,
     * and it will be tested for fit in each position of the planning list variable.
     *
     * <p>
     * The score returned by {@link RecommendedFit#scoreAnalysisDiff()}
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
     * When an element is tested for fit,
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
     * both elements have been tested for fit,
     * and solution is returned to its original order.
     * The propositions are then returned to the user,
     * who notices that both P1 and P2 are {@code mondayShift@null}.
     * This is because they shared state,
     * and the original state of the solution was for Shift to be unassigned.
     * </li>
     * </ol>
     *
     * If instead the proposition function returned Ann and Bob directly, the immutable planning variables,
     * this problem would have been avoided.
     * Alternatively, the proposition function could have returned a defensive copy of the Shift.
     *
     * @param solution never null; must be fully initialized except for one entity or element
     * @param fittedEntityOrElement never null; must be part of the solution
     * @param propositionFunction never null
     * @param fetchPolicy never null;
     *        {@link ScoreAnalysisFetchPolicy#FETCH_ALL} will include more data within {@link RecommendedFit},
     *        but will also take more time to gather that data.
     * @return never null, sorted from best to worst;
     *         designed to be JSON-friendly, see {@link RecommendedFit} Javadoc for more.
     * @param <EntityOrElement_> generic type of the unassigned entity or element
     * @param <Proposition_> generic type of the user-provided proposition;
     *        if it is a planning entity, it is recommended
     *        to make a defensive copy inside the proposition function.
     * @see PlanningEntity More information about genuine and shadow planning entities.
     */
    <EntityOrElement_, Proposition_> List<RecommendedFit<Proposition_, Score_>> recommendFit(Solution_ solution,
            EntityOrElement_ fittedEntityOrElement, Function<EntityOrElement_, Proposition_> propositionFunction,
            ScoreAnalysisFetchPolicy fetchPolicy);

}
