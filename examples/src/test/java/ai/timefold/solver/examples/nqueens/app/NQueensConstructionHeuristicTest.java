package ai.timefold.solver.examples.nqueens.app;

import java.util.stream.Stream;

import ai.timefold.solver.examples.common.app.AbstractConstructionHeuristicTest;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.nqueens.domain.NQueens;

class NQueensConstructionHeuristicTest extends AbstractConstructionHeuristicTest<NQueens> {

    @Override
    protected CommonApp<NQueens> createCommonApp() {
        return new NQueensApp();
    }

    @Override
    protected Stream<String> unsolvedFileNames() {
        return Stream.of("4queens.json", "8queens.json");
    }
}
