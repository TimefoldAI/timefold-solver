
package ai.timefold.solver.examples.conferencescheduling.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.conferencescheduling.domain.ConferenceSolution;

class ConferenceSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<ConferenceSolution> {

    @Override
    protected CommonApp<ConferenceSolution> createCommonApp() {
        return new ConferenceSchedulingApp();
    }
}
