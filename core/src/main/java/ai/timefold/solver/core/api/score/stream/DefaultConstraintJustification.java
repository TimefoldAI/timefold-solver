package ai.timefold.solver.core.api.score.stream;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;

/**
 * Default implementation of {@link ConstraintJustification}, returned by {@link ConstraintMatch#getJustification()}
 * unless the user defined a custom justification mapping.
 */
public final class DefaultConstraintJustification
        implements ConstraintJustification, Comparable<DefaultConstraintJustification> {

    public static DefaultConstraintJustification of(Score<?> impact, Object fact) {
        return of(impact, Collections.singletonList(fact));
    }

    public static DefaultConstraintJustification of(Score<?> impact, Object factA, Object factB) {
        return of(impact, Arrays.asList(factA, factB));
    }

    public static DefaultConstraintJustification of(Score<?> impact, Object factA, Object factB, Object factC) {
        return of(impact, Arrays.asList(factA, factB, factC));
    }

    public static DefaultConstraintJustification of(Score<?> impact, Object factA, Object factB, Object factC, Object factD) {
        return of(impact, Arrays.asList(factA, factB, factC, factD));
    }

    public static DefaultConstraintJustification of(Score<?> impact, Object... facts) {
        return of(impact, Arrays.asList(facts));
    }

    public static DefaultConstraintJustification of(Score<?> impact, List<Object> facts) {
        return new DefaultConstraintJustification(impact, facts);
    }

    private final Score<?> impact;
    private final List<Object> facts;

    private DefaultConstraintJustification(Score<?> impact, List<Object> facts) {
        this.impact = impact;
        this.facts = facts;
    }

    public <Score_ extends Score<Score_>> Score_ getImpact() {
        return (Score_) impact;
    }

    /**
     *
     * @return never null; may contain null
     */
    public List<Object> getFacts() {
        return facts;
    }

    @Override
    public String toString() {
        return facts.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultConstraintJustification other) {
            return this.compareTo(other) == 0; // Ensure consistency with compareTo().
        }
        return false;
    }

    @Override
    public int hashCode() {
        return facts.hashCode();
    }

    @Override
    public int compareTo(DefaultConstraintJustification other) {
        var justificationList = this.getFacts();
        var otherJustificationList = other.getFacts();
        if (justificationList != otherJustificationList) {
            if (justificationList.size() != otherJustificationList.size()) {
                return Integer.compare(justificationList.size(), otherJustificationList.size());
            } else { // Both lists have the same size.
                for (var i = 0; i < justificationList.size(); i++) {
                    var left = justificationList.get(i);
                    var right = otherJustificationList.get(i);
                    var comparison = compareElements(left, right);
                    if (comparison != 0) { // Element at position i differs between the two lists.
                        return comparison;
                    }
                }
            }
        }
        return 0;
    }

    private static int compareElements(Object left, Object right) {
        if (left == right) {
            return 0;
        } else if (left == null) {
            return -1;
        } else if (right == null) {
            return 1;
        } else {
            // Left and right are different, not equal and not null.
            var leftClass = left.getClass();
            var rightClass = right.getClass();
            if (leftClass != rightClass) { // Different classes; compare by class name.
                return leftClass.getCanonicalName().compareTo(rightClass.getCanonicalName());
            }
            // Both are instances of the same class.
            if (left instanceof Comparable comparable) {
                return comparable.compareTo(right);
            } else if (Objects.equals(left, right)) { // They are not comparable, but at least they're equal.
                return 0;
            } else { // Nothing to compare by; use hash code for consistent ordering.
                return Integer.compare(left.hashCode(), right.hashCode());
            }
        }
    }

}
