package ai.timefold.solver.constraint.streams.bavet.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.config.solver.EnvironmentMode;

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
    private final Map<Object, AbstractGroup<OutTuple_, ResultContainer_>> groupMap;
    /**
     * Used when {@link #hasMultipleGroups} is false, otherwise {@link #groupMap} is used.
     *
     * @implNote The field is lazy initialized in order to maintain the same semantics as with the groupMap above.
     *           When all tuples are removed, the field will be set to null, as if the group never existed.
     */
    private AbstractGroup<OutTuple_, ResultContainer_> singletonGroup;
    private final GroupPropagationQueue<OutTuple_, ResultContainer_> propagationQueue;
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
        this.propagationQueue = new GroupPropagationQueue<>(nextNodesTupleLifecycle,
                hasCollector ? group -> {
                    OutTuple_ outTuple = group.outTuple;
                    TupleState state = outTuple.state;
                    if (state == TupleState.CREATING || state == TupleState.UPDATING) {
                        updateOutTupleToFinisher(outTuple, group.getResultContainer());
                    }
                } : null);
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
    public void insert(InTuple_ tuple) {
        if (tuple.getStore(groupStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tuple
                    + ") was already added in the tupleStore.");
        }
        GroupKey_ userSuppliedKey = hasMultipleGroups ? groupKeyFunction.apply(tuple) : null;
        createTuple(tuple, userSuppliedKey);
    }

    private void createTuple(InTuple_ tuple, GroupKey_ userSuppliedKey) {
        AbstractGroup<OutTuple_, ResultContainer_> newGroup = getOrCreateGroup(userSuppliedKey);
        OutTuple_ outTuple = accumulate(tuple, newGroup);
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

    private OutTuple_ accumulate(InTuple_ tuple, AbstractGroup<OutTuple_, ResultContainer_> group) {
        if (hasCollector) {
            Runnable undoAccumulator = accumulate(group.getResultContainer(), tuple);
            tuple.setStore(undoStoreIndex, undoAccumulator);
        }
        tuple.setStore(groupStoreIndex, group);
        return group.outTuple;
    }

    private AbstractGroup<OutTuple_, ResultContainer_> getOrCreateGroup(GroupKey_ userSuppliedKey) {
        Object groupMapKey = useAssertingGroupKey ? new AssertingGroupKey(userSuppliedKey) : userSuppliedKey;
        if (hasMultipleGroups) {
            // Avoids computeIfAbsent in order to not create lambdas on the hot path.
            AbstractGroup<OutTuple_, ResultContainer_> group = groupMap.get(groupMapKey);
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

    private AbstractGroup<OutTuple_, ResultContainer_> createGroupWithGroupKey(Object groupMapKey) {
        GroupKey_ userSuppliedKey = extractUserSuppliedKey(groupMapKey);
        OutTuple_ outTuple = createOutTuple(userSuppliedKey);
        AbstractGroup<OutTuple_, ResultContainer_> group =
                hasCollector ? new GroupWithAccumulateAndGroupKey<>(groupMapKey, supplier.get(), outTuple)
                        : new GroupWithoutAccumulate<>(groupMapKey, outTuple);
        propagationQueue.insert(group);
        return group;
    }

    private AbstractGroup<OutTuple_, ResultContainer_> createGroupWithoutGroupKey() {
        OutTuple_ outTuple = createOutTuple(null);
        if (!hasCollector) {
            throw new IllegalStateException("Impossible state: The node (" + this + ") has no collector, "
                    + "but it is still trying to create a group without a group key.");
        }
        AbstractGroup<OutTuple_, ResultContainer_> group = new GroupWithAccumulateWithoutGroupKey<>(supplier.get(), outTuple);
        propagationQueue.insert(group);
        return group;
    }

    private GroupKey_ extractUserSuppliedKey(Object groupMapKey) {
        return useAssertingGroupKey ? ((AssertingGroupKey) groupMapKey).getKey() : (GroupKey_) groupMapKey;
    }

    @Override
    public void update(InTuple_ tuple) {
        AbstractGroup<OutTuple_, ResultContainer_> oldGroup = tuple.getStore(groupStoreIndex);
        if (oldGroup == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insert(tuple);
            return;
        }
        if (hasCollector) {
            Runnable undoAccumulator = tuple.getStore(undoStoreIndex);
            undoAccumulator.run();
        }

        GroupKey_ oldUserSuppliedGroupKey = hasMultipleGroups ? extractUserSuppliedKey(oldGroup.getGroupKey()) : null;
        GroupKey_ newUserSuppliedGroupKey = hasMultipleGroups ? groupKeyFunction.apply(tuple) : null;
        if (Objects.equals(newUserSuppliedGroupKey, oldUserSuppliedGroupKey)) {
            // No need to change parentCount because it is the same group
            OutTuple_ outTuple = accumulate(tuple, oldGroup);
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

    private void killTuple(AbstractGroup<OutTuple_, ResultContainer_> group) {
        int newParentCount = --group.parentCount;
        boolean killGroup = (newParentCount == 0);
        if (killGroup) {
            Object groupKey = hasMultipleGroups ? group.getGroupKey() : null;
            AbstractGroup<OutTuple_, ResultContainer_> old = removeGroup(groupKey);
            if (old == null) {
                throw new IllegalStateException("Impossible state: the group for the groupKey ("
                        + groupKey + ") doesn't exist in the groupMap.\n" +
                        "Maybe groupKey hashcode changed while it shouldn't have?");
            }
        }
        OutTuple_ outTuple = group.outTuple;
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

    private AbstractGroup<OutTuple_, ResultContainer_> removeGroup(Object groupKey) {
        if (hasMultipleGroups) {
            return groupMap.remove(groupKey);
        } else {
            AbstractGroup<OutTuple_, ResultContainer_> old = singletonGroup;
            singletonGroup = null;
            return old;
        }
    }

    @Override
    public void retract(InTuple_ tuple) {
        AbstractGroup<OutTuple_, ResultContainer_> group = tuple.removeStore(groupStoreIndex);
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
    protected final GroupPropagationQueue<OutTuple_, ResultContainer_> getPropagationQueue() {
        return propagationQueue;
    }

    /**
     *
     * @param groupKey null if the node only has one group
     * @return never null
     */
    protected abstract OutTuple_ createOutTuple(GroupKey_ groupKey);

    private void updateOutTupleToFinisher(OutTuple_ outTuple, ResultContainer_ resultContainer) {
        Result_ result = finisher.apply(resultContainer);
        updateOutTupleToResult(outTuple, result);
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
            GroupKey_ key = getKey();
            return key == null ? 0 : key.hashCode();
        }
    }

}
