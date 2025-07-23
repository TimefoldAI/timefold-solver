package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.notEquals;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniDataStream<Solution_, A> extends DataStream<Solution_> {

    /**
     * Exhaustively test each fact against the {@link Predicate}
     * and match if {@link Predicate#test(Object)} returns true.
     */
    UniDataStream<Solution_, A> filter(Predicate<A> predicate);

    /**
     * Create a new {@link BiDataStream} for every combination of A and B for which the {@link BiJoiner}
     * is true (for the properties it extracts from both facts).
     * <p>
     * Important: Joining is faster and more scalable than a {@link BiDataStream#filter(BiPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiJoiner} is true
     */
    <B> @NonNull BiDataStream<Solution_, A, B> join(@NonNull UniDataStream<Solution_, B> otherStream, @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link BiDataStream} for every combination of A and B
     * for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * The stream will include all facts or entities of the given class,
     * regardless of their pinning status.
     * <p>
     * Important: Joining is faster and more scalable than a {@link BiDataStream#filter(BiPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiJoiner} is true
     */
    <B> @NonNull BiDataStream<Solution_, A, B> join(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B exists for which all {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifExists(Class<B> otherClass, BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B exists for which all {@link BiJoiner}s are true
     * (for the properties it extracts from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifExists(UniDataStream<Solution_, B> otherStream, BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A, if another A exists that does not {@link Object#equals(Object)}
     * the first, and for which the {@link BiJoiner}s are true (for the properties they extract from both facts).
     *
     * @return a stream that matches every A where a different A exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    default UniDataStream<Solution_, A> ifExistsOther(Class<A> otherClass, BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifExists(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniDataStream} for every A where B does not exist for which the {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifNotExists(Class<B> otherClass, BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B does not exist for which the {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifNotExists(UniDataStream<Solution_, B> otherStream, BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A, if no other A exists that does not {@link Object#equals(Object)}
     * the first,
     * for which the {@link BiJoiner}s are true (for the properties they extract from both facts).
     *
     * @return a stream that matches every A where a different A does not exist
     */
    @SuppressWarnings("unchecked")
    default UniDataStream<Solution_, A> ifNotExistsOther(Class<A> otherClass, BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifNotExists(otherClass, allJoiners);
    }

}
