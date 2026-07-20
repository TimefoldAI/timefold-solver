package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * Basic representation of an individual.
 */
public interface Individual<Solution_, Score_ extends Score<Score_>> extends Comparable<Individual<Solution_, Score_>> {

    /**
     * The solution representation of the individual.
     */
    Solution_ getSolution();

    /**
     * @return a representation of the solution encoded as an array of chromosome entries,
     *         each carrying the planning value and its owning entity.
     */
    ChromosomeEntry[] getChromosome();

    /**
     * @return the individual size
     */
    int size();

    /**
     * Calculates the difference between two individuals according to some strategy.
     *
     * @param otherIndividual the other individual
     * @return a double where a higher value reflects a greater difference between the two individuals.
     */
    double diff(Individual<Solution_, Score_> otherIndividual);

    /**
     * The method analyzes the feasibility based on the score of the solution.
     *
     * @return true if the individual is feasible.
     */
    boolean isFeasible();

    /**
     * Clone the individual and its related information.
     */
    Individual<Solution_, Score_> clone(InnerScoreDirector<Solution_, Score_> scoreDirector);

    /**
     * @return the score of the first parent that generated this individual.
     */
    InnerScore<Score_> getFirstParentScore();

    /**
     * @return the score of the second parent that generated this individual.
     */
    InnerScore<Score_> getSecondParentScore();

    /**
     * The individual raw score.
     */
    InnerScore<Score_> getScore();
}
