package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * For each assigned value that is not pinned, creates a move to reassign it to a different position in a list variable.
 * <p>
 * When {@code crossingNull} is {@code true} (the default when the variable
 * {@link PlanningListVariableMetaModel#allowsUnassignedValues() allows unassigned values}),
 * this provider also creates unassigned-to-list (assign) and list-to-unassigned (unassign) moves.
 * <p>
 * This does not remove the need for {@code ListAssignMoveProvider} and {@code ListUnassignMoveProvider}:
 * here, a null-crossing move is one candidate among many, so it arrives rarely.
 * A configuration that wants such moves often should add {@code ListAssignMoveProvider}/{@code ListUnassignMoveProvider} in
 * addition to turning this flag off to avoid further oversampling.
 *
 * <p>
 * To reassign a value, creates:
 *
 * <ul>
 * <li>A move for every unpinned position in every entity's list variable to reassign the value before that position.</li>
 * <li>A move for every entity to reassign the value to the last position in the list variable.</li>
 * </ul>
 * <p>
 * This is a generic move provider that works with any list variable;
 * user-defined change move providers needn't be this complex, as they understand the specifics of the domain.
 * <p>
 * For a set of values drawn together and gathered at one destination, see {@code MassListChangeMoveProvider}.
 *
 * @see ListAssignMoveProvider Assigning a single unassigned value at a time.
 * @see ListUnassignMoveProvider Unassigning a single value at a time.
 * @see MassListChangeMoveProvider A set of values drawn together and gathered at one destination.
 */
@NullMarked
public final class ListChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean crossingNull;

    public ListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, variableMetaModel.allowsUnassignedValues());
    }

    /**
     * @param crossingNull if {@code true}, also creates assign and unassign moves;
     *        variable must {@link PlanningListVariableMetaModel#allowsUnassignedValues() allow unassigned values},
     *        otherwise the constructor throws {@link IllegalArgumentException}
     */
    public ListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            boolean crossingNull) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (crossingNull && !variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException("""
                    The crossingNull (true) of variableMetaModel (%s) requires a variable \
                    which allows unassigned values, but this variable does not.
                    Maybe set crossingNull to false."""
                    .formatted(variableMetaModel));
        }
        this.crossingNull = crossingNull;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        // Only widen to forEachDestinationIncludingUnassigned when crossingNull:
        // unlike forEachDestination, it represents the unassigned destination with a null entity internally,
        // which entity-provided value ranges cannot resolve -
        // avoid tripping that path when this provider has no use for it anyway.
        var destinationDataset = (crossingNull
                ? moveStreamFactory.forEachDestinationIncludingUnassigned(variableMetaModel)
                : moveStreamFactory.forEachDestination(variableMetaModel)
                        .map((solutionView, position) -> (ElementPosition) position))
                .asCachedDataset();
        // Unassigned values are admitted too when crossingNull, unconditionally otherwise excluded,
        // mirroring ChangeMoveProvider's own source-side widening.
        var sourceDataset = (crossingNull ? moveStreamFactory.forEach(variableMetaModel.type(), false)
                : moveStreamFactory.forEachAssignedValue(variableMetaModel))
                .asCachedDataset();
        // A source value paired with a destination searched by isValidChange, same as SubListChangeMoveProvider and MassListChangeMoveProvider -
        // not a persisted destinations x sources join, which used to make this provider pay for a second full cross product on top of forEachDestination's own entities x values join.
        return moveStreamFactory.buildMoveStream((session, random) -> new ListChangeMoveIterator<>(session, random,
                variableMetaModel, sourceDataset, destinationDataset));
    }

    @NullMarked
    private static final class ListChangeMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<Value_, ElementPosition> {

        private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<Value_> sourceValueIterator;
        private final UniDatasetInstance<ElementPosition> destinationInstance;

        private @Nullable Move<Solution_> nextMove = null;

        ListChangeMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Value_> sourceDataset, UniDataset<Solution_, ElementPosition> destinationDataset) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            var sourceInstance = (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(sourceDataset);
            this.sourceValueIterator = sourceInstance.retiringRandomIterator(random);
            this.destinationInstance = session.getInstance(destinationDataset);
        }

        @Override
        public boolean hasNext() {
            return nextMove != null || RetiringBiWalk.advance(sourceValueIterator, this);
        }

        @Override
        public Move<Solution_> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var move = Objects.requireNonNull(nextMove);
            nextMove = null;
            return move;
        }

        @Override
        public Iterator<ElementPosition> createRightIterator(Value_ sourceValue) {
            var bailOutSize = destinationInstance.size() * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            return new FilteringIterator<>(destinationInstance.iterator(random),
                    destination -> isValidChange(sourceValue, destination), bailOutSize);
        }

        private boolean isValidChange(Value_ value, ElementPosition targetPosition) {
            var currentPosition = solutionView.getPositionOf(variableMetaModel, value);
            if (currentPosition.equals(targetPosition)) { // No change needed; also excludes both-unassigned.
                return false;
            }

            if (currentPosition instanceof PositionInList currentAssigned) {
                if (targetPosition instanceof PositionInList targetAssigned) {
                    if (currentAssigned.entity() == targetAssigned.entity()) { // The value is already in the list.
                        var valueCount = solutionView.countValues(variableMetaModel, currentAssigned.entity());
                        if (valueCount == 1) { // The value is the only value in the list; no change.
                            return false;
                        } else {
                            // Either same list, same position (ignore),
                            // or trying to move the value past the end of the list.
                            return targetAssigned.index() != valueCount;
                        }
                    }
                    // We can move freely between entities, assuming the target entity accepts the value.
                    return solutionView.isValueInRange(variableMetaModel, targetAssigned.entity(), value);
                } else { // Unassigning an assigned value never violates a value range.
                    return true;
                }
            } else { // Assigning a currently unassigned value; targetPosition is a PositionInList (checked above).
                var targetAssigned = (PositionInList) targetPosition;
                return solutionView.isValueInRange(variableMetaModel, targetAssigned.entity(), value);
            }
        }

        @Override
        public void accept(Value_ sourceValue, ElementPosition targetPosition) {
            var currentPosition = solutionView.getPositionOf(variableMetaModel, sourceValue);
            if (currentPosition instanceof PositionInList currentAssigned) {
                if (targetPosition instanceof PositionInList targetAssigned) {
                    nextMove = Moves.change(variableMetaModel, currentAssigned, targetAssigned);
                } else { // Assigned value moving to the unassigned pool.
                    nextMove = Moves.unassign(variableMetaModel, currentAssigned);
                }
            } else { // Unassigned value moving into the list; isValidChange already excluded both-unassigned.
                nextMove = Moves.assign(variableMetaModel, sourceValue, (PositionInList) targetPosition);
            }
        }

    }

}
