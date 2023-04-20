package ai.timefold.solver.examples.projectjobscheduling.app;

import java.util.Collections;
import java.util.Set;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.projectjobscheduling.domain.Schedule;
import ai.timefold.solver.examples.projectjobscheduling.persistence.ProjectJobSchedulingImporter;
import ai.timefold.solver.examples.projectjobscheduling.persistence.ProjectJobSchedulingSolutionFileIO;
import ai.timefold.solver.examples.projectjobscheduling.swingui.ProjectJobSchedulingPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class ProjectJobSchedulingApp extends CommonApp<Schedule> {

    public static final String SOLVER_CONFIG =
            "ai/timefold/solver/examples/projectjobscheduling/projectJobSchedulingSolverConfig.xml";

    public static final String DATA_DIR_NAME = "projectjobscheduling";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new ProjectJobSchedulingApp().init();
    }

    public ProjectJobSchedulingApp() {
        super("Project job scheduling",
                "Official competition name:" +
                        " multi-mode resource-constrained multi-project scheduling problem (MRCMPSP)\n\n" +
                        "Schedule all jobs in time and execution mode.\n\n" +
                        "Minimize project delays.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                ProjectJobSchedulingPanel.LOGO_PATH);
    }

    @Override
    protected ProjectJobSchedulingPanel createSolutionPanel() {
        return new ProjectJobSchedulingPanel();
    }

    @Override
    public SolutionFileIO<Schedule> createSolutionFileIO() {
        return new ProjectJobSchedulingSolutionFileIO();
    }

    @Override
    protected Set<AbstractSolutionImporter<Schedule>> createSolutionImporters() {
        return Collections.singleton(new ProjectJobSchedulingImporter());
    }

}
