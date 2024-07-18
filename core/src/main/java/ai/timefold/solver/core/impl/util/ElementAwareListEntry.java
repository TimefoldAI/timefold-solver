package ai.timefold.solver.core.impl.util;

/**
 * An entry of {@link ElementAwareList}
 *
 * @param <T> The element type. Often a tuple.
 */
public final class ElementAwareListEntry<T> {

    private ElementAwareList<T> list;
    private final T element;
    ElementAwareListEntry<T> previous;
    ElementAwareListEntry<T> next;

    ElementAwareListEntry(ElementAwareList<T> list, T element, ElementAwareListEntry<T> previous) {
        this.list = list;
        this.element = element;
        this.previous = previous;
        this.next = null;
    }

    public ElementAwareListEntry<T> previous() {
        return previous;
    }

    public ElementAwareListEntry<T> next() {
        return next;
    }

    public void remove() {
        if (list == null) {
            throw new IllegalStateException("The element (" + element + ") was already removed.");
        }
        list.remove(this);
        list = null;
    }

    public T getElement() {
        return element;
    }

    public ElementAwareList<T> getList() {
        return list;
    }

    @Override
    public String toString() {
        return element.toString();
    }

}
