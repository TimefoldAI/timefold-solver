package ai.timefold.solver.examples.tsp.app;

import java.util.stream.Stream;

import ai.timefold.solver.examples.common.app.AbstractConstructionHeuristicTest;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.tsp.domain.TspSolution;

class TspConstructionHeuristicTest extends AbstractConstructionHeuristicTest<TspSolution> {

    @Override
    protected CommonApp<TspSolution> createCommonApp() {
        return new TspApp();
    }

    @Override
    protected Stream<String> unsolvedFileNames() {
        return Stream.of("europe40.json");
    }
}
