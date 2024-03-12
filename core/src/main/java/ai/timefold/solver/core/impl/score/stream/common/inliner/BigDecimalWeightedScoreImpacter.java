package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;

final class BigDecimalWeightedScoreImpacter<Score_ extends Score<Score_>, Context_ extends ScoreContext<Score_, ?>>
        implements WeightedScoreImpacter<Score_, Context_> {

    private final BigDecimalImpactFunction<Score_, Context_> impactFunction;
    private final Context_ context;

    public BigDecimalWeightedScoreImpacter(BigDecimalImpactFunction<Score_, Context_> impactFunction,
            Context_ context) {
        this.impactFunction = Objects.requireNonNull(impactFunction);
        this.context = context;
    }

    @Override
    public UndoScoreImpacter impactScore(int matchWeight, ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, BigDecimal.valueOf(matchWeight), constraintMatchSupplier);
    }

    @Override
    public UndoScoreImpacter impactScore(long matchWeight, ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, BigDecimal.valueOf(matchWeight), constraintMatchSupplier);
    }

    @Override
    public UndoScoreImpacter impactScore(BigDecimal matchWeight, ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, matchWeight, constraintMatchSupplier);
    }

    @Override
    public Context_ getContext() {
        return context;
    }

}
