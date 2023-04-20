package ai.timefold.solver.examples.curriculumcourse.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractSolutionImporter;
import ai.timefold.solver.examples.common.persistence.ImportDataFilesTest;
import ai.timefold.solver.examples.curriculumcourse.app.CurriculumCourseApp;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

class CurriculumCourseImporterTest extends ImportDataFilesTest<CourseSchedule> {

    @Override
    protected AbstractSolutionImporter<CourseSchedule> createSolutionImporter() {
        return new CurriculumCourseImporter();
    }

    @Override
    protected String getDataDirName() {
        return CurriculumCourseApp.DATA_DIR_NAME;
    }
}
