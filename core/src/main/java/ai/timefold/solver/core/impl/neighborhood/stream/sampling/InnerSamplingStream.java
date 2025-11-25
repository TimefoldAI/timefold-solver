package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.SamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerSamplingStream<Solution_, Out_ extends AbstractTuple> extends SamplingStream {

    AbstractDataset<Solution_> getDataset();

}
