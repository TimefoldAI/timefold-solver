package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.variable.provided.GroupVariableReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class DefaultGroupVariableReference<Solution_, Entity_, ParentValue_, Value_>
        extends AbstractVariableReference<Entity_, List<Value_>>
        implements GroupVariableReference<Entity_, Value_> {
    @NonNull
    SolutionDescriptor<Solution_> solutionDescriptor;

    @NonNull
    SupplyManager supplyManager;

    @NonNull
    AbstractVariableReference<Entity_, ParentValue_> parent;

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

    public DefaultGroupVariableReference(@NonNull SolutionDescriptor<Solution_> solutionDescriptor,
            @NonNull SupplyManager supplyManager,
            @NonNull DefaultGroupVariableReference<Solution_, Entity_, ?, ParentValue_> parent,
            @NonNull GraphNavigator<ParentValue_, Value_> navigator,
            @NonNull Class<? extends Entity_> entityClass,
            @NonNull Class<? extends ParentValue_> parentType,
            @NonNull Class<? extends Value_> valueType) {
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.parent = (AbstractVariableReference<Entity_, ParentValue_>) parent;
        this.navigator = navigator;
        this.entityClass = entityClass;
        this.parentType = parentType;
        this.valueType = valueType;
        this.grouper = null;
    }

    public DefaultGroupVariableReference(@NonNull SolutionDescriptor<Solution_> solutionDescriptor,
            @NonNull SupplyManager supplyManager,
            @NonNull DefaultSingleVariableReference<Solution_, Entity_, ?, ParentValue_> parent,
            @NonNull GraphNavigator<ParentValue_, List<Value_>> grouper,
            @NonNull Class<? extends Entity_> entityClass,
            @NonNull Class<? extends ParentValue_> parentType,
            @NonNull Class<? extends Value_> valueType) {
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.parent = parent;
        this.grouper = grouper;
        this.entityClass = entityClass;
        this.parentType = parentType;
        this.valueType = valueType;
        this.navigator = null;
    }

    public <NewValue_> DefaultGroupVariableReference<Solution_, Entity_, Value_, NewValue_> child(
            Class<? extends NewValue_> newValueType,
            GraphNavigator<Value_, NewValue_> function) {
        return new DefaultGroupVariableReference<>(solutionDescriptor, supplyManager,
                this,
                function,
                entityClass, valueType, newValueType);
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
        return grouper.getVariableId();
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
            return parentList.stream().map(navigator::getValueEdge).toList();
        }
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    List<Value_> getValueFromParent(@NonNull Object parentValue) {
        if (parentValue == null) {
            return null;
        }
        if (grouper != null) {
            return grouper.getValueEdge((ParentValue_) parentValue);
        } else {
            var parentList = (List<ParentValue_>) parentValue;
            if (parentList == null) {
                return null;
            }
            return parentList.stream().map(navigator::getValueEdge).toList();
        }
    }

    @Override
    @Nullable
    AbstractVariableReference<Entity_, ?> getParent() {
        return parent;
    }

    @Override
    void processVariableReference(@NonNull VariableReferenceGraph graph) {
        parent.processVariableReference(graph);
    }

    @Override
    void processObject(@NonNull VariableReferenceGraph graph, @NonNull Object object) {
        if (entityClass.isInstance(object)) {
            var variableId = getVariableId();
            var toEdge = graph.addVariableReferenceEntity(variableId, object, this);
            graph.addFixedEdge(
                    graph.addVariableReferenceEntity(parent.getVariableId(), object, parent),
                    toEdge);
        }
        parent.processObject(graph, object);
    }

    @Override
    void addReferences(@NonNull DefaultShadowVariableFactory<?> factory) {
        if (navigator instanceof VariableGraphNavigator<?, ?> variableGraphNavigator) {
            factory.addShadowVariableReference(variableGraphNavigator.variableDescriptor.getVariableName(), this);
        }
    }

    @Override
    public String toString() {
        if (grouper != null) {
            return "%s.group(%s)".formatted(parent, grouper.getVariableId().variableName());
        } else {
            return "%s.%s".formatted(parent, navigator.getVariableId().variableName());
        }
    }
}
