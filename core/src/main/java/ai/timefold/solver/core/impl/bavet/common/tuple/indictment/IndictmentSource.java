package ai.timefold.solver.core.impl.bavet.common.tuple.indictment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;

public sealed interface IndictmentSource {
    IndictmentSource DISABLED = new DisabledIndictmentSource();

    void visitSources(Consumer<Object> sourceConsumer);

    static IndictmentSource of(Object source) {
        return new RootIndictmentSource(source);
    }

    static IndictmentSource joining(Tuple left, Tuple right) {
        if (left.getIndictmentSource() == DISABLED) {
            return DISABLED;
        }
        return new JoinedIndictmentSource(left.getIndictmentSource(), right.getIndictmentSource());
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
            return new AggregateIndictmentSource(collection);
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
            return new AggregateIndictmentSource(collection);
        }
    }

    static IndictmentSource sourceWithSupport(Tuple carry, Tuple support) {
        if (carry.getIndictmentSource() == DISABLED) {
            return DISABLED;
        }
        if (carry.getIndictmentSource() instanceof IndictmentSourceWithSupport indictmentSourceWithSupport) {
            indictmentSourceWithSupport.support.add(support.getIndictmentSource());
            return indictmentSourceWithSupport;
        } else {
            throw new IllegalStateException("Carry tuple (%s) does not have a %s %s; its source is (%s) instead."
                    .formatted(carry, IndictmentSourceWithSupport.class.getSimpleName(), IndictmentSource.class.getSimpleName(),
                            support.getIndictmentSource()));
        }
    }

    record DisabledIndictmentSource() implements IndictmentSource {
        @Override
        public void visitSources(Consumer<Object> sourceConsumer) {
            throw new UnsupportedOperationException("Impossible state: indictments are disabled.");
        }
    }

    record RootIndictmentSource(Object source) implements IndictmentSource {
        @Override
        public void visitSources(Consumer<Object> sourceConsumer) {
            sourceConsumer.accept(source);
        }
    }

    record JoinedIndictmentSource(IndictmentSource left, IndictmentSource right) implements IndictmentSource {
        @Override
        public void visitSources(Consumer<Object> sourceConsumer) {
            left.visitSources(sourceConsumer);
            right.visitSources(sourceConsumer);
        }
    }

    record AggregateIndictmentSource(List<IndictmentSource> sourceList) implements IndictmentSource {
        @Override
        public void visitSources(Consumer<Object> sourceConsumer) {
            for (var source : sourceList) {
                source.visitSources(sourceConsumer);
            }
        }
    }

    record IndictmentSourceWithSupport(IndictmentSource source, List<IndictmentSource> support) implements IndictmentSource {
        @Override
        public void visitSources(Consumer<Object> sourceConsumer) {
            source.visitSources(sourceConsumer);
            for (var support : support) {
                support.visitSources(sourceConsumer);
            }
        }
    }
}
