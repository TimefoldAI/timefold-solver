package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.EnumeratingJoiners;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingMapper;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingPredicate;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.UniEnumeratingFilter;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.UniSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSessionFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.sampling.DefaultUniSamplingStream;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveStreamFactory<Solution_>
        implements MoveStreamFactory<Solution_> {

    private final EnumeratingStreamFactory<Solution_> enumeratingStreamFactory;
    private final DatasetSessionFactory<Solution_> datasetSessionFactory;
    // In order for node sharing to work properly,
    // the function instances must be identical.
    // Since these functions require the variable meta model,
    // we need to cache them per variable meta model.
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
    public PlanningSolutionMetaModel<Solution_> getSolutionMetaModel() {
        return enumeratingStreamFactory.getSolutionDescriptor().getMetaModel();
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
    public <Entity_, Value_> UniEnumeratingStream<Solution_, ElementPosition>
            forEachAssignablePosition(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        // Stream with unpinned entities;
        // includes null if the variable allows unassigned values.
        var unpinnedEntities =
                forEach(variableMetaModel.entity().type(), variableMetaModel.allowsUnassignedValues());
        // Stream with unpinned values, which are assigned to any list variable;
        // always includes null so that we can later create a position at the end of the list,
        // i.e. with no value after it.
        var nodeSharingSupportFunctions = getNodeSharingSupportFunctions(variableMetaModel);
        var unpinnedValues = forEach(variableMetaModel.type(), true)
                .filter(nodeSharingSupportFunctions.unpinnedValueFilter);
        // Joins the two previous streams to create pairs of (entity, value),
        // eliminating values which do not match that entity's value range.
        // It maps these pairs to expected target positions in that entity's list variable.
        return unpinnedEntities.join(unpinnedValues,
                EnumeratingJoiners.filtering(nodeSharingSupportFunctions.valueInRangeFilter))
                .map(nodeSharingSupportFunctions.toElementPositionMapper)
                .distinct();
    }

    @SuppressWarnings("unchecked")
    public <Entity_, Value_> ListVariableNodeSharingSupportFunctions<Solution_, Entity_, Value_>
            getNodeSharingSupportFunctions(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return (ListVariableNodeSharingSupportFunctions<Solution_, Entity_, Value_>) listVariableNodeSharingSupportFunctionsMap
                .computeIfAbsent(variableMetaModel, ListVariableNodeSharingSupportFunctions::new);
    }

    @Override
    public <A> UniSamplingStream<Solution_, A> pick(UniEnumeratingStream<Solution_, A> enumeratingStream) {
        return new DefaultUniSamplingStream<>(
                ((AbstractUniEnumeratingStream<Solution_, A>) enumeratingStream).createLeftDataset());
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return enumeratingStreamFactory.getSolutionDescriptor();
    }

    public record NodeSharingSupportFunctions<Solution_, Entity_, Value_>(
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            BiEnumeratingPredicate<Solution_, Entity_, Value_> differentValueFilter,
            BiEnumeratingPredicate<Solution_, Entity_, Value_> valueInRangeFilter) {

        public NodeSharingSupportFunctions(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
            this(variableMetaModel,
                    (solutionView, entity, value) -> !Objects.equals(solutionView.getValue(variableMetaModel, entity), value),
                    (solutionView, entity, value) -> solutionView.isValueInRange(variableMetaModel, entity, value));
        }

    }

    public record ListVariableNodeSharingSupportFunctions<Solution_, Entity_, Value_>(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniEnumeratingFilter<Solution_, Value_> unpinnedValueFilter,
            BiEnumeratingPredicate<Solution_, Entity_, Value_> valueInRangeFilter,
            BiEnumeratingMapper<Solution_, Entity_, Value_, ElementPosition> toElementPositionMapper) {

        public ListVariableNodeSharingSupportFunctions(
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
            this(variableMetaModel,
                    (solutionView, value) -> value == null
                            || solutionView.getPositionOf(variableMetaModel, value) instanceof PositionInList,
                    (solutionView, entity, value) -> {
                        if (entity == null || value == null) {
                            // Necessary for the null to survive until the later stage,
                            // where we will use it as a special marker to either unassign the value,
                            // or move it to the end of list.
                            return true;
                        }
                        return solutionView.isValueInRange(variableMetaModel, entity, value);
                    },
                    (solutionView, entity, value) -> {
                        if (entity == null) { // Null entity means we need to unassign the value.
                            return ElementPosition.unassigned();
                        }
                        var valueCount = solutionView.countValues(variableMetaModel, entity);
                        if (value == null || valueCount == 0) { // This will trigger assignment of the value at the end of the list.
                            return ElementPosition.of(entity, valueCount);
                        } else { // This will trigger assignment of the value immediately before this value.
                            return solutionView.getPositionOf(variableMetaModel, value);
                        }
                    });
        }

    }

}
