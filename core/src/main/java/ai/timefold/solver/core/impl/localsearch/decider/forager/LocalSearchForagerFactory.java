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
        var pickEarlyType_ = Objects.requireNonNullElse(foragerConfig.getPickEarlyType(), LocalSearchPickEarlyType.NEVER);
        var acceptedCountLimit_ = Objects.requireNonNullElse(foragerConfig.getAcceptedCountLimit(), Integer.MAX_VALUE);
        var finalistPodiumType_ =
                Objects.requireNonNullElse(foragerConfig.getFinalistPodiumType(), FinalistPodiumType.HIGHEST_SCORE);
        // Breaking ties randomly leads to better results statistically
        boolean breakTieRandomly_ = Objects.requireNonNullElse(foragerConfig.getBreakTieRandomly(), true);
        return new AcceptedLocalSearchForager<>(finalistPodiumType_.buildFinalistPodium(), pickEarlyType_,
                acceptedCountLimit_, breakTieRandomly_);
    }
}
