package ai.timefold.solver.core.api.score.stream.bi;

import java.util.Collection;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

/**
 * Used to build a {@link Constraint} out of a {@link BiConstraintStream}, applying optional configuration.
 * To build the constraint, use one of the terminal operations, such as {@link #asConstraint(String)}.
 * <p>
 * Unless {@link #justifyWith(TriFunction)} is called,
 * the default justification mapping will be used.
 * The function takes the input arguments and converts them into a {@link java.util.List}.
 * <p>
 * Unless {@link #indictWith(BiFunction)} is called, the default indicted objects' mapping will be used.
 * The function takes the input arguments and converts them into a {@link java.util.List}.
 */
public interface BiConstraintBuilder<A, B, Score_ extends Score<Score_>> extends ConstraintBuilder {

    /**
     * Sets a custom function to apply on a constraint match to justify it.
     *
     * @see ConstraintMatch
     * @param justificationMapping never null
     * @return this
     */
    <ConstraintJustification_ extends ConstraintJustification> BiConstraintBuilder<A, B, Score_> justifyWith(
            TriFunction<A, B, Score_, ConstraintJustification_> justificationMapping);

    /**
     * Sets a custom function to mark any object returned by it as responsible for causing the constraint to match.
     * Each object in the collection returned by this function will become an {@link Indictment}
     * and be available as a key in {@link ScoreExplanation#getIndictmentMap()}.
     *
     * @param indictedObjectsMapping never null
     * @return this
     */
    BiConstraintBuilder<A, B, Score_> indictWith(BiFunction<A, B, Collection<Object>> indictedObjectsMapping);

}
