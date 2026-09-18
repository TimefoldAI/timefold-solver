package ai.timefold.solver.core.impl.bavet.common.tuple.indictment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Used to track the original indicted object as tuples pass through
 * the various constraint streams. Form a tree, based on the number of
 * "inputs" a node has. For instance, `join` nodes have two inputs, since
 * two tuples affect the out tuple the `join` node produces. This tree can
 * then be navigated to find the indicted objects for each constraint match.
 */
@NullMarked
public sealed interface IndictmentSource {
    /**
     * The indictment source to be used when indictments are disabled.
     * Can be used to check if indictments are enabled to influence node behavior.
     */
    IndictmentSource DISABLED = new DisabledIndictmentSource();

    /**
     * Does a tree traversal of the indicted objects referenced by this source.
     * 
     * @param visited The sources already visited
     * @param involvedNodeIds The nodes involved in the triggered constraint
     * @param sourceConsumer The indicted object consumer, may receive the same indicted object multiple times
     */
    void visitSources(Set<IndictmentSource> visited, long @Nullable [] involvedNodeIds, Consumer<Object> sourceConsumer);

    /**
     * Get the support of an indictment source. The support is a map from
     * node id to supporting indictment sources. This is used when a node reuses
     * tuples and adds additional indicted objects, such as `ifExists` nodes.
     * 
     * @return a map from node id to additional indictment sources
     */
    Map<Long, Set<IndictmentSource>> support();

    /**
     * Do a tree transversal visiting all supporting nodes with an empty visited set
     * 
     * @see #visitSources(Set, long[], Consumer)
     */
    default void visitAllSources(Consumer<Object> sourceConsumer) {
        visitSources(new HashSet<>(), null, sourceConsumer);
    }

    /**
     * Do a tree transversal visiting only the supporting nodes matching involvedNodeIds with an empty visited set
     * 
     * @see #visitSources(Set, long[], Consumer)
     */
    default void visitSources(long[] involvedNodeIds, Consumer<Object> sourceConsumer) {
        visitSources(new HashSet<>(), involvedNodeIds, sourceConsumer);
    }

    /**
     * Get the support set for a given node id.
     * 
     * @param nodeId The node id to get the support set of
     * @return A set to be used as the support of a given node id
     * @see #support()
     */
    default Set<IndictmentSource> getSupportForNodeId(long nodeId) {
        return support().computeIfAbsent(nodeId, ignored -> new LinkedHashSet<>());
    }

    static boolean checkIfAlreadyVisitedAndVisitSupport(IndictmentSource self, Set<IndictmentSource> visited,
            long @Nullable [] involvedNodeIds, Consumer<Object> sourceConsumer) {
        if (!visited.add(self)) {
            return true;
        }

        if (involvedNodeIds == null) {
            for (var indictmentSourceSet : self.support().values()) {
                for (var indictmentSource : indictmentSourceSet) {
                    indictmentSource.visitSources(visited, null, sourceConsumer);
                }
            }
        } else {
            for (var nodeId : involvedNodeIds) {
                for (var indictmentSource : self.support().getOrDefault(nodeId, Collections.emptySet())) {
                    indictmentSource.visitSources(visited, involvedNodeIds, sourceConsumer);
                }
            }
        }
        return false;
    }

    /**
     * Create an indictment source with the given object as the indictee
     * 
     * @param source the indicted object
     * @return a new indictment source indicting only the given object
     */
    static IndictmentSource of(Object source) {
        return new RootIndictmentSource(source, new LinkedHashMap<>());
    }

    /**
     * Create an indictment source from the sources of the two tuples.
     * 
     * @apiNote will return {@link #DISABLED} if indictments are disabled
     */
    static IndictmentSource joining(Tuple left, Tuple right) {
        if (left.getIndictmentSource() == DISABLED) {
            return DISABLED;
        }
        return new JoinedIndictmentSource(left.getIndictmentSource(), right.getIndictmentSource(), new LinkedHashMap<>());
    }

    /**
     * Create an (or return the existing) aggregate indictment source and attach it to the given tuple
     */
    static AggregateIndictmentSource getPrecomputeAggregation(Tuple outTuple) {
        if (outTuple.getIndictmentSource() != DISABLED) {
            return (AggregateIndictmentSource) outTuple.getIndictmentSource();
        }
        var out = new AggregateIndictmentSource(new ArrayList<>(), new LinkedHashMap<>());
        outTuple.setIndictmentSource(out);
        return out;
    }

    /**
     * Adds the given element indictment source to the group indictment sources, and return
     * the group's aggregate indictment source (creating it if does not exist yet).
     * 
     * @param elementTuple the tuple being aggregated
     * @param groupTuple the aggregation tuple
     * @return the indictment source of the aggregation
     * @apiNote will return {@link #DISABLED} if indictments are disabled
     */
    static IndictmentSource aggregating(Tuple elementTuple, Tuple groupTuple) {
        if (elementTuple.getIndictmentSource() == DISABLED) {
            return DISABLED;
        }
        if (groupTuple.getIndictmentSource() instanceof AggregateIndictmentSource aggregateIndictmentSource) {
            aggregateIndictmentSource.sourceList.add(elementTuple.getIndictmentSource());
            return aggregateIndictmentSource;
        } else {
            var collection = new ArrayList<IndictmentSource>();
            collection.add(elementTuple.getIndictmentSource());
            return new AggregateIndictmentSource(collection, new LinkedHashMap<>());
        }
    }

