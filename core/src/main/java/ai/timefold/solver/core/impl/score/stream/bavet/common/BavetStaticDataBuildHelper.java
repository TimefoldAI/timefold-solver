package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetRootNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public final class BavetStaticDataBuildHelper<Tuple_ extends AbstractTuple> {
    private final NodeNetwork nodeNetwork;
    private final RecordingTupleLifecycle<Tuple_> recordingTupleLifecycle;
    private final Class<?>[] sourceClasses;

    public <Solution_> BavetStaticDataBuildHelper(BavetAbstractConstraintStream<Solution_> staticConstraintStream) {
        var streamList = new ArrayList<BavetAbstractConstraintStream<Solution_>>();
        var queue = new ArrayDeque<BavetAbstractConstraintStream<Solution_>>();
        queue.addLast(staticConstraintStream);

        while (!queue.isEmpty()) {
            var current = queue.pollFirst();
            streamList.add(current);
            if (current instanceof BavetConstraintStreamBinaryOperation<?> binaryOperation) {
                queue.addLast((BavetAbstractConstraintStream<Solution_>) binaryOperation.getLeftParent());
                queue.addLast((BavetAbstractConstraintStream<Solution_>) binaryOperation.getRightParent());
            } else {
                if (current.getParent() != null) {
                    queue.addLast(current.getParent());
                }
            }
        }
        Collections.reverse(streamList);
        var streamSet = new LinkedHashSet<>(streamList);

        var buildHelper = new ConstraintNodeBuildHelper<>(new ConsistencyTracker<>(), streamSet,
                AbstractScoreInliner.buildScoreInliner(new SimpleScoreDefinition(), Collections.emptyMap(),
                        ConstraintMatchPolicy.DISABLED));

        var declaredClassToNodeMap = new LinkedHashMap<Class<?>, List<BavetRootNode<?>>>();
        var nodeList = buildHelper.buildNodeList(streamSet, buildHelper,
                BavetAbstractConstraintStream::buildNode,
                node -> {
                    if (!(node instanceof BavetRootNode<?> sourceRootNode)) {
                        return;
                    }
                    var nodeSourceClasses = sourceRootNode.getSourceClasses();
                    for (Class<?> nodeSourceClass : nodeSourceClasses) {
                        var sourceNodeList = declaredClassToNodeMap.computeIfAbsent(nodeSourceClass, k -> new ArrayList<>(2));
                        sourceNodeList.add(sourceRootNode);
                    }
                });

        this.nodeNetwork = AbstractNodeBuildHelper.buildNodeNetwork(nodeList, declaredClassToNodeMap);
        this.recordingTupleLifecycle =
                (RecordingTupleLifecycle<Tuple_>) buildHelper.getAggregatedTupleLifecycle(List.of(staticConstraintStream));
        this.sourceClasses = declaredClassToNodeMap.keySet().toArray(new Class<?>[0]);
    }

    public NodeNetwork getNodeNetwork() {
        return nodeNetwork;
    }

    public RecordingTupleLifecycle<Tuple_> getRecordingTupleLifecycle() {
        return recordingTupleLifecycle;
    }

    public Class<?>[] getSourceClasses() {
        return sourceClasses;
    }
}
