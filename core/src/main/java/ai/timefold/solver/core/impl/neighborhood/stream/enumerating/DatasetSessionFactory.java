package ai.timefold.solver.core.impl.neighborhood.stream.enumerating;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.score.director.SessionContext;

public final class DatasetSessionFactory<Solution_> {

    private final EnumeratingStreamFactory<Solution_> enumeratingStreamFactory;

    public DatasetSessionFactory(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory) {
        this.enumeratingStreamFactory = enumeratingStreamFactory;
    }

    public DatasetSession<Solution_> buildSession(SessionContext<Solution_> context) {
        var activeEnumeratingStreamSet = new LinkedHashSet<AbstractEnumeratingStream<Solution_>>();
        var datasets = enumeratingStreamFactory.getDatasets();
        for (var dataset : datasets) {
            dataset.collectActiveEnumeratingStreams(activeEnumeratingStreamSet);
        }
        var buildHelper = new DataNodeBuildHelper<>(context, activeEnumeratingStreamSet);
        var session = new DatasetSession<Solution_>(buildNodeNetwork(activeEnumeratingStreamSet, buildHelper, null));
        for (var datasetInstance : buildHelper.getDatasetInstanceList()) {
            session.registerDatasetInstance(datasetInstance.getParent(), datasetInstance);
        }
        return session;
    }

    private NodeNetwork buildNodeNetwork(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet,
            DataNodeBuildHelper<Solution_> buildHelper, Consumer<String> nodeNetworkVisualizationConsumer) {
        var declaredClassToNodeMap = new LinkedHashMap<Class<?>, List<AbstractForEachUniNode<?>>>();
        var nodeList = buildHelper.buildNodeList(enumeratingStreamSet, buildHelper,
                AbstractEnumeratingStream::buildNode, node -> {
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
