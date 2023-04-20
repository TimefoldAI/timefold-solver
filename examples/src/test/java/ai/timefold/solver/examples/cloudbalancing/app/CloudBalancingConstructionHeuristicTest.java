package ai.timefold.solver.examples.cloudbalancing.app;

import java.util.stream.Stream;

import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.common.app.AbstractConstructionHeuristicTest;
import ai.timefold.solver.examples.common.app.CommonApp;

class CloudBalancingConstructionHeuristicTest extends AbstractConstructionHeuristicTest<CloudBalance> {

    @Override
    protected CommonApp<CloudBalance> createCommonApp() {
        return new CloudBalancingApp();
    }

    @Override
    protected Stream<String> unsolvedFileNames() {
        return Stream.of("2computers-6processes.json", "3computers-9processes.json");
    }
}
