package ai.timefold.solver.spring.boot.autoconfigure.dummy.list.constraints.easy;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution;

import org.jspecify.annotations.NonNull;

public class DummySpringListEasyScore implements EasyScoreCalculator<TestdataSpringSolution, SimpleScore> {
    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataSpringSolution testdataSpringSolution) {
        return null;
    }
}
