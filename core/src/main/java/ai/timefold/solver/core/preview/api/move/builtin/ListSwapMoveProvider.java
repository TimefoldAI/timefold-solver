package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.AbstractRepeatedDrawMoveIterator;
import ai.timefold.solver.core.impl.move.builtin.ListCandidatePoolSource;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ListSwapMoveProvider<Solution_, Entity_, Value_> implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        // Unassigned values are admitted only when the variable allows them:
        // the one-side-unassigned case below composes unassign+assign, which would otherwise
        // leave a value unassigned on a variable that forbids it.
        var valueStream =
                MoveProviderUtil.values(moveStreamFactory, variableMetaModel, variableMetaModel.allowsUnassignedValues())
                        // No diamond: ECJ infers a type from it that makes the casts below fail to compile.
                        .map((solutionView, value) -> new FullElementPosition<Value_>(value,
                                solutionView.getPositionOf(variableMetaModel, value)));
        // We do not exclude duplicate swaps (A<>B and B<>A) to keep it simple and fast.
        var valueDataset = valueStream.asCachedDataset();
        if (variableMetaModel.isValueRangeOnSolution()) {
            // Every entity accepts every value; only the swap itself can be invalid, so no range is checked.
            return moveStreamFactory.buildMoveStream((session, random) -> {
                var valueInstance =
                        (DefaultUniDatasetInstance<Solution_, FullElementPosition<Value_>>) session.getInstance(valueDataset);
                var partnerIterator = valueInstance.iterator(random);
                return new AbstractRepeatedDrawMoveIterator<>(valueInstance.retiringRandomIterator(random)) {
                    @Override
                    protected @Nullable Move<Solution_> tryBuildMove(FullElementPosition<Value_> left) {
                        var partner = drawMatching(partnerIterator, valueInstance.size(),
                                right -> right != null && isValidSwap(left.elementPosition(), right.elementPosition()));
                        return partner == null ? null : buildMove(left, partner);
                    }
                };
            });
        }
        // Partners come from the values which share a value range with the left value.
        var entityDataset = moveStreamFactory.forEach(variableMetaModel.entity().type(), false).asCachedDataset();
        var unassignedValueDataset = variableMetaModel.allowsUnassignedValues()
                ? moveStreamFactory.forEachUnassignedValue(variableMetaModel).asCachedDataset()
                : null;
        return moveStreamFactory.buildMoveStream((session, random) -> {
            var valueInstance =
                    (DefaultUniDatasetInstance<Solution_, FullElementPosition<Value_>>) session.getInstance(valueDataset);
            var solutionView = session.getSolutionView();
            var candidatePool = unassignedValueDataset != null
                    ? ListCandidatePoolSource.of(solutionView, variableMetaModel, random, session.getInstance(entityDataset),
                            session.getInstance(unassignedValueDataset))
                    : ListCandidatePoolSource.of(solutionView, variableMetaModel, random, session.getInstance(entityDataset),
                            false);
            return new AbstractRepeatedDrawMoveIterator<>(valueInstance.retiringRandomIterator(random)) {
                @Override
                protected @Nullable Move<Solution_> tryBuildMove(FullElementPosition<Value_> left) {
                    // An unassigned left value needs an assigned partner.
                    var partner = candidatePool.drawPartner(left.value(),
                            unassignedValueDataset != null && left.elementPosition() instanceof PositionInList,
                            value -> isValidPartner(solutionView, left, value));
                    return partner == null ? null
                            : buildMove(left, positionOf(solutionView, partner));
                }
            };
        });
    }

    private Move<Solution_> buildMove(FullElementPosition<Value_> a,
            FullElementPosition<Value_> b) {
        if (a.elementPosition() instanceof PositionInList aPosition
                && b.elementPosition() instanceof PositionInList bPosition) {
            return Moves.swap(variableMetaModel, aPosition, bPosition);
        }
        // Exactly one side is unassigned (isValidSwap already excluded both-unassigned):
        // unassign the assigned side, then assign the incoming value at the same index.
        var assignedPosition = (PositionInList) (a.elementPosition() instanceof PositionInList
                ? a.elementPosition()
                : b.elementPosition());
        var incomingValue = a.elementPosition() instanceof PositionInList ? b.value() : a.value();
        return Moves.compose(
                Moves.unassign(variableMetaModel, assignedPosition),
                Moves.assign(variableMetaModel, incomingValue, assignedPosition));
    }

    private FullElementPosition<Value_> positionOf(SolutionView<Solution_> solutionView, Value_ value) {
        return new FullElementPosition<>(value, solutionView.getPositionOf(variableMetaModel, value));
    }

    /**
     * @return false if both positions are the same, or both are unassigned
     */
    private static boolean isValidSwap(ElementPosition left, ElementPosition right) {
        return !left.equals(right) && (left instanceof PositionInList || right instanceof PositionInList);
    }

    /**
     * The candidate pool only gives an assigned partner from an entity which accepts the left value,
     * so only the left entity is checked.
     */
    private boolean isValidPartner(SolutionView<Solution_> solutionView, FullElementPosition<Value_> left, Value_ partner) {
        var leftPosition = left.elementPosition();
        if (!isValidSwap(leftPosition, solutionView.getPositionOf(variableMetaModel, partner))) {
            return false;
        }
        return !(leftPosition instanceof PositionInList leftAssigned)
                || solutionView.isValueInRange(variableMetaModel, leftAssigned.entity(), partner);
    }

    /**
     * @return {@code ListSwapMoveProvider(<Entity>.<variable>)}
     */
    @Override
    public String getId() {
        return MoveProviderUtil.id(ListSwapMoveProvider.class, variableMetaModel);
    }

    @NullMarked
    private record FullElementPosition<Value_>(Value_ value, ElementPosition elementPosition) {

        @Override
        public String toString() {
            return value + "@" + elementPosition;
        }

    }

}
