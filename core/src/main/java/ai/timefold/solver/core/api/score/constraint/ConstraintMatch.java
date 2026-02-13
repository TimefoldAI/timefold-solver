package ai.timefold.solver.core.api.score.constraint;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
@NullMarked
public final class ConstraintMatch<Score_ extends Score<Score_>> implements Comparable<ConstraintMatch<Score_>> {

    private final ConstraintRef constraintRef;
    private final @Nullable ConstraintJustification justification;
    private final List<@Nullable Object> indictedObjectList;
    private final Score_ score;

    /**
     * @param constraintRef unique identifier of the constraint
     * @param justification only null if justifications are disabled
     * @param indictedObjectList never null, empty if justifications are disabled
     * @param score penalty or reward associated with the constraint match
     */
    public ConstraintMatch(ConstraintRef constraintRef, @Nullable ConstraintJustification justification,
            Collection<Object> indictedObjectList, Score_ score) {
        this.constraintRef = requireNonNull(constraintRef);
        this.justification = justification;
        this.indictedObjectList =
                requireNonNull(indictedObjectList) instanceof List<Object> list ? list : List.copyOf(indictedObjectList);
        this.score = requireNonNull(score);
    }

    public ConstraintRef getConstraintRef() {
        return constraintRef;
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
     * <li>It may return null, if justification support was disabled altogether.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public <Justification_ extends ConstraintJustification> @Nullable Justification_ getJustification() {
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
     * <li>It may return an empty list, if justification support was disabled altogether.</li>
     * </ul>
     *
     * @return may be empty or contain null
     */
    public List<@Nullable Object> getIndictedObjectList() {
        return indictedObjectList;
    }

    public Score_ getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public String getIdentificationString() {
        return getConstraintRef().constraintName() + "/" + justification;
    }

    @Override
    public int compareTo(ConstraintMatch<Score_> other) {
        if (!constraintRef.equals(other.constraintRef)) {
            return constraintRef.compareTo(other.constraintRef);
        } else if (!score.equals(other.score)) {
            return score.compareTo(other.score);
        } else if (justification == null) {
            return other.justification == null ? 0 : -1;
        } else if (other.justification == null) {
            return 1;
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
