package ai.timefold.solver.examples.nqueens.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.nqueens.domain.NQueens;
import ai.timefold.solver.examples.nqueens.persistence.NQueensSolutionFileIO;
import ai.timefold.solver.examples.nqueens.swingui.NQueensPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

/**
 * For an easy example, look at {@link NQueensHelloWorld} instead.
 */
public class NQueensApp extends CommonApp<NQueens> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/nqueens/nqueensSolverConfig.xml";

    public static final String DATA_DIR_NAME = "nqueens";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new NQueensApp().init();
    }

    public NQueensApp() {
        super("N queens",
                "Place queens on a chessboard.\n\n" +
                        "No 2 queens must be able to attack each other.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                NQueensPanel.LOGO_PATH);
    }

    @Override
    protected NQueensPanel createSolutionPanel() {
        return new NQueensPanel();
    }

    @Override
    public SolutionFileIO<NQueens> createSolutionFileIO() {
        return new NQueensSolutionFileIO();
    }

}
