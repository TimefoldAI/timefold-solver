package ai.timefold.solver.examples.pas.app;

import java.util.Collections;
import java.util.Set;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionExporter;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.pas.domain.PatientAdmissionSchedule;
import ai.timefold.solver.examples.pas.persistence.PatientAdmissionScheduleExporter;
import ai.timefold.solver.examples.pas.persistence.PatientAdmissionScheduleImporter;
import ai.timefold.solver.examples.pas.persistence.PatientAdmissionScheduleSolutionFileIO;
import ai.timefold.solver.examples.pas.swingui.PatientAdmissionSchedulePanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class PatientAdmissionScheduleApp extends CommonApp<PatientAdmissionSchedule> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/pas/patientAdmissionScheduleSolverConfig.xml";

    public static final String DATA_DIR_NAME = "pas";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new PatientAdmissionScheduleApp().init();
    }

    public PatientAdmissionScheduleApp() {
        super("Hospital bed planning",
                "Official competition name: PAS - Patient admission scheduling\n\n" +
                        "Assign patients to beds.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                PatientAdmissionSchedulePanel.LOGO_PATH);
    }

    @Override
    protected PatientAdmissionSchedulePanel createSolutionPanel() {
        return new PatientAdmissionSchedulePanel();
    }

    @Override
    public SolutionFileIO<PatientAdmissionSchedule> createSolutionFileIO() {
        return new PatientAdmissionScheduleSolutionFileIO();
    }

    @Override
    protected Set<AbstractSolutionImporter<PatientAdmissionSchedule>> createSolutionImporters() {
        return Collections.singleton(new PatientAdmissionScheduleImporter());
    }

    @Override
    protected Set<AbstractSolutionExporter<PatientAdmissionSchedule>> createSolutionExporters() {
        return Collections.singleton(new PatientAdmissionScheduleExporter());
    }

}
