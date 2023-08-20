package ai.timefold.solver.core.api.score.constraint;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;

/**
 * Retrievable from {@link ConstraintMatchTotal#getConstraintMatchSet()}
 * and {@link Indictment#getConstraintMatchSet()}.
 *
 * <p>
 * This class implements {@link Comparable} for consistent ordering of constraint matches in visualizations.
 * The details of this ordering are unspecified and are subject to change.
 *
 * @param <Score_> the actual score type
 */
public final class ConstraintMatch<Score_ extends Score<Score_>> implements Comparable<ConstraintMatch<Score_>> {

    private final String constraintId;
    private final String constraintPackage;
    private final String constraintName;

    private final ConstraintJustification justification;
    private final List<Object> indictedObjectList;
    private final Score_ score;

    /**
     * @deprecated Prefer {@link ConstraintMatch#ConstraintMatch(String, String, ConstraintJustification, Collection, Score)}.
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
     * @param constraintPackage never null
     * @param constraintName never null
     * @param justification never null
     * @param score never null
     */
    public ConstraintMatch(String constraintPackage, String constraintName, ConstraintJustification justification,
            Collection<Object> indictedObjectList, Score_ score) {
        this(ConstraintMatchTotal.composeConstraintId(constraintPackage, constraintName), constraintPackage, constraintName,
                justification, indictedObjectList, score);
    }

    /**
     * @param constraint never null
     * @param justification never null
     * @param score never null
     */
    public ConstraintMatch(Constraint constraint, ConstraintJustification justification, Collection<Object> indictedObjectList,
            Score_ score) {
        this(constraint.getConstraintId(), constraint.getConstraintPackage(), constraint.getConstraintName(), justification,
                indictedObjectList, score);
    }

    /**
     * @param constraintId never null
     * @param constraintPackage never null
     * @param constraintName never null
     * @param justification never null
     * @param score never null
     */
    public ConstraintMatch(String constraintId, String constraintPackage, String constraintName,
            ConstraintJustification justification, Collection<Object> indictedObjectList, Score_ score) {
        this.constraintId = requireNonNull(constraintId);
        this.constraintPackage = requireNonNull(constraintPackage);
        this.constraintName = requireNonNull(constraintName);
        this.justification = requireNonNull(justification);
        this.indictedObjectList =
                requireNonNull(indictedObjectList) instanceof List<Object> list ? list : List.copyOf(indictedObjectList);
        this.score = requireNonNull(score);
    }

    public String getConstraintPackage() {
        return constraintPackage;
    }

    public String getConstraintName() {
        return constraintName;
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
     * @return never null, may be empty
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

    public String getConstraintId() {
        return constraintId;
    }

    public String getIdentificationString() {
        return getConstraintId() + "/" + justification;
    }

    @Override
    public int compareTo(ConstraintMatch<Score_> other) {
        if (!constraintId.equals(other.constraintId)) {
            return constraintId.compareTo(other.constraintId);
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
