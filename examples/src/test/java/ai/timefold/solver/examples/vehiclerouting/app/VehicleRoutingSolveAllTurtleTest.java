package ai.timefold.solver.examples.vehiclerouting.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.ImportDirSolveAllTurtleTest;
import ai.timefold.solver.examples.vehiclerouting.domain.VehicleRoutingSolution;
import ai.timefold.solver.examples.vehiclerouting.optional.score.VehicleRoutingEasyScoreCalculator;

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
