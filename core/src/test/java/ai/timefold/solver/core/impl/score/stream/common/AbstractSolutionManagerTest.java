package ai.timefold.solver.core.impl.score.stream.common;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionManagerTest;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListValue;

import org.junit.jupiter.api.Test;

public abstract class AbstractSolutionManagerTest {

    protected abstract ScoreDirectorFactoryConfig buildScoreDirectorFactoryConfig();

    protected abstract ScoreDirectorFactoryConfig buildUnassignedWithPinningScoreDirectorFactoryConfig();

    @Test
    void updateAssignedValueWithNullInverseRelation() {
        // Create the environment.
        var scoreDirectorFactoryConfig = buildUnassignedWithPinningScoreDirectorFactoryConfig();
        var solverConfig = new SolverConfig();
        solverConfig.setSolutionClass(TestdataPinnedUnassignedValuesListSolution.class);
        solverConfig.setEntityClassList(
                List.of(TestdataPinnedUnassignedValuesListEntity.class,
                        TestdataPinnedUnassignedValuesListValue.class));
        solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        SolverFactory<TestdataPinnedUnassignedValuesListSolution> solverFactory = SolverFactory.create(solverConfig);
        SolutionManager<TestdataPinnedUnassignedValuesListSolution, SimpleScore> solutionManager =
                SolutionManagerTest.SolutionManagerSource.FROM_SOLVER_FACTORY.createSolutionManager(solverFactory);

        // Prepare the solution.
        var solution = new TestdataPinnedUnassignedValuesListSolution();
        var entity = new TestdataPinnedUnassignedValuesListEntity("e1");
        var assignedValue = new TestdataPinnedUnassignedValuesListValue("assigned");

        entity.setValueList(List.of(assignedValue));

        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(assignedValue));
        solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SHADOW_VARIABLES_ONLY);

        assertSoftly(softly -> {
            softly.assertThat(assignedValue.getEntity()).isSameAs(entity);
            softly.assertThat(assignedValue.getIndex()).isZero();
        });
    }

}
