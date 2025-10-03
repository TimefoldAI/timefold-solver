package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.TupleSourceRoot;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.StaticDataUniNode;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetConstraintStreamBinaryOperation;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public class BavetStaticDataUniConstraintStream<Solution_, A> extends BavetAbstractUniConstraintStream<Solution_, A>
        implements TupleSource {
    private final BavetAbstractConstraintStream<Solution_> recordingStaticConstraintStream;
    private BavetAftBridgeUniConstraintStream<Solution_, A> aftStream;

    public BavetStaticDataUniConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> staticConstraintStream) {
        super(constraintFactory, RetrievalSemantics.STANDARD);
        this.recordingStaticConstraintStream = new BavetRecordingUniConstraintStream<>(constraintFactory,
                staticConstraintStream);
        staticConstraintStream.getChildStreamList().add(recordingStaticConstraintStream);
    }

    public void setAftBridge(BavetAftBridgeUniConstraintStream<Solution_, A> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var networkRecorderPair = buildNodeNetwork(recordingStaticConstraintStream);
        var staticNodeNetwork = networkRecorderPair.network;
        var recorder = networkRecorderPair.recorder;

        var outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        buildHelper.addNode(new StaticDataUniNode<>(staticNodeNetwork,
                recorder,
                outputStoreSize,
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()),
                staticNodeNetwork.declaredClassToNodeMap().values().stream().flatMap(List::stream)
                        .filter(node -> node instanceof AbstractForEachUniNode<?>)
                        .map(node -> ((AbstractForEachUniNode<?>) node).getForEachClass())
                        .toArray(Class[]::new)),
                this);
    }

    private NetworkRecorderPair<A> buildNodeNetwork(BavetAbstractConstraintStream<Solution_> staticConstraintStream) {
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

        var declaredClassToNodeMap = new LinkedHashMap<Class<?>, List<TupleSourceRoot<?>>>();
        var recorderReference = new AtomicReference<RecordingTupleNode<UniTuple<A>>>();
        var nodeList = buildHelper.buildNodeList(streamSet, buildHelper,
                BavetAbstractConstraintStream::buildNode,
                node -> {
                    if (node instanceof RecordingTupleNode<?> recordingTupleNode) {
                        recorderReference.setPlain((RecordingTupleNode<UniTuple<A>>) recordingTupleNode);
                        return;
                    }
                    if (!(node instanceof AbstractForEachUniNode<?> forEachUniNode)) {
                        return;
                    }
                    var forEachClass = forEachUniNode.getForEachClass();
                    var forEachUniNodeList =
                            declaredClassToNodeMap.computeIfAbsent(forEachClass, k -> new ArrayList<>(2));
                    forEachUniNodeList.add(forEachUniNode);
                });
        return new NetworkRecorderPair<>(
                AbstractNodeBuildHelper.buildNodeNetwork(nodeList, declaredClassToNodeMap),
                recorderReference.getPlain());
    }

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        constraintStreamSet.add(this);
    }

    private record NetworkRecorderPair<A>(NodeNetwork network, RecordingTupleNode<UniTuple<A>> recorder) {
    }
}
