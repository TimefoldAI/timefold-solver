package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.notEqualsForDataStreams;

import java.util.Arrays;
import java.util.stream.Stream;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataJoiner;
import ai.timefold.solver.core.impl.move.streams.maybeapi.DataJoiners;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataMapper;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniDataStream<Solution_, A> extends DataStream<Solution_> {

    /**
     * Exhaustively test each fact against the {@link UniDataFilter}
     * and match if {@link UniDataFilter#test(SolutionView, Object)} returns true.
     */
    UniDataStream<Solution_, A> filter(UniDataFilter<Solution_, A> filter);

    /**
     * Create a new {@link BiDataStream} for every combination of A and B for which the {@link BiDataJoiner}
     * is true (for the properties it extracts from both facts).
     * <p>
     * Important: Joining is faster and more scalable than a {@link BiDataStream#filter(BiDataFilter) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiDataJoiner} is true
     */
    <B> BiDataStream<Solution_, A, B> join(UniDataStream<Solution_, B> otherStream, BiDataJoiner<A, B>... joiners);

    /**
     * Create a new {@link BiDataStream} for every combination of A and B
     * for which the {@link BiDataJoiner} is true (for the properties it extracts from both facts).
     * The stream will include all facts or entities of the given class,
     * regardless of their pinning status.
     * <p>
     * Important: Joining is faster and more scalable than a {@link BiDataStream#filter(BiDataFilter) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiDataJoiner} is true
     */
    <B> BiDataStream<Solution_, A, B> join(Class<B> otherClass, BiDataJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B exists for which all {@link BiDataJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiDataJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifExists(Class<B> otherClass, BiDataJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B exists for which all {@link BiDataJoiner}s are true
     * (for the properties it extracts from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiDataJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifExists(UniDataStream<Solution_, B> otherStream, BiDataJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A, if another A exists that does not {@link Object#equals(Object)}
     * the first, and for which the {@link BiDataJoiner}s are true (for the properties they extract from both facts).
     *
     * @return a stream that matches every A where a different A exists for which the {@link BiDataJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    default UniDataStream<Solution_, A> ifExistsOther(Class<A> otherClass, BiDataJoiner<A, A>... joiners) {
        BiDataJoiner<A, A> otherness = DataJoiners.filtering(notEqualsForDataStreams());

        @SuppressWarnings("unchecked")
        BiDataJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiDataJoiner[]::new);
        return ifExists(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniDataStream} for every A where B does not exist for which the {@link BiDataJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiDataJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifNotExists(Class<B> otherClass, BiDataJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B does not exist for which the {@link BiDataJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiDataJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniDataStream<Solution_, A> ifNotExists(UniDataStream<Solution_, B> otherStream, BiDataJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A, if no other A exists that does not {@link Object#equals(Object)}
     * the first,
     * for which the {@link BiDataJoiner}s are true (for the properties they extract from both facts).
     *
     * @return a stream that matches every A where a different A does not exist
     */
    @SuppressWarnings("unchecked")
    default UniDataStream<Solution_, A> ifNotExistsOther(Class<A> otherClass, BiDataJoiner<A, A>... joiners) {
        BiDataJoiner<A, A> otherness = DataJoiners.filtering(notEqualsForDataStreams());

        @SuppressWarnings("unchecked")
        BiDataJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiDataJoiner[]::new);
        return ifNotExists(otherClass, allJoiners);
    }

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    /**
     * Transforms the stream in such a way that tuples are remapped using the given function.
     * This may produce a stream with duplicate tuples.
     * See {@link #distinct()} for details.
     * <p>
     * There are several recommendations for implementing the mapping function:
     *
     * <ul>
     * <li>Purity.
     * The mapping function should only depend on its input.
     * That is, given the same input, it always returns the same output.</li>
     * <li>Bijectivity.
     * No two input tuples should map to the same output tuple,
     * or to tuples that are {@link Object#equals(Object) equal}.
     * Not following this recommendation creates a data stream with duplicate tuples,
     * and may force you to use {@link #distinct()} later, which comes at a performance cost.</li>
     * <li>Immutable data carriers.
     * The objects returned by the mapping function should be identified by their contents and nothing else.
     * If two of them have contents which {@link Object#equals(Object) equal},
     * then they should likewise {@link Object#equals(Object) equal} and preferably be the same instance.
     * The objects returned by the mapping function should also be immutable,
     * meaning their contents should not be allowed to change.</li>
     * </ul>
     *
     * <p>
     * Simple example: assuming a data stream of tuples of {@code Person}s
     * {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30)]},
     * calling {@code map(Person::getAge)} on such stream will produce a stream of {@link Integer}s
     * {@code [20, 25, 30]},
     *
     * <p>
     * Example with a non-bijective mapping function: assuming a data stream of tuples of {@code Person}s
     * {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]},
     * calling {@code map(Person::getAge)} on such stream will produce a stream of {@link Integer}s
     * {@code [20, 25, 30, 30, 20]}.
     *
     * <p>
     * Use with caution,
     * as the increased memory allocation rates coming from tuple creation may negatively affect performance.
     *
     * @param mapping function to convert the original tuple into the new tuple
     * @param <ResultA_> the type of the only fact in the resulting {@link UniDataStream}'s tuple
     */
    <ResultA_> UniDataStream<Solution_, ResultA_> map(UniDataMapper<Solution_, A, ResultA_> mapping);

    /**
     * As defined by {@link #map(UniDataMapper)}, only resulting in {@link BiDataStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiDataStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiDataStream}'s tuple
     */
    <ResultA_, ResultB_> BiDataStream<Solution_, ResultA_, ResultB_> map(UniDataMapper<Solution_, A, ResultA_> mappingA,
            UniDataMapper<Solution_, A, ResultB_> mappingB);

    /**
     * Transforms the stream in such a way that all the tuples going through it are distinct.
     * (No two tuples will {@link Object#equals(Object) equal}.)
     *
     * <p>
     * By default, tuples going through a data stream are distinct.
     * However, operations such as {@link #map(UniDataMapper)} may create a stream which breaks that promise.
     * By calling this method on such a stream,
     * duplicate copies of the same tuple will be omitted at a performance cost.
     */
    UniDataStream<Solution_, A> distinct();

}
