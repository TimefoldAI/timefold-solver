package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import java.util.Comparator;

import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;

/**
 * Investigate deeper nodes first, in order.
 */
public class OriginalOrderNodeComparator<Solution_> implements Comparator<ExhaustiveSearchNode<Solution_>> {

    @Override
    public int compare(ExhaustiveSearchNode<Solution_> a, ExhaustiveSearchNode<Solution_> b) {
        // Investigate deeper first
        var depthComparison = Integer.compare(a.getDepth(), b.getDepth());
        if (depthComparison != 0) {
            return depthComparison;
        }
        // Investigate lower breadth index first (to respect ValueSortingManner)
        return Long.compare(b.getBreadth(), a.getBreadth());
    }

}
