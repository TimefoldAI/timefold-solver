package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;

/**
 * Investigate the nodes with a better optimistic bound first, then deeper nodes.
 */
public class OptimisticBoundFirstNodeComparator<Solution_> implements Comparator<ExhaustiveSearchNode<Solution_>> {

    public OptimisticBoundFirstNodeComparator(boolean scoreBounderEnabled) {
        if (!scoreBounderEnabled) {
            throw new IllegalArgumentException("This %s only works if scoreBounderEnabled (%s) is true."
                    .formatted(getClass().getSimpleName(), scoreBounderEnabled));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compare(ExhaustiveSearchNode<Solution_> a, ExhaustiveSearchNode<Solution_> b) {
        // Investigate better optimistic bound first (ignore initScore to avoid depth first ordering)
        var optimisticBoundComparison = a.getOptimisticBound().compareTo(b.getOptimisticBound());
        if (optimisticBoundComparison < 0) {
            return -1;
        } else if (optimisticBoundComparison > 0) {
            return 1;
        }
        // Investigate better score first
        Score aScore = a.getScore().raw();
        Score bScore = b.getScore().raw();
        var scoreComparison = aScore.compareTo(bScore);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        // Investigate deeper first
        var depthComparison = Integer.compare(a.getDepth(), b.getDepth());
        if (depthComparison != 0) {
            return depthComparison;
        }
        // Investigate higher parent breadth index first (to reduce on the churn on workingSolution)
        var parentBreadthComparison = Long.compare(a.getParentBreadth(), b.getParentBreadth());
        if (parentBreadthComparison != 0) {
            return parentBreadthComparison;
        }
        // Investigate lower breadth index first (to respect ValueSortingManner)
        return Long.compare(b.getBreadth(), a.getBreadth());
    }

}
