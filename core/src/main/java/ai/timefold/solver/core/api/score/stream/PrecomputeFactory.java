package ai.timefold.solver.core.api.score.stream;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

/**
 * Similar to a {@link ConstraintFactory}, except its methods (and the
 * {@link ConstraintStream}s they return) do not apply any automatic
 * filters (like those mentioned in {@link ConstraintFactory#forEach(Class)}).
 */
public interface PrecomputeFactory {
    /**
     * As defined by {@link ConstraintFactory#forEachUnfiltered(Class)},
     * with the additional change of any joining stream will also be unfiltered.
     * <p>
     * For example,
     * <p>
     * 
     * <pre>
     * precomputeFactory.forEachUnfiltered(Shift.class)
     *         .join(Shift.class, Joiners.equal(Shift::getLocation));
     * </pre>
     * <p>
     * Would roughly be equivalent to
     * <p>
     * 
     * <pre>
     * constraintFactory.forEachUnfiltered(Shift.class)
     *         .join(constraintFactory.forEachUnfiltered(Shift.class),
     *                 Joiners.equal(Shift::getLocation));
     * </pre>
     * <p>
     * Important: no variables can be referenced in any operations performed
     * by the returned {@link ConstraintStream}, otherwise a score corruption will
     * occur.
     * See the note in {@link ConstraintFactory#precompute(Function)} for
     * more details.
     * 
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     */
    <A> UniConstraintStream<A> forEachUnfiltered(Class<A> sourceClass);

    /**
     * As defined by {@link ConstraintFactory#forEachUniquePair(Class)},
     * with the additional change that the problem facts/entities are unfiltered.
     * <p>
     * For example,
     * <p>
     *
     * <pre>
     * precomputeFactory.forEachUnfilteredUniquePair(Shift.class);
     * </pre>
     * <p>
     * Would roughly be equivalent to
     * <p>
     *
     * <pre>
     * constraintFactory.forEachUnfiltered(Shift.class)
     *         .join(constraintFactory.forEachUnfiltered(Shift.class),
     *                 Joiners.lessThan(Shift::getId));
     * </pre>
     * <p>
     * Important: no variables can be referenced in any operations performed
     * by the returned {@link ConstraintStream}, otherwise a score corruption will
     * occur.
     * See the note in {@link ConstraintFactory#precompute(Function)} for
     * more details.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     */
    @SuppressWarnings("unchecked")
    default <A> BiConstraintStream<A, A> forEachUnfilteredUniquePair(Class<A> sourceClass) {
        return forEachUnfilteredUniquePair(sourceClass, new BiJoiner[] {});
    }

    /**
     * As defined by {@link ConstraintFactory#forEachUniquePair(Class, BiJoiner)},
     * with the additional change that the problem facts/entities are unfiltered.
     * <p>
     * For example,
     * <p>
     *
     * <pre>
     * precomputeFactory.forEachUnfilteredUniquePair(Shift.class, Joiners.equal(Shift::getLocation));
     * </pre>
     * <p>
     * Would roughly be equivalent to
     * <p>
     *
     * <pre>
     * constraintFactory.forEachUnfiltered(Shift.class)
     *         .join(constraintFactory.forEachUnfiltered(Shift.class),
     *                 Joiners.lessThan(Shift::getId),
     *                 Joiners.equal(Shift::getLocation));
     * </pre>
     * <p>
     * Important: no variables can be referenced in any operations performed
     * by the returned {@link ConstraintStream}, otherwise a score corruption will
     * occur.
     * See the note in {@link ConstraintFactory#precompute(Function)} for
     * more details.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     */
    @SuppressWarnings("unchecked")
    default <A> BiConstraintStream<A, A> forEachUnfilteredUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner) {
        return forEachUnfilteredUniquePair(sourceClass, new BiJoiner[] { joiner });
    }

    /**
     * As defined by {@link #forEachUnfilteredUniquePair(Class, BiJoiner)}.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    @SuppressWarnings("unchecked")
    default <A> BiConstraintStream<A, A> forEachUnfilteredUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2) {
        return forEachUnfilteredUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link #forEachUnfilteredUniquePair(Class, BiJoiner)}.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    @SuppressWarnings("unchecked")
    default <A> BiConstraintStream<A, A> forEachUnfilteredUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2, BiJoiner<A, A> joiner3) {
        return forEachUnfilteredUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * As defined by {@link #forEachUnfilteredUniquePair(Class, BiJoiner)}.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    @SuppressWarnings("unchecked")
    default <A> BiConstraintStream<A, A> forEachUnfilteredUniquePair(Class<A> sourceClass, BiJoiner<A, A> joiner1,
            BiJoiner<A, A> joiner2, BiJoiner<A, A> joiner3, BiJoiner<A, A> joiner4) {
        return forEachUnfilteredUniquePair(sourceClass, new BiJoiner[] { joiner1, joiner2, joiner3, joiner4 });
    }

    /**
     * As defined by {@link #forEachUnfilteredUniquePair(Class, BiJoiner)}.
     * <p>
     * This method causes <i>Unchecked generics array creation for varargs parameter</i> warnings,
     * but we can't fix it with a {@link SafeVarargs} annotation because it's an interface method.
     * Therefore, there are overloaded methods with up to 4 {@link BiJoiner} parameters.
     *
     * @param <A> the type of the matched problem fact or {@link PlanningEntity planning entity}
     * @return a stream that matches every unique combination of A and another A for which all the
     *         {@link BiJoiner joiners} are true
     */
    <A> BiConstraintStream<A, A> forEachUnfilteredUniquePair(Class<A> sourceClass, BiJoiner<A, A>... joiners);
}
