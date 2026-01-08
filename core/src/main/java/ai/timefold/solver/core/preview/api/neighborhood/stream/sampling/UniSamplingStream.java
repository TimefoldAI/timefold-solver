package ai.timefold.solver.core.preview.api.neighborhood.stream.sampling;

import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniSamplingStream<Solution_, A> extends SamplingStream {

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[0]);
    }

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner });
    }

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B>... joiners);

}
