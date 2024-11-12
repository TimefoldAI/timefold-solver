package ai.timefold.solver.benchmark.impl.ranking;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

/**
 * Able to compare {@link Score}s of different types or nulls.
 */
final class ResilientScoreComparator implements Comparator<Score> {

    private final ScoreDefinition aScoreDefinition;

    public ResilientScoreComparator(ScoreDefinition aScoreDefinition) {
        this.aScoreDefinition = aScoreDefinition;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public int compare(Score a, Score b) {
        if (a == null) {
            return b == null ? 0 : -1;
        } else if (b == null) {
            return 1;
        }
        if (!aScoreDefinition.isCompatibleArithmeticArgument(a) ||
                !aScoreDefinition.isCompatibleArithmeticArgument(b)) {
            var aNumbers = a.toLevelNumbers();
            var bNumbers = b.toLevelNumbers();
            for (var i = 0; i < aNumbers.length || i < bNumbers.length; i++) {
                var aToken = i < aNumbers.length ? aNumbers[i] : 0;
                var bToken = i < bNumbers.length ? bNumbers[i] : 0;
                int comparison;
                if (aToken.getClass().equals(bToken.getClass())
                        && aToken instanceof Comparable aTokenComparable
                        && bToken instanceof Comparable bTokenComparable) {
                    comparison = aTokenComparable.compareTo(bTokenComparable);
                } else {
                    comparison = Double.compare(aToken.doubleValue(), bToken.doubleValue());
                }
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }
        return a.compareTo(b);
    }

}
