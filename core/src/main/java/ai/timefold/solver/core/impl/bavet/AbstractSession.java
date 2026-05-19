package ai.timefold.solver.core.impl.bavet;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode.LifecycleOperation;

public abstract class AbstractSession<Network_ extends AbstractBavetNodeNetwork> {

    protected final Network_ nodeNetwork;
    private final Map<Class<?>, AbstractRootNode<Object>[]> insertEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractRootNode<Object>[]> updateEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractRootNode<Object>[]> retractEffectiveClassToNodeArrayMap;
    private boolean settled = true;

    protected AbstractSession(Network_ nodeNetwork) {
        this.nodeNetwork = nodeNetwork;
        this.insertEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.updateEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.retractEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
    }

    public final void insert(Object fact) {
        settled = false;
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, AbstractRootNode.LifecycleOperation.INSERT)) {
            node.insert(fact);
        }
    }

    @SuppressWarnings("unchecked")
    private AbstractRootNode<Object>[] findNodes(Class<?> factClass, LifecycleOperation lifecycleOperation) {
        var effectiveClassToNodeArrayMap = switch (lifecycleOperation) {
            case INSERT -> insertEffectiveClassToNodeArrayMap;
            case UPDATE -> updateEffectiveClassToNodeArrayMap;
            case RETRACT -> retractEffectiveClassToNodeArrayMap;
        };
        // Map.computeIfAbsent() would have created lambdas on the hot path, this will not.
        var nodeArray = effectiveClassToNodeArrayMap.get(factClass);
        if (nodeArray == null) {
            nodeArray = nodeNetwork.getRootNodesAcceptingType(factClass)
                    .filter(node -> node.supports(lifecycleOperation))
                    .toArray(AbstractRootNode[]::new);
            effectiveClassToNodeArrayMap.put(factClass, nodeArray);
        }
        return nodeArray;
    }

    public final void update(Object fact) {
        settled = false;
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, AbstractRootNode.LifecycleOperation.UPDATE)) {
            node.update(fact);
        }
    }

    public final void retract(Object fact) {
        settled = false;
        var factClass = fact.getClass();
        for (var node : findNodes(factClass, AbstractRootNode.LifecycleOperation.RETRACT)) {
            node.retract(fact);
        }
    }

    public final void settle() {
        if (settled) {
            return;
        }
        nodeNetwork.settle();
        settled = true;
    }

    public Network_ getNodeNetwork() {
        return nodeNetwork;
    }
}
