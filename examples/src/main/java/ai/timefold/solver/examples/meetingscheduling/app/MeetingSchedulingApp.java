package ai.timefold.solver.examples.meetingscheduling.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.meetingscheduling.domain.MeetingSchedule;
import ai.timefold.solver.examples.meetingscheduling.persistence.MeetingSchedulingXlsxFileIO;
import ai.timefold.solver.examples.meetingscheduling.swingui.MeetingSchedulingPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class MeetingSchedulingApp extends CommonApp<MeetingSchedule> {

    public static final String SOLVER_CONFIG =
            "ai/timefold/solver/examples/meetingscheduling/meetingSchedulingSolverConfig.xml";

    public static final String DATA_DIR_NAME = "meetingscheduling";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new MeetingSchedulingApp().init();
    }

    public MeetingSchedulingApp() {
        super("Meeting scheduling",
                "Assign meetings a starting time and a room.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                MeetingSchedulingPanel.LOGO_PATH);
    }

    @Override
    protected MeetingSchedulingPanel createSolutionPanel() {
        return new MeetingSchedulingPanel();
    }

    @Override
    public SolutionFileIO<MeetingSchedule> createSolutionFileIO() {
        return new MeetingSchedulingXlsxFileIO();
    }

}
