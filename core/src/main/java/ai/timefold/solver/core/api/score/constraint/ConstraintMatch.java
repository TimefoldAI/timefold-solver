package ai.timefold.solver.core.api.score.constraint;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;

/**
 * Retrievable from {@link ConstraintMatchTotal#getConstraintMatchSet()}
 * and {@link Indictment#getConstraintMatchSet()}.
 *
 * <p>
 * This class implements {@link Comparable} for consistent ordering of constraint matches in visualizations.
 * The details of this ordering are unspecified and are subject to change.
 *
 * <p>
 * If possible, prefer using {@link SolutionManager#analyze(Object)} instead.
 *
 * @param <Score_> the actual score type
 */
public final class ConstraintMatch<Score_ extends Score<Score_>> implements Comparable<ConstraintMatch<Score_>> {

    private final ConstraintRef constraintRef;
    private final ConstraintJustification justification;
    private final List<Object> indictedObjectList;
    private final Score_ score;

    /**
     * @deprecated Prefer {@link ConstraintMatch#ConstraintMatch(ConstraintRef, ConstraintJustification, Collection, Score)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param justificationList never null, sometimes empty
     * @param score never null
     */
    @Deprecated(forRemoval = true)
    public ConstraintMatch(String constraintPackage, String constraintName, List<Object> justificationList, Score_ score) {
        this(constraintPackage, constraintName, DefaultConstraintJustification.of(score, justificationList),
                justificationList, score);
    }

    /**
     * @deprecated Prefer {@link ConstraintMatch#ConstraintMatch(ConstraintRef, ConstraintJustification, Collection, Score)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param justification never null
     * @param score never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public ConstraintMatch(String constraintPackage, String constraintName, ConstraintJustification justification,
            Collection<Object> indictedObjectList, Score_ score) {
        this(ConstraintRef.of(constraintPackage, constraintName), justification, indictedObjectList, score);
    }

    /**
     * @deprecated Prefer {@link ConstraintMatch#ConstraintMatch(ConstraintRef, ConstraintJustification, Collection, Score)}.
     * @param constraint never null
     * @param justification never null
     * @param score never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public ConstraintMatch(Constraint constraint, ConstraintJustification justification, Collection<Object> indictedObjectList,
            Score_ score) {
        this(constraint.getConstraintRef(), justification, indictedObjectList, score);
    }

    /**
     * @deprecated Prefer {@link ConstraintMatch#ConstraintMatch(ConstraintRef, ConstraintJustification, Collection, Score)}.
     * @param constraintId never null
     * @param constraintPackage never null
     * @param constraintName never null
     * @param justification never null
     * @param score never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public ConstraintMatch(String constraintId, String constraintPackage, String constraintName,
            ConstraintJustification justification, Collection<Object> indictedObjectList, Score_ score) {
        this(new ConstraintRef(constraintPackage, constraintName, constraintId), justification, indictedObjectList, score);
    }

    /**
     * @param constraintRef never null
     * @param justification never null
     * @param score never null
     */
    public ConstraintMatch(ConstraintRef constraintRef, ConstraintJustification justification,
            Collection<Object> indictedObjectList, Score_ score) {
        this.constraintRef = requireNonNull(constraintRef);
        this.justification = requireNonNull(justification);
        this.indictedObjectList =
                requireNonNull(indictedObjectList) instanceof List<Object> list ? list : List.copyOf(indictedObjectList);
        this.score = requireNonNull(score);
    }

    public ConstraintRef getConstraintRef() {
        return constraintRef;
    }

    /**
     * @deprecated Prefer {@link #getConstraintRef()} instead.
     * @return maybe null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public String getConstraintPackage() {
        return constraintRef.packageName();
    }

    /**
     * @deprecated Prefer {@link #getConstraintRef()} instead.
     * @return never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public String getConstraintName() {
        return constraintRef.constraintName();
    }

    /**
     * @deprecated Prefer {@link #getConstraintRef()} instead.
     * @return never null
     */
    @Deprecated(forRemoval = true, since = "1.4.0")
    public String getConstraintId() {
        return constraintRef.constraintId();
    }

    /**
     * Return a list of justifications for the constraint.
     * <p>
     * This method has a different meaning based on which score director the constraint comes from.
     * <ul>
     * <li>For constraint streams, it returns a list of facts from the matching tuple for backwards compatibility
     * (eg. [A, B] for a bi stream),
     * unless a custom justification mapping was provided, in which case it throws an exception,
     * pointing users towards {@link #getJustification()}.</li>
     * <li>For incremental score calculation, it returns what the calculator is implemented to return.</li>
     * </ul>
     *
     * @deprecated Prefer {@link #getJustification()} or {@link #getIndictedObjectList()}.
     * @return never null
     */
    @Deprecated(forRemoval = true)
    public List<Object> getJustificationList() {
        if (justification instanceof DefaultConstraintJustification constraintJustification) { // No custom function provided.
            return constraintJustification.getFacts();
        } else {
            throw new IllegalStateException("Cannot retrieve list of facts from a custom constraint justification ("
                    + justification + ").\n" +
                    "Use ConstraintMatch#getJustification() method instead.");
        }
    }

    /**
     * Return a singular justification for the constraint.
     * <p>
     * This method has a different meaning based on which score director the constraint comes from.
     * <ul>
     * <li>For constraint streams, it returns {@link DefaultConstraintJustification} from the matching tuple
     * (eg. [A, B] for a bi stream), unless a custom justification mapping was provided,
     * in which case it returns the return value of that function.</li>
     * <li>For incremental score calculation, it returns what the calculator is implemented to return.</li>
     * </ul>
     *
     * @return never null
     */
    public <Justification_ extends ConstraintJustification> Justification_ getJustification() {
        return (Justification_) justification;
    }

    /**
     * Returns a set of objects indicted for causing this constraint match.
     * <p>
     * This method has a different meaning based on which score director the constraint comes from.
     * <ul>
     * <li>For constraint streams, it returns the facts from the matching tuple
     * (eg. [A, B] for a bi stream), unless a custom indictment mapping was provided,
     * in which case it returns the return value of that function.</li>
     * <li>For incremental score calculation, it returns what the calculator is implemented to return.</li>
     * </ul>
     *
     * @return never null, may be empty or contain null
     */
    public List<Object> getIndictedObjectList() {
        return indictedObjectList;
    }

    public Score_ getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public String getIdentificationString() {
        return getConstraintRef().constraintId() + "/" + justification;
    }

    @Override
    public int compareTo(ConstraintMatch<Score_> other) {
        if (!constraintRef.equals(other.constraintRef)) {
            return constraintRef.compareTo(other.constraintRef);
        } else if (!score.equals(other.score)) {
            return score.compareTo(other.score);
        } else if (justification instanceof Comparable comparable) {
            return comparable.compareTo(other.justification);
        }
        return Integer.compare(System.identityHashCode(justification),
                System.identityHashCode(other.justification));
    }

    @Override
    public String toString() {
        return getIdentificationString() + "=" + score;
    }

}
