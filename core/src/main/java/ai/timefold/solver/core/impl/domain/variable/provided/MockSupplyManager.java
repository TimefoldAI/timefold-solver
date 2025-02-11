package ai.timefold.solver.core.impl.domain.variable.provided;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public class MockSupplyManager implements SupplyManager {
    final MockListStateSupply<?> mockListStateSupply;

    public MockSupplyManager(MockListStateSupply<?> mockListStateSupply) {
        this.mockListStateSupply = mockListStateSupply;
    }

    @Override
    public <Supply_ extends Supply> Supply_ demand(Demand<Supply_> demand) {
        if (demand instanceof ListVariableStateDemand<?> listVariableStateDemand) {
            return (Supply_) mockListStateSupply;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public <Supply_ extends Supply> boolean cancel(Demand<Supply_> demand) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <Supply_ extends Supply> long getActiveCount(Demand<Supply_> demand) {
        throw new UnsupportedOperationException();
    }
}
