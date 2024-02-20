package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.ScoreExplanation;
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
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned_values.TestdataAllowsUnassignedValuesListValue;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractSolutionManagerTest {

    protected abstract ScoreDirectorFactoryConfig buildScoreDirectorFactoryConfig();

    protected abstract ScoreDirectorFactoryConfig buildUnassignedScoreDirectorFactoryConfig();

    @Test
    void indictmentsPresentOnFreshExplanation() {
        // Create the environment.
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = buildScoreDirectorFactoryConfig();
        SolverConfig solverConfig = new SolverConfig();
        solverConfig.setSolutionClass(TestdataSolution.class);
        solverConfig.setEntityClassList(Collections.singletonList(TestdataEntity.class));
        solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        SolutionManager<TestdataSolution, SimpleScore> solutionManager =
                SolutionManagerTest.SolutionManagerSource.FROM_SOLVER_FACTORY.createSolutionManager(solverFactory);

        // Prepare the solution.
        int entityCount = 3;
        TestdataSolution solution = TestdataSolution.generateSolution(2, entityCount);
        ScoreExplanation<TestdataSolution, SimpleScore> scoreExplanation = solutionManager.explain(solution);

        // Check for expected results.
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(scoreExplanation.getScore())
                    .isEqualTo(SimpleScore.of(-entityCount));
            softly.assertThat(scoreExplanation.getConstraintMatchTotalMap())
                    .isNotEmpty();
            softly.assertThat(scoreExplanation.getIndictmentMap())
                    .isNotEmpty();
            List<DefaultConstraintJustification> constraintJustificationList = (List) scoreExplanation.getJustificationList();
            softly.assertThat(constraintJustificationList)
                    .isNotEmpty();
            softly.assertThat(scoreExplanation.getJustificationList(DefaultConstraintJustification.class))
                    .containsExactlyElementsOf(constraintJustificationList);
        });
    }

    @Test
    void updateAssignedValueWithNullInverseRelation() {
        // Create the environment.
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = buildUnassignedScoreDirectorFactoryConfig();
        SolverConfig solverConfig = new SolverConfig();
        solverConfig.setSolutionClass(TestdataAllowsUnassignedValuesListSolution.class);
        solverConfig.setEntityClassList(
                List.of(TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class));
        solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        SolverFactory<TestdataAllowsUnassignedValuesListSolution> solverFactory = SolverFactory.create(solverConfig);
        SolutionManager<TestdataAllowsUnassignedValuesListSolution, SimpleScore> solutionManager =
                SolutionManagerTest.SolutionManagerSource.FROM_SOLVER_FACTORY.createSolutionManager(solverFactory);

        // Prepare the solution.
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        var entity = new TestdataAllowsUnassignedValuesListEntity("e1");
        var assignedValue = new TestdataAllowsUnassignedValuesListValue("assigned");

        entity.setValueList(List.of(assignedValue));

        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(assignedValue));
        solutionManager.update(solution, SolutionUpdatePolicy.UPDATE_SHADOW_VARIABLES_ONLY);

        assertThat(assignedValue.getEntity()).isSameAs(entity);
        assertThat(assignedValue.getIndex()).isZero();
    }

}
