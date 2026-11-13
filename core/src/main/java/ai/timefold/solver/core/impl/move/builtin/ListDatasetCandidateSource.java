package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws destinations and swap partners from datasets, for a list variable with a solution value range.
 * Every entity accepts every value, so the drawn candidates do not depend on the value.
 * Whether the unassigned destination is included is decided by the destination dataset.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class ListDatasetCandidateSource<Solution_, Entity_, Value_>
        implements ListCandidateSource<Entity_, Value_> {

    private final SolutionView<Solution_> solutionView;
    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final @Nullable UniDatasetInstance<ElementPosition> destinationInstance;
    private final @Nullable Iterator<@Nullable ElementPosition> destinationIterator;
    private final @Nullable UniDatasetInstance<Value_> partnerInstance;
    private final @Nullable Iterator<@Nullable Value_> partnerIterator;

    /**
     * @param solutionView the view of the working solution
     * @param variableMetaModel the list variable
     * @param random the random generator of the move iterator
     * @param destinationInstance the destinations; null if the move iterator draws none
     * @param partnerInstance the swap partners; null if the move iterator draws none
     */
    public ListDatasetCandidateSource(SolutionView<Solution_> solutionView,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, RandomGenerator random,
            @Nullable UniDatasetInstance<ElementPosition> destinationInstance,
            @Nullable UniDatasetInstance<Value_> partnerInstance) {
        this.solutionView = Objects.requireNonNull(solutionView);
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.destinationInstance = destinationInstance;
        this.destinationIterator = destinationInstance == null ? null : destinationInstance.iterator(random);
        this.partnerInstance = partnerInstance;
        this.partnerIterator = partnerInstance == null ? null : partnerInstance.iterator(random);
    }

    @Override
    public @Nullable ElementPosition drawDestination(Value_ value, Predicate<ElementPosition> filter) {
        return AbstractLeftDrivenMoveIterator.drawMatching(Objects.requireNonNull(destinationIterator),
                Objects.requireNonNull(destinationInstance).size(), position -> position != null && filter.test(position));
    }

    @Override
    public @Nullable Value_ drawPartner(Value_ value, Predicate<Value_> filter) {
        return AbstractLeftDrivenMoveIterator.drawMatching(Objects.requireNonNull(partnerIterator),
                Objects.requireNonNull(partnerInstance).size(), partner -> partner != null && filter.test(partner));
    }

    @Override
    public @Nullable ElementPosition drawDestinationOnce(Predicate<ElementPosition> filter) {
        return drawOnce(Objects.requireNonNull(destinationIterator), filter);
    }

    @Override
    public @Nullable Value_ drawPartnerOnce(Predicate<Value_> filter) {
        return drawOnce(Objects.requireNonNull(partnerIterator), filter);
    }

    private static <T> @Nullable T drawOnce(Iterator<@Nullable T> iterator, Predicate<T> filter) {
        var candidate = iterator.hasNext() ? iterator.next() : null;
        return candidate != null && filter.test(candidate) ? candidate : null;
    }

    /**
     * @return the first value; every value has the same range
     */
    @Override
    public Value_ narrowestValue(Iterable<Value_> values) {
        return values.iterator().next();
    }

    /**
     * @return the first value; every value has the same range
     */
    @Override
    public Value_ narrowestValue(Entity_ entity, int fromIndex, int toIndex) {
        return solutionView.getValueAtIndex(variableMetaModel, entity, fromIndex);
    }

}
