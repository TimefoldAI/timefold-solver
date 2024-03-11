package ai.timefold.solver.core.impl.score.constraint;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;

/**
 * If possible, prefer using {@link SolutionManager#analyze(Object)} instead.
 *
 * @param <Score_>
 */
public final class DefaultConstraintMatchTotal<Score_ extends Score<Score_>> implements ConstraintMatchTotal<Score_>,
        Comparable<DefaultConstraintMatchTotal<Score_>> {

    private final ConstraintRef constraintRef;
    private final Score_ constraintWeight;

    private final Set<ConstraintMatch<Score_>> constraintMatchSet = new LinkedHashSet<>();
    private Score_ score;

    /**
     * @deprecated Prefer {@link #DefaultConstraintMatchTotal(ConstraintRef, Score_)}.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public DefaultConstraintMatchTotal(String constraintPackage, String constraintName) {
        this(ConstraintRef.of(constraintPackage, constraintName));
    }

    /**
     *
     * @deprecated Prefer {@link #DefaultConstraintMatchTotal(ConstraintRef, Score_)}.
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public DefaultConstraintMatchTotal(ConstraintRef constraintRef) {
        this.constraintRef = requireNonNull(constraintRef);
        this.constraintWeight = null;
    }

    /**
     * @deprecated Prefer {@link #DefaultConstraintMatchTotal(ConstraintRef, Score_)}.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public DefaultConstraintMatchTotal(Constraint constraint, Score_ constraintWeight) {
        this(constraint.getConstraintRef(), constraintWeight);
    }

    /**
     * @deprecated Prefer {@link #DefaultConstraintMatchTotal(ConstraintRef, Score_)}.
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public DefaultConstraintMatchTotal(String constraintPackage, String constraintName, Score_ constraintWeight) {
        this(ConstraintRef.of(constraintPackage, constraintName), constraintWeight);
    }

    public DefaultConstraintMatchTotal(ConstraintRef constraintRef, Score_ constraintWeight) {
        this.constraintRef = requireNonNull(constraintRef);
        this.constraintWeight = requireNonNull(constraintWeight);
        this.score = constraintWeight.zero();
    }

    @Override
    public ConstraintRef getConstraintRef() {
        return constraintRef;
    }

    @Override
    public Score_ getConstraintWeight() {
        return constraintWeight;
    }

    @Override
    public Set<ConstraintMatch<Score_>> getConstraintMatchSet() {
        return constraintMatchSet;
    }

    @Override
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
     * Additionally, the constraint match will indict the objects in the given list of justifications.
     *
     * @param justifications never null, never empty
     * @param score never null
     * @return never null
     */
    public ConstraintMatch<Score_> addConstraintMatch(List<Object> justifications, Score_ score) {
        return addConstraintMatch(DefaultConstraintJustification.of(score, justifications), justifications, score);
    }

    /**
     * Creates a {@link ConstraintMatch} and adds it to the collection returned by {@link #getConstraintMatchSet()}.
     * It will be justified with the provided {@link ConstraintJustification}.
     * Additionally, the constraint match will indict the objects in the given list of indicted objects.
     *
     * @param indictedObjects never null, may be empty
     * @param score never null
     * @return never null
     */
    public ConstraintMatch<Score_> addConstraintMatch(ConstraintJustification justification, Collection<Object> indictedObjects,
            Score_ score) {
        ConstraintMatch<Score_> constraintMatch = new ConstraintMatch<>(constraintRef, justification, indictedObjects, score);
        addConstraintMatch(constraintMatch);
        return constraintMatch;
    }

    public void addConstraintMatch(ConstraintMatch<Score_> constraintMatch) {
        Score_ constraintMatchScore = constraintMatch.getScore();
        this.score = this.score == null ? constraintMatchScore : this.score.add(constraintMatchScore);
        constraintMatchSet.add(constraintMatch);
    }

    public void removeConstraintMatch(ConstraintMatch<Score_> constraintMatch) {
        score = score.subtract(constraintMatch.getScore());
        boolean removed = constraintMatchSet.remove(constraintMatch);
        if (!removed) {
            throw new IllegalStateException("The constraintMatchTotal (" + this
                    + ") could not remove constraintMatch (" + constraintMatch
                    + ") from its constraintMatchSet (" + constraintMatchSet + ").");
        }
    }

    // ************************************************************************
    // Infrastructure methods
    // ************************************************************************

    @Override
    public int compareTo(DefaultConstraintMatchTotal<Score_> other) {
        return constraintRef.compareTo(other.constraintRef);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultConstraintMatchTotal<?> other) {
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
