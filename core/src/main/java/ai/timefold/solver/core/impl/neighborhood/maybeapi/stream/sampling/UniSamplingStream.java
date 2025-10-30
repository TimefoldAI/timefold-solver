package ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniSamplingStream<Solution_, A> extends SamplingStream {

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream) {
        return pick(uniEnumeratingStream, new BiEnumeratingJoiner[0]);
    }

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiEnumeratingJoiner<A, B> joiner) {
        return pick(uniEnumeratingStream, new BiEnumeratingJoiner[] { joiner });
    }

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiEnumeratingJoiner<A, B> joiner1, BiEnumeratingJoiner<A, B> joiner2) {
        return pick(uniEnumeratingStream, new BiEnumeratingJoiner[] { joiner1, joiner2 });
    }

    @SuppressWarnings("unchecked")
    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiEnumeratingJoiner<A, B> joiner1, BiEnumeratingJoiner<A, B> joiner2, BiEnumeratingJoiner<A, B> joiner3) {
        return pick(uniEnumeratingStream, new BiEnumeratingJoiner[] { joiner1, joiner2, joiner3 });
    }

    <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiEnumeratingJoiner<A, B>... joiners);

}
