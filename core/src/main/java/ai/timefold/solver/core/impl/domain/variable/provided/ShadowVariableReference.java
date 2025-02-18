package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.variable.provided.GroupVariableReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ShadowVariableReference<Solution_, Entity_, Value_>
        extends DefaultSingleVariableReference<Solution_, Entity_, Entity_, Value_> {
    final ShadowVariableCalculation<Solution_, Entity_, Value_> calculation;
    final VariableDescriptor<Solution_> variableDescriptor;
    final VariableDescriptor<Solution_> invalidDescriptor;
    final List<AbstractVariableReference<Solution_, ?, ?>> shadowVariableReferences;

    public ShadowVariableReference(
            @NonNull SolutionDescriptor<Solution_> solutionDescriptor,
            @NonNull SupplyManager supplyManager,
            @NonNull VariableDescriptor<Solution_> variableDescriptor,
            @NonNull ShadowVariableCalculation<Solution_, Entity_, Value_> calculation,
            @NonNull List<AbstractVariableReference<Solution_, ?, ?>> shadowVariableReferences,
            @NonNull Class<? extends Entity_> entityClass,
            @NonNull Class<? extends Value_> valueType) {
        super(calculation.shadowVariableFactory,
                solutionDescriptor, supplyManager, null,
                new VariableGraphNavigator<>(VariableId.entity(entityClass),
                        variableDescriptor),
                entityClass,
                entityClass, valueType);
        this.variableDescriptor = variableDescriptor;
        this.calculation = calculation;
        this.shadowVariableReferences = shadowVariableReferences;
        this.invalidDescriptor = solutionDescriptor.getEntityDescriptorStrict(entityClass)
                .getInvalidityMarkerVariableDescriptor();
    }

    public void invalidateShadowVariable(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity) {
        var oldValue = variableDescriptor.getValue(entity);
        if (oldValue != null) {
            changedVariableNotifier.beforeVariableChanged(variableDescriptor, entity);
            variableDescriptor.setValue(entity, null);
            changedVariableNotifier.afterVariableChanged(variableDescriptor, entity);
        }
        if (invalidDescriptor != null && !Objects.equals(true, invalidDescriptor.getValue(entity))) {
            changedVariableNotifier.beforeVariableChanged(invalidDescriptor, entity);
            invalidDescriptor.setValue(entity, true);
            changedVariableNotifier.afterVariableChanged(invalidDescriptor, entity);
        }
    }

    public boolean updateShadowVariable(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object object) {
        var entity = (Entity_) object;
        var oldValue = variableDescriptor.getValue(entity);

        var newValue = calculation.calculate(entity);

        var changed = false;
        if (!Objects.equals(oldValue, newValue)) {
            changedVariableNotifier.beforeVariableChanged(variableDescriptor, entity);
            variableDescriptor.setValue(entity, newValue);
            changedVariableNotifier.afterVariableChanged(variableDescriptor, entity);
            changed = true;
        }
        if (invalidDescriptor != null && !Objects.equals(false, invalidDescriptor.getValue(entity))) {
            changedVariableNotifier.beforeVariableChanged(invalidDescriptor, entity);
            invalidDescriptor.setValue(entity, false);
            changedVariableNotifier.afterVariableChanged(invalidDescriptor, entity);
            changed = true;
        }
        return changed;
    }

    public void visitGraph(VariableReferenceGraph<Solution_> variableReferenceGraph) {
        variableReferenceGraph.addShadowVariable(this);
        for (var shadowVariableReference : shadowVariableReferences) {
            List<AbstractVariableReference<Solution_, Entity_, ?>> path = new ArrayList<>();
            var parent = shadowVariableReference.getParent();
            while (parent != null && !parent.getVariableId().variableName().equals(DefaultShadowVariableFactory.IDENTITY)) {
                path.add(0, (AbstractVariableReference<Solution_, Entity_, ?>) parent);
                addChangeProcessor(variableReferenceGraph,
                        (AbstractVariableReference<Solution_, Entity_, ?>) shadowVariableReference,
                        path);
                parent = parent.getParent();
            }
        }
        calculation.visitGraph(variableReferenceGraph);
    }

    private void addChangeProcessor(VariableReferenceGraph<Solution_> variableReferenceGraph,
            AbstractVariableReference<Solution_, Entity_, ?> aliasReference,
            List<AbstractVariableReference<Solution_, Entity_, ?>> path) {
        // Alias is a reference to this shadow variable by a different name.
        // For instance, if first is the previous object before second, then
        // second:#id.#previous.shadow is an alias for first:#id.shadow.
        // An alias depends directly on its source, which is recorded as an
        // edge in the graph from the source to the alias.
        //
        // Path is the minimum pathway from the alias reference to the source.
        if (!(path.get(0) instanceof GroupVariableReference<?, ?>)
                && (path.get(path.size() - 1) instanceof GroupVariableReference<?, ?>)) {
            // A mix of group and single variable paths; we only add listeners for the group parts
            return;
        }

        variableReferenceGraph.addBeforeProcessor(path.get(0).getVariableId().rootId(),
                (g, alias) -> {
                    var aliasSource = alias;
                    for (var ref : path) {
                        aliasSource = ref.getSingleValueFromSingleParent(aliasSource);
                        if (aliasSource == null) {
                            return;
                        }
                    }

                    g.removeEdge(
                            g.lookup(getVariableId(), aliasSource),
                            g.lookup(aliasReference.getVariableId(), alias));
                });
        variableReferenceGraph.addAfterProcessor(path.get(0).getVariableId().rootId(),
                (g, alias) -> {
                    var aliasSource = alias;
                    for (var ref : path) {
                        aliasSource = ref.getSingleValueFromSingleParent(aliasSource);
                        if (aliasSource == null) {
                            return;
                        }
                    }
                    g.addEdge(
                            g.lookup(getVariableId(), aliasSource),
                            g.lookup(aliasReference.getVariableId(), alias));
                });
    }

    public void visitEntity(VariableReferenceGraph<Solution_> variableReferenceGraph, Object entity) {
        calculation.visitEntity(this, variableReferenceGraph, entity);
    }

    @Override
    @Nullable
    Value_ getValue(@NonNull Object entity) {
        return navigator.getValueEdge((Entity_) entity);
    }

    @Override
    void processVariableReference(@NonNull VariableReferenceGraph<Solution_> graph) {
        super.processVariableReference(graph);
        calculation.visitGraph(graph);
    }

    @Override
    void processObject(@NonNull VariableReferenceGraph<Solution_> graph, @NonNull Object object) {
        super.processObject(graph, object);
        calculation.visitEntity(this, graph, object);
    }

    @Override
    void addReferences(@NonNull DefaultShadowVariableFactory<Solution_> factory) {
    }
}
