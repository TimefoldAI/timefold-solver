package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractNodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;
import ai.timefold.solver.core.impl.bavet.common.ProfilingPropagator;
import ai.timefold.solver.core.impl.bavet.common.tuple.AggregatedTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.ProfilingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;
import ai.timefold.solver.core.impl.score.stream.bavet.ConstraintStreamsBavetNodeNetwork;
import ai.timefold.solver.core.impl.score.stream.common.ForEachFilteringCriteria;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ConstraintNodeBuildHelper<Solution_, Score_ extends Score<Score_>>
        extends AbstractNodeBuildHelper<BavetAbstractConstraintStream<Solution_>> {

    private final AbstractScoreInliner<Score_> scoreInliner;
    private final ConsistencyTracker<Solution_> consistencyTracker;
    private final @Nullable InnerConstraintProfiler constraintProfiler;
    private final Map<EntityDescriptor<Solution_>, Map<ForEachFilteringCriteria, @Nullable Predicate<Object>>> entityDescriptorToForEachCriteriaToPredicateMap;
    private final Map<BavetAbstractConstraintStream<Solution_>, List<Set<ConstraintNodeProfileId>>> streamToProfileIdSets;

    private long nextLifecycleProfilingId = 0;

    public ConstraintNodeBuildHelper(ConsistencyTracker<Solution_> consistencyTracker,
            Set<BavetAbstractConstraintStream<Solution_>> activeStreamSet, AbstractScoreInliner<Score_> scoreInliner,
            @Nullable InnerConstraintProfiler profiler) {
        super(activeStreamSet);
        this.consistencyTracker = consistencyTracker;
        this.scoreInliner = scoreInliner;
        this.constraintProfiler = profiler;
        this.entityDescriptorToForEachCriteriaToPredicateMap = new HashMap<>();
        this.streamToProfileIdSets = HashMap.newHashMap(Math.max(16, activeStreamSet.size() / 2));
    }

    @Override
    public <Tuple_ extends Tuple> void putInsertUpdateRetract(BavetAbstractConstraintStream<Solution_> stream,
            TupleLifecycle<Tuple_> tupleLifecycle) {
        if (constraintProfiler != null) {
            var out = TupleLifecycle.profiling(constraintProfiler, nextLifecycleProfilingId, stream, tupleLifecycle);
            super.putInsertUpdateRetract(stream, out);
            updateConstraintProfileIdSet(stream, out);

            if (tupleLifecycle instanceof Scorer<Tuple_> scorer) {
                // This is a scorer, so we can navigate up its parents
                // to find all locations corresponding to this constraint
                var queue = new ArrayDeque<BavetStream>();
                var constraintSet = new LinkedHashSet<ConstraintNodeProfileId>();
                queue.add(stream);
                while (!queue.isEmpty()) {
                    var currentStream = (BavetAbstractConstraintStream<Solution_>) queue.poll();
                    var streamSets = streamToProfileIdSets.computeIfAbsent(currentStream, ignored -> new ArrayList<>());
                    streamSets.add(constraintSet);
                    var lifecycle = getTupleLifecycle(currentStream);
                    if (lifecycle instanceof ProfilingTupleLifecycle<?> profilingTupleLifecycle) {
                        constraintSet.add(profilingTupleLifecycle.profileId());
                    }
                    if (currentStream instanceof BavetStreamBinaryOperation<?> binaryOperation) {
                        queue.add(binaryOperation.getLeftParent());
                        queue.add(binaryOperation.getRightParent());
                    } else if (currentStream.getParent() != null) {
                        queue.add(currentStream.getParent());
                    }
                }
                constraintProfiler.registerConstraint(scorer.getConstraintRef(), constraintSet);
            }
            nextLifecycleProfilingId++;
        } else {
            super.putInsertUpdateRetract(stream, tupleLifecycle);
        }
    }

    private void updateConstraintProfileIdSet(BavetAbstractConstraintStream<Solution_> stream,
            TupleLifecycle<?> tupleLifecycle) {
        if (tupleLifecycle instanceof ProfilingTupleLifecycle<?> profilingTupleLifecycle) {
            var affectedSets = streamToProfileIdSets.getOrDefault(stream, Collections.emptyList());
            for (var affectedSet : affectedSets) {
                affectedSet.add(profilingTupleLifecycle.profileId());
            }
        } else if (tupleLifecycle instanceof AggregatedTupleLifecycle<?> aggregated) {
            for (var innerLifecycle : aggregated.downstream()) {
                updateConstraintProfileIdSet(stream, innerLifecycle);
            }
        }
    }

    public AbstractScoreInliner<Score_> getScoreInliner() {
        return scoreInliner;
    }

    @SuppressWarnings("unchecked")
    public <A> @Nullable Predicate<A> getForEachPredicateForEntityDescriptorAndCriteria(
            EntityDescriptor<Solution_> entityDescriptor, ForEachFilteringCriteria criteria) {
        var predicateMap =
                entityDescriptorToForEachCriteriaToPredicateMap.computeIfAbsent(entityDescriptor, ignored -> new HashMap<>());
        return (Predicate<A>) predicateMap.computeIfAbsent(criteria,
                ignored -> criteria.getFilterForEntityDescriptor(consistencyTracker, entityDescriptor));
    }

    public ConstraintStreamsBavetNodeNetwork buildNodeNetwork(List<AbstractNode> nodeList,
            Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap) {
        return (ConstraintStreamsBavetNodeNetwork) super.buildNodeNetwork(nodeList, declaredClassToNodeMap,
                (classToNodeMap, layeredNodes) -> new ConstraintStreamsBavetNodeNetwork(classToNodeMap, layeredNodes,
                        constraintProfiler),
                node -> {
                    if (constraintProfiler == null) {
                        return node.getPropagator();
                    }
                    var profileKey = nextLifecycleProfilingId++;
                    var profileId = new ConstraintNodeProfileId(profileKey, node.getStreamKind(),
                            ConstraintNodeProfileId.Qualifier.NODE, node.getLocationSet());
                    constraintProfiler.register(profileId);
                    var stream = getNodeCreator(node);
                    for (var affectedSet : streamToProfileIdSets.get(stream)) {
                        affectedSet.add(profileId);
                    }
                    return new ProfilingPropagator(constraintProfiler, profileId, node.getPropagator());
                });
    }

}
