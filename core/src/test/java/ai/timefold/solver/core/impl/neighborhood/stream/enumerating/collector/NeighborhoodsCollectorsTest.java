package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector;

import static ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors.compose;
import static ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.BiNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.BiNeighborhoodsCollectorValueHandle;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class NeighborhoodsCollectorsTest {

    // ************************************************************************
    // Helpers
    // ************************************************************************

    @SuppressWarnings("unchecked")
    private static <S_, A, C_, R_> UniNeighborhoodsCollectorValueHandle<A> accumulate(
            UniNeighborhoodsCollector<S_, A, C_, R_> collector, Object container, A value) {
        var slot = collector.accumulator()
                .intoGroup(null, (C_) container);
        slot.add(value);
        return slot;
    }

    @SuppressWarnings("unchecked")
    private static <S_, A, C_, R_> void assertResult(
            UniNeighborhoodsCollector<S_, A, C_, R_> collector, Object container, R_ expected) {
        assertThat(collector.finisher().apply((C_) container))
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    private static <S_, A, B, C_, R_> BiNeighborhoodsCollectorValueHandle<A, B> accumulate(
            BiNeighborhoodsCollector<S_, A, B, C_, R_> collector, Object container, A a, B b) {
        var slot = collector.accumulator()
                .intoGroup(null, (C_) container);
        slot.add(a, b);
        return slot;
    }

    @SuppressWarnings("unchecked")
    private static <S_, A, B, C_, R_> void assertResult(
            BiNeighborhoodsCollector<S_, A, B, C_, R_> collector, Object container, R_ expected) {
        assertThat(collector.finisher().apply((C_) container))
                .as("Collector (" + collector + ") did not produce expected result.")
                .isEqualTo(expected);
    }

    // ************************************************************************
    // toList
    // ************************************************************************

    @Test
    void uniToList() {
        UniNeighborhoodsCollector<Object, Integer, ?, List<Integer>> collector = toList();
        Object container = collector.supplier().get();

        assertResult(collector, container, List.of());
        var h1 = accumulate(collector, container, 1);
        assertResult(collector, container, List.of(1));
        var h2 = accumulate(collector, container, 2);
        assertResult(collector, container, List.of(1, 2));
        h1.remove();
        assertResult(collector, container, List.of(2));
        h2.remove();
        assertResult(collector, container, List.of());
    }

    @Test
    void biToList() {
        BiNeighborhoodsCollector<Object, Integer, String, ?, List<Integer>> collector =
                toList((view, a, b) -> a);
        Object container = collector.supplier().get();

        assertResult(collector, container, List.of());
        var h1 = accumulate(collector, container, 1, "x");
        assertResult(collector, container, List.of(1));
        var h2 = accumulate(collector, container, 2, "y");
        assertResult(collector, container, List.of(1, 2));
        h1.remove();
        assertResult(collector, container, List.of(2));
        h2.remove();
        assertResult(collector, container, List.of());
    }

    // ************************************************************************
    // compose (Uni)
    // ************************************************************************

    @Test
    void uniCompose2() {
        UniNeighborhoodsCollector<Object, Integer, ?, Pair<List<Integer>, List<Integer>>> collector =
                compose(toList(), toList(),
                        (BiFunction<List<Integer>, List<Integer>, Pair<List<Integer>, List<Integer>>>) Pair::new);
        Object container = collector.supplier().get();

        assertResult(collector, container, new Pair<>(List.of(), List.of()));
        var h1 = accumulate(collector, container, 1);
        assertResult(collector, container, new Pair<>(List.of(1), List.of(1)));
        var h2 = accumulate(collector, container, 2);
        assertResult(collector, container, new Pair<>(List.of(1, 2), List.of(1, 2)));
        h1.remove();
        assertResult(collector, container, new Pair<>(List.of(2), List.of(2)));
        h2.remove();
        assertResult(collector, container, new Pair<>(List.of(), List.of()));
    }

    // ************************************************************************
    // compose (Bi)
    // ************************************************************************

    @Test
    void biCompose2() {
        BiNeighborhoodsCollector<Object, Integer, String, ?, Pair<List<Integer>, List<Integer>>> collector =
                compose(toList((view, a, b) -> a), toList((view, a, b) -> a),
                        (BiFunction<List<Integer>, List<Integer>, Pair<List<Integer>, List<Integer>>>) Pair::new);
        Object container = collector.supplier().get();

        assertResult(collector, container, new Pair<>(List.of(), List.of()));
        var h1 = accumulate(collector, container, 1, "x");
        assertResult(collector, container, new Pair<>(List.of(1), List.of(1)));
        var h2 = accumulate(collector, container, 2, "y");
        assertResult(collector, container, new Pair<>(List.of(1, 2), List.of(1, 2)));
        h1.remove();
        assertResult(collector, container, new Pair<>(List.of(2), List.of(2)));
        h2.remove();
        assertResult(collector, container, new Pair<>(List.of(), List.of()));
    }

}
