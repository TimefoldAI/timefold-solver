package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;

import org.junit.jupiter.api.Test;

public class MultipleDelegateListTest {
    @Test
    void testGetIndexOfValue() {
        List<Object> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<Object> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<Object> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<Object> combined =
                new MultipleDelegateList<>(new String[] { "e1", "e2", "e3" }, delegate1, delegate2, delegate3);

        @SuppressWarnings("unchecked")
        ListVariableDescriptor<Object> listVariableDescriptor = mock(ListVariableDescriptor.class);
        @SuppressWarnings("unchecked")
        EntityDescriptor<Object> entityDescriptor = mock(EntityDescriptor.class);

        when(listVariableDescriptor.getEntityDescriptor()).thenReturn(entityDescriptor);

        ListVariableStateSupply<Object> listVariableStateSupply = mock(ListVariableStateSupply.class);
        doAnswer(invocation -> {
            String value = invocation.getArgument(0);
            return switch (value) {
                case "a" -> new LocationInList("e1", 0);
                case "b" -> new LocationInList("e1", 1);
                case "c" -> new LocationInList("e1", 2);
                case "d" -> new LocationInList("e2", 0);
                case "e" -> new LocationInList("e2", 1);
                case "f" -> new LocationInList("e3", 0);
                default -> ElementLocation.unassigned();
            };
        }).when(listVariableStateSupply).getLocationInList(anyString());
        when(listVariableStateSupply.getSourceVariableDescriptor()).thenReturn(listVariableDescriptor);

        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f");
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertThat(combined.getIndexOfValue(listVariableStateSupply, expectedOrder.get(i)))
                    .isEqualTo(i);
        }

        assertThatCode(() -> combined.getIndexOfValue(listVariableStateSupply, "g"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value (g) is not contained in any entity list");
    }

    @Test
    void testMoveElementsOfDelegates() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        combined.moveElementsOfDelegates(new int[] { 1, 3, 5 });

        assertThat(delegate1).containsExactly("a", "b");
        assertThat(delegate2).containsExactly("c", "d");
        assertThat(delegate3).containsExactly("e", "f");

        combined.moveElementsOfDelegates(new int[] { 2, 4, 5 });

        assertThat(delegate1).containsExactly("a", "b", "c");
        assertThat(delegate2).containsExactly("d", "e");
        assertThat(delegate3).containsExactly("f");
    }

    @Test
    void testComplexMoveElementsOfDelegates() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e", "f", "g", "h"));
        List<String> delegate3 = new ArrayList<>(List.of("i", "j", "k", "l", "m"));
        List<String> delegate4 = new ArrayList<>(List.of("n", "o", "p", "q", "r", "s", "t", "u", "v"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3, delegate4);

        combined.moveElementsOfDelegates(new int[] { 3, 13, 18, 21 });

        assertThat(delegate1).containsExactly("a", "b", "c", "d");
        assertThat(delegate2).containsExactly("e", "f", "g", "h", "i", "j", "k", "l", "m", "n");
        assertThat(delegate3).containsExactly("o", "p", "q", "r", "s");
        assertThat(delegate4).containsExactly("t", "u", "v");
        assertThat(combined.offsets).containsExactly(0, 4, 14, 19);
        assertThat(combined.delegateSizes).containsExactly(4, 10, 5, 3);

        combined.moveElementsOfDelegates(new int[] { 2, 7, 12, 21 });

        assertThat(delegate1).containsExactly("a", "b", "c");
        assertThat(delegate2).containsExactly("d", "e", "f", "g", "h");
        assertThat(delegate3).containsExactly("i", "j", "k", "l", "m");
        assertThat(delegate4).containsExactly("n", "o", "p", "q", "r", "s", "t", "u", "v");
        assertThat(combined.offsets).containsExactly(0, 3, 8, 13);
        assertThat(combined.delegateSizes).containsExactly(3, 5, 5, 9);
    }

    @Test
    void testSize() {
        assertThat(new MultipleDelegateList<>().size()).isEqualTo(0);
        assertThat(new MultipleDelegateList<>(List.of()).size()).isEqualTo(0);
        assertThat(new MultipleDelegateList<>(List.of("a", "b", "c")).size()).isEqualTo(3);
        assertThat(new MultipleDelegateList<>(List.of("a", "b"), List.of("c"), List.of("d")).size()).isEqualTo(4);
    }

    @Test
    void testIsEmpty() {
        assertThat(new MultipleDelegateList<>().isEmpty()).isTrue();
        assertThat(new MultipleDelegateList<>(List.of()).isEmpty()).isTrue();
        assertThat(new MultipleDelegateList<>(List.of("a", "b", "c")).isEmpty()).isFalse();
        assertThat(new MultipleDelegateList<>(List.of("a", "b"), List.of("c"), List.of("d")).isEmpty()).isFalse();
    }

