package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;

abstract sealed class AbstractIndividual<Solution_, Score_ extends Score<Score_>> implements Individual<Solution_, Score_>
        permits ListVariableIndividual {

    protected final Solution_ solution;
    protected final InnerScore<Score_> score;
    protected final InnerScore<Score_> firstParentScore;
    protected final InnerScore<Score_> secondParentScore;

    protected AbstractIndividual(Solution_ solution, InnerScore<Score_> score, InnerScore<Score_> firstParentScore,
            InnerScore<Score_> secondParentScore) {
        this.solution = solution;
        this.score = score;
        this.firstParentScore = firstParentScore;
        this.secondParentScore = secondParentScore;
    }

    @Override
    public Solution_ getSolution() {
        return solution;
    }

    @Override
    public boolean isFeasible() {
        return score.raw().isFeasible();
    }

    @Override
    public InnerScore<Score_> getFirstParentScore() {
        return firstParentScore;
    }

    @Override
    public InnerScore<Score_> getSecondParentScore() {
        return secondParentScore;
    }

    @Override
    public InnerScore<Score_> getScore() {
        return score;
    }

    @Override
    public int compareTo(Individual<Solution_, Score_> otherIndividual) {
        return score.compareTo(otherIndividual.getScore());
    }
}
