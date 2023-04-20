package ai.timefold.solver.core.api.score.buildin.hardsoft;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.holder.ScoreHolder;

import org.kie.api.runtime.rule.RuleContext;

/**
 * @see HardSoftScore
 * @deprecated Score DRL is deprecated and will be removed in a future major version of Timefold.
 *             See <a href="https://timefold.ai/docs/">DRL to
 *             Constraint Streams migration recipe</a>.
 */
@Deprecated(forRemoval = true)
public interface HardSoftScoreHolder extends ScoreHolder<HardSoftScore> {

    /**
     * Penalize a match by the {@link ConstraintWeight} negated and multiplied with the weightMultiplier for all score levels.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param weightMultiplier at least 0
     */
    void penalize(RuleContext kcontext, int weightMultiplier);

    /**
     * Penalize a match by the {@link ConstraintWeight} negated and multiplied with the specific weightMultiplier per score
     * level.
     * Slower than {@link #penalize(RuleContext, int)}.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param hardWeightMultiplier at least 0
     * @param softWeightMultiplier at least 0
     */
    void penalize(RuleContext kcontext, int hardWeightMultiplier, int softWeightMultiplier);

    /**
     * Reward a match by the {@link ConstraintWeight} multiplied with the weightMultiplier for all score levels.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param weightMultiplier at least 0
     */
    void reward(RuleContext kcontext, int weightMultiplier);

    /**
     * Reward a match by the {@link ConstraintWeight} multiplied with the specific weightMultiplier per score level.
     * Slower than {@link #reward(RuleContext, int)}.
     *
     * @param kcontext never null, the magic variable in DRL
     * @param hardWeightMultiplier at least 0
     * @param softWeightMultiplier at least 0
     */
    void reward(RuleContext kcontext, int hardWeightMultiplier, int softWeightMultiplier);

    void impactScore(RuleContext kcontext, int weightMultiplier);

    /**
     * @param kcontext never null, the magic variable in DRL
     * @param hardWeight higher is better, negative for a penalty, positive for a reward
     */
    void addHardConstraintMatch(RuleContext kcontext, int hardWeight);

    /**
     * @param kcontext never null, the magic variable in DRL
     * @param softWeight higher is better, negative for a penalty, positive for a reward
     */
    void addSoftConstraintMatch(RuleContext kcontext, int softWeight);

    /**
     * @param kcontext never null, the magic variable in DRL
     * @param hardWeight higher is better, negative for a penalty, positive for a reward
     * @param softWeight higher is better, negative for a penalty, positive for a reward
     */
    void addMultiConstraintMatch(RuleContext kcontext, int hardWeight, int softWeight);
}
