package ai.timefold.solver.examples.nqueens.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.nqueens.domain.NQueens;

class NQueensSmokeTest extends SolverSmokeTest<NQueens, SimpleScore> {

    @Override
    protected NQueensApp createCommonApp() {
        return new NQueensApp();
    }

    @Override
    protected Stream<TestData<SimpleScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, "data/nqueens/unsolved/16queens.json",
                        SimpleScore.ZERO,
                        SimpleScore.ZERO,
                        SimpleScore.ZERO));
    }
}
