package ai.timefold.solver.core.impl.neighborhood.stream.picking;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.picking.PickingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerPickingStream<Solution_> extends PickingStream {

    AbstractDataset<Solution_> getDataset();

}
