package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.list;

import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public record ListSolutionState<Solution_, Score_ extends Score<Score_>>(Solution_ solution,
        List<ListValueState> assignedValueList, InnerScore<Score_> score) implements SolutionState<Solution_, Score_> {

    @Override
    public Solution_ getSolution() {
        return solution;
    }

    @Override
    public InnerScore<Score_> getScore() {
        return score;
    }
}
