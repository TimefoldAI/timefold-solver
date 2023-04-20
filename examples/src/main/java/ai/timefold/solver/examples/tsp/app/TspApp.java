package ai.timefold.solver.examples.tsp.app;

import java.util.Set;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionExporter;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.tsp.domain.TspSolution;
import ai.timefold.solver.examples.tsp.persistence.SvgTspLineAndCircleExporter;
import ai.timefold.solver.examples.tsp.persistence.SvgTspPathExporter;
import ai.timefold.solver.examples.tsp.persistence.TspExporter;
import ai.timefold.solver.examples.tsp.persistence.TspImageStipplerImporter;
import ai.timefold.solver.examples.tsp.persistence.TspImporter;
import ai.timefold.solver.examples.tsp.persistence.TspSolutionFileIO;
import ai.timefold.solver.examples.tsp.swingui.TspPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class TspApp extends CommonApp<TspSolution> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/tsp/tspSolverConfig.xml";

    public static final String DATA_DIR_NAME = "tsp";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new TspApp().init();
    }

    public TspApp() {
        super("Traveling salesman",
                "Official competition name: TSP - Traveling salesman problem\n\n" +
                        "Determine the order in which to visit all cities.\n\n" +
                        "Find the shortest route to visit all cities.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                TspPanel.LOGO_PATH);
    }

    @Override
    protected TspPanel createSolutionPanel() {
        return new TspPanel();
    }

    @Override
    public SolutionFileIO<TspSolution> createSolutionFileIO() {
        return new TspSolutionFileIO();
    }

    @Override
    protected Set<AbstractSolutionImporter<TspSolution>> createSolutionImporters() {
        return Set.of(
                new TspImporter(),
                new TspImageStipplerImporter());
    }

    @Override
    protected Set<AbstractSolutionExporter<TspSolution>> createSolutionExporters() {
        return Set.of(
                new TspExporter(),
                new SvgTspPathExporter(),
                new SvgTspLineAndCircleExporter());
    }

}
