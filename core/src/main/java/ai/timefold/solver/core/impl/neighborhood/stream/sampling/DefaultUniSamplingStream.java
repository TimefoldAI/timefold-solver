package ai.timefold.solver.core.impl.neighborhood.stream.sampling;

import java.util.Objects;
import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.BiSamplingStream;
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
    public <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiPredicate<A, B> filter) {
        return new DefaultBiFromUnisSamplingStream<>(dataset,
                ((AbstractUniEnumeratingStream<Solution_, B>) uniEnumeratingStream).createDataset(),
                filter);
    }

    @Override
    public UniDataset<Solution_, A> getDataset() {
        return dataset;
    }

}
