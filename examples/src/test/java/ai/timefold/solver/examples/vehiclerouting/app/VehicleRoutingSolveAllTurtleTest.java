package ai.timefold.solver.examples.vehiclerouting.app;

import ai.timefold.solver.examples.common.TestSystemProperties;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.ImportDirSolveAllTurtleTest;
import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;
import ai.timefold.solver.examples.vehiclerouting.optional.score.VehicleRoutingEasyScoreCalculator;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = TestSystemProperties.TURTLE_TEST_SELECTION, matches = "vehiclerouting|all")
class VehicleRoutingSolveAllTurtleTest extends ImportDirSolveAllTurtleTest<VehicleRoutingSolution> {

    @Override
    protected CommonApp<VehicleRoutingSolution> createCommonApp() {
        return new VehicleRoutingApp();
    }

    @Override
    protected Class<VehicleRoutingEasyScoreCalculator> overwritingEasyScoreCalculatorClass() {
        return VehicleRoutingEasyScoreCalculator.class;
    }
}
