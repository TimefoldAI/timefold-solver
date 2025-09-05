package ai.timefold.solver.benchmark.impl.aggregator;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReportFactory;
import ai.timefold.solver.benchmark.impl.result.BenchmarkResultIO;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkAggregator.class);

    private File benchmarkDirectory = null;
    private BenchmarkReportConfig benchmarkReportConfig = null;
    private BenchmarkReport benchmarkReport = null;

    public File getBenchmarkDirectory() {
        return benchmarkDirectory;
    }

    public void setBenchmarkDirectory(File benchmarkDirectory) {
        this.benchmarkDirectory = benchmarkDirectory;
    }

    public BenchmarkReportConfig getBenchmarkReportConfig() {
        return benchmarkReportConfig;
    }

    public void setBenchmarkReportConfig(BenchmarkReportConfig benchmarkReportConfig) {
        this.benchmarkReportConfig = benchmarkReportConfig;
    }

    public BenchmarkReport getBenchmarkReport() {
        return benchmarkReport;
    }

    public void setBenchmarkReport(BenchmarkReport benchmarkReport) {
        this.benchmarkReport = benchmarkReport;
    }

    // ************************************************************************
    // Aggregate methods
    // ************************************************************************

    public File aggregateBenchmarks(PlannerBenchmarkConfig benchmarkConfig) {
        File benchmarkDirectory = benchmarkConfig.getBenchmarkDirectory();
        if (!benchmarkDirectory.exists() || !benchmarkDirectory.isDirectory()) {
            throw new IllegalArgumentException("Benchmark directory does not exist: " + benchmarkDirectory);
        }

        // Read all existing benchmark results from the directory
        BenchmarkResultIO benchmarkResultIO = new BenchmarkResultIO();
        List<PlannerBenchmarkResult> plannerBenchmarkResults =
                benchmarkResultIO.readPlannerBenchmarkResultList(benchmarkDirectory);

        if (plannerBenchmarkResults.isEmpty()) {
            throw new IllegalArgumentException("No benchmark results found in directory: " + benchmarkDirectory);
        }

        // Collect all single benchmark results and preserve solver names
        List<SingleBenchmarkResult> allSingleBenchmarkResults = new ArrayList<>();
        Map<SolverBenchmarkResult, String> solverBenchmarkResultNameMap = new HashMap<>();

        for (PlannerBenchmarkResult plannerResult : plannerBenchmarkResults) {
            for (SolverBenchmarkResult solverResult : plannerResult.getSolverBenchmarkResultList()) {
                allSingleBenchmarkResults.addAll(solverResult.getSingleBenchmarkResultList());
                solverBenchmarkResultNameMap.put(solverResult, solverResult.getName());
            }
        }

        // Configure the aggregator instance
        this.setBenchmarkDirectory(benchmarkDirectory);
        this.setBenchmarkReportConfig(benchmarkConfig.getBenchmarkReportConfig());

        // Perform the aggregation - returns HTML report file
        return this.aggregate(allSingleBenchmarkResults, solverBenchmarkResultNameMap);
    }

    public File aggregateSelectedBenchmarksInUi(List<SingleBenchmarkResult> selectedSingleBenchmarkResults,
            Map<SolverBenchmarkResult, String> solverBenchmarkResultNameMap) {
        return this.aggregate(selectedSingleBenchmarkResults, solverBenchmarkResultNameMap);
    }

    public File aggregateSelectedBenchmarks(PlannerBenchmarkConfig benchmarkConfig, List<String> selectedDirectoryNames) {
        File benchmarkDirectory = benchmarkConfig.getBenchmarkDirectory();
        if (!benchmarkDirectory.exists() || !benchmarkDirectory.isDirectory()) {
            throw new IllegalArgumentException("Benchmark directory does not exist: " + benchmarkDirectory);
        }

        // Read all benchmark results first, then filter
        BenchmarkResultIO benchmarkResultIO = new BenchmarkResultIO();
        List<PlannerBenchmarkResult> allPlannerResults = benchmarkResultIO.readPlannerBenchmarkResultList(benchmarkDirectory);

        List<SingleBenchmarkResult> selectedSingleBenchmarkResults = new ArrayList<>();
        Map<SolverBenchmarkResult, String> solverBenchmarkResultNameMap = new HashMap<>();

        // Filter results based on selected directory names
        for (PlannerBenchmarkResult plannerResult : allPlannerResults) {
            String benchmarkDirectoryName = plannerResult.getBenchmarkReportDirectory().getName();

            if (selectedDirectoryNames.contains(benchmarkDirectoryName)) {
                LOGGER.info("Including benchmark results from directory: {}", benchmarkDirectoryName);

                for (SolverBenchmarkResult solverResult : plannerResult.getSolverBenchmarkResultList()) {
                    selectedSingleBenchmarkResults.addAll(solverResult.getSingleBenchmarkResultList());
                    solverBenchmarkResultNameMap.put(solverResult, solverResult.getName());
                }
            }
        }

        if (selectedSingleBenchmarkResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid benchmark results found for selected directories: " + selectedDirectoryNames);
        }

        // Configure the aggregator instance
        this.setBenchmarkDirectory(benchmarkDirectory);
        this.setBenchmarkReportConfig(benchmarkConfig.getBenchmarkReportConfig());

        // Perform the aggregation with selected results - returns HTML report file
        return this.aggregate(selectedSingleBenchmarkResults, solverBenchmarkResultNameMap);
    }

    public List<String> getAvailableBenchmarkDirectories(PlannerBenchmarkConfig benchmarkConfig) {
        File benchmarkDirectory = benchmarkConfig.getBenchmarkDirectory();
        List<String> directories = new ArrayList<>();

        if (!benchmarkDirectory.exists() || !benchmarkDirectory.isDirectory()) {
            return directories;
        }

        File[] subdirs = benchmarkDirectory.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                // Only include directories that have a benchmark result file
                File resultFile = new File(subdir, "plannerBenchmarkResult.xml");
                if (resultFile.exists()) {
                    directories.add(subdir.getName());
                }
            }
        }

        return directories;
    }

    public File aggregateAndShowReportInBrowser(PlannerBenchmarkConfig benchmarkConfig) {
        File benchmarkDirectoryPath = this.aggregateBenchmarks(benchmarkConfig);
        this.showReportInBrowser();
        return benchmarkDirectoryPath;
    }

    private void showReportInBrowser() {
        if (this.benchmarkReport == null) {
            throw new IllegalStateException("No benchmark report available. Run aggregation first.");
        }

        File htmlOverviewFile = this.benchmarkReport.getHtmlOverviewFile();
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Action.BROWSE)) {
            try {
                desktop.browse(htmlOverviewFile.getAbsoluteFile().toURI());
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed showing htmlOverviewFile (" + htmlOverviewFile + ") in the default browser.", e);
            }
        } else {
            LOGGER.warn("The default browser can't be opened to show htmlOverviewFile ({}).", htmlOverviewFile);
        }
    }

    private File aggregate(List<SingleBenchmarkResult> singleBenchmarkResultList,
            Map<SolverBenchmarkResult, String> solverBenchmarkResultNameMap) {
        if (benchmarkDirectory == null) {
            throw new IllegalArgumentException("The benchmarkDirectory (" + benchmarkDirectory + ") must not be null.");
        }
        if (!benchmarkDirectory.exists()) {
            throw new IllegalArgumentException("The benchmarkDirectory (" + benchmarkDirectory + ") must exist.");
        }
        if (benchmarkReportConfig == null) {
            throw new IllegalArgumentException("The benchmarkReportConfig (" + benchmarkReportConfig
                    + ") must not be null.");
        }
        if (singleBenchmarkResultList.isEmpty()) {
            throw new IllegalArgumentException("The singleBenchmarkResultList (" + singleBenchmarkResultList
                    + ") must not be empty.");
        }
        OffsetDateTime startingTimestamp = OffsetDateTime.now();
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult.getSubSingleBenchmarkResultList()) {
                subSingleBenchmarkResult.setSingleBenchmarkResult(singleBenchmarkResult);
            }
            singleBenchmarkResult.initSubSingleStatisticMaps();
        }
        // Handle renamed solver benchmarks after statistics have been read (they're resolved by
        // original solver benchmarks' names)
        if (solverBenchmarkResultNameMap != null) {
            for (Entry<SolverBenchmarkResult, String> entry : solverBenchmarkResultNameMap.entrySet()) {
                SolverBenchmarkResult result = entry.getKey();
                String newName = entry.getValue();
                if (!result.getName().equals(newName)) {
                    result.setName(newName);
                }
            }
        }

        PlannerBenchmarkResult plannerBenchmarkResult = PlannerBenchmarkResult.createMergedResult(
                singleBenchmarkResultList);
        plannerBenchmarkResult.setStartingTimestamp(startingTimestamp);
        plannerBenchmarkResult.initBenchmarkReportDirectory(benchmarkDirectory);

        BenchmarkReportFactory benchmarkReportFactory = new BenchmarkReportFactory(benchmarkReportConfig);
        BenchmarkReport benchmarkReport = benchmarkReportFactory.buildBenchmarkReport(plannerBenchmarkResult);
        plannerBenchmarkResult.accumulateResults(benchmarkReport);
        benchmarkReport.writeReport();

        // Store the benchmark report for potential browser viewing
        this.benchmarkReport = benchmarkReport;

        LOGGER.info("Aggregation ended: statistic html overview ({}).",
                benchmarkReport.getHtmlOverviewFile().getAbsolutePath());
        return benchmarkReport.getHtmlOverviewFile().getAbsoluteFile();
    }

}
