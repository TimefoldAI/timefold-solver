package ai.timefold.solver.benchmark.impl.statistic;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;

public abstract class ProblemBasedSubSingleStatistic<Solution_, StatisticPoint_ extends StatisticPoint>
        extends SubSingleStatistic<Solution_, StatisticPoint_> {

    protected ProblemStatisticType problemStatisticType;

    protected ProblemBasedSubSingleStatistic() {
        // For JAXB.
    }

    protected ProblemBasedSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult,
            ProblemStatisticType problemStatisticType) {
        super(subSingleBenchmarkResult);
        this.problemStatisticType = problemStatisticType;
    }

    @Override
    public ProblemStatisticType getStatisticType() {
        return problemStatisticType;
    }

    @Override
    public String toString() {
        return subSingleBenchmarkResult + "_" + problemStatisticType;
    }

}
