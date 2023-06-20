package ai.timefold.solver.examples.travelingtournament.app;

import ai.timefold.solver.examples.common.TestSystemProperties;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = TestSystemProperties.TURTLE_TEST_SELECTION, matches = "travelingtournament")
class TravelingTournamentSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<TravelingTournament> {

    @Override
    protected CommonApp<TravelingTournament> createCommonApp() {
        return new TravelingTournamentApp();
    }
}
