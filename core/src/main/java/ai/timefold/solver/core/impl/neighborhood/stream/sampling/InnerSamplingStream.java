package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.SamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerSamplingStream<Solution_> extends SamplingStream {

    AbstractDataset<Solution_> getDataset();

}
