package ai.timefold.solver.examples.curriculumcourse.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

class CurriculumCourseSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<CourseSchedule> {

    @Override
    protected CommonApp<CourseSchedule> createCommonApp() {
        return new CurriculumCourseApp();
    }
}
