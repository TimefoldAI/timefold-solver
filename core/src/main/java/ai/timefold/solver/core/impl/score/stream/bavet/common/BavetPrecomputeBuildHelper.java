package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetRootNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public final class BavetPrecomputeBuildHelper<Tuple_ extends Tuple> {
    private final NodeNetwork nodeNetwork;
    private final RecordingTupleLifecycle<Tuple_> recordingTupleLifecycle;
    private final Class<?>[] sourceClasses;
    private final Set<Class<?>> entityClassSet;

    public <Solution_> BavetPrecomputeBuildHelper(
            BavetAbstractConstraintStream<Solution_> recordingPrecomputeConstraintStream,
            Set<Class<?>> entityClassSet) {
        if (recordingPrecomputeConstraintStream.getRetrievalSemantics() != RetrievalSemantics.PRECOMPUTE) {
            throw new IllegalStateException(
                    "Impossible state: %s is not %s but is instead %s. Maybe you accidentally used a %s from %s instead of %s?"
                            .formatted(RetrievalSemantics.class.getSimpleName(), RetrievalSemantics.PRECOMPUTE,
                                    recordingPrecomputeConstraintStream.getRetrievalSemantics(),
                                    ConstraintStream.class.getSimpleName(), ConstraintFactory.class.getSimpleName(),
                                    PrecomputeFactory.class.getSimpleName()));
        }
        this.entityClassSet = entityClassSet;

        var streamList = new ArrayList<BavetAbstractConstraintStream<Solution_>>();
        var queue = new ArrayDeque<BavetAbstractConstraintStream<Solution_>>();
        queue.addLast(recordingPrecomputeConstraintStream);

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
                        ConstraintMatchPolicy.DISABLED),
                null);

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

        this.nodeNetwork = AbstractNodeBuildHelper.buildNodeNetwork(nodeList, declaredClassToNodeMap, buildHelper);
        this.recordingTupleLifecycle =
                (RecordingTupleLifecycle<Tuple_>) buildHelper
                        .getAggregatedTupleLifecycle(List.of(recordingPrecomputeConstraintStream));
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

    public boolean isSourceEntityClass(Class<?> maybeSourceEntityClass) {
        for (var entityClass : entityClassSet) {
            if (entityClass.isAssignableFrom(maybeSourceEntityClass)) {
                return true;
            }
        }
        return false;
    }
}
