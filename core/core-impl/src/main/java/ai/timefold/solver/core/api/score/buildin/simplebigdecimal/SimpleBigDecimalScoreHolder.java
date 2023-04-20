package ai.timefold.solver.core.api.score.buildin.simplebigdecimal;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.holder.ScoreHolder;

import org.kie.api.runtime.rule.RuleContext;

/**
 * @see SimpleBigDecimalScore
 * @deprecated Score DRL is deprecated and will be removed in a future major version of Timefold.
 *             See <a href="https://timefold.ai/docs/">DRL to
 *             Constraint Streams migration recipe</a>.
 */
@Deprecated(forRemoval = true)
public interface SimpleBigDecimalScoreHolder extends ScoreHolder<SimpleBigDecimalScore> {

    /**
     * Penalize a match by the {@link ConstraintWeight} negated and multiplied with the weightMultiplier for all score levels.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param weightMultiplier at least 0
     */
    void penalize(RuleContext kcontext, BigDecimal weightMultiplier);

    /**
     * Reward a match by the {@link ConstraintWeight} multiplied with the weightMultiplier for all score levels.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param weightMultiplier at least 0
     */
    void reward(RuleContext kcontext, BigDecimal weightMultiplier);

    void impactScore(RuleContext kcontext, BigDecimal weightMultiplier);

    /**
     * @param kcontext never null, the magic variable in DRL
     * @param weight never null, higher is better, negative for a penalty, positive for a reward
     */
    void addConstraintMatch(RuleContext kcontext, BigDecimal weight);

}
