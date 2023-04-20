package ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalstepscore;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;

public final class ConstraintMatchTotalStepScoreSubSingleStatisticTest
        extends
        AbstractSubSingleStatisticTest<ConstraintMatchTotalStepScoreStatisticPoint, ConstraintMatchTotalStepScoreSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, ConstraintMatchTotalStepScoreSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return ConstraintMatchTotalStepScoreSubSingleStatistic::new;
    }

    @Override
    protected List<ConstraintMatchTotalStepScoreStatisticPoint> getInputPoints() {
        return Collections.singletonList(new ConstraintMatchTotalStepScoreStatisticPoint(Long.MAX_VALUE, "CN", "CP",
                Integer.MAX_VALUE, SimpleScore.of(Integer.MAX_VALUE)));
    }

    @Override
    protected void runTest(SoftAssertions assertions, List<ConstraintMatchTotalStepScoreStatisticPoint> outputPoints) {
        assertions.assertThat(outputPoints)
                .hasSize(1)
                .first()
                .matches(s -> Objects.equals(s.getConstraintId(), "CN/CP"), "Constraint IDs do not match.")
                .matches(s -> s.getConstraintMatchCount() == Integer.MAX_VALUE, "Constraint match counts do not match.")
                .matches(s -> s.getScoreTotal().equals(SimpleScore.of(Integer.MAX_VALUE)), "Scores do not match.")
                .matches(s -> s.getTimeMillisSpent() == Long.MAX_VALUE, "Millis do not match.");
    }

}
