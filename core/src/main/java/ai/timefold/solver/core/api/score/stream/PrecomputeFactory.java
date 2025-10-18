package ai.timefold.solver.core.api.score.stream;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
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
}
