package ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values;

import static ai.timefold.solver.core.impl.testdata.domain.declarative.concurrent_values.TestdataConcurrentValue.BASE_START_TIME;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataConcurrentEasyScoreCalculator implements EasyScoreCalculator<TestdataConcurrentSolution, HardSoftScore> {
    @Override
    public @NonNull HardSoftScore calculateScore(@NonNull TestdataConcurrentSolution routePlan) {
        var hardScore = 0;
        var softScore = 0;

        for (var visit : routePlan.values) {
            if (visit.isAssigned()) {
                if (visit.isInvalid()) {
                    hardScore--;
                } else {
                    softScore -= (int) Duration
                            .between(BASE_START_TIME, visit.getServiceFinishTime())
                            .toMinutes();
                }
            }
        }
        return HardSoftScore.of(hardScore, softScore);
    }
}
