package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;

/**
 * Investigate the nodes with a better optimistic bound first, then deeper nodes.
 */
public class ScoreFirstNodeComparator implements Comparator<ExhaustiveSearchNode> {

    public ScoreFirstNodeComparator(boolean scoreBounderEnabled) {
        if (!scoreBounderEnabled) {
            throw new IllegalArgumentException("This " + getClass().getSimpleName()
                    + " only works if scoreBounderEnabled (" + scoreBounderEnabled + ") is true.");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compare(ExhaustiveSearchNode a, ExhaustiveSearchNode b) {
        // Investigate better score first (ignore initScore to avoid depth first ordering)
        Score aScore = a.getScore().raw();
        Score bScore = b.getScore().raw();
        var scoreComparison = aScore.compareTo(bScore);
        if (scoreComparison < 0) {
            return -1;
        } else if (scoreComparison > 0) {
            return 1;
        }
        // Investigate better optimistic bound first
        var optimisticBoundComparison = a.getOptimisticBound().compareTo(b.getOptimisticBound());
        if (optimisticBoundComparison < 0) {
            return -1;
        } else if (optimisticBoundComparison > 0) {
            return 1;
        }
        // Investigate deeper first
        var aDepth = a.getDepth();
        var bDepth = b.getDepth();
        if (aDepth < bDepth) {
            return -1;
        } else if (aDepth > bDepth) {
            return 1;
        }
        // Investigate higher parent breadth index first (to reduce on the churn on workingSolution)
        var aParentBreadth = a.getParentBreadth();
        var bParentBreadth = b.getParentBreadth();
        if (aParentBreadth < bParentBreadth) {
            return -1;
        } else if (aParentBreadth > bParentBreadth) {
            return 1;
        }
        // Investigate lower breadth index first (to respect ValueSortingManner)
        return Long.compare(b.getBreadth(), a.getBreadth());
    }

}
