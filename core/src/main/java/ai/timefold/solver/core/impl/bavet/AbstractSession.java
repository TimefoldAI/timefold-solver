package ai.timefold.solver.core.impl.bavet;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode.LifecycleOperation;

public abstract class AbstractSession {

    private final NodeNetwork nodeNetwork;
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> insertEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> updateEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> retractEffectiveClassToNodeArrayMap;

    protected AbstractSession(NodeNetwork nodeNetwork) {
        this.nodeNetwork = nodeNetwork;
        this.insertEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.updateEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.retractEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
    }

    public final void insert(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, LifecycleOperation.INSERT)) {
            node.insert(fact);
        }
    }

    @SuppressWarnings("unchecked")
    private AbstractForEachUniNode<Object>[] findNodes(Class<?> factClass, LifecycleOperation lifecycleOperation) {
        var effectiveClassToNodeArrayMap = switch (lifecycleOperation) {
            case INSERT -> insertEffectiveClassToNodeArrayMap;
            case UPDATE -> updateEffectiveClassToNodeArrayMap;
            case RETRACT -> retractEffectiveClassToNodeArrayMap;
        };
        // Map.computeIfAbsent() would have created lambdas on the hot path, this will not.
        var nodeArray = effectiveClassToNodeArrayMap.get(factClass);
        if (nodeArray == null) {
            nodeArray = nodeNetwork.getForEachNodes(factClass)
                    .filter(node -> node.supports(lifecycleOperation))
                    .toArray(AbstractForEachUniNode[]::new);
            effectiveClassToNodeArrayMap.put(factClass, nodeArray);
        }
        return nodeArray;
    }

    public final void update(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, LifecycleOperation.UPDATE)) {
            node.update(fact);
        }
    }

    public final void retract(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, LifecycleOperation.RETRACT)) {
            node.retract(fact);
        }
    }

    public void settle() {
        nodeNetwork.settle();
    }

}
