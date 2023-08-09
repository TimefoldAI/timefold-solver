package ai.timefold.solver.constraint.streams.bavet.common;

public abstract class AbstractNode<T, PropagationQueue_ extends PropagationQueue<T>> {

    private long id;

    public final void calculateScore() {
        getPropagationQueue().propagateAndClear();
    }

    abstract protected PropagationQueue_ getPropagationQueue();

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        // Useful for debugging if a constraint has multiple nodes of the same type
        return getClass().getSimpleName() + "-" + id;
    }

}
