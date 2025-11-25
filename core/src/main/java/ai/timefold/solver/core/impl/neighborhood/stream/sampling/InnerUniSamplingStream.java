package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.UniSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerUniSamplingStream<Solution_, A>
        extends InnerSamplingStream<Solution_>, UniSamplingStream<Solution_, A> {

    @Override
    UniLeftDataset<Solution_, A> getDataset();

}
