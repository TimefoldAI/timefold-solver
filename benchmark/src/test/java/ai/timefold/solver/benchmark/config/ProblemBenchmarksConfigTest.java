package ai.timefold.solver.benchmark.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;

import org.junit.jupiter.api.Test;

class ProblemBenchmarksConfigTest {

    @Test
    void testDetermineProblemStatisticTypeList() {
        ProblemBenchmarksConfig problemBenchmarksConfig = new ProblemBenchmarksConfig();
        assertThat(problemBenchmarksConfig.determineProblemStatisticTypeList())
                .isEqualTo(ProblemStatisticType.defaultList());
        problemBenchmarksConfig.setProblemStatisticTypeList(Collections.emptyList());
        assertThat(problemBenchmarksConfig.determineProblemStatisticTypeList())
                .isEqualTo(ProblemStatisticType.defaultList());

        // This verifies that the statistic type tested below is not the default one.
        assertThat(ProblemStatisticType.defaultList()).isNotEqualTo(List.of(ProblemStatisticType.MEMORY_USE));

        problemBenchmarksConfig.setProblemStatisticTypeList(List.of(ProblemStatisticType.MEMORY_USE));
        assertThat(problemBenchmarksConfig.determineProblemStatisticTypeList())
                .containsExactly(ProblemStatisticType.MEMORY_USE);

        problemBenchmarksConfig.setProblemStatisticEnabled(true);
        assertThat(problemBenchmarksConfig.determineProblemStatisticTypeList())
                .containsExactly(ProblemStatisticType.MEMORY_USE);

        problemBenchmarksConfig.setProblemStatisticEnabled(false);
        assertThat(problemBenchmarksConfig.determineProblemStatisticTypeList()).isEmpty();
    }

    @Test
    void testDetermineSingleStatisticTypeList() {
        ProblemBenchmarksConfig problemBenchmarksConfig = new ProblemBenchmarksConfig();
        assertThat(problemBenchmarksConfig.determineSingleStatisticTypeList()).isEmpty();
        problemBenchmarksConfig.setSingleStatisticTypeList(Collections.emptyList());
        assertThat(problemBenchmarksConfig.determineSingleStatisticTypeList()).isEmpty();
        problemBenchmarksConfig.setSingleStatisticTypeList(List.of(SingleStatisticType.CONSTRAINT_MATCH_TOTAL_STEP_SCORE));
        assertThat(problemBenchmarksConfig.determineSingleStatisticTypeList())
                .containsExactly(SingleStatisticType.CONSTRAINT_MATCH_TOTAL_STEP_SCORE);
    }
}
