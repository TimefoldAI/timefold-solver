package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import ai.timefold.solver.core.impl.heuristic.selector.IterableSelector;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;

public class FairSelectorProbabilityWeightFactory<Solution_>
        implements SelectionProbabilityWeightFactory<Solution_, IterableSelector> {

    @Override
    public double createProbabilityWeight(ScoreDirector<Solution_> scoreDirector, IterableSelector selector) {
        return selector.getSize();
    }

}
