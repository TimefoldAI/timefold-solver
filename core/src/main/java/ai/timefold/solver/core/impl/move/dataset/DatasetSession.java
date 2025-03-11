package ai.timefold.solver.core.impl.move.dataset;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.AbstractSession;
import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.uni.ForEachFromSolutionUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachStaticUniNode;
import ai.timefold.solver.core.preview.api.move.SolutionView;

public final class DatasetSession<Solution_> extends AbstractSession {

    private final ForEachStaticUniNode<Object>[] effectiveClassToStaticNodeArray;
    private final ForEachFromSolutionUniNode<Solution_, Object>[] effectiveClassToFromSolutionNodeArray;
    private final Map<AbstractDataset<Solution_, ?>, DatasetInstance<Solution_, ?>> datasetInstanceMap =
            new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    DatasetSession(NodeNetwork nodeNetwork) {
        super(nodeNetwork);
        var staticNodeSet = Collections.newSetFromMap(new IdentityHashMap<ForEachStaticUniNode<?>, Boolean>());
        var fromSolutionNodeSet = Collections.newSetFromMap(new IdentityHashMap<ForEachFromSolutionUniNode<?, ?>, Boolean>());
        nodeNetwork.getForEachNodes().forEach(node -> {
            if (node instanceof ForEachStaticUniNode<?> forEachStaticUniNode) {
                staticNodeSet.add(forEachStaticUniNode);
            } else if (node instanceof ForEachFromSolutionUniNode<?, ?> forEachFromSolutionUniNode) {
                fromSolutionNodeSet.add(forEachFromSolutionUniNode);
            }
        });
        this.effectiveClassToStaticNodeArray =
                staticNodeSet.isEmpty() ? null : staticNodeSet.toArray(ForEachStaticUniNode[]::new);
        this.effectiveClassToFromSolutionNodeArray =
                fromSolutionNodeSet.isEmpty() ? null : fromSolutionNodeSet.toArray(ForEachFromSolutionUniNode[]::new);
    }

    public void registerDatasetInstance(AbstractDataset<Solution_, ?> dataset, DatasetInstance<Solution_, ?> datasetInstance) {
        var oldDatasetInstance = datasetInstanceMap.put(dataset, datasetInstance);
        if (oldDatasetInstance != null) {
            throw new IllegalStateException("The dataset (%s) has already been registered with session (%s)."
                    .formatted(dataset, this));
        }
    }

    @SuppressWarnings("unchecked")
    public <Out_ extends AbstractTuple> DatasetInstance<Solution_, Out_> getInstance(AbstractDataset<Solution_, Out_> dataset) {
        return (DatasetInstance<Solution_, Out_>) Objects.requireNonNull(datasetInstanceMap.get(dataset));
    }

    public void initialize() {
        if (effectiveClassToStaticNodeArray == null) {
            return;
        }
        for (var node : effectiveClassToStaticNodeArray) {
            node.initialize();
        }
    }

    public void updateWorkingSolution(SolutionView<Solution_> solutionView, Solution_ solution) {
        if (effectiveClassToFromSolutionNodeArray == null) {
            return;
        }
        for (var node : effectiveClassToFromSolutionNodeArray) {
            node.read(solutionView, solution);
        }
    }

    @Override
    public void settle() {
        super.settle();
    }

}
