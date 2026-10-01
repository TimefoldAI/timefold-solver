package ai.timefold.solver.core.api.score.stream.bi;

import java.util.Collection;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

import org.jspecify.annotations.NullMarked;

/**
 * Used to build a {@link Constraint} out of a {@link BiConstraintStream}, applying optional configuration.
 * To build the constraint, use one of the terminal operations, such as {@link #asConstraint(String)}.
 * <p>
 * Unless {@link #justifyWith(TriFunction)} is called,
 * the default justification mapping will be used.
 * The function takes the input arguments and converts them into a {@link java.util.List}.
 */
@NullMarked
public interface BiConstraintBuilder<A, B, Score_ extends Score<Score_>> extends ConstraintBuilder {

    /**
     * Sets a custom function to apply on a constraint match to justify it.
     * That function must not return a {@link Collection},
     * else {@link IllegalStateException} will be thrown during score calculation.
     *
     * <p>
     * Note: {@link ScoreAnalysis} in general and constraint justifications in particular
     * are exclusive to Timefold Solver Enterprise Edition.
     * Users of the open-source version of Timefold Solver can still use this method,
     * but it will have no practical effect.
     *
     * @return this
     */
    <ConstraintJustification_ extends ConstraintJustification> BiConstraintBuilder<A, B, Score_> justifyWith(
            TriFunction<A, B, Score_, ConstraintJustification_> justificationMapping);

}
