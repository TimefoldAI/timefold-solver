package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractConnectedRangesSlot;

import org.jspecify.annotations.NonNull;

final class ConnectedRangesTriConstraintCollector<A, B, C, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        extends
        AbstractReferenceBasedTriCollector<A, B, C, Interval_, ConnectedRangeChain<Interval_, Point_, Difference_>, AbstractConnectedRangesSlot.State<Interval_, Point_, Difference_>> {

    private final Function<? super Interval_, ? extends Point_> startMap;
    private final Function<? super Interval_, ? extends Point_> endMap;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;

    public ConnectedRangesTriConstraintCollector(TriFunction<? super A, ? super B, ? super C, ? extends Interval_> mapper,
            Function<? super Interval_, ? extends Point_> startMap, Function<? super Interval_, ? extends Point_> endMap,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        super(mapper);
        this.startMap = startMap;
        this.endMap = endMap;
        this.differenceFunction = differenceFunction;
    }

    @Override
    public @NonNull Supplier<AbstractConnectedRangesSlot.State<Interval_, Point_, Difference_>> supplier() {
        return () -> new AbstractConnectedRangesSlot.State<>(startMap, endMap, differenceFunction);
    }

    @Override
    public @NonNull
            Function<AbstractConnectedRangesSlot.State<Interval_, Point_, Difference_>, ConnectedRangeChain<Interval_, Point_, Difference_>>
            finisher() {
        return AbstractConnectedRangesSlot.State::result;
    }

    @Override
    protected TriConstraintCollectorValueHandle<A, B, C> newAccumulatedValue(
            AbstractConnectedRangesSlot.State<Interval_, Point_, Difference_> state) {
        return new Slot(state);
    }

    private final class Slot extends AbstractConnectedRangesSlot<Interval_, Point_, Difference_>
            implements TriConstraintCollectorValueHandle<A, B, C> {
        Slot(AbstractConnectedRangesSlot.State<Interval_, Point_, Difference_> state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c) {
            addMapped(mapper.apply(a, b, c));
        }

        @Override
        public void replaceWith(A a, B b, C c) {
            replaceWithMapped(mapper.apply(a, b, c));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectedRangesTriConstraintCollector<?, ?, ?, ?, ?, ?> that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(startMap, that.startMap) && Objects.equals(endMap,
                that.endMap) && Objects.equals(differenceFunction, that.differenceFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), startMap, endMap, differenceFunction);
    }
}
