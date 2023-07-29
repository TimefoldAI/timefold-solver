package ai.timefold.solver.examples.curriculumcourse.app;

import ai.timefold.solver.examples.common.TestSystemProperties;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = TestSystemProperties.TURTLE_TEST_SELECTION, matches = "curriculumcourse|all")
class CurriculumCourseSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<CourseSchedule> {

    @Override
    protected CommonApp<CourseSchedule> createCommonApp() {
        return new CurriculumCourseApp();
    }
}
