package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.score.director.InnerScore;

/**
 * Investigate nodes layer by layer: investigate shallower nodes first.
 * This results in horrible memory scalability.
 * <p>
 * A typical {@link ScoreBounder}'s {@link ScoreBounder#calculateOptimisticBound(ScoreDirector, InnerScore)}
 * will be weak, which results in horrible performance scalability too.
 */
public class BreadthFirstNodeComparator implements Comparator<ExhaustiveSearchNode> {

    private final boolean scoreBounderEnabled;

    public BreadthFirstNodeComparator(boolean scoreBounderEnabled) {
        this.scoreBounderEnabled = scoreBounderEnabled;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compare(ExhaustiveSearchNode a, ExhaustiveSearchNode b) {
        // Investigate shallower nodes first
        var aDepth = a.getDepth();
        var bDepth = b.getDepth();
        if (aDepth < bDepth) {
            return 1;
        } else if (aDepth > bDepth) {
            return -1;
        }
        // Investigate better score first (ignore initScore to avoid depth first ordering)
        Score aScore = a.getScore().raw();
        Score bScore = b.getScore().raw();
        var scoreComparison = aScore.compareTo(bScore);
        if (scoreComparison < 0) {
            return -1;
        } else if (scoreComparison > 0) {
            return 1;
        }
        if (scoreBounderEnabled) {
            // Investigate better optimistic bound first
            var optimisticBoundComparison = a.getOptimisticBound().compareTo(b.getOptimisticBound());
            if (optimisticBoundComparison < 0) {
                return -1;
            } else if (optimisticBoundComparison > 0) {
                return 1;
            }
        }
        // No point to investigating higher parent breadth index first (no impact on the churn on workingSolution)
        // Investigate lower breadth index first (to respect ValueSortingManner)
        return Long.compare(b.getBreadth(), a.getBreadth());
    }

}
