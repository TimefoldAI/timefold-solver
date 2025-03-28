package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.GroupVariableReference;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.SingleVariableReference;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed class DefaultSingleVariableReference<Solution_, Entity_, ParentValue_, Value_>
        implements
        InnerVariableReference<Solution_, Entity_, Value_>,
        SingleVariableReference<Entity_, Value_>
        permits AbstractShadowVariableReference {
    SolutionDescriptor<Solution_> solutionDescriptor;
    SupplyManager supplyManager;
    GraphNavigator<ParentValue_, Value_> navigator;
    Class<? extends Entity_> entityClass;
    Class<? extends ParentValue_> parentType;
    Class<? extends Value_> valueType;
    DefaultShadowVariableFactory<?> shadowVariableFactory;
    boolean allowsNullValues;

    @Nullable
    DefaultSingleVariableReference<Solution_, Entity_, ?, ParentValue_> parent;

    public DefaultSingleVariableReference(
            DefaultShadowVariableFactory<?> shadowVariableFactory,
            SolutionDescriptor<Solution_> solutionDescriptor,
            SupplyManager supplyManager,
            @Nullable DefaultSingleVariableReference<Solution_, Entity_, ?, ParentValue_> parent,
            GraphNavigator<ParentValue_, Value_> navigator,
            Class<? extends Entity_> entityClass,
            Class<? extends ParentValue_> parentType,
            Class<? extends Value_> valueType,
            boolean allowsNullValues) {
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.parent = parent;
        this.navigator = navigator;
        this.entityClass = entityClass;
        this.parentType = parentType;
        this.valueType = valueType;
        this.shadowVariableFactory = shadowVariableFactory;
        this.allowsNullValues = allowsNullValues;
    }

    public static <Solution_, Entity_> DefaultSingleVariableReference<Solution_, Entity_, Entity_, Entity_>
            entity(DefaultShadowVariableFactory<Solution_> shadowVariableFactory,
                    SolutionDescriptor<Solution_> solutionDescriptor, SupplyManager supplyManager,
                    Class<? extends Entity_> entityType) {
        return new DefaultSingleVariableReference<>(shadowVariableFactory, solutionDescriptor, supplyManager,
                null, new IdGraphNavigator<>(entityType),
                entityType, entityType, entityType, false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <NewValue_> DefaultSingleVariableReference<Solution_, Entity_, Value_, NewValue_> child(
            Class<? extends NewValue_> newValueType,
            GraphNavigator<Value_, NewValue_> function) {
        return new DefaultSingleVariableReference(shadowVariableFactory, solutionDescriptor, supplyManager, this,
                function, entityClass, valueType, newValueType, allowsNullValues);
    }

    private <NewValue_> DefaultSingleVariableReference<Solution_, Entity_, Value_, NewValue_> nullableChild(
            Class<? extends NewValue_> newValueType,
            GraphNavigator<Value_, NewValue_> function) {
        return new DefaultSingleVariableReference(shadowVariableFactory, solutionDescriptor, supplyManager, this,
                function, entityClass, valueType, newValueType, true);
    }

    @Override
    public <Fact_> SingleVariableReference<Entity_, Fact_> fact(Class<? extends Fact_> factClass,
            Function<Value_, Fact_> mapper) {
        return child(factClass,
                new FactGraphNavigator<>(navigator.getVariableId(), valueType, mapper));
    }

    @Override
    public <Variable_> SingleVariableReference<Entity_, Variable_> variable(Class<? extends Variable_> variableClass,
            String variableName) {
        return child(variableClass,
                new VariableGraphNavigator<>(navigator.getVariableId(), solutionDescriptor.getEntityDescriptorStrict(valueType)
                        .getVariableDescriptorOrFail(variableName)));
    }

    @Override
    public <Variable_> SingleVariableReference<Entity_, Variable_> intermediate(Class<? extends Variable_> intermediateClass,
            String intermediateName) {
        return child(intermediateClass,
                new IntermediateGraphNavigator<>(navigator.getVariableId(),
                        valueType,
                        intermediateClass,
                        intermediateName,
                        shadowVariableFactory.getIntermediateValueMap(intermediateName)));
    }

    @Override
    public SingleVariableReference<Entity_, Value_> previous() {
        return child(valueType,
                new PreviousGraphNavigator<>(navigator.getVariableId(),
                        supplyManager.demand(new ListVariableStateDemand<>(solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public SingleVariableReference<Entity_, Value_> next() {
        return child(valueType,
                new NextGraphNavigator<>(navigator.getVariableId(),
                        supplyManager.demand(new ListVariableStateDemand<>(solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public <Inverse_> SingleVariableReference<Entity_, Inverse_> inverse(Class<? extends Inverse_> inverseClass) {
        return child(inverseClass,
                new InverseGraphNavigator<>(navigator.getVariableId(),
                        supplyManager.demand(new ListVariableStateDemand<>(solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public <Element_> GroupVariableReference<Entity_, Element_> group(Class<? extends Element_> element,
            Function<Value_, List<Element_>> groupFunction) {
        if (parent != null) {
            throw new IllegalArgumentException("group() must be the first method called.");
        }
        return new DefaultGroupVariableReference<>(shadowVariableFactory, solutionDescriptor, supplyManager,
                this, new GroupGraphNavigator<>(element, groupFunction),
                entityClass, valueType,
                element, shadowVariableFactory.nextGroupId());
    }

    @Override
    public VariableId getVariableId() {
        return navigator.getVariableId();
    }

    @Override
    @Nullable
    public Value_ getValue(Object entity) {
        if (parent == null) {
            return (Value_) entity;
        }
        var parentValue = parent.getValue(entity);
        if (parentValue == null) {
            return null;
        }
        return navigator.getValueEdge(parentValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public Object getSingleValueFromSingleParent(Object parentValue) {
        if (parentValue == null) {
            return null;
        }
        return navigator.getValueEdge((ParentValue_) parentValue);
    }

    @Override
    @Nullable
    public InnerVariableReference<Solution_, Entity_, ?> getParent() {
        return parent;
    }

    @Override
    public void processVariableReference(VariableReferenceGraph<Solution_> graph) {
        if (parent != null) {
            parent.processVariableReference(graph);
        }
    }

    @Override
    public void processObject(VariableReferenceGraph<Solution_> graph, Object object) {
        var variableId = navigator.getVariableId();
        if (variableId.entityClass().isInstance(object)) {
            var toEdge = graph.addVariableReferenceEntity(variableId, object, this);
            if (parent != null) {
                graph.addFixedEdge(
                        graph.addVariableReferenceEntity(parent.getVariableId(), object, parent),
                        toEdge);
            }
        }
        if (parent != null) {
            parent.processObject(graph, object);
        }
    }

    @Override
    public void addReferences(DefaultShadowVariableFactory<Solution_> factory) {
        if (navigator instanceof VariableGraphNavigator<?, ?> variableGraphNavigator) {
            factory.addShadowVariableReference(variableGraphNavigator.variableDescriptor.getVariableName(), this);
        }
        if (navigator instanceof IntermediateGraphNavigator<?, ?> intermediateGraphNavigator) {
            factory.addShadowVariableReference(intermediateGraphNavigator.variableId.getLastComponent(), this);
        }
    }

    @Override
    public boolean isNullValueValid() {
        return false;
    }

    @Override
    public SingleVariableReference<Entity_, Value_> allowNullValue() {
        if (parent != null) {
            return parent.nullableChild(valueType, navigator);
        } else {
            // It is impossible for identity to return null
            return this;
        }
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName() + "." + navigator.getVariableId().variableName();
    }
}
