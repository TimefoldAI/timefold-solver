package ai.timefold.solver.core.impl.score.stream.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ScoreImpact;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ImpactHandler<Tuple_ extends Tuple> {

    /**
     * Impacts the score, providing neither constraint match information, neither constraintverifier for it.
     *
     * @param impacter knows which level(s) of the score to impact, and using which type of number
     * @param tuple the tuple that triggered the impact
     * @return run to undo the impact
     */
    ScoreImpact<?> impactNaked(WeightedScoreImpacter<?, ?> impacter, Tuple_ tuple);

    /**
     * Impacts the score, providing constraint match information but constraintverifier for it.
     *
     * @param impacter knows which level(s) of the score to impact, and using which type of number
     * @param tuple the tuple that triggered the impact
     * @return run to undo the impact
     */
    ScoreImpact<?> impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, Tuple_ tuple);

    /**
     * Impacts the score, providing all available metadata.
     *
     * @param impacter knows which level(s) of the score to impact, and using which type of number
     * @param tuple the tuple that triggered the impact
     * @return run to undo the impact
     */
    ScoreImpact<?> impactFull(WeightedScoreImpacter<?, ?> impacter, Tuple_ tuple);

}
