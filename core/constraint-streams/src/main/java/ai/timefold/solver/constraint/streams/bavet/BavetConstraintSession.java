package ai.timefold.solver.constraint.streams.bavet;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.constraint.streams.bavet.common.PropagationQueue;
import ai.timefold.solver.constraint.streams.bavet.common.Propagator;
import ai.timefold.solver.constraint.streams.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.constraint.streams.common.inliner.AbstractScoreInliner;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;

/**
 * The type is public to make it easier for Bavet-specific minimal bug reproducers to be created.
 * Instances should be created through {@link BavetConstraintStreamScoreDirectorFactory#newSession(boolean, Object)}.
 *
 * @see PropagationQueue Description of the tuple propagation mechanism.
 * @param <Score_>
 */
public final class BavetConstraintSession<Score_ extends Score<Score_>> {

    private final AbstractScoreInliner<Score_> scoreInliner;
    private final Map<Class<?>, List<AbstractForEachUniNode<Object>>> declaredClassToNodeMap;
    private final Propagator[][] layeredNodes; // First level is the layer, second determines iteration order.
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> effectiveClassToNodeArrayMap;

    BavetConstraintSession(AbstractScoreInliner<Score_> scoreInliner) {
        this(scoreInliner, Collections.emptyMap(), new Propagator[0][0]);
    }

    BavetConstraintSession(AbstractScoreInliner<Score_> scoreInliner,
            Map<Class<?>, List<AbstractForEachUniNode<Object>>> declaredClassToNodeMap,
            Propagator[][] layeredNodes) {
        this.scoreInliner = scoreInliner;
        this.declaredClassToNodeMap = declaredClassToNodeMap;
        this.layeredNodes = layeredNodes;
        this.effectiveClassToNodeArrayMap = new IdentityHashMap<>(declaredClassToNodeMap.size());
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
            nodeArray = declaredClassToNodeMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().isAssignableFrom(factClass))
                    .map(Map.Entry::getValue)
                    .flatMap(List::stream)
                    .toArray(AbstractForEachUniNode[]::new);
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
        var layerCount = layeredNodes.length;
        for (var layerIndex = 0; layerIndex < layerCount; layerIndex++) {
            calculateScoreInLayer(layerIndex);
        }
        return scoreInliner.extractScore(initScore);
    }

    private void calculateScoreInLayer(int layerIndex) {
        var nodesInLayer = layeredNodes[layerIndex];
        var nodeCount = nodesInLayer.length;
        if (nodeCount == 1) {
            nodesInLayer[0].propagateEverything();
        } else {
            for (var node : nodesInLayer) {
                node.propagateRetracts();
            }
            for (var node : nodesInLayer) {
                node.propagateUpdates();
            }
            for (var node : nodesInLayer) {
                node.propagateInserts();
            }
        }
    }

    public AbstractScoreInliner<Score_> getScoreInliner() {
        return scoreInliner;
    }

    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        return scoreInliner.getConstraintMatchTotalMap();
    }

    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        return scoreInliner.getIndictmentMap();
    }

}
