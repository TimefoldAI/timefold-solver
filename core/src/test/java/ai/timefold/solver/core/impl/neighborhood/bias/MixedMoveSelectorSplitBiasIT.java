package ai.timefold.solver.core.impl.neighborhood.bias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedDummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.UnionMoveSelector;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.neighborhood.MixedMoveSelector;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsBasedMoveRepository;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsMoveSelector;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.random.MockRandomSource;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

/**
 * Confirms that {@link MixedMoveSelector} draws from its two sides
 * in proportion to how many movers each holds,
 * not 50/50 regardless of that count:
 * 3 classic move selectors against 2 neighborhoods must yield a 3/5-2/5 split,
 * never 1/2-1/2.
 */
class MixedMoveSelectorSplitBiasIT extends AbstractBiasIT {

    @Test
    void splitsProportionallyToMoverCount() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var solution = TestdataSolution.generateSolution(2, 3);

        var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor, s -> SimpleScore.ZERO,
                EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);

        var solverScope = new SolverScope<TestdataSolution>();
        solverScope.setWorkingRandom(new MockRandomSource(splitFrom(new Random(0))));
        solverScope.setScoreDirector(scoreDirector);

        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope = new LocalSearchStepScope<>(phaseScope);

        // Classic side: 3 leaf move selectors, each an endless source of "classic" moves.
        var mixedMoveSelector = getMixedMoveSelector(solutionDescriptor);
        mixedMoveSelector.solvingStarted(solverScope);
        mixedMoveSelector.phaseStarted(phaseScope);
        mixedMoveSelector.stepStarted(stepScope);

        var iterator = mixedMoveSelector.iterator();
        var report = BiasReport.tally("mixed classic/neighborhoods split", 100_000, i -> classify(iterator.next()));

        mixedMoveSelector.stepEnded(stepScope);
        mixedMoveSelector.phaseEnded(phaseScope);
        mixedMoveSelector.solvingEnded(solverScope);

        // 3 classic move selectors vs. 2 neighborhoods: expected share is 3/5 and 2/5, never 1/2 and 1/2.
        report.expectWeights(Map.of("classic", 3.0 / 5, "neighborhood", 2.0 / 5))
                .assertWithinSigma(SIGMA_LIMIT);
    }

    private static @NonNull MixedMoveSelector<TestdataSolution>
            getMixedMoveSelector(SolutionDescriptor<TestdataSolution> solutionDescriptor) {
        List<MoveSelector<TestdataSolution>> classicChildMoveSelectorList = List.of(
                new NeverEndingDummyMoveSelector("classic-a"), new NeverEndingDummyMoveSelector("classic-b"),
                new NeverEndingDummyMoveSelector("classic-c"));
        var classicMoveSelector = new UnionMoveSelector<>(classicChildMoveSelectorList, true);

        // Neighborhoods side: 2 move providers, each an endless source of "neighborhood" moves.
        var neighborhoodsMoveSelector = getNeighborhoodsMoveSelector(solutionDescriptor);
        return new MixedMoveSelector<>(classicMoveSelector, neighborhoodsMoveSelector);
    }

    private static @NonNull NeighborhoodsMoveSelector<TestdataSolution>
            getNeighborhoodsMoveSelector(SolutionDescriptor<TestdataSolution> solutionDescriptor) {
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var neighborhoodProviderList =
                List.of(neverEndingMoveProvider("neighborhood-a"), neverEndingMoveProvider("neighborhood-b"));
        var neighborhoodsMoveRepository =
                new NeighborhoodsBasedMoveRepository<>(moveStreamFactory, neighborhoodProviderList);
        return new NeighborhoodsMoveSelector<>(neighborhoodsMoveRepository);
    }

    private static String classify(Move<TestdataSolution> move) {
        var code = ((SelectorBasedDummyMove) move).getCode();
        assertThat(code).isNotNull();
        return code.startsWith("classic") ? "classic" : "neighborhood";
    }

    private static MoveProvider<TestdataSolution> neverEndingMoveProvider(String code) {
        return moveStreamFactory -> moveStreamFactory.buildMoveStream((session, random) -> neverEndingIterator(code));
    }

    private static Iterator<Move<TestdataSolution>> neverEndingIterator(String code) {
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Move<TestdataSolution> next() {
                return new SelectorBasedDummyMove(code);
            }

        };
    }

    private static final class NeverEndingDummyMoveSelector
            extends AbstractMoveSelector<TestdataSolution> {

        private final String code;

        NeverEndingDummyMoveSelector(String code) {
            this.code = code;
        }

        @Override
        public boolean isNeverEnding() {
            return true;
        }

        @Override
        public long getSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Move<TestdataSolution>> iterator() {
            return neverEndingIterator(code);
        }

    }

}
