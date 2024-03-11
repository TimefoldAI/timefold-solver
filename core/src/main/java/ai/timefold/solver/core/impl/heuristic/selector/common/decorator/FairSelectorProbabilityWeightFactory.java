package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.IterableSelector;

public class FairSelectorProbabilityWeightFactory<Solution_>
        implements SelectionProbabilityWeightFactory<Solution_, IterableSelector> {

    @Override
    public double createProbabilityWeight(ScoreDirector<Solution_> scoreDirector, IterableSelector selector) {
        return selector.getSize();
    }

}
