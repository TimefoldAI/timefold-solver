package ai.timefold.solver.examples.curriculumcourse.app;

import java.util.Collections;
import java.util.Set;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionExporter;
import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;
import ai.timefold.solver.examples.curriculumcourse.persistence.CurriculumCourseExporter;
import ai.timefold.solver.examples.curriculumcourse.persistence.CurriculumCourseImporter;
import ai.timefold.solver.examples.curriculumcourse.persistence.CurriculumCourseSolutionFileIO;
import ai.timefold.solver.examples.curriculumcourse.swingui.CurriculumCoursePanel;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

public class CurriculumCourseApp extends CommonApp<CourseSchedule> {

    public static final String SOLVER_CONFIG =
            "ai/timefold/solver/examples/curriculumcourse/curriculumCourseSolverConfig.xml";

    public static final String DATA_DIR_NAME = "curriculumcourse";

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new CurriculumCourseApp().init();
    }

    public CurriculumCourseApp() {
        super("Course timetabling",
                "Official competition name: ITC 2007 track3 - Curriculum course scheduling\n\n" +
                        "Assign lectures to periods and rooms.",
                SOLVER_CONFIG, DATA_DIR_NAME,
                CurriculumCoursePanel.LOGO_PATH);
    }

    @Override
    protected CurriculumCoursePanel createSolutionPanel() {
        return new CurriculumCoursePanel();
    }

    @Override
    public SolutionFileIO<CourseSchedule> createSolutionFileIO() {
        return new CurriculumCourseSolutionFileIO();
    }

    @Override
    protected Set<AbstractSolutionImporter<CourseSchedule>> createSolutionImporters() {
        return Collections.singleton(new CurriculumCourseImporter());
    }

    @Override
    protected Set<AbstractSolutionExporter<CourseSchedule>> createSolutionExporters() {
        return Collections.singleton(new CurriculumCourseExporter());
    }

}
