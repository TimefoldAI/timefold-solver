package ai.timefold.solver.core.testutil;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class NeighborhoodTestUtils {

    /**
     * Builds and settles a {@code DatasetSession} directly
     * (bypassing a real {@code ScoreDirector} and solver),
     * for the cached and just-in-time dataset cases
     * that have no {@code MoveProvider} route of their own.
     */
    public static <Solution_> DefaultNeighborhoodSession<Solution_> createSession(
            DefaultMoveStreamFactory<Solution_> moveStreamFactory, Solution_ solution) {
        var scoreDirector = new EasyScoreDirectorFactory<>(moveStreamFactory.getSolutionDescriptor(),
                s -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var session = moveStreamFactory.createSession(new SessionContext<>(scoreDirector));
        moveStreamFactory.getSolutionDescriptor().visitAll(solution, session::insert);
        session.settle();
        return session;
    }

    private NeighborhoodTestUtils() {
    }

}
