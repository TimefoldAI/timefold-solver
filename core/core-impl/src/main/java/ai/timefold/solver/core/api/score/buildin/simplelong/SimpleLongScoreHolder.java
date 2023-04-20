package ai.timefold.solver.core.api.score.buildin.simplelong;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.holder.ScoreHolder;

import org.kie.api.runtime.rule.RuleContext;

/**
 * @see SimpleLongScore
 * @deprecated Score DRL is deprecated and will be removed in a future major version of Timefold.
 *             See <a href="https://timefold.ai/docs/">DRL to
 *             Constraint Streams migration recipe</a>.
 */
@Deprecated(forRemoval = true)
public interface SimpleLongScoreHolder extends ScoreHolder<SimpleLongScore> {

    /**
     * Penalize a match by the {@link ConstraintWeight} negated and multiplied with the weightMultiplier for all score levels.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param weightMultiplier at least 0
     */
    void penalize(RuleContext kcontext, long weightMultiplier);

    /**
     * Reward a match by the {@link ConstraintWeight} multiplied with the weightMultiplier for all score levels.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param weightMultiplier at least 0
     */
    void reward(RuleContext kcontext, long weightMultiplier);

    void impactScore(RuleContext kcontext, long weightMultiplier);

    /**
     * @param kcontext never null, the magic variable in DRL
     * @param weight higher is better, negative for a penalty, positive for a reward
     */
    void addConstraintMatch(RuleContext kcontext, long weight);
}