    /**
     * Removes the given element indictment source from the group indictment sources, and return
     * the group's aggregate indictment source (creating it if does not exist yet).
     * 
     * @param elementTuple the tuple being aggregated
     * @param groupTuple the aggregation tuple
     * @return the indictment source of the aggregation
     * @apiNote will return {@link #DISABLED} if indictments are disabled
     */
    static IndictmentSource removeFromAggregate(Tuple elementTuple, Tuple groupTuple) {
        if (elementTuple.getIndictmentSource() == DISABLED) {
            return DISABLED;
        }
        if (groupTuple.getIndictmentSource() instanceof AggregateIndictmentSource aggregateIndictmentSource) {
            aggregateIndictmentSource.sourceList.remove(elementTuple.getIndictmentSource());
            return aggregateIndictmentSource;
        } else {
            var collection = new ArrayList<IndictmentSource>();
            collection.add(elementTuple.getIndictmentSource());
            return new AggregateIndictmentSource(collection, new LinkedHashMap<>());
        }
    }

    /**
     * Adds the indictment source of the supporting tuple to the carrying tuple's support
     * 
     * @param nodeId The node id that created this support
     * @param carry the tuple that is propagated
     * @param support the tuple that contributed to the carry being propagated but is not propagated itself
     */
    static void addSupport(long nodeId, Tuple carry, Tuple support) {
        if (carry.getIndictmentSource() == DISABLED) {
            return;
        }
        carry.getIndictmentSource().getSupportForNodeId(nodeId).add(support.getIndictmentSource());
    }

    /**
     * Clears the carrying tuple's support
     * 
     * @param nodeId The node id that created this support
     * @param carry the tuple that is propagated
     */
    static void clearSupport(long nodeId, Tuple carry) {
        if (carry.getIndictmentSource() == DISABLED) {
            return;
        }
        carry.getIndictmentSource().getSupportForNodeId(nodeId).clear();
    }

    /**
     * Remove the indictment source of the supporting tuple from the carrying tuple's support
     * 
     * @param nodeId The node id that created this support
     * @param carry the tuple that is propagated
     * @param support the tuple that used to contribute to the carry being propagated but is not propagated itself
     */
    static void removeSupport(long nodeId, Tuple carry, Tuple support) {
        if (carry.getIndictmentSource() == DISABLED) {
            return;
        }
        carry.getIndictmentSource().getSupportForNodeId(nodeId).remove(support.getIndictmentSource());
    }

    record DisabledIndictmentSource() implements IndictmentSource {
        @Override
        public void visitSources(Set<IndictmentSource> visited, long @Nullable [] involvedNodeIds,
                Consumer<Object> sourceConsumer) {
            throw new UnsupportedOperationException("Impossible state: indictments are disabled.");
        }

        @Override
        public Map<Long, Set<IndictmentSource>> support() {
            throw new UnsupportedOperationException("Impossible state: indictments are disabled.");
        }
    }

    record RootIndictmentSource(Object source, Map<Long, Set<IndictmentSource>> support) implements IndictmentSource {
        @Override
        public void visitSources(Set<IndictmentSource> visited, long @Nullable [] involvedNodeIds,
                Consumer<Object> sourceConsumer) {
            if (checkIfAlreadyVisitedAndVisitSupport(this, visited, involvedNodeIds, sourceConsumer)) {
                return;
            }
            sourceConsumer.accept(source);
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    record JoinedIndictmentSource(IndictmentSource left, IndictmentSource right,
            Map<Long, Set<IndictmentSource>> support) implements IndictmentSource {
        @Override
        public void visitSources(Set<IndictmentSource> visited, long @Nullable [] involvedNodeIds,
                Consumer<Object> sourceConsumer) {
            if (checkIfAlreadyVisitedAndVisitSupport(this, visited, involvedNodeIds, sourceConsumer)) {
                return;
            }
            left.visitSources(visited, involvedNodeIds, sourceConsumer);
            right.visitSources(visited, involvedNodeIds, sourceConsumer);
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    record AggregateIndictmentSource(List<IndictmentSource> sourceList,
            Map<Long, Set<IndictmentSource>> support) implements IndictmentSource {
        @Override
        public void visitSources(Set<IndictmentSource> visited, long @Nullable [] involvedNodeIds,
                Consumer<Object> sourceConsumer) {
            if (checkIfAlreadyVisitedAndVisitSupport(this, visited, involvedNodeIds, sourceConsumer)) {
                return;
            }
            for (var source : sourceList) {
                source.visitSources(visited, involvedNodeIds, sourceConsumer);
            }
        }

        @Override
        public Set<IndictmentSource> getSupportForNodeId(long nodeId) {
            return support.computeIfAbsent(nodeId, ignored -> new LinkedHashSet<>());
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
