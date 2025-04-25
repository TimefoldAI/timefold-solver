package ai.timefold.solver.core.impl.score.director.easy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.shadow.corrupted.TestdataCorruptedShadowedEntity;
import ai.timefold.solver.core.testdomain.shadow.corrupted.TestdataCorruptedShadowedSolution;

import org.junit.jupiter.api.Test;

class EasyScoreDirectorTest {

    @Test
    void shadowVariableCorruption() {
        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(TestdataCorruptedShadowedSolution.buildSolutionDescriptor(),
                (solution_) -> SimpleScore.of(0));
        scoreDirectorFactory
                .setInitializingScoreTrend(InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1));
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            var solution = new TestdataCorruptedShadowedSolution("s1");
            var v1 = new TestdataValue("v1");
            var v2 = new TestdataValue("v2");
            solution.setValueList(Arrays.asList(v1, v2));
            var e1 = new TestdataCorruptedShadowedEntity("e1");
            var e2 = new TestdataCorruptedShadowedEntity("e2");
            solution.setEntityList(Arrays.asList(e1, e2));
            scoreDirector.setWorkingSolution(solution);

            scoreDirector.assertShadowVariablesAreNotStale(InnerScore.withUnassignedCount(SimpleScore.ZERO, 2), "NoChange");
            scoreDirector.beforeVariableChanged(e1, "value");
            e1.setValue(v1);
            scoreDirector.afterVariableChanged(e1, "value");
            scoreDirector.beforeVariableChanged(e2, "value");
            e2.setValue(v1);
            scoreDirector.afterVariableChanged(e2, "value");
            scoreDirector.triggerVariableListeners();
            assertThatThrownBy(
                    () -> scoreDirector.assertShadowVariablesAreNotStale(InnerScore.fullyAssigned(SimpleScore.ZERO),
                            "FirstChange"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
