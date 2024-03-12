package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;

public abstract class AbstractGroupNode<InTuple_ extends AbstractTuple, OutTuple_ extends AbstractTuple, GroupKey_, ResultContainer_, Result_>
        extends AbstractNode
        implements TupleLifecycle<InTuple_> {

    private final int groupStoreIndex;
    /**
     * Unused when {@link #hasCollector} is false.
     */
    private final int undoStoreIndex;
    /**
     * Unused when {@link #hasMultipleGroups} is false.
     */
    private final Function<InTuple_, GroupKey_> groupKeyFunction;
    /**
     * Unused when {@link #hasCollector} is false.
     */
    private final Supplier<ResultContainer_> supplier;
    /**
     * Unused when {@link #hasCollector} is false.
     */
    private final Function<ResultContainer_, Result_> finisher;
    /**
     * Some code paths may decide to not supply a grouping function.
     * In that case, every tuple accumulates into {@link #singletonGroup} and not to {@link #groupMap}.
     */
    private final boolean hasMultipleGroups;
    /**
     * Some code paths may decide to not supply a collector.
     * In that case, we skip the code path that would attempt to use it.
     */
    private final boolean hasCollector;
    /**
     * Used when {@link #hasMultipleGroups} is true, otherwise {@link #singletonGroup} is used.
     */
    private final Map<Object, Group<OutTuple_, ResultContainer_>> groupMap;
    /**
     * Used when {@link #hasMultipleGroups} is false, otherwise {@link #groupMap} is used.
     *
     * @implNote The field is lazy initialized in order to maintain the same semantics as with the groupMap above.
     *           When all tuples are removed, the field will be set to null, as if the group never existed.
     */
    private Group<OutTuple_, ResultContainer_> singletonGroup;
    private final DynamicPropagationQueue<OutTuple_, Group<OutTuple_, ResultContainer_>> propagationQueue;
    private final boolean useAssertingGroupKey;

    protected AbstractGroupNode(int groupStoreIndex, int undoStoreIndex,
            Function<InTuple_, GroupKey_> groupKeyFunction, Supplier<ResultContainer_> supplier,
            Function<ResultContainer_, Result_> finisher,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, EnvironmentMode environmentMode) {
        this.groupStoreIndex = groupStoreIndex;
        this.undoStoreIndex = undoStoreIndex;
        this.groupKeyFunction = groupKeyFunction;
        this.supplier = supplier;
        this.finisher = finisher;
        this.hasMultipleGroups = groupKeyFunction != null;
        this.hasCollector = supplier != null;
        /*
         * Not using the default sizing to 1000.
         * The number of groups can be very small, and that situation is not unlikely.
         * Therefore, the size of these collections is kept default.
         */
        this.groupMap = hasMultipleGroups ? new HashMap<>() : null;
        this.propagationQueue = hasCollector ? new DynamicPropagationQueue<>(nextNodesTupleLifecycle,
                group -> {
                    var outTuple = group.getTuple();
                    var state = outTuple.state;
                    if (state == TupleState.CREATING || state == TupleState.UPDATING) {
                        updateOutTupleToFinisher(outTuple, group.getResultContainer());
                    }
                }) : new DynamicPropagationQueue<>(nextNodesTupleLifecycle);
        this.useAssertingGroupKey = environmentMode.isAsserted();
    }

    protected AbstractGroupNode(int groupStoreIndex,
            Function<InTuple_, GroupKey_> groupKeyFunction, TupleLifecycle<OutTuple_> nextNodesTupleLifecycle,
            EnvironmentMode environmentMode) {
        this(groupStoreIndex, -1,
                groupKeyFunction, null, null, nextNodesTupleLifecycle,
                environmentMode);
    }

    @Override
    public final void insert(InTuple_ tuple) {
        if (tuple.getStore(groupStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tuple
                    + ") was already added in the tupleStore.");
        }
        var userSuppliedKey = hasMultipleGroups ? groupKeyFunction.apply(tuple) : null;
        createTuple(tuple, userSuppliedKey);
    }

    private void createTuple(InTuple_ tuple, GroupKey_ userSuppliedKey) {
        var newGroup = getOrCreateGroup(userSuppliedKey);
        var outTuple = accumulate(tuple, newGroup);
        switch (outTuple.state) {
            case CREATING, UPDATING -> {
                // Already in the correct state.
            }
            case OK, DYING -> propagationQueue.update(newGroup);
            case ABORTING -> propagationQueue.insert(newGroup);
            default -> throw new IllegalStateException("Impossible state: The group (" + newGroup + ") in node (" + this
                    + ") is in an unexpected state (" + outTuple.state + ").");
        }
    }

    private OutTuple_ accumulate(InTuple_ tuple, Group<OutTuple_, ResultContainer_> group) {
        if (hasCollector) {
            var undoAccumulator = accumulate(group.getResultContainer(), tuple);
            tuple.setStore(undoStoreIndex, undoAccumulator);
        }
        tuple.setStore(groupStoreIndex, group);
        return group.getTuple();
    }

    private Group<OutTuple_, ResultContainer_> getOrCreateGroup(GroupKey_ userSuppliedKey) {
        var groupMapKey = useAssertingGroupKey ? new AssertingGroupKey(userSuppliedKey) : userSuppliedKey;
        if (hasMultipleGroups) {
            // Avoids computeIfAbsent in order to not create lambdas on the hot path.
            var group = groupMap.get(groupMapKey);
            if (group == null) {
                group = createGroupWithGroupKey(groupMapKey);
                groupMap.put(groupMapKey, group);
            } else {
                group.parentCount++;
            }
            return group;
        } else {
            if (singletonGroup == null) {
                singletonGroup = createGroupWithoutGroupKey();
            } else {
                singletonGroup.parentCount++;
            }
            return singletonGroup;
        }
    }

    private Group<OutTuple_, ResultContainer_> createGroupWithGroupKey(Object groupMapKey) {
        var userSuppliedKey = extractUserSuppliedKey(groupMapKey);
        var outTuple = createOutTuple(userSuppliedKey);
        var group = hasCollector ? Group.create(groupMapKey, supplier.get(), outTuple)
                : Group.<OutTuple_, ResultContainer_> createWithoutAccumulate(groupMapKey, outTuple);
        propagationQueue.insert(group);
        return group;
    }

    private Group<OutTuple_, ResultContainer_> createGroupWithoutGroupKey() {
        var outTuple = createOutTuple(null);
        if (!hasCollector) {
            throw new IllegalStateException("Impossible state: The node (" + this + ") has no collector, "
                    + "but it is still trying to create a group without a group key.");
        }
        var group = Group.createWithoutGroupKey(supplier.get(), outTuple);
        propagationQueue.insert(group);
        return group;
    }

    private GroupKey_ extractUserSuppliedKey(Object groupMapKey) {
        return useAssertingGroupKey ? ((AssertingGroupKey) groupMapKey).getKey() : (GroupKey_) groupMapKey;
    }

    @Override
    public final void update(InTuple_ tuple) {
        Group<OutTuple_, ResultContainer_> oldGroup = tuple.getStore(groupStoreIndex);
        if (oldGroup == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insert(tuple);
            return;
        }
        if (hasCollector) {
            Runnable undoAccumulator = tuple.getStore(undoStoreIndex);
            undoAccumulator.run();
        }

        var oldUserSuppliedGroupKey = hasMultipleGroups ? extractUserSuppliedKey(oldGroup.getGroupKey()) : null;
        var newUserSuppliedGroupKey = hasMultipleGroups ? groupKeyFunction.apply(tuple) : null;
        if (Objects.equals(newUserSuppliedGroupKey, oldUserSuppliedGroupKey)) {
            // No need to change parentCount because it is the same group
            var outTuple = accumulate(tuple, oldGroup);
            switch (outTuple.state) {
                case CREATING, UPDATING -> {
                    // Already in the correct state.
                }
                case OK -> propagationQueue.update(oldGroup);
                default -> throw new IllegalStateException("Impossible state: The group (" + oldGroup + ") in node (" + this
                        + ") is in an unexpected state (" + outTuple.state + ").");
            }
        } else {
            killTuple(oldGroup);
            createTuple(tuple, newUserSuppliedGroupKey);
        }
    }

    private void killTuple(Group<OutTuple_, ResultContainer_> group) {
        var newParentCount = --group.parentCount;
        var killGroup = (newParentCount == 0);
        if (killGroup) {
            var groupKey = hasMultipleGroups ? group.getGroupKey() : null;
            var oldGroup = removeGroup(groupKey);
            if (oldGroup == null) {
                throw new IllegalStateException("Impossible state: the group for the groupKey ("
                        + groupKey + ") doesn't exist in the groupMap.\n" +
                        "Maybe groupKey hashcode changed while it shouldn't have?");
            }
        }
        var outTuple = group.getTuple();
        switch (outTuple.state) {
            case CREATING -> {
                if (killGroup) {
                    propagationQueue.retract(group, TupleState.ABORTING);
                }
            }
            case UPDATING -> {
                if (killGroup) {
                    propagationQueue.retract(group, TupleState.DYING);
                }
            }
            case OK -> {
                if (killGroup) {
                    propagationQueue.retract(group, TupleState.DYING);
                } else {
                    propagationQueue.update(group);
                }
            }
            default -> throw new IllegalStateException("Impossible state: The group (" + group + ") in node (" + this
                    + ") is in an unexpected state (" + outTuple.state + ").");
        }
    }

    private Group<OutTuple_, ResultContainer_> removeGroup(Object groupKey) {
        if (hasMultipleGroups) {
            return groupMap.remove(groupKey);
        } else {
            var oldGroup = singletonGroup;
            singletonGroup = null;
            return oldGroup;
        }
    }

    @Override
    public final void retract(InTuple_ tuple) {
        Group<OutTuple_, ResultContainer_> group = tuple.removeStore(groupStoreIndex);
        if (group == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        if (hasCollector) {
            Runnable undoAccumulator = tuple.removeStore(undoStoreIndex);
            undoAccumulator.run();
        }
        killTuple(group);
    }

    protected abstract Runnable accumulate(ResultContainer_ resultContainer, InTuple_ tuple);

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    /**
     *
     * @param groupKey null if the node only has one group
     * @return never null
     */
    protected abstract OutTuple_ createOutTuple(GroupKey_ groupKey);

    private void updateOutTupleToFinisher(OutTuple_ outTuple, ResultContainer_ resultContainer) {
        updateOutTupleToResult(outTuple, finisher.apply(resultContainer));
    }

    protected abstract void updateOutTupleToResult(OutTuple_ outTuple, Result_ result);

    /**
     * Group key hashcode must never change once introduced to the group map.
     * If it does, unpredictable behavior will occur.
     * Since this situation is far too frequent and users run into this,
     * we have this helper class that will optionally throw an exception when it detects this.
     */
    private final class AssertingGroupKey {

        private final GroupKey_ key;
        private final int initialHashCode;

        public AssertingGroupKey(GroupKey_ key) {
            this.key = key;
            this.initialHashCode = key == null ? 0 : key.hashCode();
        }

        public GroupKey_ getKey() {
            if (key != null && key.hashCode() != initialHashCode) {
                throw new IllegalStateException("hashCode of object (" + key + ") of class (" + key.getClass()
                        + ") has changed while it was being used as a group key within groupBy ("
                        + AbstractGroupNode.this.getClass() + ").\n"
                        + "Group key hashCode must consistently return the same integer, "
                        + "as required by the general hashCode contract.");
            }
            return key;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || getClass() != other.getClass())
                return false;
            return Objects.equals(getKey(), ((AssertingGroupKey) other).getKey());
        }

        @Override
        public int hashCode() {
            var key = getKey();
            return key == null ? 0 : key.hashCode();
        }
    }

}
