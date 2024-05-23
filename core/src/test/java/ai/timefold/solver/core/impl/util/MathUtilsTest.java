package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class MathUtilsTest {
    // https://stackoverflow.com/a/11916946

    /**
     * Iterator for the permutations of the integers 1, 2, 3, 4... up to a given number.
     */
    public static class PermutationIterator
            implements Iterator<int[]> {
        private int[] next = null;

        private final int n;
        private int[] perm;
        private int[] dirs;

        public PermutationIterator(int size) {
            n = size;
            if (n <= 0) {
                perm = (dirs = null);
            } else {
                perm = new int[n];
                dirs = new int[n];
                for (int i = 0; i < n; i++) {
                    perm[i] = i;
                    dirs[i] = -1;
                }
                dirs[0] = 0;
            }

            next = perm;
        }

        @Override
        public int[] next() {
            int[] r = makeNext();
            next = null;
            return r;
        }

        @Override
        public boolean hasNext() {
            return (makeNext() != null);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private int[] makeNext() {
            if (next != null)
                return next;
            if (perm == null)
                return null;

            // find the largest element with != 0 direction
            int i = -1, e = -1;
            for (int j = 0; j < n; j++)
                if ((dirs[j] != 0) && (perm[j] > e)) {
                    e = perm[j];
                    i = j;
                }

            if (i == -1) // no such element -> no more premutations
                return (next = (perm = (dirs = null))); // no more permutations

            // swap with the element in its direction
            int k = i + dirs[i];
            swap(i, k, dirs);
            swap(i, k, perm);
            // if it's at the start/end or the next element in the direction
            // is greater, reset its direction.
            if ((k == 0) || (k == n - 1) || (perm[k + dirs[k]] > e))
                dirs[k] = 0;

            // set directions to all greater elements
            for (int j = 0; j < n; j++)
                if (perm[j] > e)
                    dirs[j] = (j < k) ? +1 : -1;

            return (next = perm);
        }

        protected static void swap(int i, int j, int[] arr) {
            int v = arr[i];
            arr[i] = arr[j];
            arr[j] = v;
        }
    }

    private static long expectedArrangements(int listSize, int partitions) {
        Iterator<int[]> permutationIterator = new PermutationIterator(listSize);
        Set<List<List<Integer>>> seenSet = new HashSet<>();
        while (permutationIterator.hasNext()) {
            int[] permutation = permutationIterator.next();
            List<List<List<Integer>>> generated = new ArrayList<>();
            List<List<Integer>> initial = new ArrayList<>();
            for (int i = 0; i < partitions; i++) {
                initial.add(new ArrayList<>());
            }
            generated.add(initial);
            for (int i = 0; i < listSize; i++) {
                var currentGenerated = new ArrayList<>(generated);
                for (List<List<Integer>> current : currentGenerated) {
                    generated.remove(current);
                    for (int choice = 0; choice < partitions; choice++) {
                        List<List<Integer>> newList = new ArrayList<>();
                        for (var part : current) {
                            newList.add(new ArrayList<>(part));
                        }
                        newList.get(choice).add(permutation[i]);
                        generated.add(newList);
                    }
                }
            }
            seenSet.addAll(generated);
        }
        return seenSet.size();
    }

    public static long approximatePossibleArrangements(int listSize, int partitions) {
        return Math.round(Math.pow(2, MathUtils.getPossibleArrangementsScaledApproximateLog(100000, 2,
                listSize, partitions) / 100000.0));
    }

    @Test
    void testGetPossibleArrangementsScaledApproximateLog() {
        // Empty list only have 1 possible combination
        for (int i = 2; i < 100; i++) {
            assertThat(MathUtils.getPossibleArrangementsScaledApproximateLog(100L, 2, 0, i))
                    .isEqualTo(0L);
        }

        // List size 3, 2 partitions:
        // [a, b, c] []
        // [a, b] [c]
        // [a][b, c]
        // [][a, b, c]
        // 3! = 6, so 4 * 6 = 24
        assertThat(MathUtils.getPossibleArrangementsScaledApproximateLog(100L,
                24,
                3, 2))
                .isEqualTo(100L);

        // List size 3, 3 partitions:
        // [a, b, c] [] []
        // [a, b] [c] []
        // [a, b] [] [c]
        // [a] [b, c] []
        // [a] [b] [c]
        // [a] [] [b, c]
        // [] [a, b, c] []
        // [] [a, b] [c]
        // [] [a] [b, c]
        // [] [] [a, b, c]
        // 3! = 6, so 4 * 6 = 60
        assertThat(MathUtils.getPossibleArrangementsScaledApproximateLog(100L,
                60,
                3, 3))
                .isEqualTo(100L);

        // Do manual calculation for a bunch of numbers
        for (int listSize = 3; listSize < 6; listSize++) {
            for (int partitions = 1; partitions < 6; partitions++) {
                assertThat(approximatePossibleArrangements(listSize, partitions))
                        .isEqualTo(expectedArrangements(listSize, partitions));
            }
        }
    }

    @Test
    void testGetScaledApproximateLog() {
        for (int i = 2; i < 100; i++) {
            assertThat(MathUtils.getScaledApproximateLog(100, i, 1)).isZero();
            assertThat(MathUtils.getScaledApproximateLog(100, i, i)).isEqualTo(100L);
        }
        assertThat(MathUtils.getScaledApproximateLog(100, 2, 4)).isEqualTo(200L);
        assertThat(MathUtils.getScaledApproximateLog(100, 2, 16)).isEqualTo(400L);
        assertThat(MathUtils.getScaledApproximateLog(100, 4, 16)).isEqualTo(200L);
        assertThat(MathUtils.getScaledApproximateLog(100, 3, 9)).isEqualTo(200L);
        assertThat(MathUtils.getScaledApproximateLog(100, 3, 27)).isEqualTo(300L);
        assertThat(MathUtils.getScaledApproximateLog(100, 9, 3)).isEqualTo(50L);
    }

    @Test
    void testGetLogInBase() {
        var tolerance = Offset.offset(0.000000000000001); // Without the tolerance, was breaking M3 Macs w/ JDK 21.0.3.
        for (int i = 2; i < 100; i++) {
            assertThat(MathUtils.getLogInBase(i, 1)).isZero();
            assertThat(MathUtils.getLogInBase(i, i)).isEqualTo(1.0, tolerance);
        }
        assertThat(MathUtils.getLogInBase(2, 4)).isEqualTo(2.0, tolerance);
        assertThat(MathUtils.getLogInBase(2, 16)).isEqualTo(4.0, tolerance);
        assertThat(MathUtils.getLogInBase(4, 16)).isEqualTo(2.0, tolerance);
        assertThat(MathUtils.getLogInBase(3, 9)).isEqualTo(2.0, tolerance);
        assertThat(MathUtils.getLogInBase(3, 27)).isEqualTo(3.0, tolerance);
        assertThat(MathUtils.getLogInBase(9, 3)).isEqualTo(0.5, tolerance);
    }
}
