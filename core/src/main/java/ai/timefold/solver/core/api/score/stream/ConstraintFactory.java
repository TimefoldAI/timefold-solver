package ai.timefold.solver.core.api.score.stream;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

import org.jspecify.annotations.NullMarked;

/**
 * The factory to create every {@link ConstraintStream} (for example with {@link #forEach(Class)})
 * which ends in a {@link Constraint} returned by {@link ConstraintProvider#defineConstraints(ConstraintFactory)}.
 */
@NullMarked
public interface ConstraintFactory {

    // ************************************************************************
    // forEach*
    // ************************************************************************

    /**
     * Start a {@link ConstraintStream} of all instances of the sourceClass
     * that are known as {@link ProblemFactCollectionProperty problem facts} or {@link PlanningEntity planning entities}.
     * <p>
     * If the sourceClass is a {@link PlanningEntity}, then it is automatically
     * {@link UniConstraintStream#filter(Predicate) filtered} to only contain entities
     * that are {@link ShadowVariablesInconsistent consistent}
     * and for which each genuine {@link PlanningVariable} (of the sourceClass or a superclass thereof) has a non-null value.
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
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     */
    <A> UniConstraintStream<A> forEach(Class<A> sourceClass);

    /**
     * As defined by {@link #forEach(Class)},
     * but without any filtering of unassigned {@link PlanningEntity planning entities}
     * (for {@link PlanningVariable#allowsUnassigned()})
     * or shadow entities not assigned to any applicable list variable
     * (for {@link PlanningListVariable#allowsUnassignedValues()}).
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     */
    <A> UniConstraintStream<A> forEachIncludingUnassigned(Class<A> sourceClass);

    /**
     * As defined by {@link #forEach(Class)},
     * but without any filtering of {@link ShadowVariablesInconsistent inconsistent} or unassigned {@link PlanningEntity
     * planning entities}
     * (for {@link PlanningVariable#allowsUnassigned()})
     * or shadow entities not assigned to any applicable list variable
     * (for {@link PlanningListVariable#allowsUnassignedValues()}).
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     */
    <A> UniConstraintStream<A> forEachUnfiltered(Class<A> sourceClass);

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
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which the {@link BiJoiner} is true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass,
            BiJoiner<A, A> joiner) {
        return forEachUniquePair(sourceClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #forEachUniquePair(Class, BiJoiner)}.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass,
            BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2) {
        return forEachUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #forEachUniquePair(Class, BiJoiner)}.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass,
            BiJoiner<A, A> joiner1, BiJoiner<A, A> joiner2,
            BiJoiner<A, A> joiner3) {
        return forEachUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #forEachUniquePair(Class, BiJoiner)}.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    default <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass,
            BiJoiner<A, A> joiner1, BiJoiner<A, A> joiner2,
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
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    <A> BiConstraintStream<A, A> forEachUniquePair(Class<A> sourceClass, BiJoiner<A, A>... joiners);

    // ************************************************************************
    // staticData
    //************************************************************************

    /**
     * Computes and caches the tuples that would be produced by the given stream.
     * <p>
     * IMPORTANT: As this is cached, it is vital the stream does not reference any variables
     * (genuine or otherwise), as a score corruption would occur.
     * <p>
     * For example, if employee is a {@link PlanningVariable} on Shift (a {@link PlanningEntity}),
     * and start/end are facts on Shift, the following Constraint would cause a score corruption:
     * 
     * <pre>
     * BiConstraintStream&lt;Shift, Shift&gt; overlappingShifts(PrecomputeFactory precomputeFactory) {
     *     return precomputeFactory.forEachUnfiltered(Shift.class)
     *             .join(Shift.class,
     *                     Joiners.overlapping(Shift::getStart, Shift::getEnd),
     *                     Joiners.equal(Shift::getEmployee))
     *             .filter((left, right) -> left != right);
     * }
     *
     * Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
     *     return constraintFactory.precompute(this::overlappingShifts)
     *             .penalize(HardSoftScore.ONE_HARD)
     *             .asConstraint("Overlapping shifts");
     * }
     * </pre>
     * <p>
     * You can (and should) use variables after the precompute. So the example above
     * can be rewritten correctly like this and would not cause score corruptions:
     * <p>
     * 
     * <pre>
     * BiConstraintStream&lt;Shift, Shift&gt; overlappingShifts(PrecomputeFactory precomputeFactory) {
     *     return precomputeFactory.forEachUnfiltered(Shift.class)
     *             .join(Shift.class,
     *                     Joiners.overlapping(Shift::getStart, Shift::getEnd))
     *             .filter((left, right) -> left != right);
     * }
     *
     * Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
     *     return constraintFactory.precompute(this::overlappingShifts)
     *             .filter((left, right) -> left.getEmployee() != null &amp;&amp; left.getEmployee().equals(right.getEmployee()))
     *             .penalize(HardSoftScore.ONE_HARD)
     *             .asConstraint("Overlapping shifts");
     * }
     * </pre>
     */
    <Stream_ extends ConstraintStream> Stream_
            precompute(Function<PrecomputeFactory, Stream_> precomputeSupplier);

}
