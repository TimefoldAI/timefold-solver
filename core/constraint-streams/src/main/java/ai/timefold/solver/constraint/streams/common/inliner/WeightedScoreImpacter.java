package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;

/**
 * There are several valid ways how an impacter could be called from a constraint stream:
 *
 * <ul>
 * <li>{@code .penalize(..., (int) 1)}</li>
 * <li>{@code .penalizeLong(..., (int) 1)}</li>
 * <li>{@code .penalizeLong(..., (long) 1)}</li>
 * <li>{@code .penalizeBigDecimal(..., (int) 1)}</li>
 * <li>{@code .penalizeBigDecimal(..., (long) 1)}</li>
 * <li>{@code .penalizeBigDecimal(..., BigDecimal.ONE)}</li>
 * <li>Plus reward variants of the above.</li>
 * </ul>
 *
 * An implementation of this interface can throw an {@link UnsupportedOperationException}
 * for the method types it doesn't support. The CS API guarantees no types are mixed. For example,
 * a {@link BigDecimal} parameter method won't be called on an instance built with an {@link IntImpactFunction}.
 */
public interface WeightedScoreImpacter<Context_ extends ScoreContext<?, ?>> {

    static <Context_ extends ScoreContext<?, ?>> WeightedScoreImpacter<Context_>
            of(Context_ context, IntImpactFunction<Context_> impactFunction) {
        return new IntWeightedScoreImpacter<>(impactFunction, context);
    }

    static <Context_ extends ScoreContext<?, ?>> WeightedScoreImpacter<Context_>
            of(Context_ context, LongImpactFunction<Context_> impactFunction) {
        return new LongWeightedScoreImpacter<>(impactFunction, context);
    }

    static <Context_ extends ScoreContext<?, ?>> WeightedScoreImpacter<Context_>
            of(Context_ context, BigDecimalImpactFunction<Context_> impactFunction) {
        return new BigDecimalWeightedScoreImpacter<>(impactFunction, context);
    }

    /**
     * @param matchWeight never null
     * @param justificationsSupplier ignored unless constraint match enableds
     * @return never null
     */
    UndoScoreImpacter impactScore(int matchWeight, JustificationsSupplier justificationsSupplier);

    /**
     * @param matchWeight never null
     * @param justificationsSupplier ignored unless constraint match enabled
     * @return never null
     */
    UndoScoreImpacter impactScore(long matchWeight, JustificationsSupplier justificationsSupplier);

    /**
     * @param matchWeight never null
     * @param justificationsSupplier ignored unless constraint match enabled
     * @return never null
     */
    UndoScoreImpacter impactScore(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier);

    Context_ getContext();

    @FunctionalInterface
    interface IntImpactFunction<Context_ extends ScoreContext<?, ?>> {

        UndoScoreImpacter impact(Context_ context, int matchWeight, JustificationsSupplier justificationsSupplier);

    }

    @FunctionalInterface
    interface LongImpactFunction<Context_ extends ScoreContext<?, ?>> {

        UndoScoreImpacter impact(Context_ context, long matchWeight, JustificationsSupplier justificationsSupplier);

    }

    @FunctionalInterface
    interface BigDecimalImpactFunction<Context_ extends ScoreContext<?, ?>> {

        UndoScoreImpacter impact(Context_ context, BigDecimal matchWeight, JustificationsSupplier justificationsSupplier);

    }

}
