package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.Nullable;

public abstract class AbstractDataStream<Solution_>
        implements BavetStream {

    protected final DataStreamFactory<Solution_> dataStreamFactory;
    protected final @Nullable AbstractDataStream<Solution_> parent;
    protected final List<AbstractDataStream<Solution_>> childStreamList = new ArrayList<>(2);

    protected AbstractDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            @Nullable AbstractDataStream<Solution_> parent) {
        this.dataStreamFactory = dataStreamFactory;
        this.parent = parent;
    }

    public final <Stream_ extends AbstractDataStream<Solution_>> Stream_ shareAndAddChild(Stream_ stream) {
        return dataStreamFactory.share(stream, childStreamList::add);
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> constraintStreamSet) {
        if (parent == null) { // Maybe a join/ifExists/forEach forgot to override this?
            throw new IllegalStateException("Impossible state: the stream (" + this + ") does not have a parent.");
        }
        parent.collectActiveDataStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    /**
     * Returns the stream which first produced the tuple that this stream operates on.
     * If a stream does not have a single parent nor is it a source, it is expected to override this method.
     *
     * @return this if {@link TupleSource}, otherwise parent's tuple source.
     */
    public AbstractDataStream<Solution_> getTupleSource() {
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
    public final @Nullable AbstractDataStream<Solution_> getParent() {
        return parent;
    }

    public final List<AbstractDataStream<Solution_>> getChildStreamList() {
        return childStreamList;
    }

}
