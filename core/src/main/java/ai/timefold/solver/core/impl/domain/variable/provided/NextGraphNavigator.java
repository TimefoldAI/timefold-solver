package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;

import org.jspecify.annotations.NonNull;

public class NextGraphNavigator<Entity_> implements GraphNavigator<Entity_, Entity_> {
    @NonNull
    final VariableId variableId;

    @NonNull
    final ListVariableStateSupply<?> stateSupply;

    public NextGraphNavigator(@NonNull final VariableId parentVariableId,
            @NonNull ListVariableStateSupply<?> stateSupply) {
        this.variableId = parentVariableId.child(stateSupply.getSourceVariableDescriptor().getElementType(),
                DefaultShadowVariableFactory.NEXT);
        this.stateSupply = stateSupply;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entity_ getValueEdge(Entity_ entity) {
        return (Entity_) stateSupply.getNextElement(entity);
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
    public List<Entity_> getParentEdges(Entity_ entity) {
        var out = (Entity_) stateSupply.getPreviousElement(entity);
        if (out == null) {
            return Collections.emptyList();
        }
        return List.of(out);
    }

    @Override
    public VariableId getParentVariableId() {
        return VariableId.entity(stateSupply.getSourceVariableDescriptor().getElementType())
                .child(stateSupply.getSourceVariableDescriptor().getElementType(),
                        DefaultShadowVariableFactory.PREVIOUS);
    }
}
