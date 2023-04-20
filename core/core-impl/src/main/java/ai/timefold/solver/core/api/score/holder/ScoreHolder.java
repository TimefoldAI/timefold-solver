package ai.timefold.solver.core.api.score.holder;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.Score;

import org.kie.api.runtime.rule.RuleContext;

/**
 * This is the base interface for all score holder implementations.
 *
 * @deprecated Score DRL is deprecated and will be removed in a future major version of OptaPlanner.
 *             See <a href="https://www.optaplanner.org/download/upgradeRecipe/drl-to-constraint-streams-migration.html">DRL to
 *             Constraint Streams migration recipe</a>.
 * @param <Score_> the {@link Score} type
 */
@Deprecated(forRemoval = true)
public interface ScoreHolder<Score_ extends Score<Score_>> {

    /**
     * Penalize a match by the {@link ConstraintWeight} negated.
     *
     * @param kcontext never null, the magic variable in DRL
     */
    void penalize(RuleContext kcontext);

    /**
     * Reward a match by the {@link ConstraintWeight}.
     *
     * @param kcontext never null, the magic variable in DRL
     */
    void reward(RuleContext kcontext);

}
