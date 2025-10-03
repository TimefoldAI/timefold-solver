package ai.timefold.solver.core.impl.bavet;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.common.TupleSourceRoot;
import ai.timefold.solver.core.impl.bavet.common.TupleSourceRoot.LifecycleOperation;

public abstract class AbstractSession {

    private final NodeNetwork nodeNetwork;
    private final Map<Class<?>, TupleSourceRoot<Object>[]> insertEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, TupleSourceRoot<Object>[]> updateEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, TupleSourceRoot<Object>[]> retractEffectiveClassToNodeArrayMap;

    protected AbstractSession(NodeNetwork nodeNetwork) {
        this.nodeNetwork = nodeNetwork;
        this.insertEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.updateEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.retractEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
    }

    public final void insert(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, TupleSourceRoot.LifecycleOperation.INSERT)) {
            node.insert(fact);
        }
    }

    @SuppressWarnings("unchecked")
    private TupleSourceRoot<Object>[] findNodes(Class<?> factClass, LifecycleOperation lifecycleOperation) {
        var effectiveClassToNodeArrayMap = switch (lifecycleOperation) {
            case INSERT -> insertEffectiveClassToNodeArrayMap;
            case UPDATE -> updateEffectiveClassToNodeArrayMap;
            case RETRACT -> retractEffectiveClassToNodeArrayMap;
        };
        // Map.computeIfAbsent() would have created lambdas on the hot path, this will not.
        var nodeArray = effectiveClassToNodeArrayMap.get(factClass);
        if (nodeArray == null) {
            nodeArray = nodeNetwork.getTupleSourceRootNodes(factClass)
                    .filter(node -> node.supports(lifecycleOperation))
                    .toArray(TupleSourceRoot[]::new);
            effectiveClassToNodeArrayMap.put(factClass, nodeArray);
        }
        return nodeArray;
    }

    public final void update(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, TupleSourceRoot.LifecycleOperation.UPDATE)) {
            node.update(fact);
        }
    }

    public final void retract(Object fact) {
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, TupleSourceRoot.LifecycleOperation.RETRACT)) {
            node.retract(fact);
        }
    }

    public void settle() {
        nodeNetwork.settle();
    }

}
