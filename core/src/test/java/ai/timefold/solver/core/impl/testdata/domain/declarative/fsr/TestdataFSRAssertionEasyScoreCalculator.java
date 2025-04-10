package ai.timefold.solver.core.impl.testdata.domain.declarative.fsr;

import static ai.timefold.solver.core.impl.testdata.domain.declarative.fsr.TestdataFSRVisit.BASE_START_TIME;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataFSRAssertionEasyScoreCalculator implements EasyScoreCalculator<TestdataFSRRoutePlan, HardSoftScore> {
    @Override
    public @NonNull HardSoftScore calculateScore(@NonNull TestdataFSRRoutePlan routePlan) {
        var hardScore = 0;
        var softScore = 0;

        for (var visit : routePlan.visits) {
            if (visit.isAssigned()) {
                if (visit.getExpectedInvalid()) {
                    hardScore--;
                } else {
                    softScore -= (int) Duration
                            .between(BASE_START_TIME, visit.getExpectedServiceFinishTime())
                            .toMinutes();
                }
            }
        }
        return HardSoftScore.of(hardScore, softScore);
    }
}
