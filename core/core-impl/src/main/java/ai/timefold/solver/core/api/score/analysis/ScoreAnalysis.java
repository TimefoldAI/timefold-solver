package ai.timefold.solver.core.api.score.analysis;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;

/**
 * Represents the breakdown of a {@link Score} into individual {@link ConstraintAnalysis} instances,
 * one for each constraint.
 * Compared to {@link ScoreExplanation}, this is JSON-friendly and faster to generate.
 *
 * <p>
 * In order to be fully serializable to JSON, {@link MatchAnalysis} instances must be serializable to JSON
 * and that requires any implementations of {@link ConstraintJustification} to be serializable to JSON.
 * This is the responsibility of the user.
 *
 * <p>
 * For deserialization from JSON, the user needs to provide the deserializer themselves.
 * This is due to the fact that, once the {@link ScoreAnalysis} is received over the wire,
 * we no longer know which {@link Score} type or {@link ConstraintJustification} type was used.
 * The user has all of that information in their domain model,
 * and so they are the correct party to provide the deserializer.
 *
 * <p>
 * Note: the constructors of this record are off-limits.
 * We ask users to use exclusively {@link SolutionManager#analyze(Object)} to obtain instances of this record.
 *
 * @param score never null
 * @param constraintMap never null;
 *        for each constraint identified by its {@link Constraint#getConstraintRef()},
 *        the {@link ConstraintAnalysis} that describes the impact of that constraint on the overall score.
 *        Constraints are present even if they have no matches, unless their weight is zero;
 *        zero-weight constraints are not present.
 *        Entries in the map have a stable iteration order; items are ordered first by {@link ConstraintAnalysis#weight()},
 *        then by {@link ConstraintAnalysis#constraintRef()}.
 *
 * @param <Score_>
 */
public record ScoreAnalysis<Score_ extends Score<Score_>>(Score_ score,
        Map<ConstraintRef, ConstraintAnalysis<Score_>> constraintMap) {

    public ScoreAnalysis {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(constraintMap, "constraintMap");
        if (constraintMap.isEmpty()) {
            throw new IllegalArgumentException("The constraintMap must not be empty.");
        }
        // Ensure consistent order and no external interference.
        var comparator = Comparator.<ConstraintAnalysis<Score_>, Score_> comparing(ConstraintAnalysis::weight)
                .reversed()
                .thenComparing(ConstraintAnalysis::constraintRef);
        constraintMap = Collections.unmodifiableMap(constraintMap.values()
                .stream()
                .sorted(comparator)
                .collect(Collectors.toMap(
                        ConstraintAnalysis::constraintRef,
                        Function.identity(),
                        (constraintAnalysis, otherConstraintAnalysis) -> constraintAnalysis,
                        LinkedHashMap::new)));
    }

    /**
     * Performs a lookup on {@link #constraintMap()}.
     * Equivalent to {@code constraintMap().get(constraintRef)}.
     *
     * @param constraintRef never null
     * @return null if no constraint matches of such constraint are present
     */
    public ConstraintAnalysis<Score_> getConstraintAnalysis(ConstraintRef constraintRef) {
        return constraintMap.get(constraintRef);
    }

    /**
     * As defined by {@link #getConstraintAnalysis(ConstraintRef)}
     * where the arguments are first composed into a singular constraint ID.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @return null if no constraint matches of such constraint are present
     */
    public ConstraintAnalysis<Score_> getConstraintAnalysis(String constraintPackage, String constraintName) {
        return getConstraintAnalysis(ConstraintRef.of(constraintPackage, constraintName));
    }

    /**
     * Compare this {@link ScoreAnalysis} to another {@link ScoreAnalysis}
     * and retrieve the difference between them.
     * The comparison is in the direction of {@code this - other}.
     * <p>
     * Example: if {@code this} has a score of 100 and {@code other} has a score of 90,
     * the returned {@link ScoreAnalysis#score} will be 10.
     * If this and other were inverted, the score would have been -10.
     * The same applies to all other properties of {@link ScoreAnalysis}.
     *
     * <p>
     * In order to properly diff {@link MatchAnalysis} against each other,
     * we rely on the user implementing {@link ConstraintJustification} equality correctly.
     * In other words, the diff will consider two justifications equal if the user says they are equal,
     * and it expects the hash code to be consistent with equals.
     *
     * <p>
     * If one {@link ScoreAnalysis} provides {@link MatchAnalysis} and the other doesn't, exception is thrown.
     * Such {@link ScoreAnalysis} instances are mutually incompatible.
     *
     * @param other never null
     * @return never null
     */
    public ScoreAnalysis<Score_> diff(ScoreAnalysis<Score_> other) {
        var result = Stream.concat(constraintMap.keySet().stream(),
                other.constraintMap.keySet().stream())
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        constraintRef -> {
                            var constraintAnalysis = getConstraintAnalysis(constraintRef);
                            var otherConstraintAnalysis = other.getConstraintAnalysis(constraintRef);
                            return ConstraintAnalysis.diff(constraintRef, constraintAnalysis, otherConstraintAnalysis);
                        },
                        (constraintRef, otherConstraintRef) -> constraintRef,
                        TreeMap::new));
        return new ScoreAnalysis<>(score.subtract(other.score()), result);
    }

}
