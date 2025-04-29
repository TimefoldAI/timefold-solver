package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

public final class DatasetSessionFactory<Solution_> {

    private final DataStreamFactory<Solution_> dataStreamFactory;

    public DatasetSessionFactory(DataStreamFactory<Solution_> dataStreamFactory) {
        this.dataStreamFactory = dataStreamFactory;
    }

    public DatasetSession<Solution_> buildSession() {
        var activeDataStreamSet = new LinkedHashSet<AbstractDataStream<Solution_>>();
        var datasets = dataStreamFactory.getDatasets();
        for (var dataset : datasets) {
            dataset.collectActiveDataStreams(activeDataStreamSet);
        }
        var buildHelper = new DataNodeBuildHelper<>(activeDataStreamSet);
        var session = new DatasetSession<Solution_>(buildNodeNetwork(activeDataStreamSet, buildHelper, null));
        for (var datasetInstance : buildHelper.getDatasetInstanceList()) {
            session.registerDatasetInstance(datasetInstance.getParent(), datasetInstance);
        }
        return session;
    }

    private NodeNetwork buildNodeNetwork(Set<AbstractDataStream<Solution_>> dataStreamSet,
            DataNodeBuildHelper<Solution_> buildHelper, Consumer<String> nodeNetworkVisualizationConsumer) {
        var declaredClassToNodeMap = new LinkedHashMap<Class<?>, List<AbstractForEachUniNode<?>>>();
        var nodeList = buildHelper.buildNodeList(dataStreamSet, buildHelper,
                AbstractDataStream::buildNode, node -> {
                    if (!(node instanceof AbstractForEachUniNode<?> forEachUniNode)) {
                        return;
                    }
                    var forEachClass = forEachUniNode.getForEachClass();
                    var forEachUniNodeList =
                            declaredClassToNodeMap.computeIfAbsent(forEachClass, k -> new ArrayList<>(2));
                    if (forEachUniNodeList.size() == 2) {
                        // Each class can have at most two forEach nodes: one including null vars, the other excluding them.
                        throw new IllegalStateException(
                                "Impossible state: For class (%s) there are already 2 nodes (%s), not adding another (%s)."
                                        .formatted(forEachClass, forEachUniNodeList, forEachUniNode));
                    }
                    forEachUniNodeList.add(forEachUniNode);
                });
        if (nodeNetworkVisualizationConsumer != null) {
            // TODO implement node network visualization
            throw new UnsupportedOperationException("Not implemented yet");
        }
        return AbstractNodeBuildHelper.buildNodeNetwork(nodeList, declaredClassToNodeMap);
    }

}
