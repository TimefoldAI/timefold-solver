package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSessionFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.picking.DefaultUniPickingStream;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.picking.UniPickingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveStreamFactory<Solution_>
        implements MoveStreamFactory<Solution_> {

    private final EnumeratingStreamFactory<Solution_> enumeratingStreamFactory;
    private final DatasetSessionFactory<Solution_> datasetSessionFactory;
    // In order for node sharing to work properly, the function instances must be identical.
    // Since these functions require the variable meta model, we need to cache them per variable meta model.
    private final Map<GenuineVariableMetaModel<Solution_, ?, ?>, NodeSharingSupportFunctions<Solution_, ?, ?>> nodeSharingSupportFunctionMap =
            new HashMap<>();
    private final Map<PlanningListVariableMetaModel<Solution_, ?, ?>, ListVariableNodeSharingSupportFunctions<Solution_, ?, ?>> listVariableNodeSharingSupportFunctionsMap =
            new HashMap<>();

    public DefaultMoveStreamFactory(SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        this.enumeratingStreamFactory = new EnumeratingStreamFactory<>(solutionDescriptor, environmentMode);
        this.datasetSessionFactory = new DatasetSessionFactory<>(enumeratingStreamFactory);
    }

    public DefaultNeighborhoodSession<Solution_> createSession(SessionContext<Solution_> context) {
        var session = datasetSessionFactory.buildSession(context);
        return new DefaultNeighborhoodSession<>(session, context.solutionView());
    }

    @Override
    public <A> UniEnumeratingStream<Solution_, A> forEach(Class<A> sourceClass, boolean includeNull) {
        var entityDescriptor = getSolutionDescriptor().findEntityDescriptor(sourceClass);
        if (entityDescriptor == null) { // Not an entity, can't be pinned.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (entityDescriptor.isGenuine()) { // Genuine entity can be pinned.
            return enumeratingStreamFactory.forEachExcludingPinned(sourceClass, includeNull);
        }
        // From now on, we are testing a shadow entity.
        var listVariableDescriptor = getSolutionDescriptor().getListVariableDescriptor();
        if (listVariableDescriptor == null) { // Can't be pinned when there are only basic variables.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (!listVariableDescriptor.supportsPinning()) { // The genuine entity does not support pinning.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (!listVariableDescriptor.acceptsValueType(sourceClass)) { // Can't be used as an element.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        // Finally a valid pin-supporting type.
        return enumeratingStreamFactory.forEachExcludingPinned(sourceClass, includeNull);
    }

    @Override
    public <A> UniEnumeratingStream<Solution_, A> forEachUnfiltered(Class<A> sourceClass, boolean includeNull) {
        return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
    }

    @SuppressWarnings("unchecked")
    public <Entity_, Value_> NodeSharingSupportFunctions<Solution_, Entity_, Value_>
            getNodeSharingSupportFunctions(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return (NodeSharingSupportFunctions<Solution_, Entity_, Value_>) nodeSharingSupportFunctionMap
                .computeIfAbsent(variableMetaModel, ignored -> new NodeSharingSupportFunctions<>(variableMetaModel));
    }

    @Override
    public <Entity_, Value_> UniEnumeratingStream<Solution_, Value_>
            forEachAssignedValueUnfiltered(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        var nodeSharingSupportFunctions = getNodeSharingSupportFunctions(variableMetaModel);
        return forEachUnfiltered(variableMetaModel.type(), false)
                .filter(nodeSharingSupportFunctions.assignedValueFilter);
    }

    @Override
    public <Entity_, Value_> UniEnumeratingStream<Solution_, Value_>
            forEachAssignedValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        var nodeSharingSupportFunctions = getNodeSharingSupportFunctions(variableMetaModel);
        return forEach(variableMetaModel.type(), false)
                .filter(nodeSharingSupportFunctions.assignedValueFilter);
    }

    @Override
    public <Entity_, Value_> UniEnumeratingStream<Solution_, Value_>
            forEachUnassignedValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        var nodeSharingSupportFunctions = getNodeSharingSupportFunctions(variableMetaModel);
        return forEach(variableMetaModel.type(), false)
                .filter(nodeSharingSupportFunctions.unassignedValueFilter);
    }

    @Override
    public <Entity_, Value_> UniEnumeratingStream<Solution_, PositionInList>
            forEachDestination(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        var nodeSharingSupportFunctions = getNodeSharingSupportFunctions(variableMetaModel);
        // Insert-before an unpinned assigned value: the value's own current position, entity-independent.
        // A value assigned to entity E is always in E's value range, so no join or range check is needed here:
        // the entity a position's join used to bring in was never anything other than the value's own entity.
        var valuePositions = forEachAssignedValue(variableMetaModel)
                .map(nodeSharingSupportFunctions.toOwnPositionMapper);
        // End-of-list slot, one per unpinned entity.
        var endPositions = forEach(variableMetaModel.entity().type(), false)
                .map(nodeSharingSupportFunctions.toEndOfListPositionMapper);
        return valuePositions.concat(endPositions);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <Entity_, Value_> UniEnumeratingStream<Solution_, ElementPosition>
            forEachDestinationIncludingUnassigned(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        if (!variableMetaModel.allowsUnassignedValues()) {
            return (UniEnumeratingStream) forEachDestination(variableMetaModel);
        }
        var nodeSharingSupportFunctions = getNodeSharingSupportFunctions(variableMetaModel);
        // The single UnassignedElement row; forEach(_, true) yields the null-entity row exactly once.
        var unassigned = forEach(variableMetaModel.entity().type(), true)
                .filter(nodeSharingSupportFunctions.isNullEntityFilter)
                .map(nodeSharingSupportFunctions.toUnassignedElementMapper);
        UniEnumeratingStream<Solution_, ElementPosition> destinations =
                (UniEnumeratingStream) forEachDestination(variableMetaModel);
        return destinations.concat(unassigned);
    }

    @SuppressWarnings("unchecked")
    public <Entity_, Value_> ListVariableNodeSharingSupportFunctions<Solution_, Entity_, Value_>
            getNodeSharingSupportFunctions(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return (ListVariableNodeSharingSupportFunctions<Solution_, Entity_, Value_>) listVariableNodeSharingSupportFunctionsMap
                .computeIfAbsent(variableMetaModel, ListVariableNodeSharingSupportFunctions::new);
    }

    @Override
    public <A> UniPickingStream<Solution_, A> pick(UniEnumeratingStream<Solution_, A> enumeratingStream) {
        return new DefaultUniPickingStream<>(
                ((AbstractUniEnumeratingStream<Solution_, A>) enumeratingStream).asCachedDataset());
    }

    @Override
    public MoveStream<Solution_> buildMoveStream(MoveIteratorProvider<Solution_> iteratorProvider) {
        return new IteratorMoveStream<>(iteratorProvider);
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return enumeratingStreamFactory.getSolutionDescriptor();
    }

    public record NodeSharingSupportFunctions<Solution_, Entity_, Value_>(
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniNeighborhoodsPredicate<Solution_, Entity_> assignedValueFilter,
            BiNeighborhoodsPredicate<Solution_, Entity_, Value_> differentValueFilter,
            BiNeighborhoodsPredicate<Solution_, Entity_, Value_> valueInRangeFilter) {

        public NodeSharingSupportFunctions(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
            this(variableMetaModel,
                    (solutionView, entity) -> solutionView.getValue(variableMetaModel, entity) != null,
                    (solutionView, entity, value) -> !Objects.equals(solutionView.getValue(variableMetaModel, entity), value),
                    (solutionView, entity, value) -> solutionView.isValueInRange(variableMetaModel, entity, value));
        }

    }

    public record ListVariableNodeSharingSupportFunctions<Solution_, Entity_, Value_>(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniNeighborhoodsPredicate<Solution_, Value_> assignedValueOrNullFilter,
            UniNeighborhoodsPredicate<Solution_, Value_> assignedValueFilter,
            UniNeighborhoodsPredicate<Solution_, Value_> unassignedValueFilter,
            BiNeighborhoodsPredicate<Solution_, Value_, PositionInList> valueInRangeFilterForPosition,
            UniNeighborhoodsMapper<Solution_, Value_, PositionInList> toOwnPositionMapper,
            UniNeighborhoodsMapper<Solution_, Entity_, PositionInList> toEndOfListPositionMapper,
            UniNeighborhoodsPredicate<Solution_, Entity_> isNullEntityFilter,
            UniNeighborhoodsMapper<Solution_, Entity_, ElementPosition> toUnassignedElementMapper) {

        public ListVariableNodeSharingSupportFunctions(
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
            this(variableMetaModel,
                    (solutionView, value) -> value == null || solutionView.isAssigned(variableMetaModel, value),
                    (solutionView, value) -> solutionView.isAssigned(variableMetaModel, value),
                    (solutionView, value) -> !solutionView.isAssigned(variableMetaModel, value),
                    (solutionView, value, positionInList) -> {
                        Entity_ entity = positionInList.entity();
                        if (value == null) {
                            // Necessary for the null to survive until the later stage, where we will use it as a special marker to move it to the end of list.
                            return true;
                        }
                        return solutionView.isValueInRange(variableMetaModel, entity, value);
                    },
                    // Insert-before this value: the value's own current position.
                    (solutionView, value) -> solutionView.getPositionOf(variableMetaModel, value).ensureAssigned(),
                    // Insert at the end of this entity's list.
                    (solutionView, entity) -> ElementPosition.of(entity, solutionView.countValues(variableMetaModel, entity)),
                    (solutionView, entity) -> entity == null,
                    (solutionView, entity) -> ElementPosition.unassigned());
        }

    }

}
