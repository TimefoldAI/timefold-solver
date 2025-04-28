package ai.timefold.solver.core.impl.bavet;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode.LifecycleOperation;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public abstract class AbstractSession implements AutoCloseable {

    private final NodeNetwork nodeNetwork;
    private final Map<Class<?>, AbstractForEachUniNode.InitializableForEachNode<Object>[]> initializeEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> insertEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> updateEffectiveClassToNodeArrayMap;
    private final Map<Class<?>, AbstractForEachUniNode<Object>[]> retractEffectiveClassToNodeArrayMap;

    protected AbstractSession(NodeNetwork nodeNetwork) {
        this.nodeNetwork = nodeNetwork;
        this.initializeEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.insertEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.updateEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
        this.retractEffectiveClassToNodeArrayMap = new IdentityHashMap<>(nodeNetwork.forEachNodeCount());
    }

    public final void initialize(Object workingSolution, SupplyManager supplyManager) {
        for (var node : findInitializableNodes()) {
            node.initialize(workingSolution, supplyManager);
        }
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

    @SuppressWarnings("unchecked")
    private AbstractForEachUniNode.InitializableForEachNode<Object>[] findInitializableNodes() {
        // There will only be one solution class in the problem.
        // Therefore we do not need to know what it is, and using the annotation class will serve as a unique key.
        var factClass = PlanningSolution.class;
        var effectiveClassToNodeArrayMap = initializeEffectiveClassToNodeArrayMap;
        // Map.computeIfAbsent() would have created lambdas on the hot path, this will not.
        var nodeArray = effectiveClassToNodeArrayMap.get(factClass);
        if (nodeArray == null) {
            nodeArray = nodeNetwork.getForEachNodes(factClass)
                    .flatMap(node -> {
                        if (node instanceof AbstractForEachUniNode.InitializableForEachNode<?> initializableForEachNode) {
                            return Stream.of(initializableForEachNode);
                        } else {
                            return Stream.empty();
                        }
                    })
                    .toArray(AbstractForEachUniNode.InitializableForEachNode[]::new);
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

    @Override
    public final void close() {
        for (var node : findInitializableNodes()) {
            // Initializable nodes get a supply manager, fair to assume they will be demanding supplies.
            // Give them the opportunity to cancel those demands.
            node.close();
        }
    }

}
