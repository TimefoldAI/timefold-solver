package ai.timefold.solver.benchmark.util;

import ai.timefold.solver.benchmark.impl.result.BenchmarkResultIO;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

final class TestableBenchmarkResultIO extends BenchmarkResultIO {

    public PlannerBenchmarkResult read(File file) {
        return super.readPlannerBenchmarkResult(file);
    }

    public void write(File file, PlannerBenchmarkResult result) throws IOException {
        try (Writer w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            super.write(result, w);
        }
    }
}
