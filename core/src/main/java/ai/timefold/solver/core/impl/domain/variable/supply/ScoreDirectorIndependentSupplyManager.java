package ai.timefold.solver.core.impl.domain.variable.supply;

import java.util.HashMap;
import java.util.Map;

public class ScoreDirectorIndependentSupplyManager implements SupplyManager {
    private final Map<Demand<?>, Supply> demandToSupply = new HashMap<>();
    private final Map<Demand<?>, Long> demandToActiveCount = new HashMap<>();

    @Override
    public <Supply_ extends Supply> Supply_ demand(Demand<Supply_> demand) {
        demandToActiveCount.merge(demand, 1L, Long::sum);
        return (Supply_) demandToSupply.computeIfAbsent(demand, ignored -> demand.createExternalizedSupply(this));
    }

    @Override
    public <Supply_ extends Supply> boolean cancel(Demand<Supply_> demand) {
        if (!demandToActiveCount.containsKey(demand)) {
            return false;
        }
        var newActiveCount = (long) demandToActiveCount.merge(demand, -1L, Long::sum);
        if (newActiveCount <= 0L) {
            demandToActiveCount.remove(demand);
            demandToSupply.remove(demand);
        }
        return true;
    }

    @Override
    public <Supply_ extends Supply> long getActiveCount(Demand<Supply_> demand) {
        return demandToActiveCount.getOrDefault(demand, 0L);
    }
}
