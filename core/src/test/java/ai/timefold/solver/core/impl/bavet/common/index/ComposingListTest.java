package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ComposingListTest {

    @Test
    void emptySublists() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Collections.emptyList());
        composingList.addSubList(Collections.emptyList());
        composingList.addSubList(Collections.emptyList());

        assertThat(composingList).isEmpty();
    }

    @Test
    void singleSublist() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b", "c"));

        assertThat(composingList)
                .hasSize(3)
                .containsExactly("a", "b", "c");
    }

    @Test
    void multipleSublists() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b"));
        composingList.addSubList(Arrays.asList("c", "d", "e"));
        composingList.addSubList(List.of("f"));

        assertThat(composingList)
                .hasSize(6)
                .containsExactly("a", "b", "c", "d", "e", "f");
    }

    @Test
    void mixedEmptyAndNonEmptySublists() {
        var composingList = new ComposingList<Integer>();
        composingList.addSubList(Arrays.asList(1, 2));
        composingList.addSubList(Collections.emptyList());
        composingList.addSubList(Arrays.asList(3, 4, 5));
        composingList.addSubList(Collections.emptyList());
        composingList.addSubList(List.of(6));

        assertThat(composingList)
                .hasSize(6)
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void onDemandSublist() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(() -> Arrays.asList("a", "b", "c"), 3);

        assertThat(composingList)
                .hasSize(3)
                .containsExactly("a", "b", "c");
    }

    @Test
    void multipleOnDemandSublists() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(() -> Arrays.asList("a", "b"), 2);
        composingList.addSubList(() -> Arrays.asList("c", "d", "e"), 3);
        composingList.addSubList(() -> List.of("f"), 1);

        assertThat(composingList)
                .hasSize(6)
                .containsExactly("a", "b", "c", "d", "e", "f");
    }

    @Test
    void mixedEagerAndOnDemandSublists() {
        var composingList = new ComposingList<Integer>();
        composingList.addSubList(Arrays.asList(1, 2));
        composingList.addSubList(() -> Arrays.asList(3, 4, 5), 3);
        composingList.addSubList(List.of(6));

        assertThat(composingList)
                .hasSize(6)
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void emptyOnDemandSublist() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Collections::emptyList, 0);

        assertThat(composingList).isEmpty();
    }

    @Test
    void onDemandSublistLazyInitialization() {
        var composingList = new ComposingList<String>();
        var supplierCalled = new AtomicBoolean(false);
        composingList.addSubList(() -> {
            supplierCalled.set(true);
            return Arrays.asList("a", "b");
        }, 2);

        assertThat(supplierCalled).isFalse();
        assertThat(composingList).hasSize(2);
        assertThat(supplierCalled).isFalse();

        assertThat(composingList.get(0)).isEqualTo("a");
        assertThat(supplierCalled).isTrue();
    }

    @Test
    void onDemandSublistCachesResult() {
        var composingList = new ComposingList<String>();
        var callCount = new MutableInt(0);
        composingList.addSubList(() -> {
            callCount.increment();
            return Arrays.asList("a", "b");
        }, 2);

        composingList.get(0);
        composingList.get(1);
        composingList.get(0);

        assertThat(callCount.intValue()).isEqualTo(1);
    }

    @Test
    void getFromFirstSublist() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b"));
        composingList.addSubList(Arrays.asList("c", "d"));

        assertThat(composingList.get(0)).isEqualTo("a");
        assertThat(composingList.get(1)).isEqualTo("b");
    }

    @Test
    void getFromMiddleSublist() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b"));
        composingList.addSubList(Arrays.asList("c", "d"));
        composingList.addSubList(Arrays.asList("e", "f"));

        assertThat(composingList.get(2)).isEqualTo("c");
        assertThat(composingList.get(3)).isEqualTo("d");
    }

    @Test
    void getFromLastSublist() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b"));
        composingList.addSubList(Arrays.asList("c", "d"));

        assertThat(composingList.get(2)).isEqualTo("c");
        assertThat(composingList.get(3)).isEqualTo("d");
    }

    @Test
    void indexOutOfBoundsNegative() {
        var composingList = new ComposingList<String>();

        assertThat(composingList).isEmpty();
        assertThatThrownBy(() -> composingList.get(-1))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("Index: -1")
                .hasMessageContaining("Size: 0");
    }

    @Test
    void indexOutOfBoundsAtSize() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b", "c"));

        assertThatThrownBy(() -> composingList.get(3))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("Index: 3")
                .hasMessageContaining("Size: 3");
    }

    @Test
    void indexOutOfBoundsLargerThanSize() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b", "c"));

        assertThatThrownBy(() -> composingList.get(10))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("Index: 10")
                .hasMessageContaining("Size: 3");
    }

    @Test
    void iteration() {
        var composingList = new ComposingList<String>();
        composingList.addSubList(Arrays.asList("a", "b"));
        composingList.addSubList(Arrays.asList("c", "d", "e"));

        assertThat(composingList).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void stream() {
        var composingList = new ComposingList<Integer>();
        composingList.addSubList(Arrays.asList(1, 2));
        composingList.addSubList(Arrays.asList(3, 4, 5));

        var sum = composingList.stream().mapToInt(Integer::intValue).sum();

        assertThat(sum).isEqualTo(15);
    }

}