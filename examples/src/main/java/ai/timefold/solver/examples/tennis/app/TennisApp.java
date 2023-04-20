package ai.timefold.solver.examples.tennis.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.tennis.domain.TennisSolution;
import ai.timefold.solver.examples.tennis.persistence.TennisSolutionFileIO;
import ai.timefold.solver.examples.tennis.swingui.TennisPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class TennisApp extends CommonApp<TennisSolution> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/tennis/tennisSolverConfig.xml";

    public static final String DATA_DIR_NAME = "tennis";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new TennisApp().init();
    }

    public TennisApp() {
        super("Tennis club scheduling",
                "Assign available spots to teams.\n\n" +
                        "Each team must play an almost equal number of times.\n" +
                        "Each team must play against each other team an almost equal number of times.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                TennisPanel.LOGO_PATH);
    }

    @Override
    protected TennisPanel createSolutionPanel() {
        return new TennisPanel();
    }

    @Override
    public SolutionFileIO<TennisSolution> createSolutionFileIO() {
        return new TennisSolutionFileIO();
    }

}
