package ai.timefold.solver.core.api.score.stream;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

/**
 * The factory to create every {@link ConstraintStream} (for example with {@link #forEach(Class)})
 * which ends in a {@link Constraint} returned by {@link ConstraintProvider#defineConstraints(ConstraintFactory)}.
 */
public interface ConstraintFactory {

    /**
     * @return never null
     * @deprecated Do not rely on any constraint package in user code.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    String getDefaultConstraintPackage();

    // ************************************************************************
    // forEach*
    // ************************************************************************

    /**
     * Start a {@link ConstraintStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts} or {@link PlanningEntity planning entities}.
     * <p>
     * If the sourceClass is a {@link PlanningEntity}, then it is automatically
     * {@link UniConstraintStream#filter(Predicate) filtered} to only contain entities
     * for which each genuine {@link PlanningVariable} (of the sourceClass or a superclass thereof) has a non-null value.
     * <p>
     * If the sourceClass is a shadow entity (an entity without any genuine planning variables),
     * and if there exists a genuine {@link PlanningEntity} with a {@link PlanningListVariable}
     * which accepts instances of this shadow entity as values in that list,
     * and if that list variable {@link PlanningListVariable#allowsUnassignedValues() allows unassigned values},
     * then this stream will filter out all sourceClass instances
     * which are not present in any instances of that list variable.
     * This is achieved in one of two ways:
     *
     * <ul>
     * <li>If the sourceClass has {@link InverseRelationShadowVariable} field
     * referencing instance of an entity with the list variable,
     * the value of that field will be used to determine if the value is assigned.
     * Null in that field means the instance of sourceClass is unassigned.</li>
     * <li>As fallback, the value is considered assigned if there exists
     * an instance of the entity where its list variable contains the value.
     * This will perform significantly worse and only exists
     * so that using the {@link InverseRelationShadowVariable} can remain optional.
     * Adding the field is strongly recommended.</li>
     * </ul>
     *
     * @param sourceClass never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return never null
     */
    <A> UniConstraintStream<A> forEach(Class<A> sourceClass);

    /**
     * As defined by {@link #forEachIncludingUnassigned(Class)}.
     * 
     * @deprecated Use {@link #forEachIncludingUnassigned(Class)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default <A> UniConstraintStream<A> forEachIncludingNullVars(Class<A> sourceClass) {
        return forEachIncludingUnassigned(sourceClass);
    }

    /**
     * As defined by {@link #forEach(Class)},
     * but without any filtering of unassigned {@link PlanningEntity planning entities}
     * (for {@link PlanningVariable#allowsUnassigned()})
     * or shadow entities not assigned to any applicable list variable
     * (for {@link PlanningListVariable#allowsUnassignedValues()}).
     *
     * @param sourceClass never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return never null
     */
    <A> UniConstraintStream<A> forEachIncludingUnassigned(Class<A> sourceClass);

