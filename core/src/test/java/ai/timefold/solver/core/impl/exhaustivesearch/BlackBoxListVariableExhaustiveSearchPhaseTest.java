package ai.timefold.solver.core.impl.exhaustivesearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchType;
import ai.timefold.solver.core.config.exhaustivesearch.NodeExplorationType;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;
import ai.timefold.solver.core.testdomain.list.sort.comparator.TestdataListSortableEntity;
import ai.timefold.solver.core.testdomain.list.sort.comparator.TestdataListSortableSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The test runs through all available configuration combinations related to ExhaustiveSearch and compares the results
 * with manually calculated data.
 * It uses list variables and tries to find the best solution for three values and entities.
 * <p>
 * A solution state is represented by a string containing three groups of characters representing the values of each entity.
 * Uninitialized entities are marked by {@code -} character,
 * e.g. {@code [1]--} means that the first entity includes value 1,
 * the second and third don't have a value and the score is {@code 0hard/-2soft}.
 * The hard score is reduced for every list with more than one value,
 * and the soft score is reduced if the list is empty, which means a solution of {@code [12]--} has the score of
 * {@code -1hard/-2soft}.
 * <p>
 * The solver will not create a separate step to analyze a complete solution such as {@code [2][1,2]},
 * as the solution will be evaluated solely based on the step {@code [2][1]}.
 * Consequently,
 * the generated steps will not include the complete solutions, except for the best one selected by the solver.
 */
class BlackBoxListVariableExhaustiveSearchPhaseTest {

    /**
     * Initialize combination of input parameters.
     *
     * @return collection of combination of input parameters
     */
    static Collection<Object[]> params() {
        return Stream.concat(getBranchAndBoundConfigs(), getBruteForceConfigs())
                .toList();
    }

    private static Stream<Object[]> getBranchAndBoundConfigs() {
        return Stream.of(
                getBranchAndBoundDepthFirstConfigs(),
                getBranchAndBoundBreadthFirstConfigs(),
                getBranchAndBoundScoreFirstConfigs(),
                getBranchAndBoundOptimisticBoundFirstConfigs(),
                getBranchAndBoundOriginalOrderConfigs())
                .flatMap(i -> i);
    }

    private static Stream<Object[]> getBranchAndBoundDepthFirstConfigs() {
        return Stream.of(
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("---", "--[1]", "--[3]", "--[2]", "--[2,1]", "--[2,3]", "--[3,1]", "--[3,2]", "--[1,3]",
                                "--[1,2]", "[1]--", "[1]-[3]", "[1]-[2]", "[3]--", "[3]-[1]", "[3]-[2]", "[2]--", "[2]-[1]",
                                "[2]-[3]", "-[1]-", "-[1][3]", "-[1][2]", "[3][1]-", "[3][1][2]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "--[3]", "--[2]", "--[1]", "--[1,3]", "--[1,2]", "--[2,3]", "--[2,1]", "--[3,2]",
                                "--[3,1]", "[3]--", "[3]-[2]", "[3]-[1]", "[2]--", "[2]-[3]", "[2]-[1]", "[1]--", "[1]-[3]",
                                "[1]-[2]", "-[3]-", "-[3][2]", "-[3][1]", "[2][3]-", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "--[1]", "--[2]", "--[3]", "--[3,1]", "--[3,2]", "--[2,1]", "--[2,3]", "--[1,2]",
                                "--[1,3]", "[1]--", "[1]-[2]", "[1]-[3]", "[2]--", "[2]-[1]", "[2]-[3]", "[3]--", "[3]-[1]",
                                "[3]-[2]", "-[1]-", "-[1][2]", "-[1][3]", "[2][1]-", "[2][1][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.NONE,
                        List.of("---", "[1]--", "[3]--", "[2]--", "[2,1]--", "[2,3]--", "[3,1]--", "[3,2]--", "[1,3]--",
                                "[1,2]--", "-[1]-", "[3][1]-", "[2][1]-", "-[3]-", "[1][3]-", "[2][3]-", "-[2]-", "[1][2]-",
                                "[3][2]-", "--[1]", "[3]-[1]", "[2]-[1]", "-[3][1]", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "[3]--", "[2]--", "[1]--", "[1,3]--", "[1,2]--", "[2,3]--", "[2,1]--", "[3,2]--",
                                "[3,1]--", "-[3]-", "[2][3]-", "[1][3]-", "-[2]-", "[3][2]-", "[1][2]-", "-[1]-", "[3][1]-",
                                "[2][1]-", "--[3]", "[2]-[3]", "[1]-[3]", "-[2][3]", "[1][2][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "[1]--", "[2]--", "[3]--", "[3,1]--", "[3,2]--", "[2,1]--", "[2,3]--", "[1,2]--",
                                "[1,3]--", "-[1]-", "[2][1]-", "[3][1]-", "-[2]-", "[1][2]-", "[3][2]-", "-[3]-", "[1][3]-",
                                "[2][3]-", "--[1]", "[2]-[1]", "[3]-[1]", "-[2][1]", "[3][2][1]")));
    }

