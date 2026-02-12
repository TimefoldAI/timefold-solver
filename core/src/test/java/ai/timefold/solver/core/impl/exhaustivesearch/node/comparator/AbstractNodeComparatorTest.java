package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public abstract class AbstractNodeComparatorTest {

    protected <Solution_> ExhaustiveSearchNode<Solution_> buildNode(int depth, InnerScore<SimpleScore> score,
            long parentBreadth, long breadth) {
        return buildNode(depth, score, InnerScore.fullyAssigned(score.raw()), parentBreadth, breadth);
    }

    protected <Solution_> ExhaustiveSearchNode<Solution_> buildNode(int depth, String score, long parentBreadth, long breadth) {
        return buildNode(depth, InnerScore.fullyAssigned(SimpleScore.parseScore(score)), parentBreadth, breadth);
    }

    protected <Solution_> ExhaustiveSearchNode<Solution_> buildNode(int depth, InnerScore<SimpleScore> score,
            int optimisticBound,
            long parentBreadth, long breadth) {
        return buildNode(depth, score, InnerScore.fullyAssigned(SimpleScore.of(optimisticBound)), parentBreadth, breadth);
    }

    protected <Solution_> ExhaustiveSearchNode<Solution_> buildNode(int depth, String score, int optimisticBound,
            long parentBreadth, long breadth) {
        return buildNode(depth, InnerScore.fullyAssigned(SimpleScore.parseScore(score)), optimisticBound, parentBreadth,
                breadth);
    }

    protected <Solution_> ExhaustiveSearchNode<Solution_> buildNode(int depth, InnerScore<SimpleScore> score,
            InnerScore<SimpleScore> optimisticBound,
            long parentBreadth, long breadth) {
        var node = mock(ExhaustiveSearchNode.class);
        when(node.getDepth()).thenReturn(depth);
        doReturn(score).when(node).getScore();
        doReturn(optimisticBound).when(node).getOptimisticBound();
        when(node.getParentBreadth()).thenReturn(parentBreadth);
        when(node.getBreadth()).thenReturn(breadth);
        when(node.toString()).thenReturn(score.toString());
        return node;
    }

    protected static <Solution_> void assertLesser(Comparator<ExhaustiveSearchNode<Solution_>> comparator,
            ExhaustiveSearchNode<Solution_> a, ExhaustiveSearchNode<Solution_> b) {
        assertSoftly(softly -> {
            softly.assertThat(comparator.compare(a, b))
                    .as("Node (" + a + ") must be lesser than node (" + b + ").")
                    .isLessThan(0);
            softly.assertThat(comparator.compare(b, a))
                    .as("Node (" + b + ") must be greater than node (" + a + ").")
                    .isGreaterThan(0);
        });
    }

    protected static <Solution_> void assertScoreCompareToOrder(Comparator<ExhaustiveSearchNode<Solution_>> comparator,
            ExhaustiveSearchNode<Solution_>... nodes) {
        for (var i = 0; i < nodes.length; i++) {
            for (var j = i + 1; j < nodes.length; j++) {
                assertLesser(comparator, nodes[i], nodes[j]);
            }
        }
    }

}
