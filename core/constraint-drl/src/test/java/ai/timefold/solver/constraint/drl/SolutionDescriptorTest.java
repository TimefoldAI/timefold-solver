package ai.timefold.solver.constraint.drl;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Collections;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.solutionproperties.TestdataNoProblemFactPropertySolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SolutionDescriptorTest {

    @Test
    void noProblemFactPropertyWithDroolsScoreCalculation() {
        assertThatIllegalStateException().isThrownBy(() -> buildSolverFactoryWithDroolsScoreDirector(
                TestdataNoProblemFactPropertySolution.class, TestdataEntity.class));
    }

    private static <Solution_> SolverFactory<Solution_> buildSolverFactoryWithDroolsScoreDirector(
            Class<Solution_> solutionClass, Class<?>... entityClasses) {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(solutionClass, entityClasses);
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig =
                solverConfig.getScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(null);
        scoreDirectorFactoryConfig.setScoreDrlList(
                Collections.singletonList("ai/timefold/solver/constraint/drl/dummySimpleScoreDroolsConstraints.drl"));
        return SolverFactory.create(solverConfig);
    }

}
