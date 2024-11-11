package ai.timefold.solver.core.preview.api.move;

import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.notEquals;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;

import org.jspecify.annotations.NonNull;

public interface UniDataStream<Solution_, A> extends DataStream<Solution_> {

    /**
     * Exhaustively test each fact against the {@link Predicate}
     * and match if {@link Predicate#test(Object)} returns true.
     */
    @NonNull
    UniDataStream<Solution_, A> filter(@NonNull Predicate<A> predicate);

    /**
     * Create a new {@link UniDataStream} for every A where B exists for which all {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> @NonNull UniDataStream<Solution_, A> ifExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B exists for which all {@link BiJoiner}s are true
     * (for the properties it extracts from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> @NonNull UniDataStream<Solution_, A> ifExists(@NonNull UniDataStream<Solution_, B> otherStream,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B exists for which the {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> @NonNull UniDataStream<Solution_, A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A, if another A exists that does not {@link Object#equals(Object)}
     * the first, and for which the {@link BiJoiner}s are true (for the properties they extract from both facts).
     *
     * @return a stream that matches every A where a different A exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    default @NonNull UniDataStream<Solution_, A> ifExistsOther(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifExists(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniDataStream} for every A,
     * if another A exists that does not {@link Object#equals(Object)} the first,
     * and for which the {@link BiJoiner}s are true (for the properties they extract from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     *
     * @return a stream that matches every A where a different A exists for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    default @NonNull UniDataStream<Solution_, A> ifExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifExistsIncludingUnassigned(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniDataStream} for every A where B does not exist for which the {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> @NonNull UniDataStream<Solution_, A> ifNotExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B does not exist for which the {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> @NonNull UniDataStream<Solution_, A> ifNotExists(@NonNull UniDataStream<Solution_, B> otherStream,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A where B does not exist for which the {@link BiJoiner}s are true
     * (for the properties they extract from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner}s are true
     */
    @SuppressWarnings("unchecked")
    <B> @NonNull UniDataStream<Solution_, A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniDataStream} for every A, if no other A exists that does not {@link Object#equals(Object)}
     * the first,
     * for which the {@link BiJoiner}s are true (for the properties they extract from both facts).
     *
     * @return a stream that matches every A where a different A does not exist
     */
    @SuppressWarnings("unchecked")
    default @NonNull UniDataStream<Solution_, A> ifNotExistsOther(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifNotExists(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniDataStream} for every A,
     * if no other A exists that does not {@link Object#equals(Object)} the first,
     * for which the {@link BiJoiner}s are true (for the properties they extract from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     *
     * @return a stream that matches every A where a different A does not exist
     */
    @SuppressWarnings("unchecked")
    default @NonNull UniDataStream<Solution_, A> ifNotExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifNotExistsIncludingUnassigned(otherClass, allJoiners);
    }

}
