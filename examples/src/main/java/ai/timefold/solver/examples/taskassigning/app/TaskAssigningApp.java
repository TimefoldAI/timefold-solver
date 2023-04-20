package ai.timefold.solver.examples.taskassigning.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.taskassigning.domain.TaskAssigningSolution;
import ai.timefold.solver.examples.taskassigning.persistence.TaskAssigningSolutionFileIO;
import ai.timefold.solver.examples.taskassigning.swingui.TaskAssigningPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class TaskAssigningApp extends CommonApp<TaskAssigningSolution> {

    public static final String SOLVER_CONFIG =
            "ai/timefold/solver/examples/taskassigning/taskAssigningSolverConfig.xml";

    public static final String DATA_DIR_NAME = "taskassigning";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new TaskAssigningApp().init();
    }

    public TaskAssigningApp() {
        super("Task assigning",
                "Assign tasks to employees in a sequence.\n\n"
                        + "Match skills and affinity.\n"
                        + "Prioritize critical tasks.\n"
                        + "Minimize the makespan.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                TaskAssigningPanel.LOGO_PATH);
    }

    @Override
    protected TaskAssigningPanel createSolutionPanel() {
        return new TaskAssigningPanel();
    }

    @Override
    public SolutionFileIO<TaskAssigningSolution> createSolutionFileIO() {
        return new TaskAssigningSolutionFileIO();
    }

}
