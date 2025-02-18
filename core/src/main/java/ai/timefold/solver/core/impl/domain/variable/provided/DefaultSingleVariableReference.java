package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.variable.provided.GroupVariableReference;
import ai.timefold.solver.core.preview.api.variable.provided.SingleVariableReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public sealed class DefaultSingleVariableReference<Solution_, Entity_, ParentValue_, Value_>
        extends AbstractVariableReference<Solution_, Entity_, Value_>
        implements
        SingleVariableReference<Entity_, Value_> permits ShadowVariableReference {
    @NonNull
    SolutionDescriptor<Solution_> solutionDescriptor;

    @NonNull
    SupplyManager supplyManager;

    @Nullable
    DefaultSingleVariableReference<Solution_, Entity_, ?, ParentValue_> parent;

    @NonNull
    GraphNavigator<ParentValue_, Value_> navigator;

    @NonNull
    Class<? extends Entity_> entityClass;

    @NonNull
    Class<? extends ParentValue_> parentType;

    @NonNull
    Class<? extends Value_> valueType;

    DefaultShadowVariableFactory<?> shadowVariableFactory;

    public DefaultSingleVariableReference(
            @NonNull DefaultShadowVariableFactory<?> shadowVariableFactory,
            @NonNull SolutionDescriptor<Solution_> solutionDescriptor,
            @NonNull SupplyManager supplyManager,
            @Nullable DefaultSingleVariableReference<Solution_, Entity_, ?, ParentValue_> parent,
            @NonNull GraphNavigator<ParentValue_, Value_> navigator,
            @NonNull Class<? extends Entity_> entityClass,
            @NonNull Class<? extends ParentValue_> parentType,
            @NonNull Class<? extends Value_> valueType) {
        this.solutionDescriptor = solutionDescriptor;
        this.supplyManager = supplyManager;
        this.parent = parent;
        this.navigator = navigator;
        this.entityClass = entityClass;
        this.parentType = parentType;
        this.valueType = valueType;
        this.shadowVariableFactory = shadowVariableFactory;
    }

    public static <Solution_, Entity_> @NonNull DefaultSingleVariableReference<Solution_, Entity_, Entity_, Entity_>
            entity(@NonNull DefaultShadowVariableFactory<Solution_> shadowVariableFactory,
                    @NonNull SolutionDescriptor<Solution_> solutionDescriptor, @NonNull SupplyManager supplyManager,
                    Class<? extends Entity_> entityType) {
        return new DefaultSingleVariableReference<>(shadowVariableFactory, solutionDescriptor, supplyManager,
                null, new IdGraphNavigator<>(entityType),
                entityType, entityType, entityType);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <NewValue_> @NonNull DefaultSingleVariableReference<Solution_, Entity_, Value_, NewValue_> child(
            Class<? extends NewValue_> newValueType,
            GraphNavigator<Value_, NewValue_> function) {
        return new DefaultSingleVariableReference(shadowVariableFactory, solutionDescriptor, supplyManager, this,
                function, entityClass, valueType, newValueType);
    }

    @Override
    public <Fact_> @NonNull SingleVariableReference<Entity_, Fact_> fact(Class<? extends Fact_> factClass,
            Function<Value_, Fact_> mapper) {
        return child(factClass,
                new FactGraphNavigator<>(navigator.getVariableId(), valueType, mapper));
    }

    @Override
    public <Variable_> @NonNull SingleVariableReference<Entity_, Variable_> variable(Class<? extends Variable_> variableClass,
            String variableName) {
        return child(variableClass,
                new VariableGraphNavigator<>(navigator.getVariableId(), solutionDescriptor.getEntityDescriptorStrict(valueType)
                        .getVariableDescriptorOrFail(variableName)));
    }

    @Override
    public @NonNull SingleVariableReference<Entity_, Value_> previous() {
        return child(valueType,
                new PreviousGraphNavigator<>(navigator.getVariableId(),
                        supplyManager.demand(new ListVariableStateDemand<>(solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public @NonNull SingleVariableReference<Entity_, Value_> next() {
        return child(valueType,
                new NextGraphNavigator<>(navigator.getVariableId(),
                        supplyManager.demand(new ListVariableStateDemand<>(solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public <Inverse_> @NonNull SingleVariableReference<Entity_, Inverse_> inverse(Class<? extends Inverse_> inverseClass) {
        return child(inverseClass,
                new InverseGraphNavigator<>(navigator.getVariableId(),
                        supplyManager.demand(new ListVariableStateDemand<>(solutionDescriptor.getListVariableDescriptor()))));
    }

    @Override
    public <Element_> @NonNull GroupVariableReference<Entity_, Element_> group(Class<? extends Element_> element,
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
    VariableId getVariableId() {
        return navigator.getVariableId();
    }

    @Override
    @Nullable
    Value_ getValue(@NonNull Object entity) {
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
    Object getSingleValueFromSingleParent(@NonNull Object parentValue) {
        if (parentValue == null) {
            return null;
        }
        return navigator.getValueEdge((ParentValue_) parentValue);
    }

    @Override
    @Nullable
    AbstractVariableReference<Solution_, Entity_, ?> getParent() {
        return parent;
    }

    @Override
    void processVariableReference(@NonNull VariableReferenceGraph<Solution_> graph) {
        if (parent != null) {
            parent.processVariableReference(graph);
        }
    }

    @Override
    void processObject(@NonNull VariableReferenceGraph<Solution_> graph, @NonNull Object object) {
        if (entityClass.isInstance(object)) {
            var variableId = navigator.getVariableId();
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
    void addReferences(@NonNull DefaultShadowVariableFactory<Solution_> factory) {
        if (navigator instanceof VariableGraphNavigator<?, ?> variableGraphNavigator) {
            factory.addShadowVariableReference(variableGraphNavigator.variableDescriptor.getVariableName(), this);
        }
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName() + "." + navigator.getVariableId().variableName();
    }
}
