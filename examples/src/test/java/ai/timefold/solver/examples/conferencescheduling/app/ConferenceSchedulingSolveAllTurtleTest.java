
package ai.timefold.solver.examples.conferencescheduling.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.conferencescheduling.domain.ConferenceSolution;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "ai.timefold.solver.examples.turtle", matches = "conferencescheduling")
class ConferenceSchedulingSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<ConferenceSolution> {

    @Override
    protected CommonApp<ConferenceSolution> createCommonApp() {
        return new ConferenceSchedulingApp();
    }
}
