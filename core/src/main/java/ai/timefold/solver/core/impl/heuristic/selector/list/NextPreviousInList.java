package ai.timefold.solver.core.impl.heuristic.selector.list;

/**
 * Points to a list variable next and previous elements position specified by an entity.
 */
public class NextPreviousInList {

    private Object tuple;
    private NextPreviousInList next;
    private NextPreviousInList previous;

    public NextPreviousInList(Object tuple, NextPreviousInList previous, NextPreviousInList next) {
        this.tuple = tuple;
        this.next = next;
        this.previous = previous;
    }

    public NextPreviousInList(Object tuple, NextPreviousInList next) {
        this.tuple = tuple;
        this.next = next;
    }

    public Object getTuple() {
        return tuple;
    }

    public void setTuple(Object tuple) {
        this.tuple = tuple;
    }

    public NextPreviousInList getNext() {
        return next;
    }

    public void setNext(NextPreviousInList next) {
        this.next = next;
    }

    public NextPreviousInList getPrevious() {
        return previous;
    }

    public void setPrevious(NextPreviousInList previous) {
        this.previous = previous;
    }
}
