package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.GroupVariableReference;

import org.jspecify.annotations.Nullable;

public sealed abstract class AbstractShadowVariableReference<Solution_, Entity_, Value_>
        extends DefaultSingleVariableReference<Solution_, Entity_, Entity_, Value_>
        permits IntermediateShadowVariableReference, ShadowVariableReference {
    final ShadowVariableCalculation<Solution_, Entity_, Value_> calculation;
    final List<InnerVariableReference<Solution_, ?, ?>> shadowVariableReferences;
    @Nullable
    final VariableDescriptor<Solution_> invalidDescriptor;

    public AbstractShadowVariableReference(DefaultShadowVariableFactory<?> shadowVariableFactory,
            SolutionDescriptor<Solution_> solutionDescriptor, SupplyManager supplyManager,
            @Nullable DefaultSingleVariableReference<Solution_, Entity_, ?, Entity_> parent,
            GraphNavigator<Entity_, Value_> navigator, Class<? extends Entity_> entityClass,
            Class<? extends Entity_> parentType,
            Class<? extends Value_> valueType, boolean allowsNullValues,
            ShadowVariableCalculation<Solution_, Entity_, Value_> calculation,
            List<InnerVariableReference<Solution_, ?, ?>> shadowVariableReferences) {
        super(shadowVariableFactory, solutionDescriptor, supplyManager, parent, navigator, entityClass, parentType, valueType,
                allowsNullValues);
        this.calculation = calculation;
        this.shadowVariableReferences = shadowVariableReferences;
        this.invalidDescriptor = solutionDescriptor.getEntityDescriptorStrict(entityClass)
                .getInvalidityMarkerVariableDescriptor();
    }

    public void visitGraph(VariableReferenceGraph<Solution_> variableReferenceGraph) {
        variableReferenceGraph.addShadowVariable(this);
        for (var shadowVariableReference : shadowVariableReferences) {
            List<InnerVariableReference<Solution_, Entity_, ?>> path = new ArrayList<>();
            var parent = shadowVariableReference.getParent();
            while (parent != null && !parent.getVariableId().variableName().equals(DefaultShadowVariableFactory.IDENTITY)) {
                path.add(0, (InnerVariableReference<Solution_, Entity_, ?>) parent);
                addChangeProcessor(variableReferenceGraph,
                        (InnerVariableReference<Solution_, Entity_, ?>) shadowVariableReference,
                        path);
                parent = parent.getParent();
            }
        }
        calculation.visitGraph(variableReferenceGraph);
    }

    private void addChangeProcessor(VariableReferenceGraph<Solution_> variableReferenceGraph,
            InnerVariableReference<Solution_, Entity_, ?> aliasReference,
            List<InnerVariableReference<Solution_, Entity_, ?>> path) {
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

        variableReferenceGraph.addBeforeProcessor(path.get(0).getVariableId(),
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
        variableReferenceGraph.addAfterProcessor(path.get(0).getVariableId(),
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
    public Value_ getValue(Object entity) {
        return navigator.getValueEdge((Entity_) entity);
    }

    @Override
    public void processVariableReference(VariableReferenceGraph<Solution_> graph) {
        super.processVariableReference(graph);
        calculation.visitGraph(graph);
    }

    @Override
    public void processObject(VariableReferenceGraph<Solution_> graph, Object object) {
        super.processObject(graph, object);
        calculation.visitEntity(this, graph, object);
    }

    @Override
    public void addReferences(DefaultShadowVariableFactory<Solution_> factory) {
    }

    final boolean markValid(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity) {
        if (invalidDescriptor != null && !Objects.equals(false, invalidDescriptor.getValue(entity))) {
            changedVariableNotifier.beforeVariableChanged(invalidDescriptor, entity);
            invalidDescriptor.setValue(entity, false);
            changedVariableNotifier.afterVariableChanged(invalidDescriptor, entity);
            return true;
        }
        return false;
    }

    final boolean markInvalid(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity) {
        if (invalidDescriptor != null && !Objects.equals(true, invalidDescriptor.getValue(entity))) {
            changedVariableNotifier.beforeVariableChanged(invalidDescriptor, entity);
            invalidDescriptor.setValue(entity, true);
            changedVariableNotifier.afterVariableChanged(invalidDescriptor, entity);
            return true;
        }
        return false;
    }

    abstract boolean update(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity);

    abstract void invalidate(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity);
}
