package ai.timefold.solver.core.api.score.stream;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.lookup.ClassAndPlanningIdComparator;

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
    private Comparator<Object> classAndIdPlanningComparator;

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
    public int compareTo(DefaultConstraintJustification other) {
        String impactClassName = impact.getClass().getCanonicalName();
        String otherImpactClassName = other.impact.getClass().getCanonicalName();
        int scoreClassComparison = impactClassName.compareTo(otherImpactClassName);
        if (scoreClassComparison != 0) { // Don't fail on two different score types.
            return scoreClassComparison;
        }
        int scoreComparison = ((Score) impact).compareTo(other.impact);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        List<?> justificationList = this.getFacts();
        List<?> otherJustificationList = other.getFacts();
        if (justificationList != otherJustificationList) {
            if (justificationList.size() != otherJustificationList.size()) {
                return Integer.compare(justificationList.size(), otherJustificationList.size());
            } else {
                Comparator<Object> comparator = getClassAndIdPlanningComparator(other);
                for (int i = 0; i < justificationList.size(); i++) {
                    Object left = justificationList.get(i);
                    Object right = otherJustificationList.get(i);
                    if (left != right) {
                        int comparison = comparator.compare(left, right);
                        if (comparison != 0) {
                            return comparison;
                        }
                    }
                }
            }
        }
        return Integer.compare(System.identityHashCode(this), System.identityHashCode(other));
    }

    private Comparator<Object> getClassAndIdPlanningComparator(DefaultConstraintJustification other) {
        /*
         * The comparator performs some expensive operations, which can be cached.
         * For optimal performance, this cache (MemberAccessFactory) needs to be shared between comparators.
         * In order to prevent the comparator from being shared in a static field creating a de-facto memory leak,
         * we cache the comparator inside this class, and we minimize the number of instances that will be created
         * by creating the comparator when none of the constraint matches already carry it,
         * and we store it in both.
         */
        if (classAndIdPlanningComparator != null) {
            return classAndIdPlanningComparator;
        } else if (other.classAndIdPlanningComparator != null) {
            return other.classAndIdPlanningComparator;
        } else {
            /*
             * FIXME Using reflection will break Quarkus once we don't open up classes for reflection any more.
             * Use cases which need to operate safely within Quarkus should use SolutionDescriptor's MemberAccessorFactory.
             */
            classAndIdPlanningComparator =
                    new ClassAndPlanningIdComparator(new MemberAccessorFactory(), DomainAccessType.REFLECTION, false);
            other.classAndIdPlanningComparator = classAndIdPlanningComparator;
            return classAndIdPlanningComparator;
        }
    }

}
