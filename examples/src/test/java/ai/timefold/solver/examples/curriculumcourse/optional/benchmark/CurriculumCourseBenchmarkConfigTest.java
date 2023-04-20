package ai.timefold.solver.examples.curriculumcourse.optional.benchmark;

import ai.timefold.solver.examples.common.app.AbstractBenchmarkConfigTest;
import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

class CurriculumCourseBenchmarkConfigTest extends AbstractBenchmarkConfigTest {

    @Override
    protected CommonBenchmarkApp getBenchmarkApp() {
        return new CurriculumCourseBenchmarkApp();
    }
}
