package ai.timefold.solver.core.impl.bavet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.StreamKind;
import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintStreamImplSupport;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishExtra;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishSolution;

import org.junit.jupiter.api.Test;

/**
 * See {@link ActivitySupport}.
 */
class BavetNodeDeactivationTest {

    /**
     * Builds the network for the given constraints + solution; setWorkingSolution settles it (read-only; no scoring).
     * <p>
     * Typed as {@link AbstractBavetNodeNetwork}, not the concrete ConstraintStreamsBavetNodeNetwork: the seam methods
     * getActiveNodes()/getNodes() are package-private in AbstractBavetNodeNetwork, and a subclass in a different
     * package does not inherit package-private members (JLS 8.4.8), so a call on the concrete type would not compile.
     * This test lives in package ai.timefold.solver.core.impl.bavet, so a base-typed reference resolves them.
     * <p>
     * Scoring is performed by a Scorer (a TupleLifecycle) held outside the node network, so no node carries
     * StreamKind.SCORING; the activity of a constraint's terminal node already reflects whether its scorer stayed
     * active (terminal isActive() == canProduceTuples() &amp;&amp; scorer.isActive()).
     */
    private static AbstractBavetNodeNetwork settle(ConstraintProvider provider, TestdataLavishSolution solution) {
        var implSupport = new BavetConstraintStreamImplSupport(ConstraintMatchPolicy.DISABLED);
        InnerScoreDirector<TestdataLavishSolution, SimpleScore> scoreDirector =
                implSupport.buildScoreDirector(TestdataLavishSolution.buildSolutionDescriptor(), provider);
        // setWorkingSolution -> afterSetWorkingSolution -> session.settle() (guarded); network is settled on return.
        scoreDirector.setWorkingSolution(solution);
        var director = (BavetConstraintStreamScoreDirector<TestdataLavishSolution, SimpleScore>) scoreDirector;
        var session = director.getSession();
        assertThat(session).isNotNull();
        return session.getNodeNetwork();
    }

    private static boolean active(AbstractBavetNodeNetwork net, StreamKind kind) {
        return net.getActiveNodes()
                .stream()
                .anyMatch(n -> n.getStreamKind() == kind);
    }

    private static boolean present(AbstractBavetNodeNetwork net, StreamKind kind) {
        return net.getNodes()
                .stream()
                .anyMatch(n -> n.getStreamKind() == kind);
    }

    private static boolean activeForEach(AbstractBavetNodeNetwork net, Class<?> forEachClass) {
        return matchesForEach(net.getActiveNodes(), forEachClass);
    }

    private static long forEachCount(Collection<AbstractNode> nodes, Class<?> forEachClass) {
        return nodes.stream()
                .filter(n -> n instanceof AbstractForEachUniNode<?> fe && fe.getForEachClass().equals(forEachClass))
                .count();
    }

    private static boolean matchesForEach(Collection<AbstractNode> nodes, Class<?> forEachClass) {
        return forEachCount(nodes, forEachClass) > 0;
    }

    private static TestdataLavishSolution solutionWithoutExtras() {
        return TestdataLavishSolution.generateSolution(); // entityList populated; extraList empty by default
    }

    private static TestdataLavishSolution solutionWithExtras() {
        var solution = TestdataLavishSolution.generateSolution();
        var extras = new ArrayList<>(solution.getExtraList());
        extras.add(new TestdataLavishExtra("extra1"));
        solution.setExtraList(extras);
        return solution;
    }

    @Test
    void emptyJoinSideDeactivatesChain() {
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishExtra.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("entityJoinExtra")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(active(net, StreamKind.JOIN)).isFalse();
        assertThat(present(net, StreamKind.JOIN)).isTrue(); // node exists but is inactive
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isFalse();
        assertThat(activeForEach(net, TestdataLavishEntity.class)).isFalse(); // feeds only the dead join
    }

    @Test
    void nonEmptyJoinSideKeepsChainActive() {
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishEntity.class)
                        .join(TestdataLavishExtra.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("entityJoinExtra")
        };
        var net = settle(provider, solutionWithExtras());

