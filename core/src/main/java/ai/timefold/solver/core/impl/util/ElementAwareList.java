package ai.timefold.solver.core.impl.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.function.QuadConsumer;
import ai.timefold.solver.core.api.function.TriConsumer;

/**
 * Linked list that allows to add and remove an element in O(1) time.
 * Ideal for incremental operations with frequent undo.
 *
 * @param <T> The element type. Often a tuple.
 */
public final class ElementAwareList<T> {

    private int size = 0;
    private ElementAwareListEntry<T> first = null;
    private ElementAwareListEntry<T> last = null;
    /**
     * It is a frequent pattern that an entry is added immediately after one is removed.
     * By reusing the entry, we can reduce the considerable GC pressure this creates.
     */
    private ElementAwareListEntry<T> availableBlankEntry = null;

    public ElementAwareListEntry<T> add(T element) {
        var entry = newInstance(element, last);
        if (first == null) {
            first = entry;
        } else {
            last.next = entry;
        }
        last = entry;
        size++;
        return entry;
    }

    private ElementAwareListEntry<T> newInstance(T element, ElementAwareListEntry<T> previous) {
        if (availableBlankEntry == null) {
            return new ElementAwareListEntry<>(this, element, previous);
        }
        var entry = availableBlankEntry;
        availableBlankEntry = null;
        entry.list = this;
        entry.element = element;
        entry.previous = previous;
        return entry;
    }

    public void remove(ElementAwareListEntry<T> entry) {
        relinkNext(entry);
        relinkPrevious(entry);
        entry.clear();
        availableBlankEntry = entry;
        size--;
    }

    private void relinkNext(ElementAwareListEntry<T> entry) {
        if (first == entry) {
            first = entry.next;
        } else {
            entry.previous.next = entry.next;
        }
    }

    private void relinkPrevious(ElementAwareListEntry<T> entry) {
        if (last == entry) {
            last = entry.previous;
        } else {
            entry.next.previous = entry.previous;
        }
    }

    public ElementAwareListEntry<T> first() {
        return first;
    }

    public ElementAwareListEntry<T> last() {
        return last;
    }

    public int size() {
        return size;
    }

    /**
     * Convenience method for where it is easy to use a non-capturing lambda.
     * If a capturing lambda consumer were to be created for this method, use either of the following instead,
     * which will consume less memory:
     * 
     * <ul>
     * <li>{@link #forEach(Object, BiConsumer)} for when one extra argument is required.</li>
     * <li>{@link #forEach(Object, Object, TriConsumer)} for when two extra arguments are required.</li>
     * <li>{@link #forEach(Object, Object, Object, QuadConsumer)} for when three extra arguments are required.</li>
     * </ul>
     * 
     * <p>
     * For example, the following code is perfectly fine:
     * <code>
     *     for (int i = 0; i &lt; 3; i++) {
     *         elementAwareList.forEach(entry -&gt; doSomething(entry));
     *     }
     * </code>
     * It will create only one lambda instance, regardless of the number of iterations;
     * it doesn't need to capture any state.
     * On the contrary, the following code will create three instances of a capturing lambda,
     * one for each iteration of the for loop:
     * <code>
     *     for (int a: List.of(1, 2, 3)) {
     *         elementAwareList.forEach(entry -&gt; doSomething(entry, a));
     *     }
     * </code>
     * In this case, the lambda would need to capture "a" which is different in every iteration.
     * It will generally be better to use a {@link #forEach(Object, BiConsumer)} variant with one extra argument,
     * as that will not result in any new instances of the lambda (or iterator) being created:
     * <code>
     *     for (int a: List.of(1, 2, 3)) {
     *         elementAwareList.forEach(a, (entry, a_) -&gt; doSomething(entry, a_));
     *     }
     * </code>
     * This is only an issue on the hot path,
     * where this method can create quite a large garbage collector pressure
     * on account of creating throw-away instances of capturing lambdas.
     *
     * @param elementConsumer The action to be performed for each element
     */
    public void forEach(Consumer<? super T> elementConsumer) {
        var entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            var next = entry.next;
            elementConsumer.accept(entry.element);
            entry = next;
        }
    }

    /**
     * As {@link #forEach(Consumer)}, but with an extra argument.
     *
     * @param other Extra argument to be passed to the consumer.
     * @param elementConsumer The action to be performed for each element
     */
    public <U> void forEach(U other, BiConsumer<? super T, U> elementConsumer) {
        var entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            var next = entry.next;
            elementConsumer.accept(entry.element, other);
            entry = next;
        }
    }

    /**
     * As {@link #forEach(Consumer)}, but with two extra arguments.
     *
     * @param other Extra argument to be passed to the consumer.
     * @param another Another extra argument to be passed to the consumer.
     * @param elementConsumer The action to be performed for each element
     */
    public <U, V> void forEach(U other, V another, TriConsumer<? super T, U, V> elementConsumer) {
        var entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            var next = entry.next;
            elementConsumer.accept(entry.element, other, another);
            entry = next;
        }
    }

    /**
     * As {@link #forEach(Consumer)}, but with three extra arguments.
     *
     * @param other Extra argument to be passed to the consumer.
     * @param another Another extra argument to be passed to the consumer.
     * @param yetAnother Yet another extra argument to be passed to the consumer.
     * @param elementConsumer The action to be performed for each element
     */
    public <U, V, W> void forEach(U other, V another, W yetAnother, QuadConsumer<? super T, U, V, W> elementConsumer) {
        var entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            var next = entry.next;
            elementConsumer.accept(entry.element, other, another, yetAnother);
            entry = next;
        }
    }

    @Override
    public String toString() {
        var content = switch (size) {
            case 0 -> "";
            case 1 -> first.element;
            default -> {
                var builder = new StringBuilder();
                forEach(builder, (element, builder_) -> builder_.append(element).append(", "));
                var length = builder.length();
                builder.replace(length - 2, length, ""); // Remove the final ", ".
                yield builder.toString();
            }
        };
        return "[" + content + "]";
    }

}
