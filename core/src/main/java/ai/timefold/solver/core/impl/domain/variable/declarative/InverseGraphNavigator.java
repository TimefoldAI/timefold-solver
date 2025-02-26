package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class InverseGraphNavigator<Entity_, Inverse_> implements GraphNavigator<Entity_, Inverse_> {
    final VariableId variableId;
    final ListVariableStateSupply<?> stateSupply;

    public InverseGraphNavigator(VariableId parentVariableId,
            ListVariableStateSupply<?> stateSupply) {
        this.variableId = parentVariableId.child(stateSupply.getSourceVariableDescriptor().getElementType(),
                DefaultShadowVariableFactory.INVERSE);
        this.stateSupply = stateSupply;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Inverse_ getValueEdge(Entity_ entity) {
        return (Inverse_) stateSupply.getInverseSingleton(entity);
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }

    @Override
    public boolean hasParentEdge() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Entity_> getParentEdges(Inverse_ entity) {
        return (List<Entity_>) stateSupply.getSourceVariableDescriptor().getValue(entity);
    }

    @Override
    public VariableId getParentVariableId() {
        return VariableId.entity(stateSupply.getSourceVariableDescriptor().getEntityDescriptor().getEntityClass())
                .child(stateSupply.getSourceVariableDescriptor().getEntityDescriptor().getEntityClass(),
                        stateSupply.getSourceVariableDescriptor().getVariableName());
    }
}
