package ai.timefold.solver.examples.nqueens.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.nqueens.domain.NQueens;
import ai.timefold.solver.examples.nqueens.optional.score.NQueensEasyScoreCalculator;

class NQueensSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<NQueens> {

    @Override
    protected CommonApp<NQueens> createCommonApp() {
        return new NQueensApp();
    }

    @Override
    protected Class<NQueensEasyScoreCalculator> overwritingEasyScoreCalculatorClass() {
        return NQueensEasyScoreCalculator.class;
    }
}
