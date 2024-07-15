package ai.timefold.solver.core.api.score.stream.tri;

import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.biConstantNull;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.triConstantNull;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.triConstantOne;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.triConstantOneBigDecimal;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.triConstantOneLong;
import static ai.timefold.solver.core.impl.util.ConstantLambdaUtils.uniConstantNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;
import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
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
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

/**
 * A {@link ConstraintStream} that matches three facts.
 *
 * @param <A> the type of the first fact in the tuple.
 * @param <B> the type of the second fact in the tuple.
 * @param <C> the type of the third fact in the tuple.
 * @see ConstraintStream
 */
public interface TriConstraintStream<A, B, C> extends ConstraintStream {

    // ************************************************************************
    // Filter
    // ************************************************************************

    /**
     * Exhaustively test each tuple of facts against the {@link TriPredicate}
     * and match if {@link TriPredicate#test(Object, Object, Object)} returns true.
     * <p>
     * Important: This is slower and less scalable than {@link BiConstraintStream#join(UniConstraintStream, TriJoiner)}
     * with a proper {@link TriJoiner} predicate (such as {@link Joiners#equal(BiFunction, Function)},
     * because the latter applies hashing and/or indexing, so it doesn't create every combination just to filter it out.
     *
     * @param predicate never null
     * @return never null
     */
    TriConstraintStream<A, B, C> filter(TriPredicate<A, B, C> predicate);

    // ************************************************************************
    // Join
    // ************************************************************************

