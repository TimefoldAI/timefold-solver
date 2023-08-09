package ai.timefold.solver.constraint.streams.bavet;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractNode;
import ai.timefold.solver.constraint.streams.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.constraint.streams.common.inliner.AbstractScoreInliner;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;

final class BavetConstraintSession<Score_ extends Score<Score_>> {

    private final AbstractScoreInliner<Score_> scoreInliner;
    private final Map<Class<?>, List<AbstractForEachUniNode<Object>>> declaredClassToNodeMap;
    private final AbstractNode[][] layeredNodes; // First level is the layer, second determines iteration order.
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> effectiveClassToNodeArrayMap;

    public BavetConstraintSession(AbstractScoreInliner<Score_> scoreInliner,
            Map<Class<?>, List<AbstractForEachUniNode<Object>>> declaredClassToNodeMap,
            AbstractNode[][] layeredNodes) {
        this.scoreInliner = scoreInliner;
        this.declaredClassToNodeMap = declaredClassToNodeMap;
        this.layeredNodes = layeredNodes;
        this.effectiveClassToNodeArrayMap = new IdentityHashMap<>(declaredClassToNodeMap.size());
    }

    public void insert(Object fact) {
        Class<?> factClass = fact.getClass();
        for (AbstractForEachUniNode<Object> node : findNodes(factClass)) {
            node.insert(fact);
        }
    }

    private AbstractForEachUniNode<Object>[] findNodes(Class<?> factClass) {
        // Map.computeIfAbsent() would have created lambdas on the hot path, this will not.
        AbstractForEachUniNode<Object>[] nodeArray = effectiveClassToNodeArrayMap.get(factClass);
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
        Class<?> factClass = fact.getClass();
        for (AbstractForEachUniNode<Object> node : findNodes(factClass)) {
            node.update(fact);
        }
    }

    public void retract(Object fact) {
        Class<?> factClass = fact.getClass();
        for (AbstractForEachUniNode<Object> node : findNodes(factClass)) {
            node.retract(fact);
        }
    }

    public Score_ calculateScore(int initScore) {
        int layerCount = layeredNodes.length;
        for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
            calculateScoreInLayer(layerIndex);
        }
        return scoreInliner.extractScore(initScore);
    }

    private void calculateScoreInLayer(int layerIndex) {
        AbstractNode[] nodesInLayer = layeredNodes[layerIndex];
        int nodeCount = nodesInLayer.length;
        if (nodeCount == 1) { // Avoid iteration.
            nodesInLayer[0].propagateEverything();
        } else {
            for (AbstractNode abstractNode : nodesInLayer) {
                abstractNode.propagateRetracts();
            }
            for (AbstractNode abstractNode : nodesInLayer) {
                abstractNode.propagateUpdates();
            }
            for (AbstractNode node : nodesInLayer) {
                node.propagateInserts();
                node.clearPropagationQueue();
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
