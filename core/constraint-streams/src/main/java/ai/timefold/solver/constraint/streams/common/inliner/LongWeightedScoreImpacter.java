package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;
import java.util.Objects;

final class LongWeightedScoreImpacter<Context_ extends ScoreContext<?, ?>>
        implements WeightedScoreImpacter<Context_> {

    private final LongImpactFunction<Context_> impactFunction;
    private final Context_ context;

    public LongWeightedScoreImpacter(LongImpactFunction<Context_> impactFunction, Context_ context) {
        this.impactFunction = Objects.requireNonNull(impactFunction);
        this.context = context;
    }

    @Override
    public UndoScoreImpacter impactScore(int matchWeight, JustificationsSupplier justificationsSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, matchWeight, justificationsSupplier); // int can be cast to long
    }

    @Override
    public UndoScoreImpacter impactScore(long matchWeight, JustificationsSupplier justificationsSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, matchWeight, justificationsSupplier);
    }

    @Override
    public UndoScoreImpacter impactScore(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        throw new UnsupportedOperationException("Impossible state: passing BigDecimal into a long impacter.");
    }

    @Override
    public Context_ getContext() {
        return context;
    }

}
