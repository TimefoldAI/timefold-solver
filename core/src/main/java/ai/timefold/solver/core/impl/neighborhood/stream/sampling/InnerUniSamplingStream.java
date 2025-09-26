package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.UniSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerUniSamplingStream<Solution_, A>
        extends InnerSamplingStream<Solution_, UniTuple<A>>, UniSamplingStream<Solution_, A> {

    @Override
    UniDataset<Solution_, A> getDataset();

}
