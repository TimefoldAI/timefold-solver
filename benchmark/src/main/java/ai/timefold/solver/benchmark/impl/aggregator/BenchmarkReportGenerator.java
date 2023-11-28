package ai.timefold.solver.benchmark.impl.aggregator;
import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import java.io.File;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReportFactory;

public class BenchmarkReportGenerator {
    private BenchmarkReportConfig benchmarkReportConfig;

    public BenchmarkReportGenerator(BenchmarkReportConfig benchmarkReportConfig) {
        this.benchmarkReportConfig = benchmarkReportConfig;
    }

    public File generateReport(PlannerBenchmarkResult plannerBenchmarkResult) {
        BenchmarkReportFactory benchmarkReportFactory = new BenchmarkReportFactory(benchmarkReportConfig);
        BenchmarkReport benchmarkReport = benchmarkReportFactory.buildBenchmarkReport(plannerBenchmarkResult);
        plannerBenchmarkResult.accumulateResults(benchmarkReport);
        benchmarkReport.writeReport();
        return benchmarkReport.getHtmlOverviewFile().getAbsoluteFile();
    }
}

