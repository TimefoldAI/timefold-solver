package ai.timefold.solver.core.impl.neighborhood.bias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tallies {@code sampleCount} independent draws into categories, then asserts a statistical
 * property of the resulting distribution: uniform, weighted by an explicit share per category
 * ({@link #assertWithinSigma(double)}), or a deliberately non-uniform ratio between two categories
 * ({@link #assertShareRatioAtLeast}). Every assertion logs the reached numbers at INFO before
 * asserting, and repeats them in the failure message via AssertJ's {@code as(...)}, so the actual
 * margin against the bound is never silently discarded on a pass; see
 * {@link AbstractBiasIT#SIGMA_LIMIT}'s javadoc for why the bound is a sigma count, not a
 * hand-picked percentage.
 *
 * @param <Category_> what a single draw is classified into (a bucket, an entity code, a whole
 *        draw order, ...)
 */
final class BiasReport<Category_> {

    private static final Logger LOG = LoggerFactory.getLogger(BiasReport.class);
    private static final int MAX_DETAIL_ROW_COUNT = 32;

    private final String label;
    private final int sampleCount;
    private final Map<Category_, Long> countByCategory;
    private Map<Category_, Double> expectedShareByCategory = Map.of();

    private BiasReport(String label, int sampleCount, Map<Category_, Long> countByCategory) {
        this.label = Objects.requireNonNull(label);
        this.sampleCount = sampleCount;
        this.countByCategory = Objects.requireNonNull(countByCategory);
    }

    static <Category_> BiasReport<Category_> tally(String label, int sampleCount, IntFunction<Category_> sampler) {
        var countByCategory = new HashMap<Category_, Long>();
        for (var i = 0; i < sampleCount; i++) {
            countByCategory.merge(sampler.apply(i), 1L, Long::sum);
        }
        return new BiasReport<>(label, sampleCount, countByCategory);
    }

    /**
     * Every category in {@code expectedCategoryCollection} is expected in equal share
     * ({@code 1 / expectedCategoryCollection.size()}). A category absent from the collection is
     * ignored by {@link #assertWithinSigma(double)}, even if it was drawn.
     */
    BiasReport<Category_> expectUniform(Collection<Category_> expectedCategoryCollection) {
        var share = 1.0 / expectedCategoryCollection.size();
        var freshExpectedShareByCategory = new HashMap<Category_, Double>();
        for (var category : expectedCategoryCollection) {
            freshExpectedShareByCategory.put(category, share);
        }
        this.expectedShareByCategory = freshExpectedShareByCategory;
        return this;
    }

    /**
     * As {@link #expectUniform}, but each category's expected share is given explicitly instead of
     * assumed equal. Shares need not sum to 1 (a fixture may deliberately omit an unreachable
     * category); they are used only to compute each category's own expected count and sigma.
     */
    BiasReport<Category_> expectWeights(Map<Category_, Double> expectedShareByCategory) {
        this.expectedShareByCategory = Map.copyOf(expectedShareByCategory);
        return this;
    }

    /**
     * Asserts every expected category was actually drawn, then that no category's observed count
     * deviates from its expected count by more than {@code sigmaLimit} standard deviations of
     * binomial sampling noise. Requires {@link #expectUniform} or {@link #expectWeights} to have
     * been called first.
     */
    void assertWithinSigma(double sigmaLimit) {
        assertThat(expectedShareByCategory)
                .as("call expectUniform() or expectWeights() before assertWithinSigma()")
                .isNotEmpty();
        var deviationList = deviations();
        var worst = deviationList.stream().max(Comparator.comparingDouble(Deviation::sigma)).orElseThrow();
        LOG.info(table(deviationList, worst, sigmaLimit));

        assertThat(countByCategory.keySet())
                .as(() -> table(deviationList, worst, sigmaLimit))
                .containsAll(expectedShareByCategory.keySet());
        assertThat(worst.sigma())
                .as(() -> table(deviationList, worst, sigmaLimit))
                .isLessThanOrEqualTo(sigmaLimit);
    }

    /**
     * For a deliberately non-uniform fixture (no {@code expect*} call needed): asserts that
     * {@code smallCategory}'s per-member rate is at least {@code minRatio} times
     * {@code largeCategory}'s, where a category's rate is its observed count divided by the given
     * member count (e.g. its bucket size).
     */
    void assertShareRatioAtLeast(Category_ smallCategory, int smallCategorySize, Category_ largeCategory,
            int largeCategorySize, double minRatio) {
        var smallCount = countByCategory.getOrDefault(smallCategory, 0L);
        var largeCount = countByCategory.getOrDefault(largeCategory, 0L);
        var smallRate = smallCount / (double) smallCategorySize;
        var largeRate = largeCount / (double) largeCategorySize;
        var summary = "[bias] %s: %d samples; %s rate %.2f (count %d / %d), %s rate %.2f (count %d / %d)"
                .formatted(label, sampleCount, smallCategory, smallRate, smallCount, smallCategorySize, largeCategory,
                        largeRate, largeCount, largeCategorySize);
        LOG.info(summary);

        assertThat(smallCount).as(() -> summary + "; both categories must be reachable at all").isPositive();
        assertThat(largeCount).as(() -> summary + "; both categories must be reachable at all").isPositive();
        assertThat(smallRate)
                .as(() -> summary + "; %s's per-member rate must be at least %sx %s's".formatted(smallCategory, minRatio,
                        largeCategory))
                .isGreaterThan(largeRate * minRatio);
    }

    private record Deviation<Category_>(Category_ category, long observed, double expected, double sigma) {

        double deviationPercent() {
            return expected == 0 ? 0 : (observed - expected) / expected * 100;
        }

    }

    private List<Deviation<Category_>> deviations() {
        var deviationList = new ArrayList<Deviation<Category_>>(expectedShareByCategory.size());
        for (var entry : expectedShareByCategory.entrySet()) {
            var category = entry.getKey();
            var p = entry.getValue();
            var expected = sampleCount * p;
            var observed = countByCategory.getOrDefault(category, 0L);
            var binomialStandardDeviation = Math.sqrt(sampleCount * p * (1 - p));
            var sigma = binomialStandardDeviation == 0 ? 0 : Math.abs(observed - expected) / binomialStandardDeviation;
            deviationList.add(new Deviation<>(category, observed, expected, sigma));
        }
        return deviationList;
    }

    private String table(List<Deviation<Category_>> deviationList, Deviation<Category_> worst, double sigmaLimit) {
        var statistics = new SummaryStatistics();
        for (var deviation : deviationList) {
            statistics.addValue(deviation.observed());
        }
        var unreachedCategoryList = deviationList.stream()
                .filter(d -> d.expected() > 0 && d.observed() == 0)
                .map(d -> d.category())
                .toList();
        // Every category's expected count is (about) the same only for a uniform expectation; the "expected"
        // binomial spread reported here is therefore an average across categories, informative, not exact.
        var averageBinomialStandardDeviation = deviationList.stream()
                .mapToDouble(d -> Math.sqrt(sampleCount * (d.expected() / sampleCount) * (1 - d.expected() / sampleCount)))
                .average()
                .orElse(0);
        var builder = new StringBuilder();
        builder.append("[bias] %s: %d samples over %d categories, limit %.1f sigma%n"
                .formatted(label, sampleCount, deviationList.size(), sigmaLimit));
        builder.append(
                "  worst deviation at %s: observed %d, expected %.1f (%+.2f%%), %.2f sigma%n".formatted(worst.category(),
                        worst.observed(), worst.expected(), worst.deviationPercent(), worst.sigma()));
        builder.append("  spread: min %.0f, max %.0f, stdev %.1f, average expected stdev %.1f (binomial)%n"
                .formatted(statistics.getMin(), statistics.getMax(), statistics.getStandardDeviation(),
                        averageBinomialStandardDeviation));
        builder.append("  headroom: %.2f sigma to the limit; unreached categories: %s%n"
                .formatted(sigmaLimit - worst.sigma(), unreachedCategoryList.isEmpty() ? "none" : unreachedCategoryList));
        var sortedBySigmaDescending = deviationList.stream()
                .sorted(Comparator.comparingDouble(Deviation<Category_>::sigma).reversed())
                .toList();
        var detailList =
                sortedBySigmaDescending.size() <= MAX_DETAIL_ROW_COUNT ? sortedBySigmaDescending
                        : sortedBySigmaDescending
                                .subList(0, 5);
        for (var deviation : detailList) {
            builder.append("    %s: observed %d, expected %.1f (%+.2f%%), %.2f sigma%n".formatted(deviation.category(),
                    deviation.observed(), deviation.expected(), deviation.deviationPercent(), deviation.sigma()));
        }
        return builder.toString();
    }

}
