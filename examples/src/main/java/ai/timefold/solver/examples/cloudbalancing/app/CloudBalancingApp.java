package ai.timefold.solver.examples.cloudbalancing.app;

import ai.timefold.solver.examples.cloudbalancing.domain.CloudBalance;
import ai.timefold.solver.examples.cloudbalancing.persistence.CloudBalanceSolutionFileIO;
import ai.timefold.solver.examples.cloudbalancing.swingui.CloudBalancingPanel;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

/**
 * For an easy example, look at {@link CloudBalancingHelloWorld} instead.
 */
public class CloudBalancingApp extends CommonApp<CloudBalance> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/cloudbalancing/cloudBalancingSolverConfig.xml";

    public static final String DATA_DIR_NAME = "cloudbalancing";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new CloudBalancingApp().init();
    }

    public CloudBalancingApp() {
        super("Cloud balancing",
                "Assign processes to computers.\n\n" +
                        "Each computer must have enough hardware to run all of its processes.\n" +
                        "Each used computer inflicts a maintenance cost.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                CloudBalancingPanel.LOGO_PATH);
    }

    @Override
    protected CloudBalancingPanel createSolutionPanel() {
        return new CloudBalancingPanel();
    }

    @Override
    public SolutionFileIO<CloudBalance> createSolutionFileIO() {
        return new CloudBalanceSolutionFileIO();
    }

}
