package ai.timefold.solver.core.config.localsearch.decider.forager;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.impl.localsearch.decider.forager.finalist.FinalistPodium;
import ai.timefold.solver.core.impl.localsearch.decider.forager.finalist.HighestScoreFinalistPodium;
import ai.timefold.solver.core.impl.localsearch.decider.forager.finalist.StrategicOscillationByLevelFinalistPodium;

@XmlEnum
public enum FinalistPodiumType {
    HIGHEST_SCORE,
    STRATEGIC_OSCILLATION,
    STRATEGIC_OSCILLATION_BY_LEVEL,
    STRATEGIC_OSCILLATION_BY_LEVEL_ON_BEST_SCORE;

    public <Solution_> FinalistPodium<Solution_> buildFinalistPodium() {
        switch (this) {
            case HIGHEST_SCORE:
                return new HighestScoreFinalistPodium<>();
            case STRATEGIC_OSCILLATION:
            case STRATEGIC_OSCILLATION_BY_LEVEL:
                return new StrategicOscillationByLevelFinalistPodium<>(false);
            case STRATEGIC_OSCILLATION_BY_LEVEL_ON_BEST_SCORE:
                return new StrategicOscillationByLevelFinalistPodium<>(true);
            default:
                throw new IllegalStateException("The finalistPodiumType (" + this + ") is not implemented.");
        }
    }

}
