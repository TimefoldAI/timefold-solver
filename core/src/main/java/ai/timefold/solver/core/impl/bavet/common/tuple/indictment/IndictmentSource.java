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

public sealed interface IndictmentSource {
    IndictmentSource DISABLED = new DisabledIndictmentSource();

    void visitSources(Set<IndictmentSource> visited, long[] involvedNodeIds, Consumer<Object> sourceConsumer);

    Map<Long, Set<IndictmentSource>> support();

    default void visitSources(long[] involvedNodeIds, Consumer<Object> sourceConsumer) {
        visitSources(new HashSet<>(), involvedNodeIds, sourceConsumer);
    }

    default Set<IndictmentSource> getSupportForNodeId(long nodeId) {
        return support().computeIfAbsent(nodeId, ignored -> new LinkedHashSet<>());
    }

    static boolean checkIfAlreadyVisitedAndVisitSupport(IndictmentSource self, Set<IndictmentSource> visited,
            long[] involvedNodeIds, Consumer<Object> sourceConsumer) {
        if (!visited.add(self)) {
            return true;
        }
        for (var nodeId : involvedNodeIds) {
            for (var indictmentSource : self.support().getOrDefault(nodeId, Collections.emptySet())) {
                indictmentSource.visitSources(visited, involvedNodeIds, sourceConsumer);
            }
        }
        return false;
    }

    static IndictmentSource of(Object source) {
        return new RootIndictmentSource(source, new LinkedHashMap<>());
    }

    static IndictmentSource joining(Tuple left, Tuple right) {
        if (left.getIndictmentSource() == DISABLED) {
            return DISABLED;
        }
        return new JoinedIndictmentSource(left.getIndictmentSource(), right.getIndictmentSource(), new LinkedHashMap<>());
    }

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

    static void addSupport(long nodeId, Tuple carry, Tuple support) {
        if (carry.getIndictmentSource() == DISABLED) {
            return;
        }
        carry.getIndictmentSource().getSupportForNodeId(nodeId).add(support.getIndictmentSource());
    }

    static void clearSupport(long nodeId, Tuple carry) {
        if (carry.getIndictmentSource() == DISABLED) {
            return;
        }
        carry.getIndictmentSource().getSupportForNodeId(nodeId).clear();
    }

    static void removeSupport(long nodeId, Tuple carry, Tuple support) {
        if (carry.getIndictmentSource() == DISABLED) {
            return;
        }
        carry.getIndictmentSource().getSupportForNodeId(nodeId).remove(support.getIndictmentSource());
    }

    record DisabledIndictmentSource() implements IndictmentSource {
        @Override
        public void visitSources(Set<IndictmentSource> visited, long[] involvedNodeIds, Consumer<Object> sourceConsumer) {
            throw new UnsupportedOperationException("Impossible state: indictments are disabled.");
        }

        @Override
        public Map<Long, Set<IndictmentSource>> support() {
            throw new UnsupportedOperationException("Impossible state: indictments are disabled.");
        }
    }

    record RootIndictmentSource(Object source, Map<Long, Set<IndictmentSource>> support) implements IndictmentSource {
        @Override
        public void visitSources(Set<IndictmentSource> visited, long[] involvedNodeIds, Consumer<Object> sourceConsumer) {
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
        public void visitSources(Set<IndictmentSource> visited, long[] involvedNodeIds, Consumer<Object> sourceConsumer) {
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
        public void visitSources(Set<IndictmentSource> visited, long[] involvedNodeIds, Consumer<Object> sourceConsumer) {
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
