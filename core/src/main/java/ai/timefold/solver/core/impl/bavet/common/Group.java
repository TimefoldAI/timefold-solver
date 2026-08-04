package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleActivitySource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleList;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

final class Group<InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
        extends AbstractPropagationMetadataCarrier<OutTuple_> implements TupleActivitySource {

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> createWithoutAccumulate(Object groupKey, OutTuple_ outTuple,
                    TupleList<InTuple_> contributors) {
        return new Group<>(groupKey, null, outTuple, contributors);
    }

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> createWithoutGroupKey(ResultContainer_ resultContainer,
                    OutTuple_ outTuple, TupleList<InTuple_> contributors) {
        return new Group<>(null, resultContainer, outTuple, contributors);
    }

    public static <InTuple_ extends Tuple, OutTuple_ extends Tuple, ResultContainer_>
            Group<InTuple_, OutTuple_, ResultContainer_> create(Object groupKey, ResultContainer_ resultContainer,
                    OutTuple_ outTuple, TupleList<InTuple_> contributors) {
        return new Group<>(groupKey, resultContainer, outTuple, contributors);
    }

    private final Object groupKey;
    private final ResultContainer_ resultContainer;
    private final OutTuple_ outTuple;
    /**
     * The input tuples currently mapped into this group. Backs both membership tracking (what used to be
     * a bare {@code parentCount}) and this group's own {@link TupleActivitySource} answer, at zero
     * per-element allocation cost regardless of how many contributors the group has.
     * <p>
     * This cost (2 reserved store slots per input tuple, plus list bookkeeping on every insert/retract) is
     * paid unconditionally today, even when nothing downstream ever reads it — {@link #isActiveTransitively()}
     * is only ever consulted by a join or {@code ifExists} node, so a groupBy feeding straight into a
     * scorer (e.g. {@code groupBy(...).penalize(...)} with no further join) never needs it at all.
     * <p>
     * A build-time "is a join/ifExists reachable anywhere downstream of this group" check — the full
     * stream graph is known before any node is built, and {@code ActivitySupport}/{@code canProduceTuples()}
     * already do a comparable whole-network reachability fold — could gate this {@link TupleList} behind
     * that check, falling back to a bare count when nothing downstream needs it. Considered and judged not
     * worth it for now: {@code getChildStreamList()} alone doesn't capture the full downstream graph (join/
     * {@code ifExists}/concat streams are consumed via {@code leftParent}/{@code rightParent}, not forward
     * child-list edges, so a naive walk dead-ends at fore-bridges — reconstructing the true forward graph
     * needs an O(N) fixup pass first), and {@code AbstractConcatNode} already doesn't propagate
     * {@code activityParent} at all (a separate, pre-existing gap that such a traversal would need to treat
     * conservatively). A static analysis that's ever wrong in the unsafe direction would silently
     * reintroduce the exact staleness crash this mechanism exists to fix — revisit only if profiling shows
     * this specific cost matters in practice.
     */
    private final TupleList<InTuple_> contributors;

    private Group(Object groupKey, ResultContainer_ resultContainer, OutTuple_ outTuple, TupleList<InTuple_> contributors) {
        this.groupKey = groupKey;
        this.resultContainer = resultContainer;
        this.outTuple = outTuple;
        this.contributors = contributors;
    }

    public Object getGroupKey() {
        return groupKey;
    }

    public ResultContainer_ getResultContainer() {
        return resultContainer;
    }

    @Override
    public OutTuple_ getTuple() {
        return outTuple;
    }

    @Override
    public TupleState getState() {
        return outTuple.getState();
    }

    @Override
    public void setState(TupleState state) {
        outTuple.setState(state);
    }

    public void addContributor(InTuple_ tuple) {
        contributors.add(tuple);
    }

    public void removeContributor(InTuple_ tuple) {
        contributors.remove(tuple);
    }

    public boolean isEmpty() {
        return contributors.size() == 0;
    }

    /**
     * A group survives as long as at least one of its current contributors does; unlike a join's
     * out-tuple (which needs all of a fixed 1-2 parents to survive), this is an OR across however many
     * contributors currently exist. Short-circuits on the first live one, which is the common case.
     */
    @Override
    public boolean isActiveTransitively() {
        for (var tuple = contributors.first(); tuple != null; tuple = contributors.next(tuple)) {
            if (tuple.isActiveTransitively()) {
                return true;
            }
        }
        return false;
    }

}
