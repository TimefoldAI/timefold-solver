package ai.timefold.solver.core.impl.solver.recaller;

import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

public interface ReusingBestSolutionUpdater<Solution_> {
    <Score_ extends Score<Score_>> void updateReusingBestSolution(SolverScope<Solution_> solverScope,
            InnerScore<Score_> score,
            List<Move<Solution_>> movesSinceLastBestSolutionList);

    Solution_ getBestSolution();
}
