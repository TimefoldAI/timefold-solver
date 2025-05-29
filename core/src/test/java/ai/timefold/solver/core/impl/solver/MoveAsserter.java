package ai.timefold.solver.core.impl.solver;

import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.preview.api.move.Move;

public class MoveAsserter<Solution_> {
    private final SolutionDescriptor<Solution_> solutionDescriptor;

    private MoveAsserter(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
    }

    public static <Solution_> MoveAsserter<Solution_> create(SolutionDescriptor<Solution_> solutionDescriptor) {
        return new MoveAsserter<>(solutionDescriptor);
    }

    public void assertMoveAndUndo(Solution_ solution, Move<Solution_> move) {
        assertMoveAndUndo(solution, move, (ignored) -> {
        });
    }

    public void assertMoveAndUndo(Solution_ solution, Move<Solution_> move, Consumer<Solution_> moveSolutionConsumer) {
        assertMove(solution, move, moveSolutionConsumer, false);
    }

    public void assertMoveAndApply(Solution_ solution, Move<Solution_> move) {
        assertMoveAndApply(solution, move, (ignored) -> {
        });
    }

    public void assertMoveAndApply(Solution_ solution, Move<Solution_> move, Consumer<Solution_> moveSolutionConsumer) {
        assertMove(solution, move, moveSolutionConsumer, true);
    }

    private void assertMove(Solution_ solution, Move<Solution_> move, Consumer<Solution_> moveSolutionConsumer,
            boolean applyMove) {
        var scoreDirectorFactory = new MoveAssertScoreDirectorFactory<>(solutionDescriptor, moveSolutionConsumer);
        scoreDirectorFactory.setTrackingWorkingSolution(true);
        try (var scoreDirector = scoreDirectorFactory.createScoreDirectorBuilder()
                .withLookUpEnabled(false)
                .build()) {
            var innerScore = InnerScore.fullyAssigned((Score) scoreDirector.getScoreDefinition().getZeroScore());
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.executeTemporaryMove(move, true);
            var corruptionResult = scoreDirector.getSolutionCorruptionAfterUndo(move, innerScore);
            if (corruptionResult.isCorrupted()) {
                throw new AssertionError("""
                        Solution corruption caused by move (%s) or its undo.
                        Analysis:
                        %s""".formatted(move, corruptionResult.message()));
            }
            if (applyMove) {
                scoreDirector.executeMove(move);
            }
        }
    }
}
