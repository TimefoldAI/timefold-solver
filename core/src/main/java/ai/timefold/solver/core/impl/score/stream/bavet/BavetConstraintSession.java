package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.PropagationQueue;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

/**
 * The type is public to make it easier for Bavet-specific minimal bug reproducers to be created.
 * Instances should be created through {@link BavetConstraintStreamScoreDirectorFactory#newSession(Object, boolean, boolean)}.
 *
 * @see PropagationQueue Description of the tuple propagation mechanism.
 * @param <Score_>
 */
public final class BavetConstraintSession<Score_ extends Score<Score_>> {

    private final AbstractScoreInliner<Score_> scoreInliner;
    private final NodeNetwork nodeNetwork;
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> effectiveClassToNodeArrayMap;

    BavetConstraintSession(AbstractScoreInliner<Score_> scoreInliner) {
        this(scoreInliner, NodeNetwork.EMPTY);
    }

    BavetConstraintSession(AbstractScoreInliner<Score_> scoreInliner, NodeNetwork nodeNetwork) {
        this.scoreInliner = scoreInliner;
        this.nodeNetwork = nodeNetwork;
        this.effectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
    }

    public void insert(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass)) {
            node.insert(fact);
        }
    }

    private AbstractForEachUniNode<Object>[] findNodes(Class<?> factClass) {
        // Map.computeIfAbsent() would have created lambdas on the hot path, this will not.
        var nodeArray = effectiveClassToNodeArrayMap.get(factClass);
        if (nodeArray == null) {
            nodeArray = nodeNetwork.getApplicableForEachNodes(factClass);
            effectiveClassToNodeArrayMap.put(factClass, nodeArray);
        }
        return nodeArray;
    }

    public void update(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass)) {
            node.update(fact);
        }
    }

    public void retract(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass)) {
            node.retract(fact);
        }
    }

    public Score_ calculateScore(int initScore) {
        nodeNetwork.propagate();
        return scoreInliner.extractScore(initScore);
    }

    public AbstractScoreInliner<Score_> getScoreInliner() {
        return scoreInliner;
    }

    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        return scoreInliner.getConstraintIdToConstraintMatchTotalMap();
    }

    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        return scoreInliner.getIndictmentMap();
    }

}
