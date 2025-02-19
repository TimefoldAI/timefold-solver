package ai.timefold.solver.core.preview.api.variable.provided;

import java.time.Duration;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class RouteEasyScoreCalculator implements EasyScoreCalculator<RoutePlan, HardSoftScore> {
    @Override
    public @NonNull HardSoftScore calculateScore(@NonNull RoutePlan routePlan) {
        var hardScore = 0;
        var softScore = 0;

        for (var visit : routePlan.visits) {
            if (visit.isInvalid()) {
                hardScore--;
            }
            if (!visit.isInvalid() && visit.isAssigned()) {
                softScore -= (int) Duration
                        .between(TestShadowVariableProvider.BASE_START_TIME, visit.getServiceFinishTime())
                        .toMinutes();
            }
        }
        return HardSoftScore.of(hardScore, softScore);
    }
}