    /**
     * Create a new {@link QuadConstraintStream} for every combination of [A, B, C] and D.
     * <p>
     * Important: {@link QuadConstraintStream#filter(QuadPredicate) Filtering} this is slower and less scalable
     * than a {@link #join(UniConstraintStream, QuadJoiner)},
     * because it doesn't apply hashing and/or indexing on the properties,
     * so it creates and checks every combination of [A, B] and C.
     *
     * @param otherStream never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D
     */
    default <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream) {
        return join(otherStream, new QuadJoiner[0]);
    }

    /**
     * Create a new {@link QuadConstraintStream} for every combination of [A, B] and C for which the {@link QuadJoiner}
     * is true (for the properties it extracts from all facts).
     * <p>
     * Important: This is faster and more scalable than a {@link #join(UniConstraintStream) join}
     * followed by a {@link QuadConstraintStream#filter(QuadPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of [A, B, C] and D.
     *
     * @param otherStream never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which the {@link QuadJoiner}
     *         is true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner) {
        return join(otherStream, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2) {
        return join(otherStream, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return join(otherStream, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3,
            QuadJoiner<A, B, C, D> joiner4) {
        return join(otherStream, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiners never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D>... joiners);

    /**
     * Create a new {@link QuadConstraintStream} for every combination of [A, B, C] and D.
     * <p>
     * Important: {@link QuadConstraintStream#filter(QuadPredicate)} Filtering} this is slower and less scalable
     * than a {@link #join(Class, QuadJoiner)},
     * because it doesn't apply hashing and/or indexing on the properties,
     * so it creates and checks every combination of [A, B, C] and D.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different range of D may be selected.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     * <p>
     * This method is syntactic sugar for {@link #join(UniConstraintStream)}.
     *
     * @param otherClass never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D
     */
    default <D> QuadConstraintStream<A, B, C, D> join(Class<D> otherClass) {
        return join(otherClass, new QuadJoiner[0]);
    }

    /**
     * Create a new {@link QuadConstraintStream} for every combination of [A, B, C] and D for which the
     * {@link QuadJoiner} is true (for the properties it extracts from all facts).
     * <p>
     * Important: This is faster and more scalable than a {@link #join(Class, QuadJoiner) join}
     * followed by a {@link QuadConstraintStream#filter(QuadPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks every combination of [A, B, C] and D.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different range of D may be selected.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     * <p>
     * This method is syntactic sugar for {@link #join(UniConstraintStream, QuadJoiner)}.
     * <p>
     * This method has overloaded methods with multiple {@link QuadJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which the {@link QuadJoiner}
     *         is true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner) {
        return join(otherClass, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2) {
        return join(otherClass, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return join(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    default <D> QuadConstraintStream<A, B, C, D> join(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3, QuadJoiner<A, B, C, D> joiner4) {
        return join(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #join(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every combination of [A, B, C] and D for which all the
     *         {@link QuadJoiner joiners} are true
     */
    <D> QuadConstraintStream<A, B, C, D> join(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners);

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    /**
     * Create a new {@link BiConstraintStream} for every tuple of A, B and C where D exists for which the
     * {@link QuadJoiner} is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link QuadJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner} is true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner) {
        return ifExists(otherClass, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2) {
        return ifExists(otherClass, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifExists(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3, QuadJoiner<A, B, C, D> joiner4) {
        return ifExists(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    <D> TriConstraintStream<A, B, C> ifExists(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every tuple of A, B and C where D exists for which the
     * {@link QuadJoiner} is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link QuadJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner} is true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner) {
        return ifExists(otherStream, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2) {
        return ifExists(otherStream, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifExists(otherStream, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3, QuadJoiner<A, B, C, D> joiner4) {
        return ifExists(otherStream, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiners never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    <D> TriConstraintStream<A, B, C> ifExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every tuple of A, B and C where D exists for which the
     * {@link QuadJoiner} is true (for the properties it extracts from the facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link QuadJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner} is true
     */
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3,
            QuadJoiner<A, B, C, D> joiner4) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiners never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D exists for which the
     *         {@link QuadJoiner}s are true
     */
    <D> TriConstraintStream<A, B, C> ifExistsIncludingUnassigned(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every tuple of A, B and C where D does not exist for which the
     * {@link QuadJoiner} is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link QuadJoiner} parameters.
     * <p>
     * Note that, if a legacy constraint stream uses {@link ConstraintFactory#from(Class)} as opposed to
     * {@link ConstraintFactory#forEach(Class)},
     * a different definition of exists applies.
     * (See {@link ConstraintFactory#from(Class)} Javadoc.)
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner} is true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner) {
        return ifNotExists(otherClass, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2) {
        return ifNotExists(otherClass, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifNotExists(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3, QuadJoiner<A, B, C, D> joiner4) {
        return ifNotExists(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExists(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param <D> the type of the fourth matched fact
     * @param otherClass never null
     * @param joiners never null
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    <D> TriConstraintStream<A, B, C> ifNotExists(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every tuple of A, B and C where D does not exist for which the
     * {@link QuadJoiner} is true (for the properties it extracts from the facts).
     * <p>
     * This method has overloaded methods with multiple {@link QuadJoiner} parameters.
     *
     * @param otherStream never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner} is true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner) {
        return ifNotExists(otherStream, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2) {
        return ifNotExists(otherStream, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifNotExists(otherStream, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherStream never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3, QuadJoiner<A, B, C, D> joiner4) {
        return ifNotExists(otherStream, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExists(UniConstraintStream, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param <D> the type of the fourth matched fact
     * @param otherStream never null
     * @param joiners never null
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    <D> TriConstraintStream<A, B, C> ifNotExists(UniConstraintStream<D> otherStream, QuadJoiner<A, B, C, D>... joiners);

    /**
     * Create a new {@link BiConstraintStream} for every tuple of A, B and C where D does not exist for which the
     * {@link QuadJoiner} is true (for the properties it extracts from the facts).
     * For classes annotated with {@link PlanningEntity},
     * this method also includes entities with null variables,
     * or entities that are not assigned to any list variable.
     * <p>
     * This method has overloaded methods with multiple {@link QuadJoiner} parameters.
     *
     * @param otherClass never null
     * @param joiner never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner} is true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     *
     * @param otherClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <D> the type of the fourth matched fact
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D> joiner1, QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3,
            QuadJoiner<A, B, C, D> joiner4) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner)}.
     * For performance reasons, indexing joiners must be placed before filtering joiners.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link QuadJoiner} parameters.
     *
     * @param <D> the type of the fourth matched fact
     * @param otherClass never null
     * @param joiners never null
     * @return never null, a stream that matches every tuple of A, B and C where D does not exist for which the
     *         {@link QuadJoiner}s are true
     */
    <D> TriConstraintStream<A, B, C> ifNotExistsIncludingUnassigned(Class<D> otherClass,
            QuadJoiner<A, B, C, D>... joiners);

    // ************************************************************************
    // Group by
    // ************************************************************************

    /**
     * Convert the {@link TriConstraintStream} to a {@link UniConstraintStream}, containing only a single tuple, the
     * result of applying {@link TriConstraintCollector}.
     *
     * @param collector never null, the collector to perform the grouping operation with
     *        See {@link ConstraintCollectors} for common operations, such as {@code count()}, {@code sum()} and others.
     * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
     * @param <Result_> the type of a fact in the destination {@link UniConstraintStream}'s tuple
     * @return never null
     */
    <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector);

    /**
     * Convert the {@link TriConstraintStream} to a {@link BiConstraintStream}, containing only a single tuple,
     * the result of applying two {@link TriConstraintCollector}s.
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
            TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
            TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB);

    /**
     * Convert the {@link TriConstraintStream} to a {@link TriConstraintStream}, containing only a single tuple,
     * the result of applying three {@link TriConstraintCollector}s.
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
                    TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC);

    /**
     * Convert the {@link TriConstraintStream} to a {@link QuadConstraintStream}, containing only a single tuple,
     * the result of applying four {@link TriConstraintCollector}s.
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
                    TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link TriConstraintStream} to a {@link UniConstraintStream}, containing the set of tuples resulting
     * from applying the group key mapping function on all tuples of the original stream.
     * Neither tuple of the new stream {@link Objects#equals(Object, Object)} any other.
     *
     * @param groupKeyMapping never null, mapping function to convert each element in the stream to a different element
     * @param <GroupKey_> the type of a fact in the destination {@link UniConstraintStream}'s tuple;
     *        must honor {@link Object#hashCode() the general contract of hashCode}.
     * @return never null
     */
    <GroupKey_> UniConstraintStream<GroupKey_> groupBy(TriFunction<A, B, C, GroupKey_> groupKeyMapping);

    /**
     * Convert the {@link TriConstraintStream} to a {@link BiConstraintStream}, consisting of unique tuples.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The second fact is the return value of a given {@link TriConstraintCollector} applied on all incoming tuples with
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
            TriFunction<A, B, C, GroupKey_> groupKeyMapping,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector);

    /**
     * Convert the {@link TriConstraintStream} to a {@link TriConstraintStream}, consisting of unique tuples with three
     * facts.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The remaining facts are the return value of the respective {@link TriConstraintCollector} applied on all
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
                    TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC);

    /**
     * Convert the {@link TriConstraintStream} to a {@link QuadConstraintStream}, consisting of unique tuples with four
     * facts.
     * <p>
     * The first fact is the return value of the group key mapping function, applied on the incoming tuple.
     * The remaining facts are the return value of the respective {@link TriConstraintCollector} applied on all
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
                    TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link TriConstraintStream} to a {@link BiConstraintStream}, consisting of unique tuples.
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
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping);

    /**
     * Combines the semantics of {@link #groupBy(TriFunction, TriFunction)} and {@link #groupBy(TriConstraintCollector)}.
     * That is, the first and second facts in the tuple follow the {@link #groupBy(TriFunction, TriFunction)} semantics,
     * and the third fact is the result of applying {@link TriConstraintCollector#finisher()} on all the tuples of the
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
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector);

    /**
     * Combines the semantics of {@link #groupBy(TriFunction, TriFunction)} and {@link #groupBy(TriConstraintCollector)}.
     * That is, the first and second facts in the tuple follow the {@link #groupBy(TriFunction, TriFunction)} semantics.
     * The third fact is the result of applying the first {@link TriConstraintCollector#finisher()} on all the tuples
     * of the original {@link TriConstraintStream} that belong to the group.
     * The fourth fact is the result of applying the second {@link TriConstraintCollector#finisher()} on all the tuples
     * of the original {@link TriConstraintStream} that belong to the group
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
                    TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link TriConstraintStream} to a {@link TriConstraintStream}, consisting of unique tuples with three
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
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
            TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping);

    /**
     * Combines the semantics of {@link #groupBy(TriFunction, TriFunction)} and {@link #groupBy(TriConstraintCollector)}.
     * That is, the first three facts in the tuple follow the {@link #groupBy(TriFunction, TriFunction)} semantics.
     * The final fact is the result of applying the first {@link TriConstraintCollector#finisher()} on all the tuples
     * of the original {@link TriConstraintStream} that belong to the group.
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
                    TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD);

    /**
     * Convert the {@link TriConstraintStream} to a {@link QuadConstraintStream}, consisting of unique tuples with four
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
                    TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping, TriFunction<A, B, C, GroupKeyD_> groupKeyDMapping);

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
    <ResultA_> UniConstraintStream<ResultA_> map(TriFunction<A, B, C, ResultA_> mapping);

    /**
     * As defined by {@link #map(TriFunction)}, only resulting in {@link BiConstraintStream}.
     *
     * @param mappingA never null, function to convert the original tuple into the first fact of a new tuple
     * @param mappingB never null, function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiConstraintStream}'s tuple
     * @return never null
     */
    <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(TriFunction<A, B, C, ResultA_> mappingA,
            TriFunction<A, B, C, ResultB_> mappingB);

    /**
     * As defined by {@link #map(TriFunction)}, only resulting in {@link TriConstraintStream}.
     *
     * @param mappingA never null, function to convert the original tuple into the first fact of a new tuple
     * @param mappingB never null, function to convert the original tuple into the second fact of a new tuple
     * @param mappingC never null, function to convert the original tuple into the third fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link TriConstraintStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link TriConstraintStream}'s tuple
     * @param <ResultC_> the type of the third fact in the resulting {@link TriConstraintStream}'s tuple
     * @return never null
     */
    <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            TriFunction<A, B, C, ResultA_> mappingA, TriFunction<A, B, C, ResultB_> mappingB,
            TriFunction<A, B, C, ResultC_> mappingC);

    /**
     * As defined by {@link #map(TriFunction)}, only resulting in {@link QuadConstraintStream}.
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
            TriFunction<A, B, C, ResultA_> mappingA, TriFunction<A, B, C, ResultB_> mappingB,
            TriFunction<A, B, C, ResultC_> mappingC, TriFunction<A, B, C, ResultD_> mappingD);

    /**
     * As defined by {@link BiConstraintStream#flattenLast(Function)}.
     *
     * @param <ResultC_> the type of the last fact in the resulting tuples.
     *        It is recommended that this type be deeply immutable.
     *        Not following this recommendation may lead to hard-to-debug hashing issues down the stream,
     *        especially if this value is ever used as a group key.
     * @param mapping never null, function to convert the last fact in the original tuple into {@link Iterable}.
     *        For performance, returning an implementation of {@link java.util.Collection} is preferred.
     * @return never null
     */
    <ResultC_> TriConstraintStream<A, B, ResultC_> flattenLast(Function<C, Iterable<ResultC_>> mapping);

    /**
     * Removes duplicate tuples from the stream, according to the tuple's facts
     * {@link Object#equals(Object) equals}/{@link Object#hashCode() hashCode}
     * methods, such that only distinct tuples remain.
     * (No two tuples will {@link Object#equals(Object) equal}.)
     *
     * <p>
     * By default, tuples going through a constraint stream are distinct.
     * However, operations such as {@link #map(TriFunction)} may create a stream which breaks that promise.
     * By calling this method on such a stream,
     * duplicate copies of the same tuple will be omitted at a performance cost.
     *
     * @return never null
     */
    TriConstraintStream<A, B, C> distinct();

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link TriConstraintStream}
     * and the provided {@link UniConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3)]}
     * and the other stream consists of {@code [C, D, E]},
     * {@code this.concat(other)} will consist of
     * {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3), (C, null), (D, null), (E, null)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    default TriConstraintStream<A, B, C> concat(UniConstraintStream<A> otherStream) {
        return concat(otherStream, uniConstantNull(), uniConstantNull());
    }

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link TriConstraintStream}
     * and the provided {@link UniConstraintStream}.
     * The {@link UniConstraintStream} tuples will be padded from the right by the results of the padding functions.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3)]}
     * and the other stream consists of {@code [C, D, E]},
     * {@code this.concat(other, a -> null, a -> null)} will consist of
     * {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3), (C, null), (D, null), (E, null)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @param paddingFunctionB never null, function to find the padding for the second fact
     * @param paddingFunctionC never null, function to find the padding for the third fact
     * @return never null
     */
    TriConstraintStream<A, B, C> concat(UniConstraintStream<A> otherStream, Function<A, B> paddingFunctionB,
            Function<A, C> paddingFunctionC);

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link TriConstraintStream}
     * and the provided {@link BiConstraintStream}.
     * The {@link BiConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3)]}
     * and the other stream consists of {@code [(C1, C2), (D1, D2), (E1, E2)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3), (C1, C2, null), (D1, D2, null), (E1, E2, null)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    default TriConstraintStream<A, B, C> concat(BiConstraintStream<A, B> otherStream) {
        return concat(otherStream, biConstantNull());
    }

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link TriConstraintStream}
     * and the provided {@link BiConstraintStream}.
     * The {@link BiConstraintStream} tuples will be padded from the right by the result of the padding function.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3)]}
     * and the other stream consists of {@code [(C1, C2), (D1, D2), (E1, E2)]},
     * {@code this.concat(other, (a, b) -> null)} will consist of
     * {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3), (C1, C2, null), (D1, D2, null), (E1, E2, null)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @param paddingFunctionC never null, function to find the padding for the third fact
     * @return never null
     */
    TriConstraintStream<A, B, C> concat(BiConstraintStream<A, B> otherStream, BiFunction<A, B, C> paddingFunctionC);

    /**
     * Returns a new {@link TriConstraintStream} containing all the tuples of both this {@link TriConstraintStream} and the
     * provided {@link TriConstraintStream}.
     * Tuples in both this {@link TriConstraintStream} and the provided {@link TriConstraintStream} will appear at least twice.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A, 1, -1), (B, 2, -2), (C, 3, -3)]} and the other stream consists of
     * {@code [(C, 3, -3), (D, 4, -4), (E, 5, -5)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A, 1, -1), (B, 2, -2), (C, 3, -3), (C, 3, -3), (D, 4, -4), (E, 5, -5)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    TriConstraintStream<A, B, C> concat(TriConstraintStream<A, B, C> otherStream);

    /**
     * Returns a new {@link QuadConstraintStream} containing all the tuples of both this {@link TriConstraintStream}
     * and the provided {@link QuadConstraintStream}.
     * The {@link TriConstraintStream} tuples will be padded from the right by null.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3)]}
     * and the other stream consists of {@code [(C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]},
     * {@code this.concat(other)} will consist of
     * {@code [(A1, A2, A3, null), (B1, B2, B3, null), (C1, C2, C3, null), (C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]}.
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @return never null
     */
    default <D> QuadConstraintStream<A, B, C, D> concat(QuadConstraintStream<A, B, C, D> otherStream) {
        return concat(otherStream, triConstantNull());
    }

    /**
     * Returns a new {@link QuadConstraintStream} containing all the tuples of both this {@link TriConstraintStream}
     * and the provided {@link QuadConstraintStream}.
     * The {@link TriConstraintStream} tuples will be padded from the right by the result of the padding function.
     *
     * <p>
     * For instance, if this stream consists of {@code [(A1, A2, A3), (B1, B2, B3), (C1, C2, C3)]}
     * and the other stream consists of {@code [(C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]},
     * {@code this.concat(other, (a, b, c) -> null)} will consist of
     * {@code [(A1, A2, A3, null), (B1, B2, B3, null), (C1, C2, C3, null), (C1, C2, C3, C4), (D1, D2, D3, D4), (E1, E2, E3, E4)]}.
     * (Assuming that the padding function returns null for the given inputs.)
     * <p>
     * This operation can be thought of as an or between streams.
     *
     * @param otherStream never null
     * @param paddingFunction never null, function to find the padding for the fourth fact
     * @return never null
     */
    <D> QuadConstraintStream<A, B, C, D> concat(QuadConstraintStream<A, B, C, D> otherStream,
            TriFunction<A, B, C, D> paddingFunction);

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
     *
     * @param mapping function to produce the new fact from the original tuple
     * @return never null
     * @param <ResultD_> type of the final fact of the new tuple
     */
    <ResultD_> QuadConstraintStream<A, B, C, ResultD_> expand(TriFunction<A, B, C, ResultD_> mapping);

    // ************************************************************************
    // complement
    // ************************************************************************

    /**
     * As defined by {@link #complement(Class, Function, Function)},
     * where the padding function pads with null.
     */
    default TriConstraintStream<A, B, C> complement(Class<A> otherClass) {
        return complement(otherClass, uniConstantNull(), uniConstantNull());
    }

    /**
     * Adds to the stream all instances of a given class which are not yet present in it.
     * These instances must be present in the solution,
     * which means the class needs to be either a planning entity or a problem fact.
     * <p>
     * The instances will be read from the first element of the input tuple.
     * When an output tuple needs to be created for the newly inserted instances,
     * the first element will be the new instance.
     * The rest of the tuple will be padded with the results of the padding functions,
     * applied on the new instance.
     *
     * @param otherClass never null
     * @param paddingFunctionB never null, function to find the padding for the second fact
     * @param paddingFunctionC never null, function to find the padding for the third fact
     * @return never null
     */
    default TriConstraintStream<A, B, C> complement(Class<A> otherClass, Function<A, B> paddingFunctionB,
            Function<A, C> paddingFunctionC) {
        var firstStream = this;
        var remapped = firstStream.map(ConstantLambdaUtils.triPickFirst());
        var secondStream = getConstraintFactory().forEach(otherClass)
                .ifNotExists(remapped, Joiners.equal());
        return firstStream.concat(secondStream, paddingFunctionB, paddingFunctionC);
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    /**
     * As defined by {@link #penalize(Score, ToIntTriFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalize(Score_ constraintWeight) {
        return penalize(constraintWeight, triConstantOne());
    }

    /**
     * As defined by {@link #penalizeLong(Score, ToLongTriFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeLong(Score_ constraintWeight) {
        return penalizeLong(constraintWeight, triConstantOneLong());
    }

    /**
     * As defined by {@link #penalizeBigDecimal(Score, TriFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeBigDecimal(Score_ constraintWeight) {
        return penalizeBigDecimal(constraintWeight, triConstantOneBigDecimal());
    }

    /**
     * Applies a negative {@link Score} impact,
     * subtracting the constraintWeight multiplied by the match weight,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight specified here can be overridden using {@link ConstraintWeightOverrides}
     * on the {@link PlanningSolution}-annotated class
     * <p>
     * For non-int {@link Score} types use {@link #penalizeLong(Score, ToLongTriFunction)} or
     * {@link #penalizeBigDecimal(Score, TriFunction)} instead.
     *
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalize(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #penalize(Score, ToIntTriFunction)}, with a penalty of type long.
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeLong(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #penalize(Score, ToIntTriFunction)}, with a penalty of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeBigDecimal(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher);

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
    default TriConstraintBuilder<A, B, C, ?> penalizeConfigurable() {
        return penalizeConfigurable(triConstantOne());
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
     * @deprecated Prefer {@link #penalize(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> penalizeConfigurable(ToIntTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #penalizeConfigurable(ToIntTriFunction)}, with a penalty of type long.
     * 
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> penalizeConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #penalizeConfigurable(ToIntTriFunction)}, with a penalty of type {@link BigDecimal}.
     * 
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> penalizeConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntTriFunction)}, where the match weight is one (1).
     *
     * @return never null
     */
    default <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> reward(Score_ constraintWeight) {
        return reward(constraintWeight, triConstantOne());
    }

    /**
     * Applies a positive {@link Score} impact,
     * adding the constraintWeight multiplied by the match weight,
     * and returns a builder to apply optional constraint properties.
     * <p>
     * The constraintWeight specified here can be overridden using {@link ConstraintWeightOverrides}
     * on the {@link PlanningSolution}-annotated class
     * <p>
     * For non-int {@link Score} types use {@link #rewardLong(Score, ToLongTriFunction)} or
     * {@link #rewardBigDecimal(Score, TriFunction)} instead.
     *
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> reward(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntTriFunction)}, with a penalty of type long.
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> rewardLong(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #reward(Score, ToIntTriFunction)}, with a penalty of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> rewardBigDecimal(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher);

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
    default TriConstraintBuilder<A, B, C, ?> rewardConfigurable() {
        return rewardConfigurable(triConstantOne());
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
     * @deprecated Prefer {@link #reward(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> rewardConfigurable(ToIntTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #rewardConfigurable(ToIntTriFunction)}, with a penalty of type long.
     * 
     * @deprecated Prefer {@link #rewardLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> rewardConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #rewardConfigurable(ToIntTriFunction)}, with a penalty of type {@link BigDecimal}.
     * 
     * @deprecated Prefer {@link #rewardBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> rewardConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher);

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
    default <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impact(Score_ constraintWeight) {
        return impact(constraintWeight, triConstantOne());
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
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impact(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #impact(Score, ToIntTriFunction)}, with an impact of type long.
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impactLong(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #impact(Score, ToIntTriFunction)}, with an impact of type {@link BigDecimal}.
     */
    <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impactBigDecimal(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher);

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
    default TriConstraintBuilder<A, B, C, ?> impactConfigurable() {
        return impactConfigurable(triConstantOne());
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
     * @deprecated Prefer {@link #impact(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> impactConfigurable(ToIntTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #impactConfigurable(ToIntTriFunction)}, with an impact of type long.
     * 
     * @deprecated Prefer {@link #impactLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> impactConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher);

    /**
     * As defined by {@link #impactConfigurable(ToIntTriFunction)}, with an impact of type BigDecimal.
     * 
     * @deprecated Prefer {@link #impactBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    TriConstraintBuilder<A, B, C, ?> impactConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher);

    // ************************************************************************
    // Deprecated declarations
    // ************************************************************************

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, QuadJoiner, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, QuadJoiner, QuadJoiner, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, QuadJoiner, QuadJoiner, QuadJoiner, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3, QuadJoiner<A, B, C, D> joiner4) {
        return ifExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifExistsIncludingUnassigned(Class, QuadJoiner...)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifExistsIncludingNullVars(Class<D> otherClass,
            QuadJoiner<A, B, C, D>... joiners) {
        return ifExistsIncludingUnassigned(otherClass, joiners);
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner, QuadJoiner, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner, QuadJoiner, QuadJoiner, QuadJoiner)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingNullVars(Class<D> otherClass, QuadJoiner<A, B, C, D> joiner1,
            QuadJoiner<A, B, C, D> joiner2, QuadJoiner<A, B, C, D> joiner3, QuadJoiner<A, B, C, D> joiner4) {
        return ifNotExistsIncludingUnassigned(otherClass, new QuadJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * @deprecated Prefer {@link #ifNotExistsIncludingUnassigned(Class, QuadJoiner...)}.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <D> TriConstraintStream<A, B, C> ifNotExistsIncludingNullVars(Class<D> otherClass,
            QuadJoiner<A, B, C, D>... joiners) {
        return ifNotExistsIncludingUnassigned(otherClass, joiners);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     * <p>
     * For non-int {@link Score} types use {@link #penalizeLong(String, Score, ToLongTriFunction)} or
     * {@link #penalizeBigDecimal(String, Score, TriFunction)} instead.
     *
     * @deprecated Prefer {@link #penalize(Score, ToIntTriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalize(String constraintName, Score<?> constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return penalize((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalize(String, Score, ToIntTriFunction)}.
     *
     * @deprecated Prefer {@link #penalize(Score, ToIntTriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalize(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return penalize((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     *
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongTriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeLong(String constraintName, Score<?> constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return penalizeLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeLong(String, Score, ToLongTriFunction)}.
     *
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongTriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return penalizeLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #penalize(String, Score)}.
     *
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, TriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeBigDecimal(String constraintName, Score<?> constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return penalizeBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeBigDecimal(String, Score, TriFunction)}.
     *
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, TriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return penalizeBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #penalizeConfigurable(String)}.
     * <p>
     * For non-int {@link Score} types use {@link #penalizeConfigurableLong(String, ToLongTriFunction)} or
     * {@link #penalizeConfigurableBigDecimal(String, TriFunction)} instead.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #penalize(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurable(String constraintName, ToIntTriFunction<A, B, C> matchWeigher) {
        return penalizeConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurable(String, ToIntTriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalize(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurable(String constraintPackage, String constraintName,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return penalizeConfigurable(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Negatively impact the {@link Score}: subtract the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #penalizeConfigurable(String)}.
     *
     * @deprecated Prefer {@link #penalizeConfigurableLong(ToLongTriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableLong(String constraintName, ToLongTriFunction<A, B, C> matchWeigher) {
        return penalizeConfigurableLong(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #penalizeConfigurableLong(String, ToLongTriFunction)}.
     *
     * @deprecated Prefer {@link #penalizeConfigurableLong(ToLongTriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalizeLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableLong(String constraintPackage, String constraintName,
            ToLongTriFunction<A, B, C> matchWeigher) {
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
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableBigDecimal(String constraintName,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return penalizeConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);

    }

    /**
     * As defined by {@link #penalizeConfigurableBigDecimal(String, TriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #penalizeBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint penalizeConfigurableBigDecimal(String constraintPackage, String constraintName,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return penalizeConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     * <p>
     * For non-int {@link Score} types use {@link #rewardLong(String, Score, ToLongTriFunction)} or
     * {@link #rewardBigDecimal(String, Score, TriFunction)} instead.
     *
     * @deprecated Prefer {@link #reward(Score, ToIntTriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint reward(String constraintName, Score<?> constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return reward((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #reward(String, Score, ToIntTriFunction)}.
     *
     * @deprecated Prefer {@link #reward(Score, ToIntTriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint reward(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return reward((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     *
     * @deprecated Prefer {@link #rewardLong(Score, ToLongTriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */

    @Deprecated(forRemoval = true)
    default Constraint rewardLong(String constraintName, Score<?> constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return rewardLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardLong(String, Score, ToLongTriFunction)}.
     *
     * @deprecated Prefer {@link #rewardLong(Score, ToLongTriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return rewardLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #reward(String, Score)}.
     *
     * @deprecated Prefer {@link #rewardBigDecimal(Score, TriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardBigDecimal(String constraintName, Score<?> constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return rewardBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardBigDecimal(String, Score, TriFunction)}.
     *
     * @deprecated Prefer {@link #rewardBigDecimal(Score, TriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return rewardBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively impact the {@link Score}: add the {@link ConstraintWeight} multiplied by the match weight.
     * Otherwise as defined by {@link #rewardConfigurable(String)}.
     * <p>
     * For non-int {@link Score} types use {@link #rewardConfigurableLong(String, ToLongTriFunction)} or
     * {@link #rewardConfigurableBigDecimal(String, TriFunction)} instead.
     *
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     * @deprecated Prefer {@link #reward(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurable(String constraintName, ToIntTriFunction<A, B, C> matchWeigher) {
        return rewardConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurable(String, ToIntTriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #reward(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurable(String constraintPackage, String constraintName,
            ToIntTriFunction<A, B, C> matchWeigher) {
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
     * @deprecated Prefer {@link #rewardLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableLong(String constraintName, ToLongTriFunction<A, B, C> matchWeigher) {
        return rewardConfigurableLong(matchWeigher)
                .asConstraint(constraintName);

    }

    /**
     * As defined by {@link #rewardConfigurableLong(String, ToLongTriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #rewardLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableLong(String constraintPackage, String constraintName,
            ToLongTriFunction<A, B, C> matchWeigher) {
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
     * @deprecated Prefer {@link #rewardBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableBigDecimal(String constraintName,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return rewardConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #rewardConfigurableBigDecimal(String, TriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #rewardBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint rewardConfigurableBigDecimal(String constraintPackage, String constraintName,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
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
     * For non-int {@link Score} types use {@link #impactLong(String, Score, ToLongTriFunction)} or
     * {@link #impactBigDecimal(String, Score, TriFunction)} instead.
     *
     * @deprecated Prefer {@link #impact(Score, ToIntTriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impact(String constraintName, Score<?> constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return impact((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impact(String, Score, ToIntTriFunction)}.
     *
     * @deprecated Prefer {@link #impact(Score, ToIntTriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impact(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
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
     * @deprecated Prefer {@link #impactLong(Score, ToLongTriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactLong(String constraintName, Score<?> constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return impactLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactLong(String, Score, ToLongTriFunction)}.
     *
     * @deprecated Prefer {@link #impactLong(Score, ToLongTriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return impactLong((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the constraintWeight multiplied by the match weight.
     * Otherwise as defined by {@link #impact(String, Score)}.
     * <p>
     * Use {@code penalizeBigDecimal(...)} or {@code rewardBigDecimal(...)} unless you intend to mix positive and
     * negative weights.
     *
     * @deprecated Prefer {@link #impactBigDecimal(Score, TriFunction)}.
     * @param constraintName never null, shows up in {@link ConstraintMatchTotal} during score justification
     * @param constraintWeight never null
     * @param matchWeigher never null, the result of this function (matchWeight) is multiplied by the constraintWeight
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactBigDecimal(String constraintName, Score<?> constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return impactBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactBigDecimal(String, Score, TriFunction)}.
     *
     * @deprecated Prefer {@link #impactBigDecimal(Score, TriFunction)}.
     * @param constraintPackage never null
     * @param constraintName never null
     * @param constraintWeight never null
     * @param matchWeigher never null
     * @return never null
     */
    @Deprecated(forRemoval = true)
    default Constraint impactBigDecimal(String constraintPackage, String constraintName, Score<?> constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return impactBigDecimal((Score) constraintWeight, matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the {@link ConstraintWeight} for each match.
     * <p>
     * Use {@code penalizeConfigurable(...)} or {@code rewardConfigurable(...)} instead, unless this constraint can both
     * have positive and negative weights.
     * <p>
     * For non-int {@link Score} types use {@link #impactConfigurableLong(String, ToLongTriFunction)} or
     * {@link #impactConfigurableBigDecimal(String, TriFunction)} instead.
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
     * @deprecated Prefer {@link #impact(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurable(String constraintName, ToIntTriFunction<A, B, C> matchWeigher) {
        return impactConfigurable(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactConfigurable(String, ToIntTriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impact(Score, ToIntTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurable(String constraintPackage, String constraintName,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return impactConfigurable(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the {@link ConstraintWeight} for each match.
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
     * @deprecated Prefer {@link #impactLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableLong(String constraintName, ToLongTriFunction<A, B, C> matchWeigher) {
        return impactConfigurableLong(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactConfigurableLong(String, ToLongTriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impactLong(Score, ToLongTriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableLong(String constraintPackage, String constraintName,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return impactConfigurableLong(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }

    /**
     * Positively or negatively impact the {@link Score} by the {@link ConstraintWeight} for each match.
     * <p>
     * Use {@code penalizeConfigurableBigDecimal(...)} or {@code rewardConfigurableLong(...)} instead, unless this
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
     * @deprecated Prefer {@link #impactBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableBigDecimal(String constraintName,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return impactConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintName);
    }

    /**
     * As defined by {@link #impactConfigurableBigDecimal(String, TriFunction)}.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @param matchWeigher never null
     * @return never null
     * @deprecated Prefer {@link #impactBigDecimal(Score, TriFunction)} and {@link ConstraintWeightOverrides}.
     */
    @Deprecated(forRemoval = true)
    default Constraint impactConfigurableBigDecimal(String constraintPackage, String constraintName,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return impactConfigurableBigDecimal(matchWeigher)
                .asConstraint(constraintPackage, constraintName);
    }
}