    @Test
    void testContains() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        assertThat(combined.contains("a")).isTrue();
        assertThat(combined.contains("b")).isTrue();
        assertThat(combined.contains("c")).isTrue();
        assertThat(combined.contains("d")).isTrue();
        assertThat(combined.contains("e")).isTrue();
        assertThat(combined.contains("f")).isTrue();
        assertThat(combined.contains("g")).isFalse();
        assertThat(combined.contains(null)).isFalse();
    }

    @Test
    void testIterator() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        assertThat(combined.iterator()).toIterable().containsExactly("a", "b", "c", "d", "e", "f");
    }

    @Test
    void testToArray() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        assertThat(combined.toArray()).containsExactly("a", "b", "c", "d", "e", "f");
    }

    @Test
    void testToArrayUsingSupplied() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        String[] supplied = new String[6];
        String[] out = combined.toArray(supplied);
        assertThat(out).isSameAs(supplied);
        assertThat(supplied).containsExactly("a", "b", "c", "d", "e", "f");

        supplied = new String[7];
        supplied[6] = "g";
        out = combined.toArray(supplied);
        assertThat(out).isSameAs(supplied);
        assertThat(supplied).containsExactly("a", "b", "c", "d", "e", "f", null);

        supplied = new String[5];
        out = combined.toArray(supplied);
        assertThat(out).isNotSameAs(supplied);
        assertThat(out).containsExactly("a", "b", "c", "d", "e", "f");
    }

    private static <T> List<List<T>> powerSet(List<T> source) {
        return Stream.concat(
                IntStream.range(0, source.size())
                        .mapToObj(index -> {
                            List<T> out = new ArrayList<>(source.size() - 1);
                            out.addAll(source.subList(0, index));
                            if (index < source.size() - 1) {
                                out.addAll(source.subList(index + 1, source.size()));
                            }
                            return out;
                        })
                        .flatMap(subsetWithOneElementRemoved -> powerSet(subsetWithOneElementRemoved).stream()),
                Stream.of(source)).distinct()
                .sorted(Comparator.comparing(List::size))
                .collect(Collectors.toList());
    }

    @Test
    void testContainsAll() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        for (List<String> subset : powerSet(List.of("a", "b", "c", "d", "e", "f"))) {
            assertThat(combined.containsAll(subset)).isTrue();
            List<String> copy = new ArrayList<>(subset);
            copy.add("g");
            assertThat(combined.containsAll(copy)).isFalse();
        }
    }

    @Test
    void testGet() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f");
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertThat(combined.get(i)).isEqualTo(expectedOrder.get(i));
        }

        assertThatCode(() -> combined.get(-1)).isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("Index (-1) out of bounds for a list of size (6)");
        assertThatCode(() -> combined.get(6)).isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("Index (6) out of bounds for a list of size (6)");
    }

    @Test
    void testSet() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f");
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertThat(combined.set(i, "v" + i)).isEqualTo(expectedOrder.get(i));
            switch (expectedOrder.get(i)) {
                case "a":
                case "b":
                case "c":
                    assertThat(delegate1.get(i)).isEqualTo("v" + i);
                    assertThat(delegate2).containsExactly("d", "e");
                    assertThat(delegate3).containsExactly("f");
                    break;
                case "d":
                case "e":
                    assertThat(delegate2.get(i - 3)).isEqualTo("v" + i);
                    assertThat(delegate1).containsExactly("v0", "v1", "v2");
                    assertThat(delegate3).containsExactly("f");
                    break;
                case "f":
                    assertThat(delegate3.get(i - 5)).isEqualTo("v" + i);
                    assertThat(delegate1).containsExactly("v0", "v1", "v2");
                    assertThat(delegate2).containsExactly("v3", "v4");
                    break;
            }
        }

        assertThatCode(() -> combined.set(-1, "")).isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("Index (-1) out of bounds for a list of size (6)");
        assertThatCode(() -> combined.set(6, "")).isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("Index (6) out of bounds for a list of size (6)");
    }

    @Test
    void testIndexOf() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined =
                new MultipleDelegateList<>(delegate1, delegate2, delegate3, delegate1, delegate2, delegate3);

        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f", "a", "b", "c", "d", "e", "f");
        for (int i = 0; i < expectedOrder.size() / 2; i++) {
            assertThat(combined.indexOf(expectedOrder.get(i))).isEqualTo(i);
        }
        assertThat(combined.indexOf("g")).isEqualTo(-1);
        assertThat(combined.indexOf(null)).isEqualTo(-1);
    }

    @Test
    void testLastIndexOf() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined =
                new MultipleDelegateList<>(delegate1, delegate2, delegate3, delegate1, delegate2, delegate3);

        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f", "a", "b", "c", "d", "e", "f");
        for (int i = 0; i < expectedOrder.size() / 2; i++) {
            assertThat(combined.lastIndexOf(expectedOrder.get(i))).isEqualTo(i + 6);
        }
        assertThat(combined.indexOf("g")).isEqualTo(-1);
        assertThat(combined.indexOf(null)).isEqualTo(-1);
    }

    @Test
    void testListIterator() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f");
        ListIterator<String> listIterator = combined.listIterator();

        assertThat(listIterator.hasPrevious()).isFalse();
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertThat(listIterator.hasNext()).isTrue();
            assertThat(listIterator.nextIndex()).isEqualTo(i + 1);
            assertThat(listIterator.next()).isEqualTo(expectedOrder.get(i));
            assertThat(listIterator.hasPrevious()).isTrue();
        }

        assertThat(listIterator.hasNext()).isFalse();

        for (int i = expectedOrder.size() - 1; i >= 0; i--) {
            assertThat(listIterator.hasPrevious()).isTrue();
            assertThat(listIterator.previousIndex()).isEqualTo(i);
            assertThat(listIterator.previous()).isEqualTo(expectedOrder.get(i));
            assertThat(listIterator.hasNext()).isTrue();
        }
        assertThat(listIterator.hasPrevious()).isFalse();
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertThat(listIterator.hasNext()).isTrue();
            listIterator.set("v" + i);
            assertThat(listIterator.next()).isEqualTo("v" + i);
            assertThat(listIterator.hasPrevious()).isTrue();
        }

        assertThat(delegate1).containsExactly("v0", "v1", "v2");
        assertThat(delegate2).containsExactly("v3", "v4");
        assertThat(delegate3).containsExactly("v5");

        assertThatCode(() -> listIterator.add("")).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(listIterator::remove).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testListIteratorStartingAtIndex() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f");
        ListIterator<String> listIterator = combined.listIterator(6);

        assertThat(listIterator.hasNext()).isFalse();
        for (int i = expectedOrder.size() - 1; i >= 0; i--) {
            assertThat(listIterator.hasPrevious()).isTrue();
            assertThat(listIterator.previousIndex()).isEqualTo(i);
            assertThat(listIterator.previous()).isEqualTo(expectedOrder.get(i));
            assertThat(listIterator.hasNext()).isTrue();
        }

        assertThat(listIterator.hasPrevious()).isFalse();
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertThat(listIterator.hasNext()).isTrue();
            assertThat(listIterator.nextIndex()).isEqualTo(i + 1);
            assertThat(listIterator.next()).isEqualTo(expectedOrder.get(i));
            assertThat(listIterator.hasPrevious()).isTrue();
        }

        assertThat(listIterator.hasNext()).isFalse();
        for (int i = expectedOrder.size() - 1; i >= 0; i--) {
            assertThat(listIterator.hasPrevious()).isTrue();
            listIterator.previous();
            listIterator.set("v" + i);
            assertThat(listIterator.next()).isEqualTo("v" + i);
            listIterator.previous();
            assertThat(listIterator.hasNext()).isTrue();
        }
        assertThat(listIterator.hasPrevious()).isFalse();

        assertThat(delegate1).containsExactly("v0", "v1", "v2");
        assertThat(delegate2).containsExactly("v3", "v4");
        assertThat(delegate3).containsExactly("v5");

        assertThatCode(() -> listIterator.add("")).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(listIterator::remove).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSublist() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        List<String> expected = List.of("a", "b", "c", "d", "e", "f");
        for (int fromIndex = 0; fromIndex < expected.size(); fromIndex++) {
            for (int toIndex = fromIndex; toIndex <= expected.size(); toIndex++) {
                List<String> subList = combined.subList(fromIndex, toIndex);
                assertThat(subList).isEqualTo(expected.subList(fromIndex, toIndex));
                assertThat(subList.size()).isEqualTo(toIndex - fromIndex);
            }
        }

        List<String> firstHalf = combined.subList(0, 3);
        List<String> secondHalf = combined.subList(3, 6);

        assertThat(firstHalf.set(0, "v0")).isEqualTo("a");
        assertThat(firstHalf.set(1, "v1")).isEqualTo("b");
        assertThat(firstHalf.set(2, "v2")).isEqualTo("c");

        assertThat(delegate1).containsExactly("v0", "v1", "v2");

        assertThat(secondHalf.set(0, "v3")).isEqualTo("d");
        assertThat(secondHalf.set(1, "v4")).isEqualTo("e");
        assertThat(secondHalf.set(2, "v5")).isEqualTo("f");

        assertThat(delegate2).containsExactly("v3", "v4");
        assertThat(delegate3).containsExactly("v5");
    }

    @Test
    void testUnsupportedOperations() {
        List<String> delegate1 = new ArrayList<>(List.of("a", "b", "c"));
        List<String> delegate2 = new ArrayList<>(List.of("d", "e"));
        List<String> delegate3 = new ArrayList<>(List.of("f"));
        MultipleDelegateList<String> combined = new MultipleDelegateList<>(delegate1, delegate2, delegate3);

        assertThatCode(() -> combined.add("g")).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> combined.add(0, "g")).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> combined.addAll(List.of("g", "h"))).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> combined.addAll(0, List.of("g", "h"))).isInstanceOf(UnsupportedOperationException.class);

        assertThatCode(combined::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> combined.remove("a")).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> combined.remove(0)).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> combined.removeAll(List.of("a", "b"))).isInstanceOf(UnsupportedOperationException.class);
        assertThatCode(() -> combined.retainAll(List.of("a", "b"))).isInstanceOf(UnsupportedOperationException.class);
    }
}
