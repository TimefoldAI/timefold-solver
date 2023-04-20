package ai.timefold.solver.examples.nurserostering.app;

import java.util.Collections;
import java.util.Set;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionExporter;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;
import ai.timefold.solver.examples.nurserostering.persistence.NurseRosterSolutionFileIO;
import ai.timefold.solver.examples.nurserostering.persistence.NurseRosteringExporter;
import ai.timefold.solver.examples.nurserostering.persistence.NurseRosteringImporter;
import ai.timefold.solver.examples.nurserostering.swingui.NurseRosteringPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class NurseRosteringApp extends CommonApp<NurseRoster> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/nurserostering/nurseRosteringSolverConfig.xml";

    public static final String DATA_DIR_NAME = "nurserostering";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new NurseRosteringApp().init();
    }

    public NurseRosteringApp() {
        super("Nurse rostering",
                "Official competition name: INRC2010 - Nurse rostering\n\n" +
                        "Assign shifts to nurses.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                NurseRosteringPanel.LOGO_PATH);
    }

    @Override
    protected NurseRosteringPanel createSolutionPanel() {
        return new NurseRosteringPanel();
    }

    @Override
    public SolutionFileIO<NurseRoster> createSolutionFileIO() {
        return new NurseRosterSolutionFileIO();
    }

    @Override
    protected Set<AbstractSolutionImporter<NurseRoster>> createSolutionImporters() {
        return Collections.singleton(new NurseRosteringImporter());
    }

    @Override
    protected Set<AbstractSolutionExporter<NurseRoster>> createSolutionExporters() {
        return Collections.singleton(new NurseRosteringExporter());
    }

}
