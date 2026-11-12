package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.builtin.ListCandidatePool;
import ai.timefold.solver.core.impl.move.builtin.ListChangeMoveIterator;
import ai.timefold.solver.core.impl.move.builtin.ListDatasetCandidateSource;
import ai.timefold.solver.core.impl.move.builtin.MoveProviderUtil;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

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
        // Unassigned values are admitted too when crossingNull, unconditionally otherwise excluded,
        // mirroring ChangeMoveProvider's own source-side widening.
        var sourceDataset = (crossingNull
                ? MoveProviderUtil.inValueRange(moveStreamFactory, variableMetaModel,
                        moveStreamFactory.forEach(variableMetaModel.type(), false))
                : moveStreamFactory.forEachAssignedValue(variableMetaModel))
                .asCachedDataset();
        if (variableMetaModel.isValueRangeOnSolution()) {
            // Only widen to forEachDestinationIncludingUnassigned when crossingNull:
            // unlike forEachDestination, it represents the unassigned destination with a null entity internally,
            // which entity-provided value ranges cannot resolve -
            // avoid tripping that path when this provider has no use for it anyway.
            var destinationDataset = (crossingNull
                    ? moveStreamFactory.forEachDestinationIncludingUnassigned(variableMetaModel)
                    : moveStreamFactory.forEachDestination(variableMetaModel)
                            .map((solutionView, position) -> (ElementPosition) position))
                    .asCachedDataset();
            return moveStreamFactory.buildMoveStream((session, random) -> new ListChangeMoveIterator<>(session, random,
                    variableMetaModel, (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(sourceDataset),
                    new ListDatasetCandidateSource<>(session.getSolutionView(), variableMetaModel, random,
                            session.getInstance(destinationDataset), null)));
        }
        // Destinations come from the entities which accept the source value.
        var entityDataset = moveStreamFactory.forEach(variableMetaModel.entity().type(), false).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new ListChangeMoveIterator<>(session, random,
                variableMetaModel, (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(sourceDataset),
                ListCandidatePool.of(session.getSolutionView(), variableMetaModel, random, session.getInstance(entityDataset),
                        crossingNull)));
    }

    /**
     * @return {@code ListChangeMoveProvider(<Entity>.<variable>,crossingNull|noCrossingNull)}
     */
    @Override
    public String getId() {
        return MoveProviderUtil.id(ListChangeMoveProvider.class, variableMetaModel,
                MoveProviderUtil.flag("crossingNull", crossingNull));
    }

}
