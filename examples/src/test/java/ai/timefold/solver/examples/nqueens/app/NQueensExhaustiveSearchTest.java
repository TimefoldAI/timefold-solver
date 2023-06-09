package ai.timefold.solver.examples.nqueens.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import ai.timefold.solver.examples.common.app.AbstractExhaustiveSearchTest;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.nqueens.domain.NQueens;

class NQueensExhaustiveSearchTest extends AbstractExhaustiveSearchTest<NQueens> {

    @Override
    protected CommonApp<NQueens> createCommonApp() {
        return new NQueensApp();
    }

    @Override
    protected Stream<String> unsolvedFileNames() {
        return Stream.of("4queens.json");
    }

    @Override
    protected void assertSolution(NQueens bestSolution) {
        super.assertSolution(bestSolution);
        assertThat(bestSolution.getScore().score()).isEqualTo(0);
    }
}
