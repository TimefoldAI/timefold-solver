package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.cascade.single.TestdataSingleCascadingEntity;
import ai.timefold.solver.core.testdomain.cascade.single.TestdataSingleCascadingSolution;
import ai.timefold.solver.core.testdomain.composite.TestdataCompositeEntity;
import ai.timefold.solver.core.testdomain.composite.TestdataCompositeSolution;
import ai.timefold.solver.core.testdomain.constraintverifier.TestdataConstraintVerifierExtendedSolution;
import ai.timefold.solver.core.testdomain.constraintverifier.TestdataConstraintVerifierFirstEntity;
import ai.timefold.solver.core.testdomain.constraintverifier.TestdataConstraintVerifierSecondEntity;
import ai.timefold.solver.core.testdomain.equals.list.TestdataEqualsByCodeListEntity;
import ai.timefold.solver.core.testdomain.equals.list.TestdataEqualsByCodeListSolution;
import ai.timefold.solver.core.testdomain.record.TestdataRecordEntity;
import ai.timefold.solver.core.testdomain.record.TestdataRecordSolution;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProblemSizeStatisticsTest {

    private static ProblemSizeStatistics getProblemSizeStatisticsFromCountLong(long scale) {
        return new ProblemSizeStatistics(0L, Collections.emptySortedMap(), 0L, 0L,
                Collections.emptySortedMap(),
                Math.log10(scale));
    }

    private static ProblemSizeStatistics getProblemSizeStatisticsFromDoubleLog(double scale) {
        return new ProblemSizeStatistics(0L, Collections.emptySortedMap(), 0L, 0L,
                Collections.emptySortedMap(),
                scale);
    }

    private static Locale defaultLocaleToRestore;

    @BeforeAll
    public static void setLocale() {
        defaultLocaleToRestore = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterAll
    public static void restoreLocale() {
        Locale.setDefault(defaultLocaleToRestore);
        defaultLocaleToRestore = null;
    }

    @Test
    void getApproximateProblemScaleLogAsFixedPointLong() {
        var statistics = getProblemSizeStatisticsFromCountLong(100L);
        assertThat(statistics.approximateProblemScaleLogAsFixedPointLong())
                .isEqualTo(MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, 10L, 100L));

        statistics = getProblemSizeStatisticsFromCountLong(250L);
        assertThat(statistics.approximateProblemScaleLogAsFixedPointLong())
                .isEqualTo(MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, 10L, 250L));
    }

    @Test
    void formatApproximateProblemScale() {
        var statistics = getProblemSizeStatisticsFromCountLong(100L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("100");

        statistics = getProblemSizeStatisticsFromCountLong(250L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("250");

        statistics = getProblemSizeStatisticsFromCountLong(1_234_567L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("1,234,567");

        statistics = getProblemSizeStatisticsFromCountLong(123_456_789L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("123,456,789");

        statistics = getProblemSizeStatisticsFromCountLong(1_123_456_789L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("1,123,456,789");

        statistics = getProblemSizeStatisticsFromCountLong(321_123_456_789L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("3.211235 × 10^11");

        // scale = -infinity
        statistics = getProblemSizeStatisticsFromDoubleLog(Double.NEGATIVE_INFINITY);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("0");

        // scale = +infinity
        statistics = getProblemSizeStatisticsFromDoubleLog(Double.POSITIVE_INFINITY);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("0");

        // scale = NaN
        statistics = getProblemSizeStatisticsFromDoubleLog(Double.NaN);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("0");

        // scale = 0
        statistics = getProblemSizeStatisticsFromDoubleLog(0);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("1");
    }

    @Test
    void simpleSolutionEntityAndVariableCount() {
        var solution = TestdataSolution.generateSolution(5, 7);
        var valueRangeManager = ValueRangeManager.of(TestdataSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(7L);
        assertThat(statistics.variableCount()).isEqualTo(7L);
        assertThat(statistics.approximateValueCount()).isEqualTo(5L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataEntity.class, 7L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataEntity.class))
                .containsEntry("value", 5L);
    }

    @Test
    void emptySolutionEntityAndVariableCount() {
        var solution = TestdataSolution.generateSolution(3, 0);
        var valueRangeManager = ValueRangeManager.of(TestdataSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(0L);
        assertThat(statistics.variableCount()).isEqualTo(0L);
        assertThat(statistics.approximateValueCount()).isEqualTo(3L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataEntity.class, 0L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataEntity.class))
                .containsEntry("value", 3L);
    }

    @Test
    void compositeValueRangesEntityAndVariableCount() {
        var solution = TestdataCompositeSolution.generateSolution(4, 6);
        var valueRangeManager = ValueRangeManager.of(TestdataCompositeSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(6L);
        assertThat(statistics.variableCount()).isEqualTo(6L);
        assertThat(statistics.approximateValueCount()).isEqualTo(8L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataCompositeEntity.class, 6L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataCompositeEntity.class))
                .containsEntry("value", 8L);
    }

    @Test
    void multipleEntityTypesEntityAndVariableCount() {
        var solution = TestdataConstraintVerifierExtendedSolution.generateSolution(5, 8);
        var valueRangeManager = ValueRangeManager.of(
                SolutionDescriptor.buildSolutionDescriptor(TestdataConstraintVerifierExtendedSolution.class,
                        TestdataConstraintVerifierFirstEntity.class,
                        TestdataConstraintVerifierSecondEntity.class),
                solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        var firstEntityCount = solution.getEntityList().size();
        var secondEntityCount = solution.getSecondEntityList().size();

        assertThat(statistics.entityCount()).isEqualTo(firstEntityCount + secondEntityCount);
        assertThat(statistics.variableCount()).isEqualTo(firstEntityCount + secondEntityCount);
        assertThat(statistics.approximateValueCount()).isEqualTo(10L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsEntry(TestdataConstraintVerifierFirstEntity.class, (long) firstEntityCount);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsEntry(TestdataConstraintVerifierSecondEntity.class, (long) secondEntityCount);
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataConstraintVerifierFirstEntity.class))
                .containsEntry("value", 5L);
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataConstraintVerifierSecondEntity.class))
                .containsEntry("value", 5L);
    }

    @Test
    void listVariableEntityAndVariableCount() {
        var solution = TestdataEqualsByCodeListSolution.generateSolution(4, 6);
        var valueRangeManager = ValueRangeManager.of(TestdataEqualsByCodeListSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(6L);
        assertThat(statistics.variableCount()).isEqualTo(6L);
        assertThat(statistics.approximateValueCount()).isEqualTo(4L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataEqualsByCodeListEntity.class, 6L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataEqualsByCodeListEntity.class))
                .containsEntry("valueList", 4L);
    }

    @Test
    void cascadingListVariableEntityAndVariableCount() {
        var solution = TestdataSingleCascadingSolution.generateUninitializedSolution(3, 5);
        var valueRangeManager = ValueRangeManager.of(TestdataSingleCascadingSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(5L);
        assertThat(statistics.variableCount()).isEqualTo(5L);
        assertThat(statistics.approximateValueCount()).isEqualTo(3L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataSingleCascadingEntity.class, 5L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataSingleCascadingEntity.class))
                .containsEntry("valueList", 3L);
    }

    @Test
    void recordEntityAndVariableCount() {
        var solution = TestdataRecordSolution.generateSolution(4, 9);
        var valueRangeManager = ValueRangeManager.of(TestdataRecordSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(9L);
        assertThat(statistics.variableCount()).isEqualTo(9L);
        assertThat(statistics.approximateValueCount()).isEqualTo(4L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataRecordEntity.class, 9L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataRecordEntity.class))
                .containsEntry("value", 4L);
    }

    @Test
    void uninitializedSolutionEntityAndVariableCount() {
        var solution = TestdataSolution.generateUninitializedSolution(6, 10);
        var valueRangeManager = ValueRangeManager.of(TestdataSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(10L);
        assertThat(statistics.variableCount()).isEqualTo(10L);
        assertThat(statistics.approximateValueCount()).isEqualTo(6L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataEntity.class, 10L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataEntity.class))
                .containsEntry("value", 6L);
    }

    @Test
    void singleEntityAndValue() {
        var solution = TestdataSolution.generateSolution(1, 1);
        var valueRangeManager = ValueRangeManager.of(TestdataSolution.buildSolutionDescriptor(), solution);
        var statistics = valueRangeManager.getProblemSizeStatistics();

        assertThat(statistics.entityCount()).isEqualTo(1L);
        assertThat(statistics.variableCount()).isEqualTo(1L);
        assertThat(statistics.approximateValueCount()).isEqualTo(1L);
        assertThat(statistics.genuineEntityClassToEntityCount())
                .containsExactlyEntriesOf(Map.of(TestdataEntity.class, 1L));
        assertThat(statistics.genuineEntityClassToVariableToValueCount().get(TestdataEntity.class))
                .containsEntry("value", 1L);
    }
}