    /**
     * Create a new {@link BiConstraintStream} for every unique combination of A and another A with a higher {@link PlanningId}.
     * <p>
     * Important: {@link BiConstraintStream#filter(BiPredicate) Filtering} this is slower and less scalable
     * than using a {@link #forEachUniquePair(Class, BiJoiner) joiner},
     * because it barely applies hashing and/or indexing on the properties,
     * so it creates and checks almost every combination of A and A.
     * <p>
     * This method is syntactic sugar for {@link UniConstraintStream#join(Class)}.
     * It automatically adds a {@link Joiners#lessThan(Function) lessThan} joiner on the {@link PlanningId} of A.
     *
     * @param sourceClass never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass) {
        return forEachUniquePair(sourceClass, new BiJoiner[0]);
    }

    /**
     * Create a new {@link BiConstraintStream} for every unique combination of A and another A with a higher {@link PlanningId}
     * for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * <p>
     * Important: This is faster and more scalable than not using a {@link #forEachUniquePair(Class)} joiner}
     * followed by a {@link BiConstraintStream#filter(BiPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks almost every combination of A and A.
     * <p>
     * This method is syntactic sugar for {@link UniConstraintStream#join(Class, BiJoiner)}.
     * It automatically adds a {@link Joiners#lessThan(Function) lessThan} joiner on the {@link PlanningId} of A.
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @param sourceClass never null
     * @param joiner never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which the {@link BiJoiner} is true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner) {
        return forEachUniquePair(sourceClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #forEachUniquePair(Class, BiJoiner)}.
     *
     * @param sourceClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2) {
        return forEachUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #forEachUniquePair(Class, BiJoiner)}.
     *
     * @param sourceClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner1, BiJoiner<A, A> joiner2,
            BiJoiner<A, A> joiner3) {
        return forEachUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #forEachUniquePair(Class, BiJoiner)}.
     *
     * @param sourceClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner1, BiJoiner<A, A> joiner2,
            BiJoiner<A, A> joiner3, BiJoiner<A, A> joiner4) {
        return forEachUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #forEachUniquePair(Class, BiJoiner)}.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param sourceClass never null
     * @param joiners never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass, BiJoiner<A, A>... joiners);

    // ************************************************************************
    // from* (deprecated)
    // ************************************************************************

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEach(Class)}, but first understand this:
     *
     * <ul>
     * <li>If none of your planning variables {@link PlanningVariable#allowsUnassigned() allow unassigned values},
     * then the replacement by {@link #forEach(Class)} has little to no impact.
     * Subsequent conditional propagation calls ({@link UniConstraintStream#ifExists} etc.)
     * will now also filter out planning entities with null variables,
     * consistently with {@link #forEach(Class)} family of methods and with joining.</li>
     * <li>If any of your planning variables {@link PlanningVariable#allowsUnassigned() allow unassigned values},
     * then there is severe impact.
     * Calls to the {@link #forEach(Class)} family of methods will now filter out planning entities with null variables,
     * so most constraints no longer need to do null checks,
     * but the constraint that penalizes unassigned entities (typically a medium constraint)
     * must now use {@link #forEachIncludingUnassigned(Class)} instead.
     * Subsequent joins and conditional propagation calls will now also consistently filter out planning entities with null
     * variables.</li>
     * </ul>
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * Start a {@link ConstraintStream} of all instances of the fromClass
     * that are known as {@link ProblemFactCollectionProperty problem facts} or {@link PlanningEntity planning entities}.
     * <p>
     * If the fromClass is a {@link PlanningEntity}, then it is automatically
     * {@link UniConstraintStream#filter(Predicate) filtered} to only contain fully initialized entities,
     * for which each genuine {@link PlanningVariable} (of the fromClass or a superclass thereof) is initialized.
     * This filtering will NOT automatically apply to genuine planning variables of subclass planning entities of the fromClass.
     *
     * @deprecated This method is deprecated in favor of {@link #forEach(Class)},
     *             which exhibits the same behavior for planning variables
     *             which both allow and don't allow unassigned values.
     * @param fromClass never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return never null
     */
    @Deprecated(forRemoval = true)
    <A> UniConstraintStream<A> from(Class<A> fromClass);

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEachIncludingUnassigned(Class)},
     * but first understand that subsequent joins and conditional propagation calls
     * ({@link UniConstraintStream#ifExists} etc.)
     * will now also consistently filter out planning entities with null variables.
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * As defined by {@link #from(Class)},
     * but without any filtering of uninitialized {@link PlanningEntity planning entities}.
     *
     * @deprecated Prefer {@link #forEachIncludingUnassigned(Class)}.
     * @param fromClass never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return never null
     */
    @Deprecated(forRemoval = true)
    <A> UniConstraintStream<A> fromUnfiltered(Class<A> fromClass);

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEachUniquePair(Class)},
     * but first understand that the same precautions apply as with the use of {@link #from(Class)}.
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * Create a new {@link BiConstraintStream} for every unique combination of A and another A with a higher {@link PlanningId}.
     * <p>
     * Important: {@link BiConstraintStream#filter(BiPredicate) Filtering} this is slower and less scalable
     * than using a {@link #fromUniquePair(Class, BiJoiner) joiner},
     * because it barely applies hashing and/or indexing on the properties,
     * so it creates and checks almost every combination of A and A.
     * <p>
     * This method is syntactic sugar for {@link UniConstraintStream#join(Class)}.
     * It automatically adds a {@link Joiners#lessThan(Function) lessThan} joiner on the {@link PlanningId} of A.
     *
     * @deprecated Prefer {@link #forEachUniquePair(Class)},
     *             which exhibits the same behavior for planning variables
     *             which both allow and don't allow unassigned values.
     * @param fromClass never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A
     */
    @Deprecated(forRemoval = true)
    default <A> BiConstraintStream<A, A> fromUniquePair(Class<A> fromClass) {
        return fromUniquePair(fromClass, new BiJoiner[0]);
    }

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEachUniquePair(Class, BiJoiner)},
     * but first understand that the same precautions apply as with the use of {@link #from(Class)}.
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * Create a new {@link BiConstraintStream} for every unique combination of A and another A with a higher {@link PlanningId}
     * for which the {@link BiJoiner} is true (for the properties it extracts from both facts).
     * <p>
     * Important: This is faster and more scalable than not using a {@link #fromUniquePair(Class)} joiner}
     * followed by a {@link BiConstraintStream#filter(BiPredicate) filter},
     * because it applies hashing and/or indexing on the properties,
     * so it doesn't create nor checks almost every combination of A and A.
     * <p>
     * This method is syntactic sugar for {@link UniConstraintStream#join(Class, BiJoiner)}.
     * It automatically adds a {@link Joiners#lessThan(Function) lessThan} joiner on the {@link PlanningId} of A.
     * <p>
     * This method has overloaded methods with multiple {@link BiJoiner} parameters.
     *
     * @deprecated Prefer {@link #forEachUniquePair(Class, BiJoiner)},
     *             which exhibits the same behavior for planning variables
     *             which both allow and don't allow unassigned values.
     * @param fromClass never null
     * @param joiner never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which the {@link BiJoiner} is true
     */
    @Deprecated(forRemoval = true)
    default <A> BiConstraintStream<A, A> fromUniquePair(Class<A> fromClass, BiJoiner<A, A> joiner) {
        return fromUniquePair(fromClass, new BiJoiner[] { joiner });
    }

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEachUniquePair(Class, BiJoiner, BiJoiner)},
     * but first understand that the same precautions apply as with the use of {@link #from(Class)}.
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * As defined by {@link #fromUniquePair(Class, BiJoiner)}.
     *
     * @deprecated Prefer {@link #forEachUniquePair(Class, BiJoiner, BiJoiner)},
     *             which exhibits the same behavior for planning variables
     *             which both allow and don't allow unassigned values.
     * @param fromClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    @Deprecated(forRemoval = true)
    default <A> BiConstraintStream<A, A> fromUniquePair(Class<A> fromClass, BiJoiner<A, A> joiner1, BiJoiner<A, A> joiner2) {
        return fromUniquePair(fromClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEachUniquePair(Class, BiJoiner, BiJoiner, BiJoiner)},
     * but first understand that the same precautions apply as with the use of {@link #from(Class)}.
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * As defined by {@link #fromUniquePair(Class, BiJoiner)}.
     *
     * @deprecated Prefer {@link #forEachUniquePair(Class, BiJoiner, BiJoiner, BiJoiner)},
     *             which exhibits the same behavior for planning variables
     *             which both allow and don't allow unassigned values.
     * @param fromClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    @Deprecated(forRemoval = true)
    default <A> BiConstraintStream<A, A> fromUniquePair(Class<A> fromClass, BiJoiner<A, A> joiner1, BiJoiner<A, A> joiner2,
            BiJoiner<A, A> joiner3) {
        return fromUniquePair(fromClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEachUniquePair(Class, BiJoiner, BiJoiner, BiJoiner, BiJoiner)},
     * but first understand that the same precautions apply as with the use of {@link #from(Class)}.
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * As defined by {@link #fromUniquePair(Class, BiJoiner)}.
     *
     * @deprecated Prefer {@link #forEachUniquePair(Class, BiJoiner, BiJoiner, BiJoiner, BiJoiner)},
     *             which exhibits the same behavior for planning variables
     *             which both allow and don't allow unassigned values.
     * @param fromClass never null
     * @param joiner1 never null
     * @param joiner2 never null
     * @param joiner3 never null
     * @param joiner4 never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    @Deprecated(forRemoval = true)
    default <A> BiConstraintStream<A, A> fromUniquePair(Class<A> fromClass, BiJoiner<A, A> joiner1, BiJoiner<A, A> joiner2,
            BiJoiner<A, A> joiner3, BiJoiner<A, A> joiner4) {
        return fromUniquePair(fromClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * This method is deprecated.
     * Migrate uses of this method to {@link #forEachUniquePair(Class, BiJoiner...)},
     * but first understand that the same precautions apply as with the use of {@link #from(Class)}.
     * <p>
     * The original Javadoc of this method follows:
     * <p>
     * As defined by {@link #fromUniquePair(Class, BiJoiner)}.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @deprecated Prefer {@link #forEachUniquePair(Class, BiJoiner...)},
     *             which exhibits the same behavior for planning variables
     *             which both allow and don't allow unassigned values.
     * @param fromClass never null
     * @param joiners never null
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    @Deprecated(forRemoval = true)
    <A> BiConstraintStream<A, A> fromUniquePair(Class<A> fromClass, BiJoiner<A, A>... joiners);

}
