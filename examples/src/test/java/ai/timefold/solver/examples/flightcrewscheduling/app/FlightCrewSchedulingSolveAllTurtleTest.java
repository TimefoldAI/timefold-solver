package ai.timefold.solver.examples.flightcrewscheduling.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.flightcrewscheduling.domain.FlightCrewSolution;

class FlightCrewSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<FlightCrewSolution> {

    @Override
    protected CommonApp<FlightCrewSolution> createCommonApp() {
        return new FlightCrewSchedulingApp();
    }
}
