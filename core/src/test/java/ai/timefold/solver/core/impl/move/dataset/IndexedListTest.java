package ai.timefold.solver.core.impl.move.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IndexedListTest {

    @Test
    void addElementIncreasesSize() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        assertThat(list).hasSize(1);
    }

    @Test
    void addElementAtIndex() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        list.add(1, "B");
        list.add(1, "C");
        assertThat(list)
                .containsExactly("A", "C", "B");
    }

    @Test
    void addDuplicateElementThrowsException() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        assertThatThrownBy(() -> list.add(1, "A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The element (A) was already added to the list.");
    }

    @Test
    void setElementUpdatesValue() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        list.set(0, "B");
        assertThat(list)
                .first()
                .isEqualTo("B");
    }

    @Test
    void setElementAtInvalidIndexThrowsException() {
        var list = new IndexedList<String>();
        assertThatThrownBy(() -> list.set(0, "A"))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("The index (0) must be between 0 and -1.");
    }

    @Test
    void removeElementDecreasesSize() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        list.add(1, "B");
        assertThat(list.remove(0))
                .isEqualTo("A");
        assertThat(list)
                .hasSize(1);
    }

    @Test
    void removeElementAtIndex() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        list.add(1, "B");
        list.add(2, "C");
        assertThat(list.remove(1))
                .isEqualTo("B");
        assertThat(list.get(0))
                .isEqualTo("A");
        assertThat(list.get(1))
                .isEqualTo("C");
    }

    @Test
    void indexOfElement() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        list.add(1, "B");
        assertThat(list.indexOf("B"))
                .isEqualTo(1);
    }

    @Test
    void indexOfNonExistentElement() {
        var list = new IndexedList<String>();
        assertThat(list.indexOf("A"))
                .isEqualTo(-1);
    }

    @Test
    void getElementAtIndex() {
        var list = new IndexedList<String>();
        list.add(0, "A");
        assertThat(list)
                .first()
                .isEqualTo("A");
    }

    @Test
    void getElementAtInvalidIndexThrowsException() {
        var list = new IndexedList<String>();
        assertThatThrownBy(() -> list.get(0))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}