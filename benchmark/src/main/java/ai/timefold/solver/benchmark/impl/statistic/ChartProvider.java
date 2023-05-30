package ai.timefold.solver.benchmark.impl.statistic;

import java.util.List;

import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.Chart;

public interface ChartProvider<Chart_ extends Chart> {

    void createChartList(BenchmarkReport benchmarkReport);

    /**
     *
     * @return null unless {@link #createChartList(BenchmarkReport)} was called
     */
    List<Chart_> getChartList();

}
