package ai.timefold.solver.benchmark.impl.statistic;

import java.util.List;

import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;

import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.Chart;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalbestscore.ConstraintMatchTotalBestScoreSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalstepscore.ConstraintMatchTotalStepScoreSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypebestscore.PickedMoveTypeBestScoreDiffSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypestepscore.PickedMoveTypeStepScoreDiffSubSingleStatistic;

/**
 * 1 statistic of {@link SubSingleBenchmarkResult}.
 */
@XmlSeeAlso({
        ConstraintMatchTotalBestScoreSubSingleStatistic.class,
        ConstraintMatchTotalStepScoreSubSingleStatistic.class,
        PickedMoveTypeBestScoreDiffSubSingleStatistic.class,
        PickedMoveTypeStepScoreDiffSubSingleStatistic.class
})
public abstract class PureSubSingleStatistic<Solution_, StatisticPoint_ extends StatisticPoint, Chart_ extends Chart>
        extends SubSingleStatistic<Solution_, StatisticPoint_>
        implements ChartProvider<Chart_> {

    protected SingleStatisticType singleStatisticType;

    @XmlTransient
    protected List<Chart_> chartList;

    protected PureSubSingleStatistic() {
        // For JAXB.
    }

    protected PureSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult,
            SingleStatisticType singleStatisticType) {
        super(subSingleBenchmarkResult);
        this.singleStatisticType = singleStatisticType;
    }

    @Override
    public SingleStatisticType getStatisticType() {
        return singleStatisticType;
    }

    @Override
    public final void createChartList(BenchmarkReport benchmarkReport) {
        this.chartList = generateCharts(benchmarkReport);
    }

    protected abstract List<Chart_> generateCharts(BenchmarkReport benchmarkReport);

    @Override
    public final List<Chart_> getChartList() {
        return chartList;
    }

    @Override
    public String toString() {
        return subSingleBenchmarkResult + "_" + singleStatisticType;
    }

}
