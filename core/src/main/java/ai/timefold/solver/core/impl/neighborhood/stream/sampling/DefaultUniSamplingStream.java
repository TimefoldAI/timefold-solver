package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.BiEnumeratingJoinerComber;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.function.BiEnumeratingJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.BiSamplingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultUniSamplingStream<Solution_, A> implements InnerUniSamplingStream<Solution_, A> {

    private final UniLeftDataset<Solution_, A> dataset;

    public DefaultUniSamplingStream(UniLeftDataset<Solution_, A> dataset) {
        this.dataset = Objects.requireNonNull(dataset);
    }

    @Override
    public UniLeftDataset<Solution_, A> getDataset() {
        return dataset;
    }

    @Override
    public <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiEnumeratingJoiner<A, B>... joiners) {
        var comber = BiEnumeratingJoinerComber.<Solution_, A, B> comb(joiners);
        return new DefaultBiSamplingStream<>(dataset,
                ((AbstractUniEnumeratingStream<Solution_, B>) uniEnumeratingStream).createRightDataset(comber));
    }

}
