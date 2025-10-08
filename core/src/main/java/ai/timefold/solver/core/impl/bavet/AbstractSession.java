package ai.timefold.solver.core.impl.bavet;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.common.BavetRootNode;
import ai.timefold.solver.core.impl.bavet.common.BavetRootNode.LifecycleOperation;

public abstract class AbstractSession {

    private final NodeNetwork nodeNetwork;
    private final Map<Class<?>, BavetRootNode<Object>[]> insertEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, BavetRootNode<Object>[]> updateEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, BavetRootNode<Object>[]> retractEffectiveClassToNodeArrayMap;
    private final BavetRootNode<Object>[] settleNodes;

    protected AbstractSession(NodeNetwork nodeNetwork) {
        this.nodeNetwork = nodeNetwork;
        this.insertEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.updateEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.retractEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.settleNodes = nodeNetwork.getAllTupleSourceRootNodes()
                .filter(node -> node.supports(LifecycleOperation.SETTLE))
                .toArray(BavetRootNode[]::new);
    }

    public final void insert(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, BavetRootNode.LifecycleOperation.INSERT)) {
            node.insert(fact);
        }
    }

    @SuppressWarnings("unchecked")
    private BavetRootNode<Object>[] findNodes(Class<?> factClass, LifecycleOperation lifecycleOperation) {
        var effectiveClassToNodeArrayMap = switch (lifecycleOperation) {
            case INSERT -> insertEffectiveClassToNodeArrayMap;
            case UPDATE -> updateEffectiveClassToNodeArrayMap;
            case RETRACT -> retractEffectiveClassToNodeArrayMap;
            case SETTLE ->
                throw new IllegalArgumentException("impossible state: findNodes should not be called for settle nodes");
        };
        // Map.computeIfAbsent() would have created lambdas on the hot path, this will not.
        var nodeArray = effectiveClassToNodeArrayMap.get(factClass);
        if (nodeArray == null) {
            nodeArray = nodeNetwork.getTupleSourceRootNodes(factClass)
                    .filter(node -> node.supports(lifecycleOperation))
                    .toArray(BavetRootNode[]::new);
            effectiveClassToNodeArrayMap.put(factClass, nodeArray);
        }
        return nodeArray;
    }

    public final void update(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, BavetRootNode.LifecycleOperation.UPDATE)) {
            node.update(fact);
        }
    }

    public final void retract(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, BavetRootNode.LifecycleOperation.RETRACT)) {
            node.retract(fact);
        }
    }

    public void settle() {
        for (var node : settleNodes) {
            node.settle();
        }
        nodeNetwork.settle();
    }

}
