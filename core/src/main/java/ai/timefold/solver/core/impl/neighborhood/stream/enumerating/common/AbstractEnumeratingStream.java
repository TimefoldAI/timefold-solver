package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractEnumeratingStream<Solution_>
        implements BavetStream {

    protected final EnumeratingStreamFactory<Solution_> enumeratingStreamFactory;
    protected final @Nullable AbstractEnumeratingStream<Solution_> parent;
    protected final List<AbstractEnumeratingStream<Solution_>> childStreamList = new ArrayList<>(2);

    protected AbstractEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            @Nullable AbstractEnumeratingStream<Solution_> parent) {
        this.enumeratingStreamFactory = enumeratingStreamFactory;
        this.parent = parent;
    }

    public final <Stream_ extends AbstractEnumeratingStream<Solution_>> Stream_ shareAndAddChild(Stream_ stream) {
        return enumeratingStreamFactory.share(stream, childStreamList::add);
    }

    protected boolean guaranteesDistinct() {
        if (parent != null) {
            // It is generally safe to take this from the parent; if the stream disagrees, it may override.
            return parent.guaranteesDistinct();
        } else { // Streams need to explicitly opt-in by overriding this method.
            return false;
        }
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        if (parent == null) { // Maybe a join/ifExists/forEach forgot to override this?
            throw new IllegalStateException("Impossible state: the stream (%s) does not have a parent."
                    .formatted(this));
        }
        parent.collectActiveEnumeratingStreams(enumeratingStreamSet);
        enumeratingStreamSet.add(this);
    }

    /**
     * Returns the stream which first produced the tuple that this stream operates on.
     * If a stream does not have a single parent nor is it a source, it is expected to override this method.
     *
     * @return this if {@link TupleSource}, otherwise parent's tuple source.
     */
    public AbstractEnumeratingStream<Solution_> getTupleSource() {
        if (this instanceof TupleSource) {
            return this;
        } else if (parent == null) { // Maybe some stream forgot to override this?
            throw new IllegalStateException("Impossible state: the stream (" + this + ") does not have a parent.");
        }
        return parent.getTupleSource();
    }

    public abstract void buildNode(DataNodeBuildHelper<Solution_> buildHelper);

    protected void assertEmptyChildStreamList() {
        if (!childStreamList.isEmpty()) {
            throw new IllegalStateException(
                    "Impossible state: the stream (" + this + ") has a non-empty childStreamList (" + childStreamList + ").");
        }
    }

    @Override
    public final @Nullable AbstractEnumeratingStream<Solution_> getParent() {
        return parent;
    }

    public final List<AbstractEnumeratingStream<Solution_>> getChildStreamList() {
        return childStreamList;
    }

}
