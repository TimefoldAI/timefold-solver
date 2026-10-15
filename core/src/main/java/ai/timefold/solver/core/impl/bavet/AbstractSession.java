package ai.timefold.solver.core.impl.bavet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode.LifecycleOperation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractSession<Network_ extends AbstractBavetNodeNetwork> {

    @SuppressWarnings("unchecked")
    private static final AbstractRootNode<Object>[] EMPTY_NODE_ARRAY = new AbstractRootNode[0];

    protected final Network_ nodeNetwork;
    private final Map<Class<?>, AbstractRootNode<Object>[]> insertEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractRootNode<Object>[]> updateEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractRootNode<Object>[]> retractEffectiveClassToNodeArrayMap;
    /**
     * The fact class of the most recent operation, and the nodes it resolved to.
     * Consecutive operations typically carry the same class, in which case the map read is skipped.
     */
    private @Nullable Class<?> lastInsertClass;
    private @Nullable Class<?> lastUpdateClass;
    private @Nullable Class<?> lastRetractClass;
    private AbstractRootNode<Object>[] lastInsertNodeArray = EMPTY_NODE_ARRAY;
    private AbstractRootNode<Object>[] lastUpdateNodeArray = EMPTY_NODE_ARRAY;
    private AbstractRootNode<Object>[] lastRetractNodeArray = EMPTY_NODE_ARRAY;
    private boolean initialized = false;
    private boolean settled = false;

    protected AbstractSession(Network_ nodeNetwork) {
        this.nodeNetwork = nodeNetwork;
        this.insertEffectiveClassToNodeArrayMap = HashMap.newHashMap(nodeNetwork.forEachNodeCount());
        this.updateEffectiveClassToNodeArrayMap = HashMap.newHashMap(nodeNetwork.forEachNodeCount());
        this.retractEffectiveClassToNodeArrayMap = HashMap.newHashMap(nodeNetwork.forEachNodeCount());
    }

    public final void insert(Object fact) {
        settled = false;
        for (var node : getInsertNodes(fact.getClass())) {
            node.insert(fact);
        }
    }

    private AbstractRootNode<Object>[] getInsertNodes(Class<?> factClass) {
        if (factClass != lastInsertClass) {
            lastInsertNodeArray = findNodes(factClass, LifecycleOperation.INSERT);
            lastInsertClass = factClass;
        }
        return lastInsertNodeArray;
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
        for (var node : getUpdateNodes(fact.getClass())) {
            node.update(fact);
        }
    }

    private AbstractRootNode<Object>[] getUpdateNodes(Class<?> factClass) {
        if (factClass != lastUpdateClass) {
            lastUpdateNodeArray = findNodes(factClass, LifecycleOperation.UPDATE);
            lastUpdateClass = factClass;
        }
        return lastUpdateNodeArray;
    }

    public final void retract(Object fact) {
        settled = false;
        for (var node : getRetractNodes(fact.getClass())) {
            node.retract(fact);
        }
    }

    private AbstractRootNode<Object>[] getRetractNodes(Class<?> factClass) {
        if (factClass != lastRetractClass) {
            lastRetractNodeArray = findNodes(factClass, LifecycleOperation.RETRACT);
            lastRetractClass = factClass;
        }
        return lastRetractNodeArray;
    }

    public final void settle() {
        if (settled) {
            return;
        }
        nodeNetwork.settle();
        if (!initialized && nodeNetwork.isActivationCheckComplete()) {
            removeInactiveRootNodes(insertEffectiveClassToNodeArrayMap);
            removeInactiveRootNodes(updateEffectiveClassToNodeArrayMap);
            removeInactiveRootNodes(retractEffectiveClassToNodeArrayMap);
            lastInsertClass = null; // The cached arrays may include nodes which are now inactive.
            lastUpdateClass = null;
            lastRetractClass = null;
            initialized = true;
        }
        settled = true;
    }

    private void removeInactiveRootNodes(Map<Class<?>, AbstractRootNode<Object>[]> effectiveClassToNodeArrayMap) {
        // Use getActiveNodes() for this, to not rerun the activity checking logic again.
        effectiveClassToNodeArrayMap.replaceAll((k, v) -> Arrays.stream(v)
                .filter(nodeNetwork.getActiveNodes()::contains)
                .toArray(AbstractRootNode[]::new));
    }

    public Network_ getNodeNetwork() {
        return nodeNetwork;
    }
}
