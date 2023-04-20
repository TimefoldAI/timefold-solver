package ai.timefold.solver.benchmark.impl.statistic;

import java.io.File;
import java.util.List;

import javax.xml.bind.annotation.XmlSeeAlso;

import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.statistic.common.GraphSupport;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalbestscore.ConstraintMatchTotalBestScoreSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.constraintmatchtotalstepscore.ConstraintMatchTotalStepScoreSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypebestscore.PickedMoveTypeBestScoreDiffSubSingleStatistic;
import ai.timefold.solver.benchmark.impl.statistic.subsingle.pickedmovetypestepscore.PickedMoveTypeStepScoreDiffSubSingleStatistic;

import org.jfree.chart.JFreeChart;

/**
 * 1 statistic of {@link SubSingleBenchmarkResult}.
 */
@XmlSeeAlso({
        ConstraintMatchTotalBestScoreSubSingleStatistic.class,
        ConstraintMatchTotalStepScoreSubSingleStatistic.class,
        PickedMoveTypeBestScoreDiffSubSingleStatistic.class,
        PickedMoveTypeStepScoreDiffSubSingleStatistic.class
})
public abstract class PureSubSingleStatistic<Solution_, StatisticPoint_ extends StatisticPoint>
        extends SubSingleStatistic<Solution_, StatisticPoint_> {

    protected SingleStatisticType singleStatisticType;

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

    // ************************************************************************
    // Write methods
    // ************************************************************************

    public abstract void writeGraphFiles(BenchmarkReport benchmarkReport);

    protected File writeChartToImageFile(JFreeChart chart, String fileNameBase) {
        File chartFile = new File(subSingleBenchmarkResult.getResultDirectory(), fileNameBase + ".png");
        GraphSupport.writeChartToImageFile(chart, chartFile);
        return chartFile;
    }

    public File getGraphFile() {
        List<File> graphFileList = getGraphFileList();
        if (graphFileList == null || graphFileList.isEmpty()) {
            return null;
        } else if (graphFileList.size() > 1) {
            throw new IllegalStateException("Cannot get graph file for the PureSubSingleStatistic (" + this
                    + ") because it has more than 1 graph file. See method getGraphList() and "
                    + SingleStatisticType.class.getSimpleName() + ".hasScoreLevels()");
        } else {
            return graphFileList.get(0);
        }
    }

    public abstract List<File> getGraphFileList();

    @Override
    public String toString() {
        return subSingleBenchmarkResult + "_" + singleStatisticType;
    }

}
