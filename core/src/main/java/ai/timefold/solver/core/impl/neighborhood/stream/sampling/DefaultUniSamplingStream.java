package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingJoiner;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.BiSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.BiEnumeratingJoinerComber;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultUniSamplingStream<Solution_, A> implements InnerUniSamplingStream<Solution_, A> {

    private final UniDataset<Solution_, A> dataset;

    public DefaultUniSamplingStream(UniDataset<Solution_, A> dataset) {
        this.dataset = Objects.requireNonNull(dataset);
    }

    @Override
    public UniDataset<Solution_, A> getDataset() {
        return dataset;
    }

    @Override
    public <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiEnumeratingJoiner<A, B>... joiners) {
        return new DefaultBiSamplingStream<>(dataset,
                ((AbstractUniEnumeratingStream<Solution_, B>) uniEnumeratingStream).createDataset(),
                BiEnumeratingJoinerComber.comb(joiners));
    }

}
