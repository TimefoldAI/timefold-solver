package ai.timefold.solver.core.impl.score.stream.common;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionManagerTest;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListValue;

import org.junit.jupiter.api.Test;

public abstract class AbstractSolutionManagerTest {

    protected abstract ScoreDirectorFactoryConfig buildScoreDirectorFactoryConfig();

    protected abstract ScoreDirectorFactoryConfig buildUnassignedWithPinningScoreDirectorFactoryConfig();

    @Test
    void indictmentsPresentOnFreshExplanation() {
        // Create the environment.
        var scoreDirectorFactoryConfig = buildScoreDirectorFactoryConfig();
        var solverConfig = new SolverConfig();
        solverConfig.setSolutionClass(TestdataSolution.class);
        solverConfig.setEntityClassList(Collections.singletonList(TestdataEntity.class));
        solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        var solutionManager =
                SolutionManagerTest.SolutionManagerSource.FROM_SOLVER_FACTORY.createSolutionManager(solverFactory);

        // Prepare the solution.
        var entityCount = 3;
        var solution = TestdataSolution.generateSolution(2, entityCount);
        var scoreExplanation = solutionManager.explain(solution);

        // Check for expected results.
        assertSoftly(softly -> {
            softly.assertThat(scoreExplanation.getScore())
                    .isEqualTo(SimpleScore.of(-entityCount));
            softly.assertThat(scoreExplanation.getConstraintMatchTotalMap())
                    .isNotEmpty();
            softly.assertThat(scoreExplanation.getIndictmentMap())
                    .isNotEmpty();
            var constraintJustificationList = (List) scoreExplanation.getJustificationList();
            softly.assertThat(constraintJustificationList)
                    .isNotEmpty();
            softly.assertThat(scoreExplanation.getJustificationList(DefaultConstraintJustification.class))
                    .containsExactlyElementsOf(constraintJustificationList);
        });
    }

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
