package ai.timefold.solver.examples.examination.app;

import java.util.Collections;
import java.util.Set;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionExporter;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.curriculumcourse.app.CurriculumCourseApp;
import ai.timefold.solver.examples.examination.domain.Examination;
import ai.timefold.solver.examples.examination.persistence.ExaminationExporter;
import ai.timefold.solver.examples.examination.persistence.ExaminationImporter;
import ai.timefold.solver.examples.examination.persistence.ExaminationSolutionFileIO;
import ai.timefold.solver.examples.examination.swingui.ExaminationPanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

/**
 * Examination is super optimized and a bit complex.
 * {@link CurriculumCourseApp} is arguably a better example to learn from.
 */
public class ExaminationApp extends CommonApp<Examination> {

    public static final String SOLVER_CONFIG = "ai/timefold/solver/examples/examination/examinationSolverConfig.xml";

    public static final String DATA_DIR_NAME = "examination";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new ExaminationApp().init();
    }

    public ExaminationApp() {
        super("Exam timetabling",
                "Official competition name: ITC 2007 track1 - Examination timetabling\n\n" +
                        "Assign exams to timeslots and rooms.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                ExaminationPanel.LOGO_PATH);
    }

    @Override
    protected ExaminationPanel createSolutionPanel() {
        return new ExaminationPanel();
    }

    @Override
    public SolutionFileIO<Examination> createSolutionFileIO() {
        return new ExaminationSolutionFileIO();
    }

    @Override
    protected Set<AbstractSolutionImporter<Examination>> createSolutionImporters() {
        return Collections.singleton(new ExaminationImporter());
    }

    @Override
    protected Set<AbstractSolutionExporter<Examination>> createSolutionExporters() {
        return Collections.singleton(new ExaminationExporter());
    }

}
