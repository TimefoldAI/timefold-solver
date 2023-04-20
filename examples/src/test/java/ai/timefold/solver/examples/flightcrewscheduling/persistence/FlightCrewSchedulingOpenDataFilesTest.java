package ai.timefold.solver.examples.flightcrewscheduling.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.flightcrewscheduling.app.FlightCrewSchedulingApp;
import ai.timefold.solver.examples.flightcrewscheduling.domain.FlightCrewSolution;

class FlightCrewSchedulingOpenDataFilesTest extends OpenDataFilesTest<FlightCrewSolution> {

    @Override
    protected CommonApp<FlightCrewSolution> createCommonApp() {
        return new FlightCrewSchedulingApp();
    }
}