    private static Stream<Object[]> getBranchAndBoundBreadthFirstConfigs() {
        return Stream.of(
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.BREADTH_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("---", "-[1]-", "-[3]-", "-[2]-", "-[1,3]-", "[3][1]-", "[3][1][2]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.BREADTH_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "-[3]-", "-[2]-", "-[1]-", "-[3,2]-", "[2][3]-", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.BREADTH_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "-[1]-", "-[2]-", "-[3]-", "-[1,2]-", "[2][1]-", "[2][1][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.BREADTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.NONE,
                        List.of("---", "--[1]", "--[3]", "--[2]", "--[1,3]", "-[3][1]", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.BREADTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "--[3]", "--[2]", "--[1]", "--[3,2]", "-[2][3]", "[1][2][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.BREADTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "--[1]", "--[2]", "--[3]", "--[1,2]", "-[2][1]", "[3][2][1]")));
    }

    private static Stream<Object[]> getBranchAndBoundScoreFirstConfigs() {
        return Stream.of(
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.SCORE_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("---", "--[1]", "--[3]", "--[2]", "[1]--", "[1]-[3]", "[1]-[2]", "[3]--", "[3]-[1]", "[3]-[2]",
                                "[2]--", "[2]-[1]", "[2]-[3]", "-[1]-", "-[1][3]", "-[1][2]", "[3][1]-", "[3][1][2]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.SCORE_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "--[3]", "--[2]", "--[1]", "[3]--", "[3]-[2]", "[3]-[1]", "[2]--", "[2]-[3]", "[2]-[1]",
                                "[1]--", "[1]-[3]", "[1]-[2]", "-[3]-", "-[3][2]", "-[3][1]", "[2][3]-", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.SCORE_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "--[1]", "--[2]", "--[3]", "[1]--", "[1]-[2]", "[1]-[3]", "[2]--", "[2]-[1]", "[2]-[3]",
                                "[3]--", "[3]-[1]", "[3]-[2]", "-[1]-", "-[1][2]", "-[1][3]", "[2][1]-", "[2][1][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.SCORE_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.NONE,
                        List.of("---", "[1]--", "[3]--", "[2]--", "-[1]-", "[3][1]-", "[2][1]-", "-[3]-", "[1][3]-", "[2][3]-",
                                "-[2]-", "[1][2]-", "[3][2]-", "--[1]", "[3]-[1]", "[2]-[1]", "-[3][1]", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.SCORE_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "[3]--", "[2]--", "[1]--", "-[3]-", "[2][3]-", "[1][3]-", "-[2]-", "[3][2]-", "[1][2]-",
                                "-[1]-", "[3][1]-", "[2][1]-", "--[3]", "[2]-[3]", "[1]-[3]", "-[2][3]", "[1][2][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.SCORE_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "[1]--", "[2]--", "[3]--", "-[1]-", "[2][1]-", "[3][1]-", "-[2]-", "[1][2]-", "[3][2]-",
                                "-[3]-", "[1][3]-", "[2][3]-", "--[1]", "[2]-[1]", "[3]-[1]", "-[2][1]", "[3][2][1]")));
    }

    private static Stream<Object[]> getBranchAndBoundOptimisticBoundFirstConfigs() {
        return Stream.of(
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.OPTIMISTIC_BOUND_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("---", "--[1]", "--[3]", "--[2]", "[1]--", "[1]-[3]", "[1]-[2]", "[3]--", "[3]-[1]", "[3]-[2]",
                                "[2]--", "[2]-[1]", "[2]-[3]", "-[1]-", "-[1][3]", "-[1][2]", "[3][1]-", "[3][1][2]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.OPTIMISTIC_BOUND_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "--[3]", "--[2]", "--[1]", "[3]--", "[3]-[2]", "[3]-[1]", "[2]--", "[2]-[3]", "[2]-[1]",
                                "[1]--", "[1]-[3]", "[1]-[2]", "-[3]-", "-[3][2]", "-[3][1]", "[2][3]-", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.OPTIMISTIC_BOUND_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "--[1]", "--[2]", "--[3]", "[1]--", "[1]-[2]", "[1]-[3]", "[2]--", "[2]-[1]", "[2]-[3]",
                                "[3]--", "[3]-[1]", "[3]-[2]", "-[1]-", "-[1][2]", "-[1][3]", "[2][1]-", "[2][1][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.OPTIMISTIC_BOUND_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.NONE,
                        List.of("---", "[1]--", "[3]--", "[2]--", "-[1]-", "[3][1]-", "[2][1]-", "-[3]-", "[1][3]-", "[2][3]-",
                                "-[2]-", "[1][2]-", "[3][2]-", "--[1]", "[3]-[1]", "[2]-[1]", "-[3][1]", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.OPTIMISTIC_BOUND_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "[3]--", "[2]--", "[1]--", "-[3]-", "[2][3]-", "[1][3]-", "-[2]-", "[3][2]-", "[1][2]-",
                                "-[1]-", "[3][1]-", "[2][1]-", "--[3]", "[2]-[3]", "[1]-[3]", "-[2][3]", "[1][2][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.OPTIMISTIC_BOUND_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "[1]--", "[2]--", "[3]--", "-[1]-", "[2][1]-", "[3][1]-", "-[2]-", "[1][2]-", "[3][2]-",
                                "-[3]-", "[1][3]-", "[2][3]-", "--[1]", "[2]-[1]", "[3]-[1]", "-[2][1]", "[3][2][1]")));
    }

    private static Stream<Object[]> getBranchAndBoundOriginalOrderConfigs() {
        return Stream.of(
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.ORIGINAL_ORDER,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("---", "--[1]", "--[3]", "--[2]", "--[1,3]", "--[1,2]", "--[3,1]", "--[3,2]", "--[2,1]",
                                "--[2,3]", "[1]--", "[1]-[3]", "[1]-[2]", "[3]--", "[3]-[1]", "[3]-[2]", "[2]--", "[2]-[1]",
                                "[2]-[3]", "-[1]-", "-[1][3]", "-[1][2]", "[3][1]-", "[3][1][2]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.ORIGINAL_ORDER,
                        EntitySorterManner.NONE,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "--[3]", "--[2]", "--[1]", "--[3,2]", "--[3,1]", "--[2,3]", "--[2,1]", "--[1,3]",
                                "--[1,2]", "[3]--", "[3]-[2]", "[3]-[1]", "[2]--", "[2]-[3]", "[2]-[1]", "[1]--", "[1]-[3]",
                                "[1]-[2]", "-[3]-", "-[3][2]", "-[3][1]", "[2][3]-", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.ORIGINAL_ORDER,
                        EntitySorterManner.NONE,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "--[1]", "--[2]", "--[3]", "--[1,2]", "--[1,3]", "--[2,1]", "--[2,3]", "--[3,1]",
                                "--[3,2]", "[1]--", "[1]-[2]", "[1]-[3]", "[2]--", "[2]-[1]", "[2]-[3]", "[3]--", "[3]-[1]",
                                "[3]-[2]", "-[1]-", "-[1][2]", "-[1][3]", "[2][1]-", "[2][1][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.ORIGINAL_ORDER,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.NONE,
                        List.of("---", "[1]--", "[3]--", "[2]--", "[1,3]--", "[1,2]--", "[3,1]--", "[3,2]--", "[2,1]--",
                                "[2,3]--", "-[1]-", "[3][1]-", "[2][1]-", "-[3]-", "[1][3]-", "[2][3]-", "-[2]-", "[1][2]-",
                                "[3][2]-", "--[1]", "[3]-[1]", "[2]-[1]", "-[3][1]", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.ORIGINAL_ORDER,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "[3]--", "[2]--", "[1]--", "[3,2]--", "[3,1]--", "[2,3]--", "[2,1]--", "[1,3]--",
                                "[1,2]--", "-[3]-", "[2][3]-", "[1][3]-", "-[2]-", "[3][2]-", "[1][2]-", "-[1]-", "[3][1]-",
                                "[2][1]-", "--[3]", "[2]-[3]", "[1]-[3]", "-[2][3]", "[1][2][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.ORIGINAL_ORDER,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "[1]--", "[2]--", "[3]--", "[1,2]--", "[1,3]--", "[2,1]--", "[2,3]--", "[3,1]--",
                                "[3,2]--", "-[1]-", "[2][1]-", "[3][1]-", "-[2]-", "[1][2]-", "[3][2]-", "-[3]-", "[1][3]-",
                                "[2][3]-", "--[1]", "[2]-[1]", "[3]-[1]", "-[2][1]", "[3][2][1]")));
    }

    private static Stream<Object[]> getBruteForceConfigs() {
        return Stream.concat(getBruteForceLegalConfigs(), getBruteForceIllegalConfigs());
    }

    private static Stream<Object[]> getBruteForceLegalConfigs() {
        return Stream.of(
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        null,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("---", "--[1]", "--[3]", "--[2]", "--[1,3]", "--[1,2]", "--[3,1]", "--[3,2]", "--[2,1]",
                                "--[2,3]", "[1]--", "[1]-[3]", "[1]-[2]", "[3]--", "[3]-[1]", "[3]-[2]", "[2]--", "[2]-[1]",
                                "[2]-[3]", "[1,3]--", "[1,2]--", "[3,1]--", "[3,2]--", "[2,1]--", "[2,3]--", "-[1]-", "-[1][3]",
                                "-[1][2]", "[3][1]-", "[2][1]-", "-[3]-", "-[3][1]", "-[3][2]", "[1][3]-", "[2][3]-", "-[2]-",
                                "-[2][1]", "-[2][3]", "[1][2]-", "[3][2]-", "-[1,3]-", "-[1,2]-", "-[3,1]-", "-[3,2]-",
                                "-[2,1]-", "-[2,3]-", "[3][1][2]")),
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        null,
                        EntitySorterManner.NONE,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "--[3]", "--[2]", "--[1]", "--[3,2]", "--[3,1]", "--[2,3]", "--[2,1]", "--[1,3]",
                                "--[1,2]", "[3]--", "[3]-[2]", "[3]-[1]", "[2]--", "[2]-[3]", "[2]-[1]", "[1]--", "[1]-[3]",
                                "[1]-[2]", "[3,2]--", "[3,1]--", "[2,3]--", "[2,1]--", "[1,3]--", "[1,2]--", "-[3]-", "-[3][2]",
                                "-[3][1]", "[2][3]-", "[1][3]-", "-[2]-", "-[2][3]", "-[2][1]", "[3][2]-", "[1][2]-", "-[1]-",
                                "-[1][3]", "-[1][2]", "[3][1]-", "[2][1]-", "-[3,2]-", "-[3,1]-", "-[2,3]-", "-[2,1]-",
                                "-[1,3]-", "-[1,2]-", "[2][3][1]")),
                toObjectArray(ExhaustiveSearchType.BRUTE_FORCE,
                        null,
                        EntitySorterManner.NONE,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "--[1]", "--[2]", "--[3]", "--[1,2]", "--[1,3]", "--[2,1]", "--[2,3]", "--[3,1]",
                                "--[3,2]", "[1]--", "[1]-[2]", "[1]-[3]", "[2]--", "[2]-[1]", "[2]-[3]", "[3]--", "[3]-[1]",
                                "[3]-[2]", "[1,2]--", "[1,3]--", "[2,1]--", "[2,3]--", "[3,1]--", "[3,2]--", "-[1]-", "-[1][2]",
                                "-[1][3]", "[2][1]-", "[3][1]-", "-[2]-", "-[2][1]", "-[2][3]", "[1][2]-", "[3][2]-", "-[3]-",
                                "-[3][1]", "-[3][2]", "[1][3]-", "[2][3]-", "-[1,2]-", "-[1,3]-", "-[2,1]-", "-[2,3]-",
                                "-[3,1]-", "-[3,2]-", "[2][1][3]")),
                toObjectArray(ExhaustiveSearchType.BRUTE_FORCE,
                        null,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.NONE,
                        List.of("---", "[1]--", "[3]--", "[2]--", "[1,3]--", "[1,2]--", "[3,1]--", "[3,2]--", "[2,1]--",
                                "[2,3]--", "-[1]-", "[3][1]-", "[2][1]-", "-[3]-", "[1][3]-", "[2][3]-", "-[2]-", "[1][2]-",
                                "[3][2]-", "-[1,3]-", "-[1,2]-", "-[3,1]-", "-[3,2]-", "-[2,1]-", "-[2,3]-", "--[1]", "[3]-[1]",
                                "[2]-[1]", "-[3][1]", "-[2][1]", "--[3]", "[1]-[3]", "[2]-[3]", "-[1][3]", "-[2][3]", "--[2]",
                                "[1]-[2]", "[3]-[2]", "-[1][2]", "-[3][2]", "--[1,3]", "--[1,2]", "--[3,1]", "--[3,2]",
                                "--[2,1]", "--[2,3]", "[2][3][1]")),
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        null,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        List.of("---", "[3]--", "[2]--", "[1]--", "[3,2]--", "[3,1]--", "[2,3]--", "[2,1]--", "[1,3]--",
                                "[1,2]--", "-[3]-", "[2][3]-", "[1][3]-", "-[2]-", "[3][2]-", "[1][2]-", "-[1]-", "[3][1]-",
                                "[2][1]-", "-[3,2]-", "-[3,1]-", "-[2,3]-", "-[2,1]-", "-[1,3]-", "-[1,2]-", "--[3]", "[2]-[3]",
                                "[1]-[3]", "-[2][3]", "-[1][3]", "--[2]", "[3]-[2]", "[1]-[2]", "-[3][2]", "-[1][2]", "--[1]",
                                "[3]-[1]", "[2]-[1]", "-[3][1]", "-[2][1]", "--[3,2]", "--[3,1]", "--[2,3]", "--[2,1]",
                                "--[1,3]", "--[1,2]", "[1][2][3]")),
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        null,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.ASCENDING,
                        List.of("---", "[1]--", "[2]--", "[3]--", "[1,2]--", "[1,3]--", "[2,1]--", "[2,3]--", "[3,1]--",
                                "[3,2]--", "-[1]-", "[2][1]-", "[3][1]-", "-[2]-", "[1][2]-", "[3][2]-", "-[3]-", "[1][3]-",
                                "[2][3]-", "-[1,2]-", "-[1,3]-", "-[2,1]-", "-[2,3]-", "-[3,1]-", "-[3,2]-", "--[1]", "[2]-[1]",
                                "[3]-[1]", "-[2][1]", "-[3][1]", "--[2]", "[1]-[2]", "[3]-[2]", "-[1][2]", "-[3][2]", "--[3]",
                                "[1]-[3]", "[2]-[3]", "-[1][3]", "-[2][3]", "--[1,2]", "--[1,3]", "--[2,1]", "--[2,3]",
                                "--[3,1]", "--[3,2]", "[3][2][1]")));
    }

    private static Stream<Object[]> getBruteForceIllegalConfigs() {
        return Stream.of(
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        null),
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        NodeExplorationType.BREADTH_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        null),
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        NodeExplorationType.SCORE_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        null),
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        NodeExplorationType.OPTIMISTIC_BOUND_FIRST,
                        EntitySorterManner.DESCENDING,
                        ValueSorterManner.DESCENDING,
                        null));
    }

    private static Object[] toObjectArray(Object... parameters) {
        return parameters;
    }

    private static SolverConfig buildSolverConfig(
            EntitySorterManner entitySorterManner, ValueSorterManner valueSorterManner,
            ExhaustiveSearchType exhaustiveSearchType, NodeExplorationType nodeExplorationType) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSortableSolution.class, TestdataListSortableEntity.class);

        ExhaustiveSearchPhaseConfig exhaustiveSearchPhaseConfig = new ExhaustiveSearchPhaseConfig()
                .withExhaustiveSearchType(exhaustiveSearchType)
                .withNodeExplorationType(nodeExplorationType)
                .withEntitySorterManner(entitySorterManner)
                .withValueSorterManner(valueSorterManner)
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(46));

        solverConfig.setPhaseConfigList(Collections.singletonList(exhaustiveSearchPhaseConfig));
        solverConfig.setScoreDirectorFactoryConfig(new ScoreDirectorFactoryConfig()
                .withEasyScoreCalculatorClass(TestdataComparableSingleValueCalculator.class)
                .withInitializingScoreTrend("ONLY_DOWN"));

        return solverConfig;
    }

    private static TestdataListSortableSolution buildSolution() {
        var solution = new TestdataListSortableSolution();
        // Intentionally not sorted, the string is used for sorting in cases it applies.
        solution.setEntityList(List.of(new TestdataListSortableEntity("entity2", 2),
                new TestdataListSortableEntity("entity1", 1),
                new TestdataListSortableEntity("entity3", 3)));
        solution.setValueList(List.of(new TestdataSortableValue("1", 1),
                new TestdataSortableValue("3", 3),
                new TestdataSortableValue("2", 2)));
        return solution;
    }

    @ParameterizedTest(name = "{0}, NodeExplorationType-{1}, EntitySorterManner-{2}, ValueSorterManner-{3}")
    @MethodSource("params")
    void verifyExhaustiveSearchSteps(
            ExhaustiveSearchType exhaustiveSearchType,
            NodeExplorationType nodeExplorationType,
            EntitySorterManner entitySorterManner,
            ValueSorterManner valueSorterManner,
            List<String> steps) {
        var solverConfig = buildSolverConfig(
                entitySorterManner,
                valueSorterManner,
                exhaustiveSearchType,
                nodeExplorationType);
        var solverFactory = SolverFactory.<TestdataListSortableSolution> create(solverConfig);

        if (exhaustiveSearchType == ExhaustiveSearchType.BRUTE_FORCE && nodeExplorationType != null) {
            assertThatIllegalArgumentException()
                    .isThrownBy(solverFactory::buildSolver)
                    .withMessage("The phaseConfig (ExhaustiveSearchPhaseConfig) has an "
                            + "nodeExplorationType (" + nodeExplorationType.name()
                            + ") which is not compatible with its exhaustiveSearchType (BRUTE_FORCE).");
        } else {
            var solver = solverFactory.buildSolver();
            var listener = new TestdataSolutionStateRecorder();
            ((DefaultSolver<TestdataListSortableSolution>) solver).addPhaseLifecycleListener(listener);

            solver.solve(buildSolution());

            assertThat(listener.getWorkingSolutions()).containsExactlyElementsOf(steps);
        }
    }

    /**
     * This class calculates the score of a solution by penalizing entity lists with more than one value.
     */
    public static class TestdataComparableSingleValueCalculator
            implements EasyScoreCalculator<TestdataListSortableSolution, HardSoftScore> {

        @Override
        public @NonNull HardSoftScore calculateScore(@NonNull TestdataListSortableSolution solution) {
            int hardScore = 0;
            int softScore = 0;
            for (var entity : solution.getEntityList()) {
                var size = entity.getValueList().size();
                if (size == 0) {
                    softScore--;
                } else if (size > 1) {
                    hardScore -= size;
                }
            }
            return HardSoftScore.of(hardScore, softScore);
        }
    }

    static class TestdataSolutionStateRecorder extends PhaseLifecycleListenerAdapter<TestdataListSortableSolution> {

        private final List<String> workingSolutions = new ArrayList<>();

        @Override
        public void stepEnded(AbstractStepScope<TestdataListSortableSolution> abstractStepScope) {
            addWorkingSolution(abstractStepScope.getWorkingSolution());
        }

        @Override
        public void solvingEnded(SolverScope<TestdataListSortableSolution> solverScope) {
            addWorkingSolution(solverScope.getBestSolution());
        }

        private void addWorkingSolution(TestdataListSortableSolution solution) {
            workingSolutions.add(solution.getEntityList().stream()
                    .sorted(Comparator.comparing(TestdataObject::getCode))
                    .map(TestdataListSortableEntity::getValueList)
                    .map(valueList -> valueList.isEmpty() ? "-"
                            : "[" + valueList.stream()
                                    .map(TestdataSortableValue::getCode)
                                    .collect(Collectors.joining(",")) + "]")
                    .collect(Collectors.joining()));
        }

        List<String> getWorkingSolutions() {
            return workingSolutions;
        }
    }
}
