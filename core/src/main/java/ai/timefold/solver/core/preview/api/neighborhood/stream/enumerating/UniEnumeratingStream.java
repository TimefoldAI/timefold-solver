package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniEnumeratingStream<Solution_, A> extends EnumeratingStream {

    /**
     * Exhaustively test each fact against the {@link UniNeighborhoodsPredicate}
     * and match if {@link UniNeighborhoodsPredicate#test(SolutionView, Object)} returns true.
     */
    UniEnumeratingStream<Solution_, A> filter(UniNeighborhoodsPredicate<Solution_, A> filter);

    /**
     * As defined by {@link #join(UniEnumeratingStream, BiNeighborhoodsJoiner[])}, with the array being empty.
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> otherStream) {
        return join(otherStream, new BiNeighborhoodsJoiner[0]);
    }

    /**
     * As defined by {@link #join(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner) {
        return join(otherStream, new BiNeighborhoodsJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2) {
        return join(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return join(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3, BiNeighborhoodsJoiner<A, B> joiner4) {
        return join(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * Create a new {@link BiEnumeratingStream} for every combination of A and B for which the {@link BiNeighborhoodsJoiner}
     * is true (for the properties it extracts from both facts).
     * <p>
     * Important: Joining is faster and more scalable than a {@link BiEnumeratingStream#filter(BiNeighborhoodsPredicate)
     * filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiNeighborhoodsJoiner} is true
     */
    <B> BiEnumeratingStream<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B>... joiners);

    /**
     * As defined by {@link #join(Class, BiNeighborhoodsJoiner[])}, with the array being empty.
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(Class<B> otherClass) {
        return join(otherClass, new BiNeighborhoodsJoiner[0]);
    }

    /**
     * As defined by {@link #join(Class, BiNeighborhoodsJoiner[])}
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner) {
        return join(otherClass, new BiNeighborhoodsJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(Class, BiNeighborhoodsJoiner[])}
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2) {
        return join(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(Class, BiNeighborhoodsJoiner[])}
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2,
            BiNeighborhoodsJoiner<A, B> joiner3) {
        return join(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(Class, BiNeighborhoodsJoiner[])}
     */
    @SuppressWarnings("unchecked")
    default <B> BiEnumeratingStream<Solution_, A, B> join(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2,
            BiNeighborhoodsJoiner<A, B> joiner3, BiNeighborhoodsJoiner<A, B> joiner4) {
        return join(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * Create a new {@link BiEnumeratingStream} for every combination of A and B
     * for which the {@link BiNeighborhoodsJoiner} is true (for the properties it extracts from both facts).
     * The stream will include all facts or entities of the given class,
     * regardless of their pinning status.
     * <p>
     * Important: Joining is faster and more scalable than a {@link BiEnumeratingStream#filter(BiNeighborhoodsPredicate)
     * filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiNeighborhoodsJoiner} is true
     */
    <B> BiEnumeratingStream<Solution_, A, B> join(Class<B> otherClass, BiNeighborhoodsJoiner<A, B>... joiners);

    /**
     * As defined by {@link #ifExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}, with the array being empty.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(UniEnumeratingStream<Solution_, B> otherStream) {
        return ifExists(otherStream, new BiNeighborhoodsJoiner[0]);
    }

    /**
     * As defined by {@link #ifExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner) {
        return ifExists(otherStream, new BiNeighborhoodsJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2) {
        return ifExists(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return ifExists(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3, BiNeighborhoodsJoiner<A, B> joiner4) {
        return ifExists(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * Create a new {@link UniEnumeratingStream} for every A where B exists for which all {@link BiNeighborhoodsJoiner}s are
     * true
     * (for the properties it extracts from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiNeighborhoodsJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniEnumeratingStream<Solution_, A> ifExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B>... joiners);

    /**
     * As defined by {@link #ifExists(Class, BiNeighborhoodsJoiner[])}, with the array being empty.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(Class<B> otherClass) {
        return ifExists(otherClass, new BiNeighborhoodsJoiner[0]);
    }

    /**
     * As defined by {@link #ifExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner) {
        return ifExists(otherClass, new BiNeighborhoodsJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2) {
        return ifExists(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2,
            BiNeighborhoodsJoiner<A, B> joiner3) {
        return ifExists(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3, BiNeighborhoodsJoiner<A, B> joiner4) {
        return ifExists(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * Create a new {@link UniEnumeratingStream} for every A where B exists for which all {@link BiNeighborhoodsJoiner}s are
     * true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiNeighborhoodsJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniEnumeratingStream<Solution_, A> ifExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B>... joiners);

    /**
     * As defined by {@link #ifNotExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}, with the array being empty.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(UniEnumeratingStream<Solution_, B> otherStream) {
        return ifNotExists(otherStream, new BiNeighborhoodsJoiner[0]);
    }

    /**
     * As defined by {@link #ifNotExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner) {
        return ifNotExists(otherStream, new BiNeighborhoodsJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2) {
        return ifNotExists(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return ifNotExists(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(UniEnumeratingStream, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3, BiNeighborhoodsJoiner<A, B> joiner4) {
        return ifNotExists(otherStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * Create a new {@link UniEnumeratingStream} for every A where B does not exist for which the {@link BiNeighborhoodsJoiner}s
     * are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiNeighborhoodsJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniEnumeratingStream<Solution_, A> ifNotExists(UniEnumeratingStream<Solution_, B> otherStream,
            BiNeighborhoodsJoiner<A, B>... joiners);

    /**
     * As defined by {@link #ifNotExists(Class, BiNeighborhoodsJoiner[])}, with the array being empty.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(Class<B> otherClass) {
        return ifNotExists(otherClass, new BiNeighborhoodsJoiner[0]);
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner) {
        return ifNotExists(otherClass, new BiNeighborhoodsJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2) {
        return ifNotExists(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return ifNotExists(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiNeighborhoodsJoiner[])}.
     */
    @SuppressWarnings("unchecked")
    default <B> UniEnumeratingStream<Solution_, A> ifNotExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B> joiner1,
            BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3, BiNeighborhoodsJoiner<A, B> joiner4) {
        return ifNotExists(otherClass, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * Create a new {@link UniEnumeratingStream} for every A where B does not exist for which the {@link BiNeighborhoodsJoiner}s
     * are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiNeighborhoodsJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> UniEnumeratingStream<Solution_, A> ifNotExists(Class<B> otherClass, BiNeighborhoodsJoiner<A, B>... joiners);

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
     * Not following this recommendation creates a enumerating stream with duplicate tuples,
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
     * Simple example: assuming a enumerating stream of tuples of {@code Person}s
     * {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30)]},
     * calling {@code map(Person::getAge)} on such stream will produce a stream of {@link Integer}s
     * {@code [20, 25, 30]},
     *
     * <p>
     * Example with a non-bijective mapping function: assuming a enumerating stream of tuples of {@code Person}s
     * {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]},
     * calling {@code map(Person::getAge)} on such stream will produce a stream of {@link Integer}s
     * {@code [20, 25, 30, 30, 20]}.
     *
     * <p>
     * Use with caution,
     * as the increased memory allocation rates coming from tuple creation may negatively affect performance.
     *
     * @param mapping function to convert the original tuple into the new tuple
     * @param <ResultA_> the type of the only fact in the resulting {@link UniEnumeratingStream}'s tuple
     */
    <ResultA_> UniEnumeratingStream<Solution_, ResultA_> map(UniNeighborhoodsMapper<Solution_, A, ResultA_> mapping);

    /**
     * As defined by {@link #map(UniNeighborhoodsMapper)}, only resulting in {@link BiEnumeratingStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiEnumeratingStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiEnumeratingStream}'s tuple
     */
    <ResultA_, ResultB_> BiEnumeratingStream<Solution_, ResultA_, ResultB_> map(
            UniNeighborhoodsMapper<Solution_, A, ResultA_> mappingA,
            UniNeighborhoodsMapper<Solution_, A, ResultB_> mappingB);

    /**
     * Transforms the stream in such a way that all the tuples going through it are distinct.
     * (No two tuples will {@link Object#equals(Object) equal}.)
     *
     * <p>
     * By default, tuples going through an enumerating stream are distinct.
     * However, operations such as {@link #map(UniNeighborhoodsMapper)} may create a stream which breaks that promise.
     * By calling this method on such a stream,
     * duplicate copies of the same tuple will be omitted at a performance cost.
     */
    UniEnumeratingStream<Solution_, A> distinct();

}
