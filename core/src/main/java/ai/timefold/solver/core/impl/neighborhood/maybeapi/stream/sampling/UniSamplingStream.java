package ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.UniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniSamplingStream<Solution_, A> extends SamplingStream {

    default <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream) {
        return pick(uniEnumeratingStream, (a, b) -> true);
    }

    <B> BiSamplingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiPredicate<A, B> filter);

}
