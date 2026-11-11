package ai.timefold.solver.core.impl.neighborhood.bias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedDummyMove;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsBasedMoveRepository;
import ai.timefold.solver.core.impl.neighborhood.NeighborhoodsMoveSelector;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.random.MockRandomSource;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.FixedWeightMoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.selection.PhaseContext;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

/**
 * Confirms that a subclass of {@link FixedWeightMoveSelectionStrategy}
 * draws from its move providers in proportion to their weights:
 * weights 3, 1 and 0 must yield a 3/4-1/4 split, and never a move from the provider with weight 0.
 */
class FixedWeightMoveSelectionStrategyBiasIT extends AbstractBiasIT {

    private static final Map<String, Double> WEIGHT_MAP = Map.of("a", 3.0, "b", 1.0, "c", 0.0);

    @Test
    void splitsProportionallyToWeight() {
        var report = draw(WeightedStrategy::new, true);
        report.expectWeights(Map.of("a", 3.0 / 4, "b", 1.0 / 4))
                .assertWithinSigma(SIGMA_LIMIT);
    }

    @Test
    void defaultSplitsUniformly() {
        var report = draw(FixedWeightMoveSelectionStrategy::new, false);
        report.expectUniform(WEIGHT_MAP.keySet())
                .assertWithinSigma(SIGMA_LIMIT);
    }

    private static BiasReport<String> draw(Supplier<MoveSelectionStrategy<TestdataSolution>> strategyFactory,
            boolean weighted) {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor, s -> SimpleScore.ZERO,
                EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(TestdataSolution.generateSolution(2, 3));

        var solverScope = new SolverScope<TestdataSolution>();
        solverScope.setWorkingRandom(new MockRandomSource(splitFrom(new Random(0))));
        solverScope.setScoreDirector(scoreDirector);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        phaseScope.reset();
        var stepScope = new LocalSearchStepScope<>(phaseScope);

        var moveProviderMap = new LinkedHashMap<String, MoveProvider<TestdataSolution>>();
        for (var id : new String[] { "a", "b", "c" }) {
            moveProviderMap.put(id, neverEndingMoveProvider(id));
        }
        var moveSelector = new NeighborhoodsMoveSelector<>(new NeighborhoodsBasedMoveRepository<>(
                new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT), moveProviderMap,
                strategyFactory));
        moveSelector.solvingStarted(solverScope);
        moveSelector.phaseStarted(phaseScope);
        moveSelector.stepStarted(stepScope);

        var iterator = moveSelector.iterator();
        var report = BiasReport.tally("fixed weight move selection strategy", 100_000,
                i -> {
                    var code = ((SelectorBasedDummyMove) iterator.next()).getCode();
                    if (weighted) {
                        assertThat(WEIGHT_MAP.get(code)).as("weight of %s", code).isPositive();
                    }
                    return code;
                });

        moveSelector.stepEnded(stepScope);
        moveSelector.phaseEnded(phaseScope);
        moveSelector.solvingEnded(solverScope);
        return report;
    }

    private static MoveProvider<TestdataSolution> neverEndingMoveProvider(String code) {
        return moveStreamFactory -> moveStreamFactory.buildMoveStream((session, random) -> new Iterator<>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Move<TestdataSolution> next() {
                return new SelectorBasedDummyMove(code);
            }

        });
    }

    public static final class WeightedStrategy extends FixedWeightMoveSelectionStrategy<TestdataSolution> {

        @Override
        protected double getWeight(MoveSource<TestdataSolution> moveSource, PhaseContext<TestdataSolution> context) {
            return WEIGHT_MAP.get(moveSource.id());
        }

    }

}
