package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.List;
import java.util.function.Function;

public class GroupGraphNavigator<Entity_, Element_> implements GraphNavigator<Entity_, List<Element_>> {
    final VariableId variableId;
    final Function<Entity_, List<Element_>> groupFunction;

    public GroupGraphNavigator(Class<? extends Element_> groupElementClass,
            Function<Entity_, List<Element_>> groupFunction) {
        this.variableId = VariableId.entity(groupElementClass);
        this.groupFunction = groupFunction;
    }

    @Override
    public List<Element_> getValueEdge(Entity_ entity) {
        return groupFunction.apply(entity);
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }
}
