package ai.timefold.solver.examples.curriculumcourse.app;

import java.util.function.Predicate;
import java.util.stream.Stream;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.examples.common.app.AbstractConstructionHeuristicTest;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.curriculumcourse.domain.CourseSchedule;

class CurriculumCourseConstructionHeuristicTest extends AbstractConstructionHeuristicTest<CourseSchedule> {

    @Override
    protected Predicate<ConstructionHeuristicType> includeConstructionHeuristicType() {
        /*
         * TODO Delete this temporary workaround to ignore ALLOCATE_TO_VALUE_FROM_QUEUE,
         * see https://issues.redhat.com/browse/PLANNER-486
         */
        return constructionHeuristicType -> constructionHeuristicType != ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE;
    }

    @Override
    protected CommonApp<CourseSchedule> createCommonApp() {
        return new CurriculumCourseApp();
    }

    @Override
    protected Stream<String> unsolvedFileNames() {
        return Stream.of("toy01.json");
    }
}
