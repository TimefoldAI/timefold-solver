package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.variable.provided.GroupVariableReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class DefaultGroupVariableReference<Solution_, Entity_, ParentValue_, Value_>
        extends AbstractVariableReference<Solution_, Entity_, List<Value_>>
        implements GroupVariableReference<Entity_, Value_> {
    @NonNull
    DefaultShadowVariableFactory<?> shadowVariableFactory;

    @NonNull
    SolutionDescriptor<Solution_> solutionDescriptor;

    @NonNull
    SupplyManager supplyManager;

    @NonNull
    AbstractVariableReference<Solution_, Entity_, ParentValue_> parent;

    @NonNull
    Class<? extends Entity_> entityClass;

    @NonNull
    Class<? extends ParentValue_> parentType;

    @NonNull
    Class<? extends Value_> valueType;

    @Nullable
    GraphNavigator<ParentValue_, Value_> navigator;

    @Nullable
    GraphNavigator<ParentValue_, List<Value_>> grouper;

    final int groupId;

    public DefaultGroupVariableReference(
            @NonNull DefaultShadowVariableFactory<?> shadowVariableFactory,
            @NonNull SolutionDescriptor<Solution_> solutionDescriptor,
            @NonNull SupplyManager supplyManager,
            @NonNull DefaultGroupVariableReference<Solution_, Entity_, ?, ParentValue_> parent,
            @NonNull GraphNavigator<ParentValue_, Value_> navigator,
            @NonNull Class<? extends Entity_> entityClass,
            @NonNull Class<? extends ParentValue_> parentType,
            @NonNull Class<? extends Value_> valueType,
            int groupId) {
        this.shadowVariableFactory = shadowVariableFactory;
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.parent = (AbstractVariableReference<Solution_, Entity_, ParentValue_>) parent;
        this.navigator = navigator;
        this.entityClass = entityClass;
        this.parentType = parentType;
        this.valueType = valueType;
        this.grouper = null;
        this.groupId = groupId;
    }

    public DefaultGroupVariableReference(
            @NonNull DefaultShadowVariableFactory<?> shadowVariableFactory,
            @NonNull SolutionDescriptor<Solution_> solutionDescriptor,
            @NonNull SupplyManager supplyManager,
            @NonNull DefaultSingleVariableReference<Solution_, Entity_, ?, ParentValue_> parent,
            @NonNull GraphNavigator<ParentValue_, List<Value_>> grouper,
            @NonNull Class<? extends Entity_> entityClass,
            @NonNull Class<? extends ParentValue_> parentType,
            @NonNull Class<? extends Value_> valueType,
            int groupId) {
        this.shadowVariableFactory = shadowVariableFactory;
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.parent = parent;
        this.grouper = grouper;
        this.entityClass = entityClass;
        this.parentType = parentType;
        this.valueType = valueType;
        this.navigator = null;
        this.groupId = groupId;
    }

    public <NewValue_> DefaultGroupVariableReference<Solution_, Entity_, Value_, NewValue_> child(
            Class<? extends NewValue_> newValueType,
            GraphNavigator<Value_, NewValue_> function) {
        return new DefaultGroupVariableReference<>(shadowVariableFactory, solutionDescriptor, supplyManager,
                this,
                function,
                entityClass, valueType, newValueType, groupId);
    }

    @Override
    public <Fact_> GroupVariableReference<Entity_, Fact_> facts(Class<? extends Fact_> factClass,
            Function<Value_, Fact_> mapper) {
        return child(factClass, new FactGraphNavigator<>(getVariableId(), factClass, mapper));
    }

    @Override
    public <Variable_> GroupVariableReference<Entity_, Variable_> variables(Class<? extends Variable_> variableClass,
            String variableName) {
        return child(variableClass,
                new VariableGraphNavigator<>(getVariableId(),
                        solutionDescriptor.getEntityDescriptorStrict(valueType).getVariableDescriptorOrFail(variableName)));
    }

    @Override
    public GroupVariableReference<Entity_, Value_> previous() {
        return child(valueType,
                new PreviousGraphNavigator<>(getVariableId(), supplyManager.demand(new ListVariableStateDemand<>(
                        solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public GroupVariableReference<Entity_, Value_> next() {
        return child(valueType, new NextGraphNavigator<>(getVariableId(), supplyManager.demand(new ListVariableStateDemand<>(
                solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public <Inverse_> GroupVariableReference<Entity_, Inverse_> inverses(Class<? extends Inverse_> inverseClass) {
        return child(inverseClass,
                new InverseGraphNavigator<>(getVariableId(), supplyManager.demand(new ListVariableStateDemand<>(
                        solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    VariableId getVariableId() {
        if (navigator != null) {
            return navigator.getVariableId();
        }
        return grouper.getVariableId().group(entityClass, groupId);
    }

    @Override
    @Nullable
    List<Value_> getValue(@NonNull Object entity) {
        var parentValue = parent.getValue(entity);
        if (grouper != null) {
            return grouper.getValueEdge(parentValue);
        } else {
            var parentList = (List<ParentValue_>) parentValue;
            if (parentList == null) {
                return null;
            }
            return parentList.stream().map(navigator::getValueEdge)
                    .filter(Objects::nonNull).toList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    Object getSingleValueFromSingleParent(@NonNull Object parentValue) {
        if (parentValue == null) {
            return null;
        }
        if (grouper != null) {
            // A grouper create a group, so we cannot extract a single value
            // Moreover, a group must be a fact (i.e. the elements of a group
            // cannot change)
            return null;
        } else {
            return navigator.getValueEdge((ParentValue_) parentValue);
        }
    }

    @Override
    @Nullable
    AbstractVariableReference<Solution_, Entity_, ?> getParent() {
        return parent;
    }

    @Override
    void processVariableReference(@NonNull VariableReferenceGraph<Solution_> graph) {
        parent.processVariableReference(graph);
    }

    @Override
    void processObject(@NonNull VariableReferenceGraph<Solution_> graph, @NonNull Object object) {
        if (entityClass.isInstance(object)) {
            var variableId = getVariableId();
            var toEdge = graph.addVariableReferenceEntity(variableId, object, this);
            graph.addFixedEdge(
                    graph.addVariableReferenceEntity(parent.getVariableId(), object, parent),
                    toEdge);
        }
        parent.processObject(graph, object);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void processGroupElements(@NonNull VariableReferenceGraph<Solution_> graph,
            @NonNull DefaultGroupVariableReference<Solution_, ?, ?, ?> source, @NonNull Object object) {
        if (!entityClass.isInstance(object)) {
            return;
        }
        if (grouper != null) {
            var group = getValue(object);
            if (group != null) {
                for (var groupElement : group) {
                    if (source.navigator instanceof VariableGraphNavigator<?, ?> variableGraphNavigator) {
                        graph.addFixedEdge(
                                graph.addVariableReferenceEntity(source.getVariableId().rootId(),
                                        groupElement,
                                        (AbstractVariableReference<Solution_, ?, ?>) shadowVariableFactory
                                                .entity(source.entityClass)
                                                .variable(variableGraphNavigator.variableDescriptor.getVariablePropertyType(),
                                                        variableGraphNavigator.variableDescriptor.getVariableName())),
                                graph.addVariableReferenceEntity(source.getVariableId(),
                                        object,
                                        source));
                    }
                }
            }
        } else {
            ((DefaultGroupVariableReference) parent).processGroupElements(graph, source, object);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    void addReferences(@NonNull DefaultShadowVariableFactory<Solution_> factory) {
        if (navigator instanceof VariableGraphNavigator<?, ?> variableGraphNavigator) {
            factory.addShadowVariableReference(variableGraphNavigator.variableDescriptor.getVariableName(), this);
            factory.addGroupVariableReference(this);
        }
    }

    @Override
    public String toString() {
        if (grouper != null) {
            return "%s.group(%d)".formatted(parent, groupId);
        } else {
            return "%s.%s".formatted(entityClass.getSimpleName(), navigator.getVariableId().variableName());
        }
    }
}
