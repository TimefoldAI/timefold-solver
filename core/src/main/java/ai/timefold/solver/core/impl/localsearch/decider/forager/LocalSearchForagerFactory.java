package ai.timefold.solver.core.impl.localsearch.decider.forager;

import java.util.Objects;

import ai.timefold.solver.core.config.localsearch.decider.forager.FinalistPodiumType;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchPickEarlyType;

public class LocalSearchForagerFactory<Solution_> {

    public static <Solution_> LocalSearchForagerFactory<Solution_> create(LocalSearchForagerConfig foragerConfig) {
        return new LocalSearchForagerFactory<>(foragerConfig);
    }

    private final LocalSearchForagerConfig foragerConfig;

    public LocalSearchForagerFactory(LocalSearchForagerConfig foragerConfig) {
        this.foragerConfig = foragerConfig;
    }

    public LocalSearchForager<Solution_> buildForager() {
        var pickEarlyType = Objects.requireNonNullElse(foragerConfig.getPickEarlyType(), LocalSearchPickEarlyType.NEVER);
        var acceptedCountLimit = Objects.requireNonNullElse(foragerConfig.getAcceptedCountLimit(), Integer.MAX_VALUE);
        var selectedCountLimitRatio = Objects.requireNonNullElse(foragerConfig.getSelectedCountLimitRatio(), 0.0);
        var finalistPodiumType =
                Objects.requireNonNullElse(foragerConfig.getFinalistPodiumType(), FinalistPodiumType.HIGHEST_SCORE);
        // Breaking ties randomly leads to better results statistically
        var breakTieRandomly = Objects.requireNonNullElse(foragerConfig.getBreakTieRandomly(), true);
        return new AcceptedLocalSearchForager<>(finalistPodiumType.buildFinalistPodium(), pickEarlyType,
                acceptedCountLimit, selectedCountLimitRatio, breakTieRandomly);
    }
}
