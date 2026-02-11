package ai.timefold.solver.benchmark.impl.report;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.benchmark.config.ranking.SolverRankingType;
import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.benchmark.impl.ranking.TotalRankSolverRankingWeightFactory;
import ai.timefold.solver.benchmark.impl.ranking.TotalScoreSolverRankingComparator;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;

import org.junit.jupiter.api.Test;

class BenchmarkReportFactoryTest {

    @Test
    void buildWithSolverRankingTypeAndSolverRankingComparatorClass() {
        BenchmarkReportConfig config = new BenchmarkReportConfig();
        config.setSolverRankingType(SolverRankingType.TOTAL_RANKING);
        config.setSolverRankingComparatorClass(TotalScoreSolverRankingComparator.class);

        PlannerBenchmarkResult result = mock(PlannerBenchmarkResult.class);
        BenchmarkReportFactory reportFactory = new BenchmarkReportFactory(config);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> reportFactory.buildBenchmarkReport(result))
                .withMessageContaining("solverRankingType").withMessageContaining("solverRankingComparatorClass");
    }

    @Test
    void buildWithSolverRankingTypeAndSolverRankingWeightFactoryClass() {
        BenchmarkReportConfig config = new BenchmarkReportConfig();
        config.setSolverRankingType(SolverRankingType.TOTAL_RANKING);
        config.setSolverRankingWeightFactoryClass(TotalRankSolverRankingWeightFactory.class);

        PlannerBenchmarkResult result = mock(PlannerBenchmarkResult.class);
        BenchmarkReportFactory reportFactory = new BenchmarkReportFactory(config);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> reportFactory.buildBenchmarkReport(result))
                .withMessageContaining("solverRankingType").withMessageContaining("solverRankingWeightFactoryClass");
    }

    @Test
    void buildWithSolverRankingComparatorClassAndSolverRankingWeightFactoryClass() {
        BenchmarkReportConfig config = new BenchmarkReportConfig();
        config.setSolverRankingComparatorClass(TotalScoreSolverRankingComparator.class);
        config.setSolverRankingWeightFactoryClass(TotalRankSolverRankingWeightFactory.class);

        PlannerBenchmarkResult result = mock(PlannerBenchmarkResult.class);
        BenchmarkReportFactory reportFactory = new BenchmarkReportFactory(config);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> reportFactory.buildBenchmarkReport(result))
                .withMessageContaining("solverRankingComparatorClass").withMessageContaining("solverRankingWeightFactoryClass");
    }
}
