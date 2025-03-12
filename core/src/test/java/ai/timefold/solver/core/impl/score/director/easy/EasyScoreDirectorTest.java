package ai.timefold.solver.core.impl.score.director.easy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.corrupted.TestdataCorruptedShadowedEntity;
import ai.timefold.solver.core.impl.testdata.domain.shadow.corrupted.TestdataCorruptedShadowedSolution;

import org.junit.jupiter.api.Test;

class EasyScoreDirectorTest {

    @Test
    void shadowVariableCorruption() {
        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(TestdataCorruptedShadowedSolution.buildSolutionDescriptor(),
                (solution_) -> SimpleScore.of(0));
        scoreDirectorFactory
                .setInitializingScoreTrend(InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1));
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector(false, ConstraintMatchPolicy.DISABLED)) {
            var solution = new TestdataCorruptedShadowedSolution("s1");
            var v1 = new TestdataValue("v1");
            var v2 = new TestdataValue("v2");
            solution.setValueList(Arrays.asList(v1, v2));
            var e1 = new TestdataCorruptedShadowedEntity("e1");
            var e2 = new TestdataCorruptedShadowedEntity("e2");
            solution.setEntityList(Arrays.asList(e1, e2));
            scoreDirector.setWorkingSolution(solution);

            scoreDirector.assertShadowVariablesAreNotStale(SimpleScore.ofUninitialized(-2, 0), "NoChange");
            scoreDirector.beforeVariableChanged(e1, "value");
            e1.setValue(v1);
            scoreDirector.afterVariableChanged(e1, "value");
            scoreDirector.beforeVariableChanged(e2, "value");
            e2.setValue(v1);
            scoreDirector.afterVariableChanged(e2, "value");
            scoreDirector.triggerVariableListeners();
            assertThatThrownBy(
                    () -> scoreDirector.assertShadowVariablesAreNotStale(SimpleScore.ofUninitialized(0, 0), "FirstChange"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
