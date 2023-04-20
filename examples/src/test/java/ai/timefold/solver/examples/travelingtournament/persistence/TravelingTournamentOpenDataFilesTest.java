package ai.timefold.solver.examples.travelingtournament.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.travelingtournament.app.TravelingTournamentApp;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;

class TravelingTournamentOpenDataFilesTest extends OpenDataFilesTest<TravelingTournament> {

    @Override
    protected CommonApp<TravelingTournament> createCommonApp() {
        return new TravelingTournamentApp();
    }
}
