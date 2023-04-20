package ai.timefold.solver.examples.curriculumcourse.optional.benchmark;

import ai.timefold.solver.examples.common.app.CommonBenchmarkApp;

public class CurriculumCourseBenchmarkApp extends CommonBenchmarkApp {

    public static void main(String[] args) {
        new CurriculumCourseBenchmarkApp().buildAndBenchmark(args);
    }

    public CurriculumCourseBenchmarkApp() {
        super(
                new ArgOption("default",
                        "ai/timefold/solver/examples/curriculumcourse/optional/benchmark/curriculumCourseBenchmarkConfig.xml"),
                new ArgOption("stepLimit",
                        "ai/timefold/solver/examples/curriculumcourse/optional/benchmark/curriculumCourseStepLimitBenchmarkConfig.xml"),
                new ArgOption("template",
                        "ai/timefold/solver/examples/curriculumcourse/optional/benchmark/curriculumCourseBenchmarkConfigTemplate.xml.ftl",
                        true));
    }

}
