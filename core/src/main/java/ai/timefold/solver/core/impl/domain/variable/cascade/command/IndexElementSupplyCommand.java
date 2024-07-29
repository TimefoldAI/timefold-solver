package ai.timefold.solver.core.impl.domain.variable.cascade.command;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;

public class IndexElementSupplyCommand<Solution_> implements CascadingUpdateCommand<Pair<Integer, Object>> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;

    public IndexElementSupplyCommand(ListVariableStateSupply<Solution_> listVariableStateSupply) {
        this.listVariableStateSupply = listVariableStateSupply;
    }

    @Override
    public Pair<Integer, Object> getValue(Object value) {
        ElementLocation elementLocation = listVariableStateSupply.getLocationInList(value);
        if (elementLocation instanceof LocationInList location) {
            return new Pair<>(location.index(), location.entity());
        }
        // Unassigned
        return null;
    }
}
