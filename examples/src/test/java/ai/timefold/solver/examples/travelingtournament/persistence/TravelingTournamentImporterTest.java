package ai.timefold.solver.examples.travelingtournament.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.travelingtournament.app.TravelingTournamentApp;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;

class TravelingTournamentImporterTest extends ImportDataFilesTest<TravelingTournament> {

    @Override
    protected AbstractSolutionImporter<TravelingTournament> createSolutionImporter() {
        return new TravelingTournamentImporter();
    }

    @Override
    protected String getDataDirName() {
        return TravelingTournamentApp.DATA_DIR_NAME;
    }
}
