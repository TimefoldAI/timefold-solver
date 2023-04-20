package ai.timefold.solver.benchmark.impl.statistic.memoryuse;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.AbstractSubSingleStatisticTest;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;

public final class MemoryUseSubSingleStatisticTest
        extends AbstractSubSingleStatisticTest<MemoryUseStatisticPoint, MemoryUseSubSingleStatistic<TestdataSolution>> {

    @Override
    protected Function<SubSingleBenchmarkResult, MemoryUseSubSingleStatistic<TestdataSolution>>
            getSubSingleStatisticConstructor() {
        return MemoryUseSubSingleStatistic::new;
    }

    @Override
    protected List<MemoryUseStatisticPoint> getInputPoints() {
        return Collections.singletonList(MemoryUseStatisticPoint.create(Long.MAX_VALUE));
    }

    @Override
    protected void runTest(SoftAssertions assertions, List<MemoryUseStatisticPoint> outputPoints) {
        assertions.assertThat(outputPoints)
                .hasSize(1)
                .first()
                .matches(s -> s.getUsedMemory() > 0, "Used memory not recorded.")
                .matches(s -> s.getMaxMemory() > 0, "Max memory not recorded.")
                .matches(s -> s.getTimeMillisSpent() == Long.MAX_VALUE, "Millis do not match.");
    }

}
