package ai.timefold.solver.core.api.score.stream.bi;

import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.biConstantNull;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.biConstantOne;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.biConstantOneBigDecimal;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.biConstantOneLong;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.uniConstantNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

/**
 * A {@link ConstraintStream} that matches two facts.
 *
 * @param <A> the type of the first fact in the tuple.
 * @param <B> the type of the second fact in the tuple.
 * @see ConstraintStream
 */
public interface BiConstraintStream<A, B> extends ConstraintStream {

    // ************************************************************************
    // Filter
    // ************************************************************************

    /**
     * Exhaustively test each tuple of facts against the {@link BiPredicate}
     * and match if {@link BiPredicate#test(Object, Object)} returns true.
     * <p>
     * Important: This is slower and less scalable than {@link UniConstraintStream#join(UniConstraintStream, BiJoiner)}
     * with a proper {@link BiJoiner} predicate (such as {@link Joiners#equal(Function, Function)},
     * because the latter applies hashing and/or indexing, so it doesn't create every combination just to filter it out.
     *
     * @param predicate never null
     * @return never null
     */
    BiConstraintStream<A, B> filter(BiPredicate<A, B> predicate);

    // ************************************************************************
    // Join
    // ************************************************************************

    /**
     * Create a new {@link TriConstraintStream} for every combination of [A, B] and C.
     * <p>
     * Important: {@link TriConstraintStream#filter(TriPredicate)} Filtering} this is slower and less scalable
     * than a {@link #join(UniConstraintStream, TriJoiner)},
     * because it doesn't apply hashing and/or indexing on the properties,
     * so it creates and checks every combination of [A, B] and C.
     *
     * @param otherStream never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C
     */
    default <C> TriConstraintStream<A, B, C> join(UniConstraintStream<C> otherStream) {
        return join(otherStream, new TriJoiner[0]);
    }

