package ai.timefold.solver.core.impl.move.builtin;

import java.util.function.Predicate;

import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws destinations or swap partners for the values of a list variable,
 * so that a move iterator does not depend on where its value range is defined.
 * <ul>
 * <li>{@link ListCandidatePoolSource}: entity value ranges; only the entities which accept the value are drawn from.</li>
 * <li>{@link ListDatasetCandidateSource}: a solution value range; every entity accepts every value.</li>
 * </ul>
 * The searching draws ({@code drawDestination}, {@code drawPartner}) retry until the filter accepts,
 * within a bail-out budget.
 * The single draws ({@code drawDestinationOnce}, {@code drawPartnerOnce}) make one draw
 * from a pool that is the same for every value, for {@link AbstractSingleDrawMoveIterator}.
 *
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public sealed interface ListCandidateSource<Entity_, Value_>
        permits ListCandidatePoolSource, ListDatasetCandidateSource {

    /**
     * @param value the value to find a destination for
     * @param filter the caller's validity check; the destination entity is known to accept the value
     * @return null if no destination was accepted within the bail-out budget
     */
    @Nullable
    ElementPosition drawDestination(Value_ value, Predicate<ElementPosition> filter);

    /**
     * @param value the value to find a swap partner for
     * @param filter the caller's validity check; the partner's entity is known to accept the value
     * @return null if no partner was accepted within the bail-out budget
     */
    @Nullable
    Value_ drawPartner(Value_ value, Predicate<Value_> filter);

    /**
     * @param filter the caller's validity check, including whether the destination entity accepts the values
     * @return null if the draw did not give a valid destination
     */
    @Nullable
    ElementPosition drawDestinationOnce(Predicate<ElementPosition> filter);

    /**
     * @param filter the caller's validity check, including whether the partner's entity accepts the values
     * @return null if the draw did not give a valid partner
     */
    @Nullable
    Value_ drawPartnerOnce(Predicate<Value_> filter);

    /**
     * @param values at least one value
     * @return the value to draw candidates for, so that the fewest candidates are rejected
     */
    Value_ narrowestValue(Iterable<Value_> values);

    /**
     * As {@link #narrowestValue(Iterable)},
     * for the values of the entity from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * @param entity the entity which holds the values
     * @param fromIndex the first index, inclusive
     * @param toIndex the last index, exclusive; above {@code fromIndex}
     */
    Value_ narrowestValue(Entity_ entity, int fromIndex, int toIndex);

}
