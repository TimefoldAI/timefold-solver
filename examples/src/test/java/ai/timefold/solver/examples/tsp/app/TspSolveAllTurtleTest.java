package ai.timefold.solver.examples.tsp.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.tsp.domain.TspSolution;
import ai.timefold.solver.examples.tsp.optional.score.TspEasyScoreCalculator;

class TspSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<TspSolution> {

    @Override
    protected CommonApp<TspSolution> createCommonApp() {
        return new TspApp();
    }

    @Override
    protected Class<TspEasyScoreCalculator> overwritingEasyScoreCalculatorClass() {
        return TspEasyScoreCalculator.class;
    }
}
