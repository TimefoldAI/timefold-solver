package ai.timefold.solver.examples.curriculumcourse.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.curriculumcourse.app.CurriculumCourseApp;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

class CurriculumCourseOpenDataFilesTest extends OpenDataFilesTest<CourseSchedule> {

    @Override
    protected CommonApp<CourseSchedule> createCommonApp() {
        return new CurriculumCourseApp();
    }
}
