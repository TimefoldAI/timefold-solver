package ai.timefold.solver.examples.curriculumcourse.app;

import java.util.stream.Stream;

import ai.timefold.solver.examples.common.app.AbstractExhaustiveSearchTest;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

class CurriculumCourseExhaustiveSearchTest extends AbstractExhaustiveSearchTest<CourseSchedule> {

    @Override
    protected CommonApp<CourseSchedule> createCommonApp() {
        return new CurriculumCourseApp();
    }

    @Override
    protected Stream<String> unsolvedFileNames() {
        return Stream.of("toy01.json");
    }
}
