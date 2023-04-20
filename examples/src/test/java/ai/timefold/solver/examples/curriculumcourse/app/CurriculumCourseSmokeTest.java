package ai.timefold.solver.examples.curriculumcourse.app;

import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.examples.common.app.SolverSmokeTest;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

class CurriculumCourseSmokeTest extends SolverSmokeTest<CourseSchedule, HardSoftScore> {

    private static final String UNSOLVED_DATA_FILE = "data/curriculumcourse/unsolved/comp01_initialized.json";

    @Override
    protected CurriculumCourseApp createCommonApp() {
        return new CurriculumCourseApp();
    }

    @Override
    protected Stream<TestData<HardSoftScore>> testData() {
        return Stream.of(
                TestData.of(ConstraintStreamImplType.BAVET, UNSOLVED_DATA_FILE,
                        HardSoftScore.ofSoft(-55),
                        HardSoftScore.ofSoft(-64)));
    }
}
