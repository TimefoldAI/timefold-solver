package ai.timefold.solver.core.preview.api.variable.declarative.simple_list;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.declarative.simple_list.TestdataDeclarativeSimpleListConstraintProvider;
import ai.timefold.solver.core.testdomain.declarative.simple_list.TestdataDeclarativeSimpleListEntity;
import ai.timefold.solver.core.testdomain.declarative.simple_list.TestdataDeclarativeSimpleListSolution;
import ai.timefold.solver.core.testdomain.declarative.simple_list.TestdataDeclarativeSimpleListValue;

import org.junit.jupiter.api.Test;

class SimpleListTest {
    @Test
    void simpleList() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataDeclarativeSimpleListSolution.class)
                .withEntityClasses(TestdataDeclarativeSimpleListEntity.class, TestdataDeclarativeSimpleListValue.class)
                .withConstraintProviderClass(TestdataDeclarativeSimpleListConstraintProvider.class)
                .withPreviewFeature(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("-344"));

        var entityList = List.of(new TestdataDeclarativeSimpleListEntity("e1", 0, 0));
        var valueList = List.of(
                new TestdataDeclarativeSimpleListValue("v1", 1, 60),
                new TestdataDeclarativeSimpleListValue("v2", 2, 120),
                new TestdataDeclarativeSimpleListValue("v3", 3, 30));
        var problem = new TestdataDeclarativeSimpleListSolution(entityList, valueList);
        var solverFactory = SolverFactory.<TestdataDeclarativeSimpleListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        var solution = solver.solve(problem);

        // Note that we minimize the end time of all values, and not
        // the end time of the last value. Since duration is cumulative and the
        // difference in duration is larger than the difference in positions,
        // the solver wants to do the shortest duration first, and thus the
        // best solution is the one that maximizes distance.
        assertThat(solution.getEntityList().get(0).getValues())
                .map(TestdataObject::getCode)
                .containsExactly("v3", "v1", "v2");

        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        var v3 = solution.getValueList().get(2);

        assertThat(v3.getStartTime()).isEqualTo(3);
        assertThat(v3.getEndTime()).isEqualTo(33);

        assertThat(v1.getStartTime()).isEqualTo(35);
        assertThat(v1.getEndTime()).isEqualTo(95);

        assertThat(v2.getStartTime()).isEqualTo(96);
        assertThat(v2.getEndTime()).isEqualTo(216);
    }
}
