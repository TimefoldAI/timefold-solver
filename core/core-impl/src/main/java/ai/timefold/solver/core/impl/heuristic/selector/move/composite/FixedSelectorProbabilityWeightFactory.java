package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.Map;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.Selector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;

final class FixedSelectorProbabilityWeightFactory<Solution_, Selector_ extends Selector>
        implements SelectionProbabilityWeightFactory<Solution_, Selector_> {

    private final Map<Selector_, Double> fixedProbabilityWeightMap;

    public FixedSelectorProbabilityWeightFactory(Map<Selector_, Double> fixedProbabilityWeightMap) {
        this.fixedProbabilityWeightMap = fixedProbabilityWeightMap;
    }

    @Override
    public double createProbabilityWeight(ScoreDirector<Solution_> scoreDirector, Selector_ selector) {
        return fixedProbabilityWeightMap.getOrDefault(selector, 1.0);
    }

}
