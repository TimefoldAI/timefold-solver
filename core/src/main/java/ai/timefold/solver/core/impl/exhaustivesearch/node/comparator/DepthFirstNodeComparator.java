package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;

/**
 * Investigate deeper nodes first.
 */
public class DepthFirstNodeComparator<Solution_> implements Comparator<ExhaustiveSearchNode<Solution_>> {

    private final boolean scoreBounderEnabled;

    public DepthFirstNodeComparator(boolean scoreBounderEnabled) {
        this.scoreBounderEnabled = scoreBounderEnabled;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compare(ExhaustiveSearchNode<Solution_> a, ExhaustiveSearchNode<Solution_> b) {
        // Investigate deeper first
        var depthComparison = Integer.compare(a.getDepth(), b.getDepth());
        if (depthComparison != 0) {
            return depthComparison;
        }
        // Investigate better score first (ignore initScore as that's already done by investigate deeper first)
        Score aScore = a.getScore().raw();
        Score bScore = b.getScore().raw();
        var scoreComparison = aScore.compareTo(bScore);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        // Pitfall: score is compared before optimisticBound, because of this mixed ONLY_UP and ONLY_DOWN cases:
        // - Node a has score 0hard/20medium/-50soft and optimisticBound 0hard/+(infinity)medium/-50soft
        // - Node b has score 0hard/0medium/0soft and optimisticBound 0hard/+(infinity)medium/0soft
        // In non-mixed cases, the comparison order is irrelevant.
        if (scoreBounderEnabled) {
            // Investigate better optimistic bound first
            var optimisticBoundComparison = a.getOptimisticBound().compareTo(b.getOptimisticBound());
            if (optimisticBoundComparison != 0) {
                return optimisticBoundComparison;
            }
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
