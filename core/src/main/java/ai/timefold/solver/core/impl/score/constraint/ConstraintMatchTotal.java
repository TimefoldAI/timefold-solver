package ai.timefold.solver.core.impl.score.constraint;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;

import org.jspecify.annotations.NullMarked;

/**
 * If possible, prefer using {@link SolutionManager#analyze(Object)} instead.
 *
 * @param <Score_>
 */
@NullMarked
public final class ConstraintMatchTotal<Score_ extends Score<Score_>> implements Comparable<ConstraintMatchTotal<Score_>> {

    private final ConstraintRef constraintRef;
    private final Score_ constraintWeight;

    private final Set<ConstraintMatch<Score_>> constraintMatchSet = new LinkedHashSet<>();
    private Score_ score;

    public ConstraintMatchTotal(ConstraintRef constraintRef, Score_ constraintWeight) {
        this.constraintRef = requireNonNull(constraintRef);
        this.constraintWeight = requireNonNull(constraintWeight);
        this.score = constraintWeight.zero();
    }

    public ConstraintRef getConstraintRef() {
        return constraintRef;
    }

    public Score_ getConstraintWeight() {
        return constraintWeight;
    }

    public int getConstraintMatchCount() {
        return constraintMatchSet.size();
    }

    public Set<ConstraintMatch<Score_>> getConstraintMatchSet() {
        return constraintMatchSet;
    }

    public Score_ getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * Creates a {@link ConstraintMatch} and adds it to the collection returned by {@link #getConstraintMatchSet()}.
     * It will use {@link DefaultConstraintJustification},
     * whose {@link DefaultConstraintJustification#getFacts()} method will return the given list of justifications.
     *
     * @param justifications never null, never empty
     * @param score never null
     * @return never null
     */
    public ConstraintMatch<Score_> addConstraintMatch(List<Object> justifications, Score_ score) {
        return addConstraintMatch(DefaultConstraintJustification.of(score, justifications), score);
    }

    /**
     * Creates a {@link ConstraintMatch} and adds it to the collection returned by {@link #getConstraintMatchSet()}.
     * It will be justified with the provided {@link ConstraintJustification}.
     *
     * @param score never null
     * @return never null
     */
    public ConstraintMatch<Score_> addConstraintMatch(ConstraintJustification justification, Score_ score) {
        var constraintMatch = new ConstraintMatch<Score_>(constraintRef, justification, score);
        addConstraintMatch(constraintMatch);
        return constraintMatch;
    }

    public void addConstraintMatch(ConstraintMatch<Score_> constraintMatch) {
        var constraintMatchScore = constraintMatch.getScore();
        this.score = this.score.add(constraintMatchScore);
        constraintMatchSet.add(constraintMatch);
    }

    public void removeConstraintMatch(ConstraintMatch<Score_> constraintMatch) {
        score = score.subtract(constraintMatch.getScore());
        var removed = constraintMatchSet.remove(constraintMatch);
        if (!removed) {
            throw new IllegalStateException(
                    "The constraintMatchTotal (%s) could not remove constraintMatch (%s) from its constraintMatchSet (%s)."
                            .formatted(this, constraintMatch, constraintMatchSet));
        }
    }

    // ************************************************************************
    // Infrastructure methods
    // ************************************************************************

    @Override
    public int compareTo(ConstraintMatchTotal<Score_> other) {
        return constraintRef.compareTo(other.constraintRef);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConstraintMatchTotal<?> other) {
            return constraintRef.equals(other.constraintRef);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return constraintRef.hashCode();
    }

    @Override
    public String toString() {
        return constraintRef + "=" + score;
    }

}
