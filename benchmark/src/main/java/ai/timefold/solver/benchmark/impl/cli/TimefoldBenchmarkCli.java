package ai.timefold.solver.benchmark.impl.cli;

import java.io.File;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;

/**
 * Run this class from the command line interface
 * to run a benchmarkConfigFile directly (using the normal classpath from the JVM).
 */
public class TimefoldBenchmarkCli {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: TimefoldBenchmarkCli benchmarkConfigFile benchmarkDirectory");
            System.exit(1);
        }
        File benchmarkConfigFile = new File(args[0]);
        if (!benchmarkConfigFile.exists()) {
            System.err.println("The benchmarkConfigFile (" + benchmarkConfigFile + ") does not exist.");
            System.exit(1);
        }
        File benchmarkDirectory = new File(args[1]);
        PlannerBenchmarkConfig benchmarkConfig;
        if (benchmarkConfigFile.getName().endsWith(".ftl")) {
            benchmarkConfig = PlannerBenchmarkConfig.createFromFreemarkerXmlFile(benchmarkConfigFile);
        } else {
            benchmarkConfig = PlannerBenchmarkConfig.createFromXmlFile(benchmarkConfigFile);
        }
        benchmarkConfig.setBenchmarkDirectory(benchmarkDirectory);
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.create(benchmarkConfig);
        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark();
        benchmark.benchmark();
    }

}
