package ai.timefold.solver.core.impl.domain.variable.cascade.command;

import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementVariableSupply;

public class NextElementSupplyCommand implements CascadingUpdateCommand<Object> {

    private final NextElementVariableSupply nextElementVariableSupply;

    public NextElementSupplyCommand(NextElementVariableSupply nextElementVariableSupply) {
        this.nextElementVariableSupply = nextElementVariableSupply;
    }

    @Override
    public Object getValue(Object value) {
        return nextElementVariableSupply.getNext(value);
    }
}
