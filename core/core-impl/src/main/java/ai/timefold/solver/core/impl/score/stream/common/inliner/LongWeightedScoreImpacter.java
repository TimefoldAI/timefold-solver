package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;

final class LongWeightedScoreImpacter<Score_ extends Score<Score_>, Context_ extends ScoreContext<Score_, ?>>
        implements WeightedScoreImpacter<Score_, Context_> {

    private final LongImpactFunction<Score_, Context_> impactFunction;
    private final Context_ context;

    public LongWeightedScoreImpacter(LongImpactFunction<Score_, Context_> impactFunction, Context_ context) {
        this.impactFunction = Objects.requireNonNull(impactFunction);
        this.context = context;
    }

    @Override
    public UndoScoreImpacter impactScore(int matchWeight, ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, matchWeight, constraintMatchSupplier); // int can be cast to long
    }

    @Override
    public UndoScoreImpacter impactScore(long matchWeight, ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        context.getConstraint().assertCorrectImpact(matchWeight);
        return impactFunction.impact(context, matchWeight, constraintMatchSupplier);
    }

    @Override
    public UndoScoreImpacter impactScore(BigDecimal matchWeight, ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        throw new UnsupportedOperationException("Impossible state: passing BigDecimal into a long impacter.");
    }

    @Override
    public Context_ getContext() {
        return context;
    }

}