    /**
     * Create a new {@link TriConstraintStream} for every combination of [A, B] and C for which the {@link TriJoiner}
     * is true (for the properties it extracts from both facts).
     * <p>
     * Important: This is faster and more scalable than a {@link #join(UniConstraintStream) join}
     * followed by a {@link TriConstraintStream#filter(TriPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of [A, B] and C.
     *
     * @param otherStream never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which the {@link TriFunction} is
     *         true
     */
    default <C> TriConstraintStream<A, B, C> join(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner) {
        return join(otherStream, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    default <C> TriConstraintStream<A, B, C> join(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return join(otherStream, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    default <C> TriConstraintStream<A, B, C> join(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return join(otherStream, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    default <C> TriConstraintStream<A, B, C> join(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return join(otherStream, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    <C> TriConstraintStream<A, B, C> join(UniConstraintStream<C> otherStream, TriJoiner<A, B, C>... joiners);

    /**
     * Create a new {@link TriConstraintStream} for every combination of [A, B] and C.
     * <p>
     * Important: {@link TriConstraintStream#filter(TriPredicate)} Filtering} this is slower and less scalable
     * than a {@link #join(Class, TriJoiner)},
     * because it doesn't apply hashing and/or indexing on the properties,
     * so it creates and checks every combination of [A, B] and C.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different range of C may be selected.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     * <p>
     * This method is syntactic sugar for {@link #join(UniConstraintStream)}.
     *
     * @param otherClass never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C
     */
    default <C> TriConstraintStream<A, B, C> join(Class<C> otherClass) {
        return join(otherClass, new TriJoiner[0]);
    }

    /**
     * Create a new {@link TriConstraintStream} for every combination of [A, B] and C for which the {@link TriJoiner}
     * is true (for the properties it extracts from both facts).
     * <p>
     * Important: This is faster and more scalable than a {@link #join(Class, TriJoiner) join}
     * followed by a {@link TriConstraintStream#filter(TriPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of [A, B] and C.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)}, a different range of C may be selected.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     * <p>
     * This method is syntactic sugar for {@link #join(UniConstraintStream, TriJoiner)}.
     * <p>
     * This method has overloaded methods with multiple {@link TriJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which the {@link TriJoiner} is
     *         true
     */
    default <C> TriConstraintStream<A, B, C> join(Class<C> otherClass, TriJoiner<A, B, C> joiner) {
        return join(otherClass, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    default <C> TriConstraintStream<A, B, C> join(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return join(otherClass, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    default <C> TriConstraintStream<A, B, C> join(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return join(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    default <C> TriConstraintStream<A, B, C> join(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return join(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #join(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every combination of [A, B] and C for which all the
     *         {@link TriJoiner joiners} are true
     */
    <C> TriConstraintStream<A, B, C> join(Class<C> otherClass, TriJoiner<A, B, C>... joiners);

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    /**
     * Create a new {@link BiConstraintStream} for every pair of A and B where C exists for which the {@link TriJoiner}
     * is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link TriJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}
     *         is true
     */
    default <C> BiConstraintStream<A, B> ifExists(Class<C> otherClass, TriJoiner<A, B, C> joiner) {
        return ifExists(otherClass, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExists(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifExists(otherClass, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExists(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifExists(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExists(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifExists(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link TriJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    <C> BiConstraintStream<A, B> ifExists(Class<C> otherClass, TriJoiner<A, B, C>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every pair of A and B where C exists for which the {@link TriJoiner}
     * is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link TriJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}
     *         is true
     */
    default <C> BiConstraintStream<A, B> ifExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner) {
        return ifExists(otherStream, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifExists(otherStream, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifExists(otherStream, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifExists(otherStream, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link TriJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    <C> BiConstraintStream<A, B> ifExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every pair of A and B where C exists for which the {@link TriJoiner}
     * is true (for the properties it extracts from the facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link TriJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}
     *         is true
     */
    default <C> BiConstraintStream<A, B> ifExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExistsIncludingNullVars(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    default <C> BiConstraintStream<A, B> ifExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link TriJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C exists for which the {@link TriJoiner}s
     *         are true
     */
    <C> BiConstraintStream<A, B> ifExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every pair of A and B where C does not exist for which the
     * {@link TriJoiner} is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link TriJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner} is true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(Class<C> otherClass, TriJoiner<A, B, C> joiner) {
        return ifNotExists(otherClass, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifNotExists(otherClass, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifNotExists(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifNotExists(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link TriJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    <C> BiConstraintStream<A, B> ifNotExists(Class<C> otherClass, TriJoiner<A, B, C>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every pair of A and B where C does not exist for which the
     * {@link TriJoiner} is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link TriJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner} is true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner) {
        return ifNotExists(otherStream, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifNotExists(otherStream, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifNotExists(otherStream, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifNotExists(otherStream, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link TriJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    <C> BiConstraintStream<A, B> ifNotExists(UniConstraintStream<C> otherStream, TriJoiner<A, B, C>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every pair of A and B where C does not exist for which the
     * {@link TriJoiner} is true (for the properties it extracts from the facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link TriJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner} is true
     */
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link TriJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <C> the type of the third matched fact
     * @return never null, a stream that matches every pair of A and B where C does not exist for which the
     *         {@link TriJoiner}s are true
     */
    <C> BiConstraintStream<A, B> ifNotExistsIncludingUnassigned(Class<C> otherClass, TriJoiner<A, B, C>... joiners);

    // ************************************************************************
    // Group by
    // ************************************************************************

    /**
     * Runs all tuples of the stream through a given @{@link BiConstraintCollector} and converts them into a new
     * {@link UniConstraintStream} which only has a single tuple, the result of applying {@link BiConstraintCollector}.
     *
     * @param collector never null, the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <Result_> the type of a fact in the destination {@link UniConstraintStream}'s tuple
     * @return never null
     */
    <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            BiConstraintCollector<A, B, ResultContainer_, Result_> collector);

    /**
     * Convert the {@link BiConstraintStream} to a {@link BiConstraintStream}, containing only a single tuple,
     * the result of applying two {@link BiConstraintCollector}s.
     *
     * @param collectorA never null, the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorB never null, the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainerA_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultA_> the type of the first fact in the destination {@link BiConstraintStream}'s tuple
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link BiConstraintStream}'s tuple
     * @return never null
     */
    <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
            BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB);

    /**
     * Convert the {@link BiConstraintStream} to a {@link TriConstraintStream}, containing only a single tuple,
     * the result of applying three {@link BiConstraintCollector}s.
     *
     * @param collectorA never null, the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorB never null, the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC never null, the collector to perform the third grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainerA_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultA_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple
     * @return never null
     */
    <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_> groupBy(
                    BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC);

    /**
     * Convert the {@link BiConstraintStream} to a {@link QuadConstraintStream}, containing only a single tuple,
     * the result of applying four {@link BiConstraintCollector}s.
     *
     * @param collectorA never null, the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorB never null, the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC never null, the collector to perform the third grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorD never null, the collector to perform the fourth grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainerA_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     * @return never null
     */
    <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> groupBy(
                    BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link BiConstraintStream} to a {@link UniConstraintStream}, containing the set of tuples resulting
     * from applying the group key mapping function on all tuples of the original stream.
     * Neither tuple of the new stream {@link Objects#equals(Object, Object)} any other.
     *
     * @param groupKeyMapping never null, mapping function to convert each element in the stream to a different element
     * @param <GroupKey_> the type of a fact in the destination {@link UniConstraintStream}'s tuple
     * @return never null
     */
    <GroupKey_> UniConstraintStream<GroupKey_> groupBy(BiFunction<A, B, GroupKey_> groupKeyMapping);

    /**
     * Convert the {@link BiConstraintStream} to a different {@link BiConstraintStream}, consisting of unique tuples.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of a given {@link BiConstraintCollector} applied on all incoming tuples with
     * the same first fact.
     *
     * @param groupKeyMapping never null, function to convert the fact in the original tuple to a different fact
     * @param collector never null, the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKey_> the type of the first fact in the destination {@link BiConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <Result_> the type of the second fact in the destination {@link BiConstraintStream}'s tuple
     * @return never null
     */
    <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            BiFunction<A, B, GroupKey_> groupKeyMapping,
            BiConstraintCollector<A, B, ResultContainer_, Result_> collector);

    /**
     * Convert the {@link BiConstraintStream} to a {@link TriConstraintStream}, consisting of unique tuples with three
     * facts.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The remaining facts are the return value of the respective {@link BiConstraintCollector} applied on all
     * incoming tuples with the same first fact.
     *
     * @param groupKeyMapping never null, function to convert the fact in the original tuple to a different fact
     * @param collectorB never null, the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC never null, the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKey_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple
     * @return never null
     */
    <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(
                    BiFunction<A, B, GroupKey_> groupKeyMapping,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC);

    /**
     * Convert the {@link BiConstraintStream} to a {@link QuadConstraintStream}, consisting of unique tuples with four
     * facts.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The remaining facts are the return value of the respective {@link BiConstraintCollector} applied on all
     * incoming tuples with the same first fact.
     *
     * @param groupKeyMapping never null, function to convert the fact in the original tuple to a different fact
     * @param collectorB never null, the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorC never null, the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorD never null, the collector to perform the third grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKey_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerB_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     * @return never null
     */
    <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_> groupBy(
                    BiFunction<A, B, GroupKey_> groupKeyMapping,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link BiConstraintStream} to a different {@link BiConstraintStream}, consisting of unique tuples.
     * <p>
     * The first fact is the return value of the first group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of the second group key mapping function, applied on all incoming tuples with
     * the same first fact.
     *
     * @param groupKeyAMapping never null, function to convert the facts in the original tuple to a new fact
     * @param groupKeyBMapping never null, function to convert the facts in the original tuple to another new fact
     * @param <GroupKeyA_> the type of the first fact in the destination {@link BiConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link BiConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @return never null
     */
    <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping);

    /**
     * Combines the semantics of {@link #groupBy(BiFunction, BiFunction)} and {@link #groupBy(BiConstraintCollector)}.
     * That is, the first and second facts in the tuple follow the {@link #groupBy(BiFunction, BiFunction)} semantics,
     * and the third fact is the result of applying {@link BiConstraintCollector#finisher()} on all the tuples of the
     * original {@link UniConstraintStream} that belong to the group.
     *
     * @param groupKeyAMapping never null, function to convert the original tuple into a first fact
     * @param groupKeyBMapping never null, function to convert the original tuple into a second fact
     * @param collector never null, the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKeyA_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <Result_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple
     * @return never null
     */
    <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
            BiConstraintCollector<A, B, ResultContainer_, Result_> collector);

    /**
     * Combines the semantics of {@link #groupBy(BiFunction, BiFunction)} and {@link #groupBy(BiConstraintCollector)}.
     * That is, the first and second facts in the tuple follow the {@link #groupBy(BiFunction, BiFunction)} semantics.
     * The third fact is the result of applying the first {@link BiConstraintCollector#finisher()} on all the tuples
     * of the original {@link BiConstraintStream} that belong to the group.
     * The fourth fact is the result of applying the second {@link BiConstraintCollector#finisher()} on all the tuples
     * of the original {@link BiConstraintStream} that belong to the group
     *
     * @param groupKeyAMapping never null, function to convert the original tuple into a first fact
     * @param groupKeyBMapping never null, function to convert the original tuple into a second fact
     * @param collectorC never null, the collector to perform the first grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param collectorD never null, the collector to perform the second grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKeyA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerC_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     * @return never null
     */
    <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link BiConstraintStream} to a {@link TriConstraintStream}, consisting of unique tuples with three
     * facts.
     * <p>
     * The first fact is the return value of the first group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of the second group key mapping function, applied on all incoming tuples with
     * the same first fact.
     * The third fact is the return value of the third group key mapping function, applied on all incoming tuples with
     * the same first fact.
     *
     * @param groupKeyAMapping never null, function to convert the original tuple into a first fact
     * @param groupKeyBMapping never null, function to convert the original tuple into a second fact
     * @param groupKeyCMapping never null, function to convert the original tuple into a third fact
     * @param <GroupKeyA_> the type of the first fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyC_> the type of the third fact in the destination {@link TriConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @return never null
     */
    <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
            BiFunction<A, B, GroupKeyC_> groupKeyCMapping);

    /**
     * Combines the semantics of {@link #groupBy(BiFunction, BiFunction)} and {@link #groupBy(BiConstraintCollector)}.
     * That is, the first three facts in the tuple follow the {@link #groupBy(BiFunction, BiFunction)} semantics.
     * The final fact is the result of applying the first {@link BiConstraintCollector#finisher()} on all the tuples
     * of the original {@link BiConstraintStream} that belong to the group.
     *
     * @param groupKeyAMapping never null, function to convert the original tuple into a first fact
     * @param groupKeyBMapping never null, function to convert the original tuple into a second fact
     * @param groupKeyCMapping never null, function to convert the original tuple into a third fact
     * @param collectorD never null, the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <GroupKeyA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <ResultContainerD_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <ResultD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple
     * @return never null
     */
    <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_> groupBy(
                    BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    BiFunction<A, B, GroupKeyC_> groupKeyCMapping,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link BiConstraintStream} to a {@link QuadConstraintStream}, consisting of unique tuples with four
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
     * @param groupKeyAMapping never null, function to convert the original tuple into a first fact
     * @param groupKeyBMapping never null, function to convert the original tuple into a second fact
     * @param groupKeyCMapping never null, function to convert the original tuple into a third fact
     * @param groupKeyDMapping never null, function to convert the original tuple into a fourth fact
     * @param <GroupKeyA_> the type of the first fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyB_> the type of the second fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyC_> the type of the third fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @param <GroupKeyD_> the type of the fourth fact in the destination {@link QuadConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @return never null
     */
    <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> groupBy(
                    BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    BiFunction<A, B, GroupKeyC_> groupKeyCMapping, BiFunction<A, B, GroupKeyD_> groupKeyDMapping);

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    /**
     * As defined by {@link UniConstraintStream#map(Function)}.
     *
     * @param mapping never null, function to convert the original tuple into the new tuple
     * @param <ResultA_> the type of the only fact in the resulting {@link UniConstraintStream}'s tuple
     * @return never null
     */
    <ResultA_> UniConstraintStream<ResultA_> map(BiFunction<A, B, ResultA_> mapping);

    /**
     * As defined by {@link #map(BiFunction)}, only resulting in {@link BiConstraintStream}.
     *
     * @param mappingA never null, function to convert the original tuple into the first fact of a new tuple
     * @param mappingB never null, function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiConstraintStream}'s tuple
     * @return never null
     */
    <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(BiFunction<A, B, ResultA_> mappingA,
            BiFunction<A, B, ResultB_> mappingB);

    /**
     * As defined by {@link #map(BiFunction)}, only resulting in {@link TriConstraintStream}.
     *
     * @param mappingA never null, function to convert the original tuple into the first fact of a new tuple
     * @param mappingB never null, function to convert the original tuple into the second fact of a new tuple
     * @param mappingC never null, function to convert the original tuple into the third fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link TriConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link TriConstraintStream}'s tuple
     * @param <ResultC_> the type of the third fact in the resulting {@link TriConstraintStream}'s tuple
     * @return never null
     */
    <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(BiFunction<A, B, ResultA_> mappingA,
            BiFunction<A, B, ResultB_> mappingB, BiFunction<A, B, ResultC_> mappingC);

    /**
     * As defined by {@link #map(BiFunction)}, only resulting in {@link QuadConstraintStream}.
     *
     * @param mappingA never null, function to convert the original tuple into the first fact of a new tuple
     * @param mappingB never null, function to convert the original tuple into the second fact of a new tuple
     * @param mappingC never null, function to convert the original tuple into the third fact of a new tuple
     * @param mappingD never null, function to convert the original tuple into the fourth fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link QuadConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link QuadConstraintStream}'s tuple
     * @param <ResultC_> the type of the third fact in the resulting {@link QuadConstraintStream}'s tuple
     * @param <ResultD_> the type of the third fact in the resulting {@link QuadConstraintStream}'s tuple
     * @return never null
     */
    <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            BiFunction<A, B, ResultA_> mappingA, BiFunction<A, B, ResultB_> mappingB, BiFunction<A, B, ResultC_> mappingC,
            BiFunction<A, B, ResultD_> mappingD);

    /**
     * Takes each tuple and applies a mapping on the last fact, which turns it into {@link Iterable}.
     * Returns a constraint stream consisting of tuples of the first fact
     * and the contents of the {@link Iterable} one after another.
     * In other words, it will replace the current tuple with new tuples,
     * a cartesian product of A and the individual items from the {@link Iterable}.
     *
     * <p>
     * This may produce a stream with duplicate tuples.
     * See {@link #distinct()} for details.
     *
     * <p>
     * In cases where the last fact is already {@link Iterable}, use {@link Function#identity()} as the argument.
     *
     * <p>
     * Simple example: assuming a constraint stream of {@code (PersonName, Person)}
     * {@code [(Ann, (name = Ann, roles = [USER, ADMIN])), (Beth, (name = Beth, roles = [USER])),
     * (Cathy, (name = Cathy, roles = [ADMIN, AUDITOR]))]},
     * calling {@code flattenLast(Person::getRoles)} on such stream will produce a stream of
     * {@code [(Ann, USER), (Ann, ADMIN), (Beth, USER), (Cathy, ADMIN), (Cathy, AUDITOR)]}.
     *
     * @param mapping never null, function to convert the last fact in the original tuple into {@link Iterable}.
     *        For performance, returning an implementation of {@link java.util.Collection} is preferred.
     * @param <ResultB_> the type of the last fact in the resulting tuples.
     *        It is recommended that this type be deeply immutable.
     *        Not following this recommendation may lead to hard-to-debug hashing issues down the stream,
     *        especially if this value is ever used as a group key.
     * @return never null
     */
    <ResultB_> BiConstraintStream<A, ResultB_> flattenLast(Function<B, Iterable<ResultB_>> mapping);

    /**
     * Transforms the stream in such a way that all the tuples going through it are distinct.
     * (No two result tuples are {@link Object#equals(Object) equal}.)
     *
     * <p>
     * By default, tuples going through a constraint stream are distinct.
     * However, operations such as {@link #map(BiFunction)} may create a stream which breaks that promise.
     * By calling this method on such a stream,
     * duplicate copies of the same tuple are omitted at a performance cost.
     *
     * @return never null
     */
    BiConstraintStream<A, B> distinct();

    /**
     * Returns a new {@link BiConstraintStream} containing all the tuples of both this {@link BiConstraintStream}
     * and the provided {@link UniConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2), (B1, B2), (C1, C2)]}
     * and the other stream consists of {@code [C, D, E]},
     * {@code this.concat(other)} will consist of {@code [(A1, A2), (B1, B2), (C1, C2), (C, null), (D, null), (E, null)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    default BiConstraintStream<A, B> concat(UniConstraintStream<A> otherStream) {
        return concat(otherStream, uniConstantNull());
    }

    /**
     * Returns a new {@link BiConstraintStream} containing all the tuples of both this {@link BiConstraintStream}
     * and the provided {@link UniConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by the result of the padding function.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2), (B1, B2), (C1, C2)]}
     * and the other stream consists of {@code [C, D, E]},
     * {@code this.concat(other, a -> null)} will consist of
     * {@code [(A1, A2), (B1, B2), (C1, C2), (C, null), (D, null), (E, null)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @param paddingFunction never null, function to find the padding for the second fact
     * @return never null
     */
    BiConstraintStream<A, B> concat(UniConstraintStream<A> otherStream, Function<A, B> paddingFunction);

    /**
     * Returns a new {@link BiConstraintStream} containing all the tuples of both this {@link BiConstraintStream} and the
     * provided {@link BiConstraintStream}. Tuples in both this {@link BiConstraintStream} and the provided
     * {@link BiConstraintStream} will appear at least twice.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A, 1), (B, 2), (C, 3)]} and the other stream consists of
     * {@code [(C, 3), (D, 4), (E, 5)]}, {@code this.concat(other)} will consist of
     * {@code [(A, 1), (B, 2), (C, 3), (C, 3), (D, 4), (E, 5)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    BiConstraintStream<A, B> concat(BiConstraintStream<A, B> otherStream);

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link BiConstraintStream}
     * and the provided {@link TriConstraintStream}.
     * The {@link BiConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2), (B1, B2), (C1, C2)]}
     * and the other stream consists of {@code [(C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A1, A2, null), (B1, B2, null), (C1, C2, null), (C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    default <C> TriConstraintStream<A, B, C> concat(TriConstraintStream<A, B, C> otherStream) {
        return concat(otherStream, biConstantNull());
    }

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link BiConstraintStream}
     * and the provided {@link TriConstraintStream}.
     * The {@link BiConstraintStream} tuples will be padded from the right by the result of the padding function.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2), (B1, B2), (C1, C2)]}
     * and the other stream consists of {@code [(C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]},
     * {@code this.concat(other, (a, b) -> null)} will consist of
     * {@code [(A1, A2, null), (B1, B2, null), (C1, C2, null), (C1, C2, C3), (D1, D2, D3), (E1, E2, E3)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @param paddingFunction never null, function to find the padding for the third fact
     * @return never null
     */
    <C> TriConstraintStream<A, B, C> concat(TriConstraintStream<A, B, C> otherStream, BiFunction<A, B, C> paddingFunction);

    /**
     * Returns a new {@link QuadConstraintStream} containing all the tuples of both this {@link BiConstraintStream}
     * and the provided {@link QuadConstraintStream}.
     * The {@link BiConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2), (B1, B2), (C1, C2)]}
     * and the other stream consists of {@code [(C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A1, A2, null, null), (B1, B2, null, null), (C1, C2, null, null),
     * (C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    default <C, D> QuadConstraintStream<A, B, C, D> concat(QuadConstraintStream<A, B, C, D> otherStream) {
        return concat(otherStream, biConstantNull(), biConstantNull());
    }

    /**
     * Returns a new {@link QuadConstraintStream} containing all the tuples of both this {@link BiConstraintStream}
     * and the provided {@link QuadConstraintStream}.
     * The {@link BiConstraintStream} tuples will be padded from the right by the results of the padding functions.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2), (B1, B2), (C1, C2)]}
     * and the other stream consists of {@code [(C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]},
     * {@code this.concat(other, (a, b) -> null, (a, b) -> null)} will consist of
     * {@code [(A1, A2, null, null), (B1, B2, null, null), (C1, C2, null, null),
     * (C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @param paddingFunctionC never null, function to find the padding for the third fact
     * @param paddingFunctionD never null, function to find the padding for the fourth fact
     * @return never null
     */
    <C, D> QuadConstraintStream<A, B, C, D> concat(QuadConstraintStream<A, B, C, D> otherStream,
            BiFunction<A, B, C> paddingFunctionC, BiFunction<A, B, D> paddingFunctionD);

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
     * If more than two facts are to be added,
     * prefer {@link #expand(BiFunction, BiFunction)}.
     *
     * @param mapping function to produce the new fact from the original tuple
     * @return never null
     * @param <ResultC_> type of the final fact of the new tuple
     */
    <ResultC_> TriConstraintStream<A, B, ResultC_> expand(BiFunction<A, B, ResultC_> mapping);

    /**
     * Adds two facts to the end of the tuple, increasing the cardinality of the stream.
     * Useful for storing results of expensive computations on the original tuple.
     *
     * <p>
     * Use with caution,
     * as the benefits of caching computation may be outweighed by increased memory allocation rates
     * coming from tuple creation.
     *
     * @param mappingC function to produce the new third fact from the original tuple
     * @param mappingD function to produce the new final fact from the original tuple
     * @return never null
     * @param <ResultC_> type of the third fact of the new tuple
     * @param <ResultD_> type of the final fact of the new tuple
     */
    <ResultC_, ResultD_> QuadConstraintStream<A, B, ResultC_, ResultD_> expand(BiFunction<A, B, ResultC_> mappingC,
            BiFunction<A, B, ResultD_> mappingD);

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    /**
     * As defined by {@link #penalize(Score, ToIntBiFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalize(Score_ constraintWeight) {
        return penalize(constraintWeight, biConstantOne());
    }

    /**
     * As defined by {@link #penalizeLong(Score, ToLongBiFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeLong(Score_ constraintWeight) {
        return penalizeLong(constraintWeight, biConstantOneLong());
    }

    /**
     * As defined by {@link #penalizeBigDecimal(Score, BiFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeBigDecimal(Score_ constraintWeight) {
        return penalizeBigDecimal(constraintWeight, biConstantOneBigDecimal());
    }

    /**
     * Applies a negative {@link Score} impact,
     * subtracting the constraintWeight multiplied by the match weight,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight specified here can be overridden using {@link ConstraintWeightOverrides}
     * on the {@link PlanningSolution}-annotated class
     * <p>
     * For non-int {@link Score} types use {@link #penalizeLong(Score, ToLongBiFunction)} or
     * {@link #penalizeBigDecimal(Score, BiFunction)} instead.
     *
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalize(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #penalize(Score, ToIntBiFunction)}, with a penalty of type long.
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeLong(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #penalize(Score, ToIntBiFunction)}, with a penalty of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeBigDecimal(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher);

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
    default BiConstraintBuilder<A, B, ?> penalizeConfigurable() {
        return penalizeConfigurable(biConstantOne());
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
     * @deprecated Prefer {@link #penalize(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> penalizeConfigurable(ToIntBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #penalizeConfigurable(ToIntBiFunction)}, with a penalty of type long.
     * 
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> penalizeConfigurableLong(ToLongBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #penalizeConfigurable(ToIntBiFunction)}, with a penalty of type {@link BigDecimal}.
     * 
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> penalizeConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntBiFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> reward(Score_ constraintWeight) {
        return reward(constraintWeight, biConstantOne());
    }

    /**
     * Applies a positive {@link Score} impact,
     * adding the constraintWeight multiplied by the match weight,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight specified here can be overridden using {@link ConstraintWeightOverrides}
     * on the {@link PlanningSolution}-annotated class
     * <p>
     * For non-int {@link Score} types use {@link #rewardLong(Score, ToLongBiFunction)} or
     * {@link #rewardBigDecimal(Score, BiFunction)} instead.
     *
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> reward(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntBiFunction)}, with a penalty of type long.
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> rewardLong(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntBiFunction)}, with a penalty of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> rewardBigDecimal(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher);

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
     * @deprecated Prefer {@link #reward(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    default BiConstraintBuilder<A, B, ?> rewardConfigurable() {
        return rewardConfigurable(biConstantOne());
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
     * @deprecated Prefer {@link #reward(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> rewardConfigurable(ToIntBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #rewardConfigurable(ToIntBiFunction)}, with a penalty of type long.
     * 
     * @deprecated Prefer {@link #rewardLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> rewardConfigurableLong(ToLongBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #rewardConfigurable(ToIntBiFunction)}, with a penalty of type {@link BigDecimal}.
     * 
     * @deprecated Prefer {@link #rewardBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> rewardConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher);

    /**
     * Positively or negatively impacts the {@link Score} by the constraintWeight for each match
     * and returns a builder to apply optional constraint properties.
     * <p>
     * Use {@code penalize(...)} or {@code reward(...)} instead, unless this constraint can both have positive and
     * negative weights.
     *
     * @param constraintWeight never null
     * @return never null
     */
    default <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> impact(Score_ constraintWeight) {
        return impact(constraintWeight, biConstantOne());
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
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> impact(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #impact(Score, ToIntBiFunction)}, with an impact of type long.
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> impactLong(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #impact(Score, ToIntBiFunction)}, with an impact of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> impactBigDecimal(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher);

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
    default BiConstraintBuilder<A, B, ?> impactConfigurable() {
        return impactConfigurable(biConstantOne());
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
     * @deprecated Prefer {@link #impact(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> impactConfigurable(ToIntBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #impactConfigurable(ToIntBiFunction)}, with an impact of type long.
     * 
     * @deprecated Prefer {@link #impactLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> impactConfigurableLong(ToLongBiFunction<A, B> matchWeigher);

    /**
     * As defined by {@link #impactConfigurable(ToIntBiFunction)}, with an impact of type BigDecimal.
     * 
     * @deprecated Prefer {@link #impactBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    BiConstraintBuilder<A, B, ?> impactConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher);

    // ************************************************************************
    // complement
    // ************************************************************************

    /**
     * As defined by {@link #complement(Class, Function)},
     * where the padding function pads with null.
     */
    default BiConstraintStream<A, B> complement(Class<A> otherClass) {
        return complement(otherClass, uniConstantNull());
    }

    /**
     * Adds to the stream all instances of a given class which are not yet present in it.
     * These instances must be present in the solution,
     * which means the class needs to be either a planning entity or a problem fact.
     * <p>
     * The instances will be read from the first element of the input tuple.
     * When an output tuple needs to be created for the newly inserted instances,
     * the first element will be the new instance.
     * The rest of the tuple will be padded with the result of the padding function,
     * applied on the new instance.
     *
     * @param otherClass never null
     * @param paddingFunction never null, function to find the padding for the second fact
     * @return never null
     */
    default BiConstraintStream<A, B> complement(Class<A> otherClass, Function<A, B> paddingFunction) {
        var firstStream = this;
        var remapped = firstStream.map(ConstantLambdaUtils.biPickFirst());
        var secondStream = getConstraintFactory().forEach(otherClass)
                .ifNotExists(remapped, Joiners.equal());
        return firstStream.concat(secondStream, paddingFunction);
    }

    // ************************************************************************
    // Deprecated declarations
    // ************************************************************************

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, TriJoiner, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, TriJoiner, TriJoiner, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, TriJoiner, TriJoiner, TriJoiner, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, TriJoiner...)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        return ifExistsIncludingUnassigned(otherClass, joiners);
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner, TriJoiner, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner, TriJoiner, TriJoiner, TriJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C> joiner1,
            TriJoiner<A, B, C> joiner2, TriJoiner<A, B, C> joiner3, TriJoiner<A, B, C> joiner4) {
        return ifNotExistsIncludingUnassigned(otherClass, new TriJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, TriJoiner...)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <C> BiConstraintStream<A, B> ifNotExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        return ifNotExistsIncludingUnassigned(otherClass, joiners);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     * <p>
     * For non-int {@link Score} types use {@link #penalizeLong(String, Score, ToLongBiFunction)} or
     * {@link #penalizeBigDecimal(String, Score, BiFunction)} instead.
     *
     * @deprecated Prefer {@link #penalize(Score, ToIntBiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalize(String constraintName, Score<?> constraintWeight, ToIntBiFunction<A, B> matchWeigher) {
        return penalize((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalize(String, Score, ToIntBiFunction)}.
     *
     * @deprecated Prefer {@link #penalize(Score, ToIntBiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalize(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntBiFunction<A, B> matchWeigher) {
        return penalize((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     *
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongBiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeLong(String constraintName, Score<?> constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return penalizeLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeLong(String, Score, ToLongBiFunction)}.
     *
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongBiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return penalizeLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     *
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, BiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeBigDecimal(String constraintName, Score<?> constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return penalizeBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeBigDecimal(String, Score, BiFunction)}.
     *
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, BiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return penalizeBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #penalizeConfigurable(String)}.
     * <p>
     * For non-int {@link Score} types use {@link #penalizeConfigurableLong(String, ToLongBiFunction)} or
     * {@link #penalizeConfigurableBigDecimal(String, BiFunction)} instead.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #penalize(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurable(String constraintName, ToIntBiFunction<A, B> matchWeigher) {
        return penalizeConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurable(String, ToIntBiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalize(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurable(String constraintPackage, String constraintName,
            ToIntBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableLong(String constraintName, ToLongBiFunction<A, B> matchWeigher) {
        return penalizeConfigurableLong(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurableLong(String, ToLongBiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableLong(String constraintPackage, String constraintName,
            ToLongBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableBigDecimal(String constraintName,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return penalizeConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurableBigDecimal(String, BiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableBigDecimal(String constraintPackage, String constraintName,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return penalizeConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     * <p>
     * For non-int {@link Score} types use {@link #rewardLong(String, Score, ToLongBiFunction)} or
     * {@link #rewardBigDecimal(String, Score, BiFunction)} instead.
     *
     * @deprecated Prefer {@link #reward(Score, ToIntBiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint reward(String constraintName, Score<?> constraintWeight, ToIntBiFunction<A, B> matchWeigher) {
        return reward((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #reward(String, Score, ToIntBiFunction)}.
     *
     * @deprecated Prefer {@link #reward(Score, ToIntBiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint reward(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntBiFunction<A, B> matchWeigher) {
        return reward((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     *
     * @deprecated Prefer {@link #rewardLong(Score, ToLongBiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardLong(String constraintName, Score<?> constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return rewardLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardLong(String, Score, ToLongBiFunction)}.
     *
     * @deprecated Prefer {@link #rewardLong(Score, ToLongBiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return rewardLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     *
     * @deprecated Prefer {@link #rewardBigDecimal(Score, BiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardBigDecimal(String constraintName, Score<?> constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return rewardBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardBigDecimal(String, Score, BiFunction)}.
     *
     * @deprecated Prefer {@link #rewardBigDecimal(Score, BiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return rewardBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #rewardConfigurable(String)}.
     * <p>
     * For non-int {@link Score} types use {@link #rewardConfigurableLong(String, ToLongBiFunction)} or
     * {@link #rewardConfigurableBigDecimal(String, BiFunction)} instead.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #reward(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurable(String constraintName, ToIntBiFunction<A, B> matchWeigher) {
        return rewardConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurable(String, ToIntBiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #reward(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated
    default Constraint rewardConfigurable(String constraintPackage, String constraintName, ToIntBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #rewardLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableLong(String constraintName, ToLongBiFunction<A, B> matchWeigher) {
        return rewardConfigurableLong(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurableLong(String, ToLongBiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #rewardLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableLong(String constraintPackage, String constraintName,
            ToLongBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #rewardBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableBigDecimal(String constraintName, BiFunction<A, B, BigDecimal> matchWeigher) {
        return rewardConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurableBigDecimal(String, BiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #rewardBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableBigDecimal(String constraintPackage, String constraintName,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return rewardConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #impact(String, Score)}.
     * <p>
     * Use {@code penalize(...)} or {@code reward(...)} instead, unless this constraint can both have positive and
     * negative weights.
     * <p>
     * For non-int {@link Score} types use {@link #impactLong(String, Score, ToLongBiFunction)} or
     * {@link #impactBigDecimal(String, Score, BiFunction)} instead.
     *
     * @deprecated Prefer {@link #impact(Score, ToIntBiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impact(String constraintName, Score<?> constraintWeight, ToIntBiFunction<A, B> matchWeigher) {
        return impact((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impact(String, Score, ToIntBiFunction)}.
     *
     * @deprecated Prefer {@link #impact(Score, ToIntBiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impact(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #impactLong(Score, ToLongBiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactLong(String constraintName, Score<?> constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return impactLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactLong(String, Score, ToLongBiFunction)}.
     *
     * @deprecated Prefer {@link #impactLong(Score, ToLongBiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #impactBigDecimal(Score, BiFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactBigDecimal(String constraintName, Score<?> constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return impactBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactBigDecimal(String, Score, BiFunction)}.
     *
     * @deprecated Prefer {@link #impactBigDecimal(Score, BiFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
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
     * The {@link ConstraintRef#packageName() constraint package} defaults to
     * {@link ConstraintConfiguration#constraintPackage()}.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #impact(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurable(String constraintName, ToIntBiFunction<A, B> matchWeigher) {
        return impactConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactConfigurable(String, ToIntBiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impact(Score, ToIntBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurable(String constraintPackage, String constraintName,
            ToIntBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #impactLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableLong(String constraintName, ToLongBiFunction<A, B> matchWeigher) {
        return impactConfigurableLong(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactConfigurableLong(String, ToLongBiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impactLong(Score, ToLongBiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableLong(String constraintPackage, String constraintName,
            ToLongBiFunction<A, B> matchWeigher) {
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
     * @deprecated Prefer {@link #impactBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableBigDecimal(String constraintName,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return impactConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactConfigurableBigDecimal(String, BiFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impactBigDecimal(Score, BiFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableBigDecimal(String constraintPackage, String constraintName,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return impactConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

}
