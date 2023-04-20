package ai.timefold.solver.benchmark.impl.statistic.bestsolutionmutation;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;

public final class BestSolutionMutationSubSingleStatisticTest
        extends
        AbstractSubSingleStatisticTest<BestSolutionMutationStatisticPoint, BestSolutionMutationSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, BestSolutionMutationSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return BestSolutionMutationSubSingleStatistic::new;
    }

    @Override
    protected List<BestSolutionMutationStatisticPoint> getInputPoints() {
        return Collections.singletonList(new BestSolutionMutationStatisticPoint(Long.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Override
    protected void runTest(SoftAssertions assertions, List<BestSolutionMutationStatisticPoint> outputPoints) {
        assertions.assertThat(outputPoints)
                .hasSize(1)
                .first()
                .matches(s -> s.getMutationCount() == Integer.MAX_VALUE, "Mutation counts do not match.")
                .matches(s -> s.getTimeMillisSpent() == Long.MAX_VALUE, "Millis do not match.");
    }

}
