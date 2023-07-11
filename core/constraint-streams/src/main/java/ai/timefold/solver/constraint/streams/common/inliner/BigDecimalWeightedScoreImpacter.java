package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;
import java.util.Objects;

final class BigDecimalWeightedScoreImpacter<Context_ extends ScoreContext<?, ?>>
        implements WeightedScoreImpacter<Context_> {

    private final BigDecimalImpactFunction<Context_> impactFunction;
    private final Context_ context;

    public BigDecimalWeightedScoreImpacter(BigDecimalImpactFunction<Context_> impactFunction,
            Context_ context) {
        this.impactFunction = Objects.requireNonNull(impactFunction);
        this.context = context;
    }

    @Override
    public UndoScoreImpacter impactScore(int matchWeight, JustificationsSupplier justificationsSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, BigDecimal.valueOf(matchWeight), justificationsSupplier);
    }

    @Override
    public UndoScoreImpacter impactScore(long matchWeight, JustificationsSupplier justificationsSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, BigDecimal.valueOf(matchWeight), justificationsSupplier);
    }

    @Override
    public UndoScoreImpacter impactScore(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, matchWeight, justificationsSupplier);
    }

    @Override
    public Context_ getContext() {
        return context;
    }

}
