package ai.timefold.solver.core.impl.heuristic.thread;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public class SetupOperation<Solution_, Score_ extends Score<Score_>> extends MoveThreadOperation<Solution_> {

    private final InnerScoreDirector<Solution_, Score_> innerScoreDirector;

    public SetupOperation(InnerScoreDirector<Solution_, Score_> innerScoreDirector) {
        this.innerScoreDirector = innerScoreDirector;
    }

    public InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return innerScoreDirector;
    }

}
