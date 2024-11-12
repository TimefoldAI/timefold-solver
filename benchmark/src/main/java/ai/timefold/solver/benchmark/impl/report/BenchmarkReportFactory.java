package ai.timefold.solver.benchmark.impl.report;

import java.time.ZoneId;
import java.util.Comparator;

import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.benchmark.impl.ranking.SolverRankingWeightFactory;
import ai.timefold.solver.benchmark.impl.ranking.TotalRankSolverRankingWeightFactory;
import ai.timefold.solver.benchmark.impl.ranking.TotalScoreSolverRankingComparator;
import ai.timefold.solver.benchmark.impl.ranking.WorstScoreSolverRankingComparator;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.config.util.ConfigUtils;

public class BenchmarkReportFactory {

    private final BenchmarkReportConfig config;

    public BenchmarkReportFactory(BenchmarkReportConfig config) {
        this.config = config;
    }

    public BenchmarkReport buildBenchmarkReport(PlannerBenchmarkResult plannerBenchmark) {
        BenchmarkReport benchmarkReport = new BenchmarkReport(plannerBenchmark);
        benchmarkReport.setLocale(config.determineLocale());
        benchmarkReport.setTimezoneId(ZoneId.systemDefault());
        supplySolverRanking(benchmarkReport);
        return benchmarkReport;
    }

    protected void supplySolverRanking(BenchmarkReport benchmarkReport) {
        var solverRankingType = config.getSolverRankingType();
        var solverRankingComparatorClass = config.getSolverRankingComparatorClass();
        var solverRankingWeightFactoryClass = config.getSolverRankingWeightFactoryClass();
        if (solverRankingType != null && solverRankingComparatorClass != null) {
            throw new IllegalStateException(
                    "The PlannerBenchmark cannot have a solverRankingType (%s) and a solverRankingComparatorClass (%s) at the same time."
                            .formatted(solverRankingType, solverRankingComparatorClass.getName()));
        } else if (solverRankingType != null && solverRankingWeightFactoryClass != null) {
            throw new IllegalStateException(
                    "The PlannerBenchmark cannot have a solverRankingType (%s) and a solverRankingWeightFactoryClass (%s) at the same time."
                            .formatted(solverRankingType, solverRankingWeightFactoryClass.getName()));
        } else if (solverRankingComparatorClass != null && solverRankingWeightFactoryClass != null) {
            throw new IllegalStateException(
                    "The PlannerBenchmark cannot have a solverRankingComparatorClass (%s) and a solverRankingWeightFactoryClass (%s) at the same time."
                            .formatted(solverRankingComparatorClass.getName(), solverRankingWeightFactoryClass.getName()));
        }
        Comparator<SolverBenchmarkResult> solverRankingComparator = null;
        SolverRankingWeightFactory solverRankingWeightFactory = null;
        if (solverRankingType != null) {
            switch (solverRankingType) {
                case TOTAL_SCORE:
                    solverRankingComparator = new TotalScoreSolverRankingComparator();
                    break;
                case WORST_SCORE:
                    solverRankingComparator = new WorstScoreSolverRankingComparator();
                    break;
                case TOTAL_RANKING:
                    solverRankingWeightFactory = new TotalRankSolverRankingWeightFactory();
                    break;
                default:
                    throw new IllegalStateException("The solverRankingType (%s) is not implemented."
                            .formatted(solverRankingType));
            }
        }
        if (solverRankingComparatorClass != null) {
            solverRankingComparator =
                    ConfigUtils.newInstance(config, "solverRankingComparatorClass", solverRankingComparatorClass);
        }
        if (solverRankingWeightFactoryClass != null) {
            solverRankingWeightFactory =
                    ConfigUtils.newInstance(config, "solverRankingWeightFactoryClass", solverRankingWeightFactoryClass);
        }
        if (solverRankingComparator != null) {
            benchmarkReport.setSolverRankingComparator(solverRankingComparator);
        } else if (solverRankingWeightFactory != null) {
            benchmarkReport.setSolverRankingWeightFactory(solverRankingWeightFactory);
        } else {
            benchmarkReport.setSolverRankingComparator(new TotalScoreSolverRankingComparator());
        }
    }
}
