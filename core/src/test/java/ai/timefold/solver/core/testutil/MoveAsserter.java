package ai.timefold.solver.core.testutil;

import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveRunner;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Prefer {@link MoveRunner}, unless you absolutely need score corruption checks.
 * 
 * @param <Solution_>
 */
@NullMarked
public final class MoveAsserter<Solution_> {
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    @Nullable
    private final MoveRepository<Solution_> moveRepository;

    private MoveAsserter(SolutionDescriptor<Solution_> solutionDescriptor,
            @Nullable MoveRepository<Solution_> moveRepository) {
        this.solutionDescriptor = solutionDescriptor;
        this.moveRepository = moveRepository;
    }

    public static <Solution_> MoveAsserter<Solution_> create(SolutionDescriptor<Solution_> solutionDescriptor) {
        return new MoveAsserter<>(solutionDescriptor, null);
    }

    public static <Solution_> MoveAsserter<Solution_> create(SolutionDescriptor<Solution_> solutionDescriptor,
            MoveRepository<Solution_> moveRepository) {
        return new MoveAsserter<>(solutionDescriptor, moveRepository);
    }

    public void assertMoveAndUndo(Solution_ solution, Move<Solution_> move) {
        assertMoveAndUndo(solution, move, (ignored) -> {
        });
    }

    public void assertMoveAndUndo(Solution_ solution, Move<Solution_> move, Consumer<Solution_> moveSolutionConsumer) {
        assertMove(solution, move, moveSolutionConsumer, false);
    }

    public void assertMoveAndApply(Solution_ solution, Move<Solution_> move, Consumer<Solution_> moveSolutionConsumer) {
        assertMove(solution, move, moveSolutionConsumer, true);
    }

    public void assertMoveAndUndo(Solution_ solution, ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> move,
            Consumer<Solution_> moveSolutionConsumer) {
        assertMove(solution, move, moveSolutionConsumer);
    }

    private void assertMove(Solution_ solution, ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> move,
            Consumer<Solution_> moveSolutionConsumer) {
        assertMove(solution, move, moveSolutionConsumer, false);
    }

    private void assertMove(Solution_ solution, Move<Solution_> move, Consumer<Solution_> moveSolutionConsumer,
            boolean applyMove) {
        var scoreDirectorFactory = new MoveAssertScoreDirectorFactory<>(solutionDescriptor, moveSolutionConsumer,
                moveRepository);
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
