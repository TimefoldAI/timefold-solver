package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;

/**
 * Investigate nodes layer by layer: investigate shallower nodes first.
 * This results in horrible memory scalability.
 * <p>
 * A typical {@link ScoreBounder}'s {@link ScoreBounder#calculateOptimisticBound(ScoreDirector, InnerScore)}
 * will be weak, which results in horrible performance scalability too.
 */
public class BreadthFirstNodeComparator<Solution_> implements Comparator<ExhaustiveSearchNode<Solution_>> {

    private final boolean scoreBounderEnabled;

    public BreadthFirstNodeComparator(boolean scoreBounderEnabled) {
        this.scoreBounderEnabled = scoreBounderEnabled;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(ExhaustiveSearchNode<Solution_> a, ExhaustiveSearchNode<Solution_> b) {
        // Investigate shallower nodes first
        var depthComparison = Integer.compare(a.getDepth(), b.getDepth());
        if (depthComparison != 0) {
            return -depthComparison;
        }
        // Investigate better score first (ignore initScore to avoid depth first ordering)
        Score aScore = a.getScore().raw();
        Score bScore = b.getScore().raw();
        var scoreComparison = aScore.compareTo(bScore);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        if (scoreBounderEnabled) {
            // Investigate better optimistic bound first
            var optimisticBoundComparison = a.getOptimisticBound().compareTo(b.getOptimisticBound());
            if (optimisticBoundComparison != 0) {
                return optimisticBoundComparison;
            }
        }
        // No point to investigating higher parent breadth index first (no impact on the churn on workingSolution)
        // Investigate lower breadth index first (to respect ValueSortingManner)
        return Long.compare(b.getBreadth(), a.getBreadth());
    }

}
