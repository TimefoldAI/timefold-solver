package ai.timefold.solver.core.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CollectionUtils {

    /**
     * Creates a copy of the list, optionally in reverse order.
     *
     * @param originalList the list to copy, preferably {@link ArrayList}
     * @param reverse true if the resulting list should have its order reversed
     * @return mutable list, never null
     * @param <E> the type of elements in the list
     */
    public static <E> List<E> copy(List<E> originalList, boolean reverse) {
        if (!reverse) {
            return new ArrayList<>(originalList);
        }
        /*
         * Some move implementations on the hot path rely heavily on list reversal.
         * As such, the following implementation was benchmarked to perform as well as possible for lists of all sizes.
         * See PLANNER-2808 for details.
         */
        switch (originalList.size()) {
            case 0 -> {
                return new ArrayList<>(0);
            }
            case 1 -> {
                List<E> singletonList = new ArrayList<>(1);
                singletonList.add(originalList.get(0));
                return singletonList;
            }
            case 2 -> {
                List<E> smallList = new ArrayList<>(2);
                smallList.add(originalList.get(1));
                smallList.add(originalList.get(0));
                return smallList;
            }
            default -> {
                List<E> largeList = new ArrayList<>(originalList);
                Collections.reverse(largeList);
                return largeList;
            }
        }
    }

    public static <T> List<T> concat(List<T> left, List<T> right) {
        List<T> result = new ArrayList<>(left.size() + right.size());
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    public static <T> List<T> toDistinctList(Collection<T> collection) {
        int size = collection.size();
        switch (size) {
            case 0 -> {
                return Collections.emptyList();
            }
            case 1 -> {
                if (collection instanceof List<T> list) {
                    return list; // Optimization: not making a defensive copy.
                } else {
                    return Collections.singletonList(collection.iterator().next());
                }
            }
            default -> {
                if (collection instanceof Set<T> set) {
                    return new ArrayList<>(set);
                }
                /*
                 * The following is better than ArrayList(LinkedHashSet) because HashSet is cheaper,
                 * while still maintaining the original order of the collection.
                 */
                var resultList = new ArrayList<T>(size);
                var set = newHashSet(size);
                for (T element : collection) {
                    if (set.add(element)) {
                        resultList.add(element);
                    }
                }
                resultList.trimToSize();
                return resultList;
            }
        }
    }

    public static <T> Set<T> newHashSet(int size) {
        return new HashSet<>(calculateCapacityForDefaultLoadFactor(size));
    }

    private static int calculateCapacityForDefaultLoadFactor(int numElements) {
        // This guarantees the set/map will never need to grow.
        return (int) Math.ceil(numElements / 0.75f);
    }

    public static <T> Set<T> newLinkedHashSet(int size) {
        return new LinkedHashSet<>(calculateCapacityForDefaultLoadFactor(size));
    }

    public static <K, V> Map<K, V> newHashMap(int size) {
        return new HashMap<>(calculateCapacityForDefaultLoadFactor(size));
    }

    public static <K, V> Map<K, V> newIdentityHashMap(int size) {
        return new IdentityHashMap<>(calculateCapacityForDefaultLoadFactor(size));
    }

    public static <K, V> Map<K, V> newLinkedHashMap(int size) {
        return new LinkedHashMap<>(calculateCapacityForDefaultLoadFactor(size));

    }

    private CollectionUtils() {
        // No external instances.
    }

}