        assertThat(active(net, StreamKind.JOIN)).isTrue();
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isTrue();
        assertThat(activeForEach(net, TestdataLavishEntity.class)).isTrue();
    }

    @Test
    void ifNotExistsWithEmptyRightStaysActive() {
        // Right is irrelevant for ifNotExists: an empty right does not deactivate it.
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishEntity.class)
                        .ifNotExists(TestdataLavishExtra.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("entityIfNotExistsExtra")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(active(net, StreamKind.IF_EXISTS)).isTrue();
        assertThat(activeForEach(net, TestdataLavishEntity.class)).isTrue();
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isFalse(); // empty right root
    }

    @Test
    void ifExistsWithEmptyRightDeactivates() {
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishEntity.class)
                        .ifExists(TestdataLavishExtra.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("entityIfExistsExtra")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(active(net, StreamKind.IF_EXISTS)).isFalse();
        assertThat(present(net, StreamKind.IF_EXISTS)).isTrue();
        assertThat(activeForEach(net, TestdataLavishEntity.class)).isFalse();
    }

    @Test
    void concatWithOneEmptySideStaysActive() {
        // concat requires both sides to share arity and element type, so both forEach streams are mapped to Object.
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishExtra.class)
                        .map(extra -> (Object) extra)
                        .concat(factory.forEach(TestdataLavishEntity.class)
                                .map(entity -> (Object) entity))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("concatExtraEntity")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(active(net, StreamKind.CONCAT)).isTrue();
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isFalse(); // empty branch
        assertThat(activeForEach(net, TestdataLavishEntity.class)).isTrue(); // populated branch
    }

    @Test
    void groupByOverEmptyClassDeactivates() {
        // Locks the assumption that an empty groupBy emits nothing.
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishExtra.class)
                        .groupBy(ConstraintCollectors.count())
                        .penalize(SimpleScore.ONE)
                        .asConstraint("countExtras")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(active(net, StreamKind.GROUP_BY)).isFalse();
        assertThat(present(net, StreamKind.GROUP_BY)).isTrue();
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isFalse();
    }

    @Test
    void chainOverEmptyClassFullyDeactivates() {
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishExtra.class)
                        .filter(extra -> true)
                        .map(extra -> extra)
                        .penalize(SimpleScore.ONE)
                        .asConstraint("chainOverExtras")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(net.getActiveNodes()).isEmpty();
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isFalse();
        assertThat(active(net, StreamKind.MAP)).isFalse();
        // filter() compiles to a ConditionalTupleLifecycle WRAPPER, not a node, so there is no FILTER node.
        assertThat(present(net, StreamKind.FILTER)).isFalse();
    }

    @Test
    void flattenOverEmptyClassDeactivates() {
        // FlattenLastUniNode inherits the single-input default rule (upstreamCanProduceTuples && downstream).
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.forEach(TestdataLavishExtra.class)
                        .flattenLast(extra -> List.of(extra))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("flattenExtras")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(active(net, StreamKind.FLATTEN)).isFalse();
        assertThat(present(net, StreamKind.FLATTEN)).isTrue();
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isFalse();
    }

    @Test
    void sharedForEachStaysActiveWhenOneBranchIsAlive() {
        ConstraintProvider provider = factory -> {
            var base = factory.forEach(TestdataLavishEntity.class); // shared by both constraints
            return new Constraint[] {
                    base.penalize(SimpleScore.ONE)
                            .asConstraint("aliveBranch"),
                    base.join(TestdataLavishExtra.class)
                            .penalize(SimpleScore.ONE)
                            .asConstraint("deadBranch")
            };
        };
        var net = settle(provider, solutionWithoutExtras());

        // The shared Entity forEach is built exactly once and stays active via the alive branch,
        // even though the join branch is dead (empty Extra).
        assertThat(forEachCount(net.getNodes(), TestdataLavishEntity.class)).isEqualTo(1L);
        assertThat(activeForEach(net, TestdataLavishEntity.class)).isTrue();
        assertThat(active(net, StreamKind.JOIN)).isFalse();
        assertThat(activeForEach(net, TestdataLavishExtra.class)).isFalse();
    }

    @Test
    void precomputeOverEmptySourceDeactivates() {
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.precompute(pf -> pf.forEachUnfiltered(TestdataLavishExtra.class))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("precomputeExtras")
        };
        var net = settle(provider, solutionWithoutExtras());

        assertThat(active(net, StreamKind.PRECOMPUTE)).isFalse();
        assertThat(present(net, StreamKind.PRECOMPUTE)).isTrue();
    }

    @Test
    void precomputeOverPopulatedSourceStaysActive() {
        ConstraintProvider provider = factory -> new Constraint[] {
                factory.precompute(pf -> pf.forEachUnfiltered(TestdataLavishExtra.class))
                        .penalize(SimpleScore.ONE)
                        .asConstraint("precomputeExtras")
        };
        var net = settle(provider, solutionWithExtras());

        assertThat(active(net, StreamKind.PRECOMPUTE)).isTrue();
    }
}
