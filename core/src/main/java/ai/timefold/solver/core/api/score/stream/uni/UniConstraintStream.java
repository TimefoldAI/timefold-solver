package ai.timefold.solver.core.api.score.stream.uni;

import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.notEquals;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.uniConstantNull;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.uniConstantOne;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.uniConstantOneBigDecimal;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.uniConstantOneLong;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;

import org.jspecify.annotations.NonNull;

/**
 * A {@link ConstraintStream} that matches one fact.
 *
 * @param <A> the type of the first and only fact in the tuple.
 * @see ConstraintStream
 */
public interface UniConstraintStream<A> extends ConstraintStream {
    // ************************************************************************
    // Filter
    // ************************************************************************

    /**
     * Exhaustively test each fact against the {@link Predicate}
     * and match if {@link Predicate#test(Object)} returns true.
     */
    @NonNull
    UniConstraintStream<A> filter(@NonNull Predicate<A> predicate);

    // ************************************************************************
    // Join
    // ************************************************************************

    /**
     * Create a new {@link BiConstraintStream} for every combination of A and B.
     * <p>
     * Important: {@link BiConstraintStream#filter(BiPredicate) Filtering} this is slower and less scalable
     * than a {@link #join(UniConstraintStream, BiJoiner)},
     * because it doesn't apply hashing and/or indexing on the properties,
     * so it creates and checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull UniConstraintStream<B> otherStream) {
        return join(otherStream, new BiJoiner[0]);
    }

    /**
     * Create a new {@link BiConstraintStream} for every combination of A and B for which the {@link BiJoiner}
     * is true (for the properties it extracts from both facts).
     * <p>
     * Important: This is faster and more scalable than a {@link #join(UniConstraintStream) join}
     * followed by a {@link BiConstraintStream#filter(BiPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiJoiner} is true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner) {
        return join(otherStream, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners}
     *         are true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2) {
        return join(otherStream, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners}
     *         are true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return join(otherStream, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners}
     *         are true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3, @NonNull BiJoiner<A, B> joiner4) {
        return join(otherStream, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #join(UniConstraintStream, BiJoiner)}.
     * If multiple {@link BiJoiner}s are provided, for performance reasons, the indexing joiners must be placed before
     * filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners} are true
     */
    <B> @NonNull BiConstraintStream<A, B> join(@NonNull UniConstraintStream<B> otherStream, @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every combination of A and B.
     * <p>
     * Important: {@link BiConstraintStream#filter(BiPredicate) Filtering} this is slower and less scalable
     * than a {@link #join(Class, BiJoiner)},
     * because it doesn't apply hashing and/or indexing on the properties,
     * so it creates and checks every combination of A and B.
     * <p>
     * Important: This is faster and more scalable than a {@link #join(Class) join}
     * followed by a {@link BiConstraintStream#filter(BiPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different range of B may be selected.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     * <p>
     * This method is syntactic sugar for {@link #join(UniConstraintStream)}.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull Class<B> otherClass) {
        return join(otherClass, new BiJoiner[0]);
    }

    /**
     * Create a new {@link BiConstraintStream} for every combination of A and B
     * for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * <p>
     * Important: This is faster and more scalable than a {@link #join(Class) join}
     * followed by a {@link BiConstraintStream#filter(BiPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of A and B.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different range of B may be selected.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     * <p>
     * This method is syntactic sugar for {@link #join(UniConstraintStream, BiJoiner)}.
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which the {@link BiJoiner} is true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner) {
        return join(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners}
     *         are true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2) {
        return join(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners}
     *         are true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return join(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners}
     *         are true
     */
    default <B> @NonNull BiConstraintStream<A, B> join(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3, @NonNull BiJoiner<A, B> joiner4) {
        return join(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #join(Class, BiJoiner)}.
     * For performance reasons, the indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every combination of A and B for which all the {@link BiJoiner joiners} are true
     */
    <B> @NonNull BiConstraintStream<A, B> join(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners);

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    /**
     * Create a new {@link UniConstraintStream} for every A where B exists for which the {@link BiJoiner} is true
     * (for the properties it extracts from both facts).
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner} is true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner) {
        return ifExists(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(Class, BiJoiner)}. For performance reasons, indexing joiners must be placed before
     * filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2) {
        return ifExists(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(Class, BiJoiner)}. For performance reasons, indexing joiners must be placed before
     * filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return ifExists(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(Class, BiJoiner)}. For performance reasons, indexing joiners must be placed before
     * filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3, @NonNull BiJoiner<A, B> joiner4) {
        return ifExists(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExists(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    <B> @NonNull UniConstraintStream<A> ifExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniConstraintStream} for every A where B exists for which the {@link BiJoiner} is true
     * (for the properties it extracts from both facts).
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner} is true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner) {
        return ifExists(otherStream, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2) {
        return ifExists(otherStream, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return ifExists(otherStream, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3,
            @NonNull BiJoiner<A, B> joiner4) {
        return ifExists(otherStream, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    <B> @NonNull UniConstraintStream<A> ifExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniConstraintStream} for every A where B exists for which the {@link BiJoiner} is true
     * (for the properties it extracts from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which the {@link BiJoiner} is true
     */
    default <B> @NonNull UniConstraintStream<A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2,
            @NonNull BiJoiner<A, B> joiner3, @NonNull BiJoiner<A, B> joiner4) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B exists for which all the {@link BiJoiner}s are true
     */
    <B> @NonNull UniConstraintStream<A> ifExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniConstraintStream} for every A, if another A exists that does not {@link Object#equals(Object)}
     * the first.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @return a stream that matches every A where a different A exists
     */
    default @NonNull UniConstraintStream<A> ifExistsOther(@NonNull Class<A> otherClass) {
        return ifExists(otherClass, Joiners.filtering(notEquals()));
    }

    /**
     * Create a new {@link UniConstraintStream} for every A, if another A exists that does not {@link Object#equals(Object)}
     * the first, and for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @return a stream that matches every A where a different A exists for which the {@link BiJoiner} is
     *         true
     */
    default @NonNull UniConstraintStream<A> ifExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner) {
        return ifExistsOther(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExistsOther(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner1,
            @NonNull BiJoiner<A, A> joiner2) {
        return ifExistsOther(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExistsOther(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner1,
            @NonNull BiJoiner<A, A> joiner2,
            @NonNull BiJoiner<A, A> joiner3) {
        return ifExistsOther(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExistsOther(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner1,
            @NonNull BiJoiner<A, A> joiner2,
            @NonNull BiJoiner<A, A> joiner3, @NonNull BiJoiner<A, A> joiner4) {
        return ifExistsOther(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExistsOther(Class, BiJoiner)}.
     * For performance reasons, the indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifExists(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniConstraintStream} for every A,
     * if another A exists that does not {@link Object#equals(Object)} the first.
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     *
     * @return a stream that matches every A where a different A exists
     */
    default @NonNull UniConstraintStream<A> ifExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[0]);
    }

    /**
     * Create a new {@link UniConstraintStream} for every A,
     * if another A exists that does not {@link Object#equals(Object)} the first,
     * and for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @return a stream that matches every A where a different A exists for which the {@link BiJoiner} is
     *         true
     */
    default @NonNull UniConstraintStream<A> ifExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner1, @NonNull BiJoiner<A, A> joiner2) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner1, @NonNull BiJoiner<A, A> joiner2, @NonNull BiJoiner<A, A> joiner3) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner1, @NonNull BiJoiner<A, A> joiner2, @NonNull BiJoiner<A, A> joiner3,
            @NonNull BiJoiner<A, A> joiner4) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * If multiple {@link BiJoiner}s are provided, for performance reasons,
     * the indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @return a stream that matches every A where a different A exists for which all the {@link BiJoiner}s
     *         are true
     */
    default @NonNull UniConstraintStream<A> ifExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifExistsIncludingUnassigned(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniConstraintStream} for every A where B does not exist for which the {@link BiJoiner} is
     * true (for the properties it extracts from both facts).
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner} is true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner) {
        return ifNotExists(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2) {
        return ifNotExists(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return ifNotExists(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3, @NonNull BiJoiner<A, B> joiner4) {
        return ifNotExists(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are true
     */
    <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull Class<B> otherClass, @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniConstraintStream} for every A where B does not exist for which the {@link BiJoiner} is
     * true (for the properties it extracts from both facts).
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner} is true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner) {
        return ifNotExists(otherStream, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2) {
        return ifNotExists(otherStream, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return ifNotExists(otherStream, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return never null, a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3,
            @NonNull BiJoiner<A, B> joiner4) {
        return ifNotExists(otherStream, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiners never null
     * @param <B> the type of the second matched fact
     * @return never null, a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    <B> @NonNull UniConstraintStream<A> ifNotExists(@NonNull UniConstraintStream<B> otherStream,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniConstraintStream} for every A where B does not exist for which the {@link BiJoiner} is
     * true (for the properties it extracts from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which the {@link BiJoiner} is true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner1,
            @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    default <B> @NonNull UniConstraintStream<A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B> joiner1, @NonNull BiJoiner<A, B> joiner2, @NonNull BiJoiner<A, B> joiner3,
            @NonNull BiJoiner<A, B> joiner4) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <B> the type of the second matched fact
     * @return a stream that matches every A where B does not exist for which all the {@link BiJoiner}s are
     *         true
     */
    <B> @NonNull UniConstraintStream<A> ifNotExistsIncludingUnassigned(@NonNull Class<B> otherClass,
            @NonNull BiJoiner<A, B>... joiners);

    /**
     * Create a new {@link UniConstraintStream} for every A, if no other A exists that does not {@link Object#equals(Object)}
     * the first.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @return a stream that matches every A where a different A does not exist
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOther(@NonNull Class<A> otherClass) {
        return ifNotExists(otherClass, Joiners.filtering(notEquals()));
    }

    /**
     * Create a new {@link UniConstraintStream} for every A, if no other A exists that does not {@link Object#equals(Object)}
     * the first, and for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @return a stream that matches every A where a different A does not exist for which the
     *         {@link BiJoiner} is true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner) {
        return ifNotExistsOther(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExistsOther(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner1,
            @NonNull BiJoiner<A, A> joiner2) {
        return ifNotExistsOther(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExistsOther(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner1,
            @NonNull BiJoiner<A, A> joiner2, @NonNull BiJoiner<A, A> joiner3) {
        return ifNotExistsOther(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExistsOther(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A> joiner1,
            @NonNull BiJoiner<A, A> joiner2, @NonNull BiJoiner<A, A> joiner3, @NonNull BiJoiner<A, A> joiner4) {
        return ifNotExistsOther(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExistsOther(Class, BiJoiner)}.
     * For performance reasons, the indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOther(@NonNull Class<A> otherClass, @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifNotExists(otherClass, allJoiners);
    }

    /**
     * Create a new {@link UniConstraintStream} for every A,
     * if no other A exists that does not {@link Object#equals(Object)} the first.
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     *
     * @return a stream that matches every A where a different A does not exist
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[0]);
    }

    /**
     * Create a new {@link UniConstraintStream} for every A,
     * if no other A exists that does not {@link Object#equals(Object)} the first,
     * and for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @return a stream that matches every A where a different A does not exist for which the
     *         {@link BiJoiner} is true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner1, @NonNull BiJoiner<A, A> joiner2) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner1, @NonNull BiJoiner<A, A> joiner2, @NonNull BiJoiner<A, A> joiner3) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A> joiner1, @NonNull BiJoiner<A, A> joiner2, @NonNull BiJoiner<A, A> joiner3,
            @NonNull BiJoiner<A, A> joiner4) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     * If multiple {@link BiJoiner}s are provided, for performance reasons,
     * the indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @return a stream that matches every A where a different A does not exist for which all the
     *         {@link BiJoiner}s are true
     */
    default @NonNull UniConstraintStream<A> ifNotExistsOtherIncludingUnassigned(@NonNull Class<A> otherClass,
            @NonNull BiJoiner<A, A>... joiners) {
        BiJoiner<A, A> otherness = Joiners.filtering(notEquals());

        @SuppressWarnings("unchecked")
        BiJoiner<A, A>[] allJoiners = Stream.concat(Arrays.stream(joiners), Stream.of(otherness))
                .toArray(BiJoiner[]::new);
        return ifNotExistsIncludingUnassigned(otherClass, allJoiners);
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsOtherIncludingUnassigned(Class)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifNotExistsOtherIncludingNullVars(Class<A> otherClass) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[0]);
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifNotExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifNotExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifNotExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2, BiJoiner<A, A> joiner3) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifNotExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2, BiJoiner<A, A> joiner3, BiJoiner<A, A> joiner4) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsOtherIncludingUnassigned(Class, BiJoiner...)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifNotExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A>... joiners) {
        return ifNotExistsOtherIncludingUnassigned(otherClass, joiners);
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    // TODO: Continue here

    /**
     * Convert the {@link UniConstraintStream} to a different {@link UniConstraintStream}, containing only a single
     * tuple, the result of applying {@link UniConstraintCollector}.
     *
     * @param collector the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <Result_> the type of a fact in the destination {@link UniConstraintStream}'s tuple
     */
    <ResultContainer_, Result_> @NonNull UniConstraintStream<Result_> groupBy(
            @NonNull UniConstraintCollector<A, ResultContainer_, Result_> collector);

    /**
     * Convert the {@link UniConstraintStream} to a {@link BiConstraintStream}, containing only a single tuple,
     * the result of applying two {@link UniConstraintCollector}s.
     *
     * @param collectorA the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorB the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainerA_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultA_> the type of the first fact in the destination {@link BiConstraintStream}'s tuple
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link BiConstraintStream}'s tuple
     */
    <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> @NonNull BiConstraintStream<ResultA_, ResultB_> groupBy(
            @NonNull UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
            @NonNull UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB);

    /**
     * Convert the {@link UniConstraintStream} to a {@link TriConstraintStream}, containing only a single tuple,
     * the result of applying three {@link UniConstraintCollector}s.
     *
     * @param collectorA the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorB the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC the collector to perform the third grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainerA_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultA_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple
     */
    <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            @NonNull TriConstraintStream<ResultA_, ResultB_, ResultC_> groupBy(
                    @NonNull UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    @NonNull UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    @NonNull UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC);

    /**
     * Convert the {@link UniConstraintStream} to a {@link QuadConstraintStream}, containing only a single tuple,
     * the result of applying four {@link UniConstraintCollector}s.
     *
     * @param collectorA the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorB the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC the collector to perform the third grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorD the collector to perform the fourth grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainerA_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     */
    <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> groupBy(
                    @NonNull UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    @NonNull UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    @NonNull UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    @NonNull UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link UniConstraintStream} to a different {@link UniConstraintStream}, containing the set of tuples
     * resulting from applying the group key mapping function on all tuples of the original stream.
     * Neither tuple of the new stream {@link Objects#equals(Object, Object)} any other.
     *
     * @param groupKeyMapping mapping function to convert each element in the stream to a different element
     * @param <GroupKey_> the type of a fact in the destination {@link UniConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     */
    <GroupKey_> @NonNull UniConstraintStream<GroupKey_> groupBy(@NonNull Function<A, GroupKey_> groupKeyMapping);

    /**
     * Convert the {@link UniConstraintStream} to a {@link BiConstraintStream}, consisting of unique tuples with two
     * facts.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of a given {@link UniConstraintCollector} applied on all incoming tuples with
     * the same first fact.
     *
     * @param groupKeyMapping function to convert the fact in the original tuple to a different fact
     * @param collector the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKey_> the type of the first fact in the destination {@link BiConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <Result_> the type of the second fact in the destination {@link BiConstraintStream}'s tuple
     */
    <GroupKey_, ResultContainer_, Result_> @NonNull BiConstraintStream<GroupKey_, Result_> groupBy(
            @NonNull Function<A, GroupKey_> groupKeyMapping,
            @NonNull UniConstraintCollector<A, ResultContainer_, Result_> collector);

    /**
     * Convert the {@link UniConstraintStream} to a {@link TriConstraintStream}, consisting of unique tuples with three
     * facts.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The remaining facts are the return value of the respective {@link UniConstraintCollector} applied on all
     * incoming tuples with the same first fact.
     *
     * @param groupKeyMapping function to convert the fact in the original tuple to a different fact
     * @param collectorB the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKey_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple
     */
    <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            @NonNull TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(
                    @NonNull Function<A, GroupKey_> groupKeyMapping,
                    @NonNull UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    @NonNull UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC);

    /**
     * Convert the {@link UniConstraintStream} to a {@link QuadConstraintStream}, consisting of unique tuples with four
     * facts.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The remaining facts are the return value of the respective {@link UniConstraintCollector} applied on all
     * incoming tuples with the same first fact.
     *
     * @param groupKeyMapping function to convert the fact in the original tuple to a different fact
     * @param collectorB the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorD the collector to perform the third grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKey_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     */
    <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_> groupBy(
                    @NonNull Function<A, GroupKey_> groupKeyMapping,
                    @NonNull UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    @NonNull UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    @NonNull UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link UniConstraintStream} to a {@link BiConstraintStream}, consisting of unique tuples with two
     * facts.
     * <p>
     * The first fact is the return value of the first group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of the second group key mapping function, applied on all incoming tuples with
     * the same first fact.
     *
     * @param groupKeyAMapping function to convert the original tuple into a first fact
     * @param groupKeyBMapping function to convert the original tuple into a second fact
     * @param <GroupKeyA_> the type of the first fact in the destination {@link BiConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link BiConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     */
    <GroupKeyA_, GroupKeyB_> @NonNull BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            @NonNull Function<A, GroupKeyA_> groupKeyAMapping, @NonNull Function<A, GroupKeyB_> groupKeyBMapping);

    /**
     * Combines the semantics of {@link #groupBy(Function, Function)} and {@link #groupBy(UniConstraintCollector)}.
     * That is, the first and second facts in the tuple follow the {@link #groupBy(Function, Function)} semantics, and
     * the third fact is the result of applying {@link UniConstraintCollector#finisher()} on all the tuples of the
     * original {@link UniConstraintStream} that belong to the group.
     *
     * @param groupKeyAMapping function to convert the original tuple into a first fact
     * @param groupKeyBMapping function to convert the original tuple into a second fact
     * @param collector the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKeyA_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <Result_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple
     */
    <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> @NonNull TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            @NonNull Function<A, GroupKeyA_> groupKeyAMapping, @NonNull Function<A, GroupKeyB_> groupKeyBMapping,
            @NonNull UniConstraintCollector<A, ResultContainer_, Result_> collector);

    /**
     * Combines the semantics of {@link #groupBy(Function, Function)} and {@link #groupBy(UniConstraintCollector)}.
     * That is, the first and second facts in the tuple follow the {@link #groupBy(Function, Function)} semantics.
     * The third fact is the result of applying the first {@link UniConstraintCollector#finisher()} on all the tuples
     * of the original {@link UniConstraintStream} that belong to the group.
     * The fourth fact is the result of applying the second {@link UniConstraintCollector#finisher()} on all the tuples
     * of the original {@link UniConstraintStream} that belong to the group
     *
     * @param groupKeyAMapping function to convert the original tuple into a first fact
     * @param groupKeyBMapping function to convert the original tuple into a second fact
     * @param collectorC the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorD the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKeyA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     */
    <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    @NonNull Function<A, GroupKeyA_> groupKeyAMapping, @NonNull Function<A, GroupKeyB_> groupKeyBMapping,
                    @NonNull UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    @NonNull UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link UniConstraintStream} to a {@link TriConstraintStream}, consisting of unique tuples with three
     * facts.
     * <p>
     * The first fact is the return value of the first group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of the second group key mapping function, applied on all incoming tuples with
     * the same first fact.
     * The third fact is the return value of the third group key mapping function, applied on all incoming tuples with
     * the same first fact.
     *
     * @param groupKeyAMapping function to convert the original tuple into a first fact
     * @param groupKeyBMapping function to convert the original tuple into a second fact
     * @param groupKeyCMapping function to convert the original tuple into a third fact
     * @param <GroupKeyA_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyC_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     */
    <GroupKeyA_, GroupKeyB_, GroupKeyC_> @NonNull TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            @NonNull Function<A, GroupKeyA_> groupKeyAMapping, @NonNull Function<A, GroupKeyB_> groupKeyBMapping,
            @NonNull Function<A, GroupKeyC_> groupKeyCMapping);

    /**
     * Combines the semantics of {@link #groupBy(Function, Function)} and {@link #groupBy(UniConstraintCollector)}.
     * That is, the first three facts in the tuple follow the {@link #groupBy(Function, Function)} semantics.
     * The final fact is the result of applying the first {@link UniConstraintCollector#finisher()} on all the tuples
     * of the original {@link UniConstraintStream} that belong to the group.
     *
     * @param groupKeyAMapping function to convert the original tuple into a first fact
     * @param groupKeyBMapping function to convert the original tuple into a second fact
     * @param groupKeyCMapping function to convert the original tuple into a third fact
     * @param collectorD the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKeyA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     */
    <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            @NonNull QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_> groupBy(
                    @NonNull Function<A, GroupKeyA_> groupKeyAMapping, @NonNull Function<A, GroupKeyB_> groupKeyBMapping,
                    @NonNull Function<A, GroupKeyC_> groupKeyCMapping,
                    @NonNull UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link UniConstraintStream} to a {@link QuadConstraintStream}, consisting of unique tuples with four
     * facts.
     * <p>
     * The first fact is the return value of the first group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of the second group key mapping function, applied on all incoming tuples with
     * the same first fact.
     * The third fact is the return value of the third group key mapping function, applied on all incoming tuples with
     * the same first fact.
     * The fourth fact is the return value of the fourth group key mapping function, applied on all incoming tuples with
     * the same first fact.
     *
     * @param groupKeyAMapping * calling {@code map(Person::getAge)} on such stream will produce a stream of {@link Integer}s
     *        * {@code [20, 25, 30]},
     * @param groupKeyBMapping function to convert the original tuple into a second fact
     * @param groupKeyCMapping function to convert the original tuple into a third fact
     * @param groupKeyDMapping function to convert the original tuple into a fourth fact
     * @param <GroupKeyA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     */
    <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            @NonNull QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> groupBy(
                    @NonNull Function<A, GroupKeyA_> groupKeyAMapping, @NonNull Function<A, GroupKeyB_> groupKeyBMapping,
                    @NonNull Function<A, GroupKeyC_> groupKeyCMapping, @NonNull Function<A, GroupKeyD_> groupKeyDMapping);

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
     * Not following this recommendation creates a constraint stream with duplicate tuples,
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
     * Simple example: assuming a constraint stream of tuples of {@code Person}s
     * {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30)]},
     * calling {@code map(Person::getAge)} on such stream will produce a stream of {@link Integer}s
     * {@code [20, 25, 30]},
     *
     * <p>
     * Example with a non-bijective mapping function: assuming a constraint stream of tuples of {@code Person}s
     * {@code [Ann(age = 20), Beth(age = 25), Cathy(age = 30), David(age = 30), Eric(age = 20)]},
     * calling {@code map(Person::getAge)} on such stream will produce a stream of {@link Integer}s
     * {@code [20, 25, 30, 30, 20]}.
     *
     * <p>
     * Use with caution,
     * as the increased memory allocation rates coming from tuple creation may negatively affect performance.
     *
     * @param mapping function to convert the original tuple into the new tuple
     * @param <ResultA_> the type of the only fact in the resulting {@link UniConstraintStream}'s tuple
     */
    <ResultA_> @NonNull UniConstraintStream<ResultA_> map(@NonNull Function<A, ResultA_> mapping);

    /**
     * As defined by {@link #map(Function)}, only resulting in {@link BiConstraintStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiConstraintStream}'s tuple
     */
    <ResultA_, ResultB_> @NonNull BiConstraintStream<ResultA_, ResultB_> map(@NonNull Function<A, ResultA_> mappingA,
            @NonNull Function<A, ResultB_> mappingB);

    /**
     * As defined by {@link #map(Function)}, only resulting in {@link TriConstraintStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param mappingC function to convert the original tuple into the third fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link TriConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link TriConstraintStream}'s tuple
     * @param <ResultC_> the type of the third fact in the resulting {@link TriConstraintStream}'s tuple
     */
    <ResultA_, ResultB_, ResultC_> @NonNull TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            @NonNull Function<A, ResultA_> mappingA,
            @NonNull Function<A, ResultB_> mappingB, @NonNull Function<A, ResultC_> mappingC);

    /**
     * As defined by {@link #map(Function)}, only resulting in {@link QuadConstraintStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param mappingC function to convert the original tuple into the third fact of a new tuple
     * @param mappingD function to convert the original tuple into the fourth fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link QuadConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link QuadConstraintStream}'s tuple
     * @param <ResultC_> the type of the third fact in the resulting {@link QuadConstraintStream}'s tuple
     * @param <ResultD_> the type of the third fact in the resulting {@link QuadConstraintStream}'s tuple
     */
    <ResultA_, ResultB_, ResultC_, ResultD_> @NonNull QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            @NonNull Function<A, ResultA_> mappingA, @NonNull Function<A, ResultB_> mappingB,
            @NonNull Function<A, ResultC_> mappingC,
            @NonNull Function<A, ResultD_> mappingD);

    /**
     * Takes each tuple and applies a mapping on it, which turns the tuple into a {@link Iterable}.
     * Returns a constraint stream consisting of contents of those iterables.
     * This may produce a stream with duplicate tuples.
     * See {@link #distinct()} for details.
     *
     * <p>
     * In cases where the original tuple is already an {@link Iterable},
     * use {@link Function#identity()} as the argument.
     *
     * <p>
     * Simple example: assuming a constraint stream of tuples of {@code Person}s
     * {@code [Ann(roles = [USER, ADMIN]]), Beth(roles = [USER]), Cathy(roles = [ADMIN, AUDITOR])]},
     * calling {@code flattenLast(Person::getRoles)} on such stream will produce
     * a stream of {@code [USER, ADMIN, USER, ADMIN, AUDITOR]}.
     *
     * @param mapping function to convert the original tuple into {@link Iterable}.
     *        For performance, returning an implementation of {@link java.util.Collection} is preferred.
     * @param <ResultA_> the type of facts in the resulting tuples.
     *        It is recommended that this type be deeply immutable.
     *        Not following this recommendation may lead to hard-to-debug hashing issues down the stream,
     *        especially if this value is ever used as a group key.
     */
    <ResultA_> @NonNull UniConstraintStream<ResultA_> flattenLast(@NonNull Function<A, Iterable<ResultA_>> mapping);

    /**
     * Transforms the stream in such a way that all the tuples going through it are distinct.
     * (No two tuples will {@link Object#equals(Object) equal}.)
     *
     * <p>
     * By default, tuples going through a constraint stream are distinct.
     * However, operations such as {@link #map(Function)} may create a stream which breaks that promise.
     * By calling this method on such a stream,
     * duplicate copies of the same tuple will be omitted at a performance cost.
     */
    @NonNull
    UniConstraintStream<A> distinct();

    /**
     * Returns a new {@link UniConstraintStream} containing all the tuples of both this {@link UniConstraintStream}
     * and the provided {@link UniConstraintStream}.
     * Tuples in both this {@link UniConstraintStream} and the provided {@link UniConstraintStream}
     * will appear at least twice.
     *
     * <p>
     * For instance, if this stream consists of {@code [A, B, C]}
     * and the other stream consists of {@code [C, D, E]},
     * {@code this.concat(other)} will consist of {@code [A, B, C, C, D, E]}.
     * This operation can be thought of as an or between streams.
     */
    @NonNull
    UniConstraintStream<A> concat(@NonNull UniConstraintStream<A> otherStream);

    /**
     * Returns a new {@link BiConstraintStream} containing all the tuples of both this {@link UniConstraintStream}
     * and the provided {@link BiConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [A, B, C]}
     * and the other stream consists of {@code [(C1, C2), (D1, D2), (E1, E2)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A, null), (B, null), (C, null), (C1, C2), (D1, D2), (E1, E2)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     */
    default <B> @NonNull BiConstraintStream<A, B> concat(@NonNull BiConstraintStream<A, B> otherStream) {
        return concat(otherStream, uniConstantNull());
    }

    /**
     * Returns a new {@link BiConstraintStream} containing all the tuples of both this {@link UniConstraintStream}
     * and the provided {@link BiConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by the result of the padding function.
     *
     * <p>
     * For instance, if this stream consists of {@code [A, B, C]}
     * and the other stream consists of {@code [(C1, C2), (D1, D2), (E1, E2)]},
     * {@code this.concat(other, a -> null)} will consist of
     * {@code [(A, null), (B, null), (C, null), (C1, C2), (D1, D2), (E1, E2)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param paddingFunctionB function to find the padding for the second fact
     */
    <B> @NonNull BiConstraintStream<A, B> concat(@NonNull BiConstraintStream<A, B> otherStream,
            @NonNull Function<A, B> paddingFunctionB);

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link UniConstraintStream}
     * and the provided {@link TriConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [A, B, C]}
     * and the other stream consists of {@code [(C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A, null), (B, null), (C, null), (C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     */
    default <B, C> @NonNull TriConstraintStream<A, B, C> concat(@NonNull TriConstraintStream<A, B, C> otherStream) {
        return concat(otherStream, uniConstantNull(), uniConstantNull());
    }

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link UniConstraintStream}
     * and the provided {@link TriConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by the result of the padding functions.
     *
     * <p>
     * For instance, if this stream consists of {@code [A, B, C]}
     * and the other stream consists of {@code [(C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]},
     * {@code this.concat(other, a -> null, a -> null)} will consist of
     * {@code [(A, null), (B, null), (C, null), (C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param paddingFunctionB function to find the padding for the second fact
     * @param paddingFunctionC function to find the padding for the third fact
     */
    <B, C> @NonNull TriConstraintStream<A, B, C> concat(@NonNull TriConstraintStream<A, B, C> otherStream,
            @NonNull Function<A, B> paddingFunctionB,
            @NonNull Function<A, C> paddingFunctionC);

    /**
     * Returns a new {@link QuadConstraintStream} containing all the tuples of both this {@link UniConstraintStream}
     * and the provided {@link QuadConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [A, B, C]}
     * and the other stream consists of {@code [(C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A, null), (B, null), (C, null), (C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     */
    default <B, C, D> @NonNull QuadConstraintStream<A, B, C, D> concat(@NonNull QuadConstraintStream<A, B, C, D> otherStream) {
        return concat(otherStream, uniConstantNull(), uniConstantNull(), uniConstantNull());
    }

    /**
     * Returns a new {@link QuadConstraintStream} containing all the tuples of both this {@link UniConstraintStream}
     * and the provided {@link QuadConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by the result of the padding functions.
     *
     * <p>
     * For instance, if this stream consists of {@code [A, B, C]}
     * and the other stream consists of {@code [(C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]},
     * {@code this.concat(other, a -> null, a -> null, a -> null)} will consist of
     * {@code [(A, null), (B, null), (C, null), (C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param paddingFunctionB function to find the padding for the second fact
     * @param paddingFunctionC function to find the padding for the third fact
     * @param paddingFunctionD function to find the padding for the fourth fact
     */
    <B, C, D> @NonNull QuadConstraintStream<A, B, C, D> concat(@NonNull QuadConstraintStream<A, B, C, D> otherStream,
            @NonNull Function<A, B> paddingFunctionB, @NonNull Function<A, C> paddingFunctionC,
            @NonNull Function<A, D> paddingFunctionD);

    // ************************************************************************
    // expand
    // ************************************************************************

    /**
     * Adds a fact to the end of the tuple, increasing the cardinality of the stream.
     * Useful for storing results of expensive computations on the original tuple.
     *
     * <p>
     * Use with caution,
     * as the benefits of caching computation may be outweighed by increased memory allocation rates
     * coming from tuple creation.
     * If more than one fact is to be added,
     * prefer {@link #expand(Function, Function)} or {@link #expand(Function, Function, Function)}.
     *
     * @param mapping function to produce the new fact from the original tuple
     * @param <ResultB_> type of the final fact of the new tuple
     */
    <ResultB_> @NonNull BiConstraintStream<A, ResultB_> expand(@NonNull Function<A, ResultB_> mapping);

    /**
     * Adds two facts to the end of the tuple, increasing the cardinality of the stream.
     * Useful for storing results of expensive computations on the original tuple.
     *
     * <p>
     * Use with caution,
     * as the benefits of caching computation may be outweighed by increased memory allocation rates
     * coming from tuple creation.
     * If more than two facts are to be added,
     * prefer {@link #expand(Function, Function, Function)}.
     *
     * @param mappingB function to produce the new second fact from the original tuple
     * @param mappingC function to produce the new third fact from the original tuple
     * @param <ResultB_> type of the second fact of the new tuple
     * @param <ResultC_> type of the third fact of the new tuple
     */
    <ResultB_, ResultC_> @NonNull TriConstraintStream<A, ResultB_, ResultC_> expand(@NonNull Function<A, ResultB_> mappingB,
            @NonNull Function<A, ResultC_> mappingC);

    /**
     * Adds three facts to the end of the tuple, increasing the cardinality of the stream.
     * Useful for storing results of expensive computations on the original tuple.
     *
     * <p>
     * Use with caution,
     * as the benefits of caching computation may be outweighed by increased memory allocation rates
     * coming from tuple creation.
     *
     * @param mappingB function to produce the new second fact from the original tuple
     * @param mappingC function to produce the new third fact from the original tuple
     * @param mappingD function to produce the new final fact from the original tuple
     * @param <ResultB_> type of the second fact of the new tuple
     * @param <ResultC_> type of the third fact of the new tuple
     * @param <ResultD_> type of the final fact of the new tuple
     */
    <ResultB_, ResultC_, ResultD_> @NonNull QuadConstraintStream<A, ResultB_, ResultC_, ResultD_> expand(
            @NonNull Function<A, ResultB_> mappingB,
            @NonNull Function<A, ResultC_> mappingC, @NonNull Function<A, ResultD_> mappingD);

    // ************************************************************************
    // complement
    // ************************************************************************

    /**
     * Adds to the stream all instances of a given class which are not yet present in it.
     * These instances must be present in the solution,
     * which means the class needs to be either a planning entity or a problem fact.
     */
    default @NonNull UniConstraintStream<A> complement(@NonNull Class<A> otherClass) {
        var firstStream = this;
        var secondStream = getConstraintFactory().forEach(otherClass)
                .ifNotExists(firstStream, Joiners.equal());
        return firstStream.concat(secondStream);
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    /**
     * As defined by {@link #penalize(Score, ToIntFunction)}, where the match weight is one (1).
     *
     */
    default <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> penalize(@NonNull Score_ constraintWeight) {
        return penalize(constraintWeight, uniConstantOne());
    }

    /**
     * As defined by {@link #penalizeLong(Score, ToLongFunction)}, where the match weight is one (1).
     *
     */
    default <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_>
            penalizeLong(@NonNull Score_ constraintWeight) {
        return penalizeLong(constraintWeight, uniConstantOneLong());
    }

    /**
     * As defined by {@link #penalizeBigDecimal(Score, Function)}, where the match weight is one (1).
     *
     */
    default <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_>
            penalizeBigDecimal(@NonNull Score_ constraintWeight) {
        return penalizeBigDecimal(constraintWeight, uniConstantOneBigDecimal());
    }

    /**
     * Applies a negative {@link Score} impact,
     * subtracting the constraintWeight multiplied by the match weight,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight specified here can be overridden using {@link ConstraintWeightOverrides}
     * on the {@link PlanningSolution}-annotated class
     * <p>
     * For non-int {@link Score} types use {@link #penalizeLong(Score, ToLongFunction)} or
     * {@link #penalizeBigDecimal(Score, Function)} instead.
     *
     * @param matchWeigher the result of this function (matchWeight) is multiplied by the constraintWeight
     */
    <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> penalize(@NonNull Score_ constraintWeight,
            @NonNull ToIntFunction<A> matchWeigher);

    /**
     * As defined by {@link #penalize(Score, ToIntFunction)}, with a penalty of type long.
     */
    <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> penalizeLong(@NonNull Score_ constraintWeight,
            @NonNull ToLongFunction<A> matchWeigher);

    /**
     * As defined by {@link #penalize(Score, ToIntFunction)}, with a penalty of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> penalizeBigDecimal(@NonNull Score_ constraintWeight,
            @NonNull Function<A, BigDecimal> matchWeigher);

    /**
     * Negatively impacts the {@link Score},
     * subtracting the {@link ConstraintWeight} for each match,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     *
     * @return never null
     * @deprecated Prefer {@link #penalize(Score)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    default UniConstraintBuilder<A, ?> penalizeConfigurable() {
        return penalizeConfigurable(uniConstantOne());
    }

    /**
     * Negatively impacts the {@link Score},
     * subtracting the {@link ConstraintWeight} multiplied by match weight for each match,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     *
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #penalize(Score, ToIntFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> penalizeConfigurable(ToIntFunction<A> matchWeigher);

    /**
     * As defined by {@link #penalizeConfigurable(ToIntFunction)}, with a penalty of type long.
     * <p>
     *
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> penalizeConfigurableLong(ToLongFunction<A> matchWeigher);

    /**
     * As defined by {@link #penalizeConfigurable(ToIntFunction)}, with a penalty of type {@link BigDecimal}.
     * <p>
     *
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> penalizeConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntFunction)}, where the match weight is one (1).
     */
    default <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> reward(@NonNull Score_ constraintWeight) {
        return reward(constraintWeight, uniConstantOne());
    }

    /**
     * Applies a positive {@link Score} impact,
     * adding the constraintWeight multiplied by the match weight,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight specified here can be overridden using {@link ConstraintWeightOverrides}
     * on the {@link PlanningSolution}-annotated class
     * <p>
     * For non-int {@link Score} types use {@link #rewardLong(Score, ToLongFunction)} or
     * {@link #rewardBigDecimal(Score, Function)} instead.
     *
     * @param matchWeigher the result of this function (matchWeight) is multiplied by the constraintWeight
     */
    <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> reward(@NonNull Score_ constraintWeight,
            @NonNull ToIntFunction<A> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntFunction)}, with a penalty of type long.
     */
    @NonNull
    <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> rewardLong(@NonNull Score_ constraintWeight,
            @NonNull ToLongFunction<A> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntFunction)}, with a penalty of type {@link BigDecimal}.
     */
    @NonNull
    <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> rewardBigDecimal(@NonNull Score_ constraintWeight,
            @NonNull Function<A, BigDecimal> matchWeigher);

    /**
     * Positively impacts the {@link Score},
     * adding the {@link ConstraintWeight} for each match,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     *
     * @return never null
     * @deprecated Prefer {@link #reward(Score)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    default UniConstraintBuilder<A, ?> rewardConfigurable() {
        return rewardConfigurable(uniConstantOne());
    }

    /**
     * Positively impacts the {@link Score},
     * adding the {@link ConstraintWeight} multiplied by match weight for each match,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     *
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #reward(Score, ToIntFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> rewardConfigurable(ToIntFunction<A> matchWeigher);

    /**
     * As defined by {@link #rewardConfigurable(ToIntFunction)}, with a penalty of type long.
     *
     * @deprecated Prefer {@link #rewardLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> rewardConfigurableLong(ToLongFunction<A> matchWeigher);

    /**
     * As defined by {@link #rewardConfigurable(ToIntFunction)}, with a penalty of type {@link BigDecimal}.
     *
     * @deprecated Prefer {@link #rewardBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> rewardConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher);

    /**
     * Positively or negatively impacts the {@link Score} by the constraintWeight for each match
     * and returns a builder to apply optional constraint properties.
     * <p>
     * Use {@code penalize(...)} or {@code reward(...)} instead, unless this constraint can both have positive and
     * negative weights.
     */
    default <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> impact(@NonNull Score_ constraintWeight) {
        return impact(constraintWeight, uniConstantOne());
    }

    /**
     * Positively or negatively impacts the {@link Score} by constraintWeight multiplied by matchWeight for each match
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight specified here can be overridden using {@link ConstraintWeightOverrides}
     * on the {@link PlanningSolution}-annotated class
     * <p>
     * Use {@code penalize(...)} or {@code reward(...)} instead, unless this constraint can both have positive and
     * negative weights.
     *
     * @param matchWeigher the result of this function (matchWeight) is multiplied by the constraintWeight
     */
    <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> impact(@NonNull Score_ constraintWeight,
            @NonNull ToIntFunction<A> matchWeigher);

    /**
     * As defined by {@link #impact(Score, ToIntFunction)}, with an impact of type long.
     */
    <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> impactLong(@NonNull Score_ constraintWeight,
            @NonNull ToLongFunction<A> matchWeigher);

    /**
     * As defined by {@link #impact(Score, ToIntFunction)}, with an impact of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> @NonNull UniConstraintBuilder<A, Score_> impactBigDecimal(@NonNull Score_ constraintWeight,
            @NonNull Function<A, BigDecimal> matchWeigher);

    /**
     * Positively impacts the {@link Score} by the {@link ConstraintWeight} for each match,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     *
     * @return never null
     * @deprecated Prefer {@link #impact(Score)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    default UniConstraintBuilder<A, ?> impactConfigurable() {
        return impactConfigurable(uniConstantOne());
    }

    /**
     * Positively impacts the {@link Score} by the {@link ConstraintWeight} multiplied by match weight for each match,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the {@link ConstraintConfiguration},
     * so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     *
     * @return never null
     * @deprecated Prefer {@link #impact(Score, ToIntFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> impactConfigurable(ToIntFunction<A> matchWeigher);

    /**
     * As defined by {@link #impactConfigurable(ToIntFunction)}, with an impact of type long.
     *
     * @deprecated Prefer {@link #impactLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> impactConfigurableLong(ToLongFunction<A> matchWeigher);

    /**
     * As defined by {@link #impactConfigurable(ToIntFunction)}, with an impact of type BigDecimal.
     * <p>
     *
     * @deprecated Prefer {@link #impactBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    UniConstraintBuilder<A, ?> impactConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher);

    // ************************************************************************
    // Deprecated declarations
    // ************************************************************************

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner1,
            BiJoiner<A, B> joiner2) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner1,
            BiJoiner<A, B> joiner2, BiJoiner<A, B> joiner3) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner1,
            BiJoiner<A, B> joiner2, BiJoiner<A, B> joiner3, BiJoiner<A, B> joiner4) {
        return ifExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, BiJoiner...)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return ifExistsIncludingUnassigned(otherClass, joiners);
    }

    /**
     * @deprecated Prefer {@link #ifExistsOtherIncludingUnassigned(Class)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifExistsOtherIncludingNullVars(Class<A> otherClass) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[0]);
    }

    /**
     * @deprecated Prefer {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2, BiJoiner<A, A> joiner3) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner, BiJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2, BiJoiner<A, A> joiner3, BiJoiner<A, A> joiner4) {
        return ifExistsOtherIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsOtherIncludingUnassigned(Class, BiJoiner...)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default UniConstraintStream<A> ifExistsOtherIncludingNullVars(Class<A> otherClass, BiJoiner<A, A>... joiners) {
        return ifExistsOtherIncludingUnassigned(otherClass, joiners);
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner)}
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifNotExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner, BiJoiner)}
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifNotExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner1,
            BiJoiner<A, B> joiner2) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner)}
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifNotExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner1,
            BiJoiner<A, B> joiner2, BiJoiner<A, B> joiner3) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner, BiJoiner, BiJoiner, BiJoiner)}
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifNotExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B> joiner1,
            BiJoiner<A, B> joiner2, BiJoiner<A, B> joiner3, BiJoiner<A, B> joiner4) {
        return ifNotExistsIncludingUnassigned(otherClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, BiJoiner...)}
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <B> UniConstraintStream<A> ifNotExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return ifNotExistsIncludingUnassigned(otherClass, joiners);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     * <p>
     * For non-int {@link Score} types use {@link #penalizeLong(String, Score, ToLongFunction)} or
     * {@link #penalizeBigDecimal(String, Score, Function)} instead.
     *
     * @deprecated Prefer {@link #penalize(Score, ToIntFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalize(String constraintName, Score<?> constraintWeight, ToIntFunction<A> matchWeigher) {
        return penalize((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalize(String, Score, ToIntFunction)}.
     *
     * @deprecated Prefer {@link #penalize(Score, ToIntFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalize(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntFunction<A> matchWeigher) {
        return penalize((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     *
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeLong(String constraintName, Score<?> constraintWeight, ToLongFunction<A> matchWeigher) {
        return penalizeLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeLong(String, Score, ToLongFunction)}.
     *
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongFunction<A> matchWeigher) {
        return penalizeLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     *
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, Function)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeBigDecimal(String constraintName, Score<?> constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return penalizeBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeBigDecimal(String, Score, Function)}.
     *
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, Function)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return penalizeBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #penalizeConfigurable(String)}.
     * <p>
     * For non-int {@link Score} types use {@link #penalizeConfigurableLong(String, ToLongFunction)} or
     * {@link #penalizeConfigurableBigDecimal(String, Function)} instead.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #penalize(Score)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurable(String constraintName, ToIntFunction<A> matchWeigher) {
        return penalizeConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurable(String, ToIntFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalize(Score)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurable(String constraintPackage, String constraintName, ToIntFunction<A> matchWeigher) {
        return penalizeConfigurable(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #penalizeConfigurable(String)}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableLong(String constraintName, ToLongFunction<A> matchWeigher) {
        return penalizeConfigurableLong(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurableLong(String, ToLongFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableLong(String constraintPackage, String constraintName,
            ToLongFunction<A> matchWeigher) {
        return penalizeConfigurableLong(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #penalizeConfigurable(String)}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableBigDecimal(String constraintName, Function<A, BigDecimal> matchWeigher) {
        return penalizeConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurableBigDecimal(String, Function)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableBigDecimal(String constraintPackage, String constraintName,
            Function<A, BigDecimal> matchWeigher) {
        return penalizeConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     * <p>
     * For non-int {@link Score} types use {@link #rewardLong(String, Score, ToLongFunction)} or
     * {@link #rewardBigDecimal(String, Score, Function)} instead.
     *
     * @deprecated Prefer {@link #reward(Score, ToIntFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint reward(String constraintName, Score<?> constraintWeight, ToIntFunction<A> matchWeigher) {
        return reward((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #reward(String, Score, ToIntFunction)}.
     *
     * @deprecated Prefer {@link #reward(Score, ToIntFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint reward(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntFunction<A> matchWeigher) {
        return reward((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     *
     * @deprecated Prefer {@link #rewardLong(Score, ToLongFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardLong(String constraintName, Score<?> constraintWeight, ToLongFunction<A> matchWeigher) {
        return rewardLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardLong(String, Score, ToLongFunction)}.
     *
     * @deprecated Prefer {@link #rewardLong(Score, ToLongFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongFunction<A> matchWeigher) {
        return rewardLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     *
     * @deprecated Prefer {@link #rewardBigDecimal(Score, Function)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardBigDecimal(String constraintName, Score<?> constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return rewardBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardBigDecimal(String, Score, Function)}.
     *
     * @deprecated Prefer {@link #rewardBigDecimal(Score, Function)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return rewardBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #rewardConfigurable(String)}.
     * <p>
     * For non-int {@link Score} types use {@link #rewardConfigurableLong(String, ToLongFunction)} or
     * {@link #rewardConfigurableBigDecimal(String, Function)} instead.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #reward(Score, ToIntFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurable(String constraintName, ToIntFunction<A> matchWeigher) {
        return rewardConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurable(String, ToIntFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #reward(Score, ToIntFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurable(String constraintPackage, String constraintName, ToIntFunction<A> matchWeigher) {
        return rewardConfigurable(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #rewardConfigurable(String)}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #rewardLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableLong(String constraintName, ToLongFunction<A> matchWeigher) {
        return rewardConfigurableLong(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurableLong(String, ToLongFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #rewardLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableLong(String constraintPackage, String constraintName, ToLongFunction<A> matchWeigher) {
        return rewardConfigurableLong(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #rewardConfigurable(String)}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #rewardBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableBigDecimal(String constraintName, Function<A, BigDecimal> matchWeigher) {
        return rewardConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurableBigDecimal(String, Function)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #rewardBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableBigDecimal(String constraintPackage, String constraintName,
            Function<A, BigDecimal> matchWeigher) {
        return rewardConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #impact(String, Score)}.
     * <p>
     * Use {@code penalize(...)} or {@code reward(...)} instead, unless this constraint can both have positive and negative
     * weights.
     * <p>
     * For non-int {@link Score} types use {@link #impactLong(String, Score, ToLongFunction)} or
     * {@link #impactBigDecimal(String, Score, Function)} instead.
     *
     * @deprecated Prefer {@link #impact(Score, ToIntFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impact(String constraintName, Score<?> constraintWeight, ToIntFunction<A> matchWeigher) {
        return impact((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impact(String, Score, ToIntFunction)}.
     *
     * @deprecated Prefer {@link #impact(Score, ToIntFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impact(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntFunction<A> matchWeigher) {
        return impact((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #impact(String, Score)}.
     * <p>
     * Use {@code penalizeLong(...)} or {@code rewardLong(...)} instead, unless this constraint can both have positive
     * and negative weights.
     *
     * @deprecated Prefer {@link #impactLong(Score, ToLongFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactLong(String constraintName, Score<?> constraintWeight, ToLongFunction<A> matchWeigher) {
        return impactLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactLong(String, Score, ToLongFunction)}.
     *
     * @deprecated Prefer {@link #impactLong(Score, ToLongFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongFunction<A> matchWeigher) {
        return impactLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #impact(String, Score)}.
     * <p>
     * Use {@code penalizeBigDecimal(...)} or {@code rewardBigDecimal(...)} instead, unless this constraint can both
     * have positive and negative weights.
     *
     * @deprecated Prefer {@link #impactBigDecimal(Score, Function)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactBigDecimal(String constraintName, Score<?> constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return impactBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactBigDecimal(String, Score, Function)}.
     *
     * @deprecated Prefer {@link #impactBigDecimal(Score, Function)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return impactBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the {@link ConstraintWeight} multiplied by the match weight.
     * <p>
     * Use {@code penalizeConfigurable(...)} or {@code rewardConfigurable(...)} instead, unless this constraint can both
     * have positive and negative weights.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the
     * {@link ConstraintConfiguration}, so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     * <p>
     * For non-int {@link Score} types use {@link #impactConfigurableLong(String, ToLongFunction)} or
     * {@link #impactConfigurableBigDecimal(String, Function)} instead.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to
     * {@link ConstraintConfiguration#constraintPackage()}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #impact(Score, ToIntFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurable(String constraintName, ToIntFunction<A> matchWeigher) {
        return impactConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactConfigurable(String, ToIntFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impact(Score, ToIntFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurable(String constraintPackage, String constraintName, ToIntFunction<A> matchWeigher) {
        return impactConfigurable(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the {@link ConstraintWeight} multiplied by the match weight.
     * <p>
     * Use {@code penalizeConfigurableLong(...)} or {@code rewardConfigurableLong(...)} instead, unless this constraint
     * can both have positive and negative weights.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the
     * {@link ConstraintConfiguration}, so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to
     * {@link ConstraintConfiguration#constraintPackage()}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #impactLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    default Constraint impactConfigurableLong(String constraintName, ToLongFunction<A> matchWeigher) {
        return impactConfigurableLong(matchWeigher)
                .asConstraint(constraintName);

    }

    /**
     * As defined by {@link #impactConfigurableLong(String, ToLongFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impactLong(Score, ToLongFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableLong(String constraintPackage, String constraintName, ToLongFunction<A> matchWeigher) {
        return impactConfigurableLong(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the {@link ConstraintWeight} multiplied by the match weight.
     * <p>
     * Use {@code penalizeConfigurableBigDecimal(...)} or {@code rewardConfigurableBigDecimal(...)} instead, unless this
     * constraint can both have positive and negative weights.
     * <p>
     * The constraintWeight comes from an {@link ConstraintWeight} annotated member on the
     * {@link ConstraintConfiguration}, so end users can change the constraint weights dynamically.
     * This constraint may be deactivated if the {@link ConstraintWeight} is zero.
     * <p>
     * The {@link ConstraintRef#packageName() constraint package} defaults to
     * {@link ConstraintConfiguration#constraintPackage()}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #impactBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableBigDecimal(String constraintName, Function<A, BigDecimal> matchWeigher) {
        return impactConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);

    }

    /**
     * As defined by {@link #impactConfigurableBigDecimal(String, Function)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impactBigDecimal(Score, Function)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableBigDecimal(String constraintPackage, String constraintName,
            Function<A, BigDecimal> matchWeigher) {
        return impactConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

}
