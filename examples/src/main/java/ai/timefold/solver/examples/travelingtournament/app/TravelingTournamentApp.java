package ai.timefold.solver.examples.travelingtournament.app;

import java.util.Collections;
import java.util.Set;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionExporter;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;
import ai.timefold.solver.examples.travelingtournament.persistence.TravelingTournamentExporter;
import ai.timefold.solver.examples.travelingtournament.persistence.TravelingTournamentImporter;
import ai.timefold.solver.examples.travelingtournament.persistence.TravelingTournamentSolutionFileIO;
import ai.timefold.solver.examples.travelingtournament.swingui.TravelingTournamentPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

/**
 * WARNING: This is an old, complex, tailored example. You're probably better off with one of the other examples.
 */
public class TravelingTournamentApp extends CommonApp<TravelingTournament> {

    public static final String SOLVER_CONFIG =
            "ai/timefold/solver/examples/travelingtournament/travelingTournamentSolverConfig.xml";

    public static final String DATA_DIR_NAME = "travelingtournament";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new TravelingTournamentApp().init();
    }

    public TravelingTournamentApp() {
        super("Traveling tournament",
                "Official competition name: TTP - Traveling tournament problem\n\n" +
                        "Assign sport matches to days. Minimize the distance travelled.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                TravelingTournamentPanel.LOGO_PATH);
    }

    @Override
    protected TravelingTournamentPanel createSolutionPanel() {
        return new TravelingTournamentPanel();
    }

    @Override
    public SolutionFileIO<TravelingTournament> createSolutionFileIO() {
        return new TravelingTournamentSolutionFileIO();
    }

    @Override
    protected Set<AbstractSolutionImporter<TravelingTournament>> createSolutionImporters() {
        return Collections.singleton(new TravelingTournamentImporter());
    }

    @Override
    protected Set<AbstractSolutionExporter<TravelingTournament>> createSolutionExporters() {
        return Collections.singleton(new TravelingTournamentExporter());
    }

}
