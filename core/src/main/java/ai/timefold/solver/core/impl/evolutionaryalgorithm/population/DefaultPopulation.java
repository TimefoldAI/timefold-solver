package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the population of candidate solutions for the evolutionary algorithm,
 * following the biased-fitness survival strategy described in the HGS
 * (Hybrid Genetic Search) original article.
 * <p>
 * Individuals are split into two subpopulations — feasible and infeasible,
 * each kept sorted by score in descending order (best individual first).
 * The combined capacity is {@code populationSize + generationSize} and once that threshold
 * is reached, survival selection reduces the population back to {@code populationSize}
 * by removing the least-fit individuals.
 * <p>
 * <b>Fitness</b> is a composite measure that rewards both solution quality and
 * contribution to population diversity.
 * Lower fitness is better,
 * and individuals whose solution is identical to all others (zero diversity) are always removed first.
 * <p>
 * <b>Selection</b> uses binary tournament: two candidates are selected at random from
 * the combined population, and the one with the lower fitness is returned.
 * <p>
 * <b>Restart</b> preserves the {@code eliteSolutionSize} best individuals
 * (feasible first, then infeasible) before clearing the population and seeding
 * it with a new set of individuals.
 */
@NullMarked
public final class DefaultPopulation<Solution_, Score_ extends Score<Score_>> implements Population<Solution_, Score_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPopulation.class);
    private final RandomGenerator workingRandom;
    private final int populationSize;
    private final int eliteSolutionSize;
    private final int maxSize;
    private final List<InternalIndividual<Solution_, Score_>> feasibleIndividualList;
    private final List<InternalIndividual<Solution_, Score_>> infeasibleIndividualList;
    private final PopulationDiffMap<Solution_, Score_> diffMap;
    @Nullable
    private Individual<Solution_, Score_> bestIndividual = null;
    private long bestGeneration = 0L;
    private long bestIteration = 0L;
    private long generationCount = 0L;
    private long individualCount = 0L;

    private boolean feasibleFitnessUpdated = false;
    private boolean infeasibleFitnessUpdated = false;

    public DefaultPopulation(RandomGenerator workingRandom, int populationSize, int generationSize, int eliteSolutionSize) {
        this.workingRandom = workingRandom;
        this.populationSize = populationSize;
        this.eliteSolutionSize = eliteSolutionSize;
        this.maxSize = populationSize + generationSize;
        this.feasibleIndividualList = new ArrayList<>(maxSize);
        this.infeasibleIndividualList = new ArrayList<>(maxSize);
        // The map can store at most maxSize elements from both lists
        this.diffMap = new PopulationDiffMap<>(maxSize * 2);
    }

    @Override
    public void addIndividual(Individual<Solution_, Score_> individual, boolean enableSurvivalSelection) {
        var internalIndividual = addIndividualToList(individual);
        if (enableSurvivalSelection) {
            // Calculate the difference between the new individual and each individual in the related list
            var individualList = individual.isFeasible() ? feasibleIndividualList : infeasibleIndividualList;
            computeDiff(internalIndividual, individualList);
            // Analyze and apply the survival selection strategy
            analyzeSubpopulationList(individualList);
        }
    }

    private InternalIndividual<Solution_, Score_> addIndividualToList(Individual<Solution_, Score_> individual) {
        var individualList = individual.isFeasible() ? feasibleIndividualList : infeasibleIndividualList;
        var pos = 0;
        var internalIndividual = new InternalIndividual<>(individual);
        if (!individualList.isEmpty()) {
            // The list is kept sorted by score descending (best first).
            // Comparator.reverseOrder() ensures the best scores are added to the beginning of the list
            pos = Collections.binarySearch(individualList, internalIndividual, Comparator.reverseOrder());
            if (pos < 0) {
                pos = -pos - 1;
            }
        }
        individualList.add(pos, internalIndividual);
        individualCount++;
        if (individualList == feasibleIndividualList) {
            feasibleFitnessUpdated = false;
        } else {
            infeasibleFitnessUpdated = false;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Added individual iteration ({}), generation({}), feasible population size({}), infeasible population size({}), individual score ({}), first parent score ({}), second parent score ({}), best score ({}), best generation ({}), best iteration ({})",
                    individualCount, generationCount, feasibleIndividualList.size(), infeasibleIndividualList.size(),
                    individual.getScore().raw(),
                    individual.getFirstParentScore() != null ? individual.getFirstParentScore().raw() : "-",
                    individual.getSecondParentScore() != null ? individual.getSecondParentScore().raw() : "-",
                    bestIndividual != null ? bestIndividual.getScore().raw() : "-", bestGeneration, bestIteration);
        }
        if (bestIndividual == null || internalIndividual.compareTo(bestIndividual) > 0) {
            bestIndividual = individual;
            bestGeneration = generationCount;
            bestIteration = individualCount;
        }
        return internalIndividual;
    }

    @Override
    public void restart(List<Individual<Solution_, Score_>> individuals) {
        var eliteIndividuals = new ArrayList<InternalIndividual<Solution_, Score_>>(eliteSolutionSize);
        var feasibleCount = feasibleIndividualList.size();
        if (feasibleCount >= eliteSolutionSize) {
            eliteIndividuals.addAll(feasibleIndividualList.subList(0, eliteSolutionSize));
        } else {
            eliteIndividuals.addAll(feasibleIndividualList);
            var infeasibleEliteCount = Math.min(eliteSolutionSize - feasibleCount, infeasibleIndividualList.size());
            eliteIndividuals.addAll(infeasibleIndividualList.subList(0, infeasibleEliteCount));
        }
        feasibleIndividualList.clear();
        infeasibleIndividualList.clear();
        diffMap.clear();
        eliteIndividuals.forEach(individual -> this.addIndividualToList(individual.innerIndividual));
        individuals.forEach(this::addIndividualToList);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Restarting population, size({}), generation({}), best generation({}), best iscore ({})", size(),
                    generationCount, bestGeneration, bestIndividual != null ? bestIndividual.getScore().raw() : "-");
        }
    }

    @Override
    public PopulationStatistics getStatistics() {
        return new PopulationStatistics(generationCount, individualCount, bestGeneration, bestIteration);
    }

    @Override
    public int size() {
        return feasibleIndividualList.size() + infeasibleIndividualList.size();
    }

    /**
     * Calculates the difference between the given individual and all other individuals from the given list.
     *
     * @param individual the first individual
     * @param individualList the list to be evaluated
     */
    private void computeDiff(Individual<Solution_, Score_> individual,
            List<InternalIndividual<Solution_, Score_>> individualList) {
        for (var otherIndividual : individualList) {
            if (individual == otherIndividual) {
                continue;
            }
            var diff = individual.diff(otherIndividual.innerIndividual);
            diffMap.addIndividualDiff(individual, otherIndividual, diff);
        }
    }

    /**
     * The survival method removes the worst individual from the population until the population size is restored.
     * This removal is based on the fitness of each individual,
     * which is calculated according to their contribution to the diversity of the population.
     * 
     * @param subpopulationList the population to be analyzed
     */
    private void analyzeSubpopulationList(List<InternalIndividual<Solution_, Score_>> subpopulationList) {
        if (subpopulationList.size() < maxSize) {
            return;
        }
        // Remove extra individuals until the population is restored to populationSize.
        // Fitness is computed once before the removal loop,
        // and subsequent removals use the pre-computed ranks,
        // which is a valid approximation since generationSize is small relative to populationSize.
        var sizeToRemove = subpopulationList.size() - populationSize;
        updateSubpopulationFitness(subpopulationList);
        for (int i = 0; i < sizeToRemove; i++) {
            // Find and remove the worst individual
            var worstIndividualIndex = 0;
            InternalIndividual<Solution_, Score_> worstIndividual = null;
            // It means all other individuals from the subpopulation have the same solution
            var hasWorstIndividualSameSolution = false;
            for (var j = 0; j < subpopulationList.size(); j++) {
                var otherIndividual = subpopulationList.get(j);
                // The average value will be the sum of all diffs.
                // If all other solutions have diff equal to zero,
                // it means all individuals have the same solution
                var hasSameSolution = averageDiff(otherIndividual, 1) == 0d;
                // 1 - We select the individual if it has no diff and the current worst element has any diff
                // 2 - We also select the individual if the fitness is higher, which means it is worse
                if ((worstIndividual == null) || (hasSameSolution && !hasWorstIndividualSameSolution)
                        || (hasWorstIndividualSameSolution == hasSameSolution
                                && otherIndividual.getFitness() > worstIndividual.getFitness())) {
                    worstIndividualIndex = j;
                    worstIndividual = otherIndividual;
                    hasWorstIndividualSameSolution = hasSameSolution;
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Removed individual, position({}),  individual fitness ({}), individual score ({}), best score ({})",
                        worstIndividualIndex, subpopulationList.get(worstIndividualIndex).getFitness(),
                        subpopulationList.get(worstIndividualIndex).getScore().raw(),
                        bestIndividual != null ? bestIndividual.getScore().raw() : "-");
            }
            subpopulationList.remove(worstIndividualIndex);
            diffMap.removeIndividualDiff(worstIndividual);
            if (subpopulationList == feasibleIndividualList) {
                feasibleFitnessUpdated = false;
            } else {
                infeasibleFitnessUpdated = false;
            }
        }
        generationCount++;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "New generation ({}), Best generation ({}), Feasible population size ({}), Infeasible population size ({})",
                    generationCount, bestGeneration, feasibleIndividualList.size(), infeasibleIndividualList.size());
        }

    }

    /**
     * The ranking method follows the logic proposed in the HGS article,
     * using both solution quality and diversity contribution to estimate fitness.
     */
    private void updateSubpopulationFitness(List<InternalIndividual<Solution_, Score_>> subpopulationList) {
        var isFeasiblePopulation = subpopulationList == feasibleIndividualList;
        if ((isFeasiblePopulation && feasibleFitnessUpdated) || (!isFeasiblePopulation && infeasibleFitnessUpdated)) {
            return;
        }
        var subpopulationSize = subpopulationList.size();
        var avgDiffs = new double[subpopulationSize];
        for (var i = 0; i < subpopulationSize; i++) {
            avgDiffs[i] = averageDiff(subpopulationList.get(i), eliteSolutionSize);
        }
        // sortedIndices[rank] = original index in subpopulationList, sorted by descending avgDiff
        var sortedIndices = new Integer[subpopulationSize];
        for (var i = 0; i < subpopulationSize; i++) {
            sortedIndices[i] = i;
        }
        // Rank according to the average diff and contribution to the diversity
        Arrays.sort(sortedIndices, Comparator.comparingDouble(i -> -avgDiffs[i]));
        if (subpopulationSize > 1) {
            var rankRatio = 1.0 / (double) (subpopulationSize - 1);
            var diversityWeight = (subpopulationSize >= eliteSolutionSize)
                    ? 1.0 - (double) eliteSolutionSize / (double) subpopulationSize
                    : 0.0;
            for (var rank = 0; rank < subpopulationSize; rank++) {
                int idx = sortedIndices[rank];
                // The list is already sorted by the score
                var scoreRank = rank * rankRatio;
                var diffRank = idx * rankRatio;
                subpopulationList.get(idx).setFitness(diffRank + diversityWeight * scoreRank);
            }
        }
        if (isFeasiblePopulation) {
            feasibleFitnessUpdated = true;
        } else {
            infeasibleFitnessUpdated = true;
        }

    }

    /**
     * Calculates the average diff to the {@code size} nearest (most similar) individuals.
     *
     * @param individual the individual to be evaluated
     * @param size the number of nearest individuals
     * @return a double where a higher value reflects the greater average difference to nearest individuals
     */
    private double averageDiff(Individual<Solution_, Score_> individual, int size) {
        var individualDiffMap = diffMap.getIndividualDiffMap(individual);
        var otherIndividualsCount = individualDiffMap.size();
        if (otherIndividualsCount == 0) {
            return 0.0;
        }
        // Hot path for a size of one
        if (size == 1) {
            var min = Double.MAX_VALUE;
            for (var diff : individualDiffMap.values()) {
                if (diff < min) {
                    min = diff;
                }
            }
            return min;
        }
        // All other individuals fit within the limit, so we can calculate the average without sorting
        if (otherIndividualsCount <= size) {
            var result = 0.d;
            for (var diff : individualDiffMap.values()) {
                result += diff;
            }
            return result / (double) otherIndividualsCount;
        }
        // Sort the individuals ascending and compute only k nearst ones
        var diffs = new double[otherIndividualsCount];
        var i = 0;
        for (var diff : individualDiffMap.values()) {
            diffs[i++] = diff;
        }
        Arrays.sort(diffs);
        var result = 0.d;
        for (var j = 0; j < size; j++) {
            result += diffs[j];
        }
        return result / (double) size;
    }

    @Override
    public Individual<Solution_, Score_> selectIndividual() {
        var size = feasibleIndividualList.size() + infeasibleIndividualList.size();
        var firstIdx = workingRandom.nextInt(0, size);
        var secondIdx = size > 1 ? workingRandom.nextInt(0, size - 1) : firstIdx;
        if (size > 1 && secondIdx >= firstIdx) {
            secondIdx++;
        }
        var firstIndividual = (firstIdx >= feasibleIndividualList.size())
                ? infeasibleIndividualList.get(firstIdx - feasibleIndividualList.size())
                : feasibleIndividualList.get(firstIdx);
        var secondIndividual = (secondIdx >= feasibleIndividualList.size())
                ? infeasibleIndividualList.get(secondIdx - feasibleIndividualList.size())
                : feasibleIndividualList.get(secondIdx);
        var updateFeasiblePopulation = firstIdx >= feasibleIndividualList.size() || secondIdx < feasibleIndividualList.size();
        var updateInfeasiblePopulation =
                firstIdx >= feasibleIndividualList.size() || secondIdx >= feasibleIndividualList.size();
        if (updateFeasiblePopulation) {
            updateSubpopulationFitness(feasibleIndividualList);
        }
        if (updateInfeasiblePopulation) {
            updateSubpopulationFitness(infeasibleIndividualList);
        }
        return firstIndividual.getFitness() < secondIndividual.getFitness() ? firstIndividual : secondIndividual;
    }

    @Override
    public @Nullable Individual<Solution_, Score_> getBestIndividual() {
        return bestIndividual;
    }

    private static class InternalIndividual<Solution_, Score_ extends Score<Score_>> implements Individual<Solution_, Score_> {

        private final Individual<Solution_, Score_> innerIndividual;
        private double fitness;

        private InternalIndividual(Individual<Solution_, Score_> innerIndividual) {
            this.innerIndividual = innerIndividual;
        }

        @Override
        public Solution_ getSolution() {
            return innerIndividual.getSolution();
        }

        @Override
        public ChromosomeEntry[] getChromosome() {
            return innerIndividual.getChromosome();
        }

        @Override
        public int size() {
            return innerIndividual.size();
        }

        @Override
        public double diff(Individual<Solution_, Score_> otherIndividual) {
            return innerIndividual.diff(otherIndividual);
        }

        @Override
        public boolean isFeasible() {
            return innerIndividual.isFeasible();
        }

        @Override
        public Individual<Solution_, Score_> clone(InnerScoreDirector<Solution_, Score_> scoreDirector) {
            return innerIndividual.clone(scoreDirector);
        }

        @Override
        public InnerScore<Score_> getFirstParentScore() {
            return innerIndividual.getFirstParentScore();
        }

        @Override
        public InnerScore<Score_> getSecondParentScore() {
            return innerIndividual.getSecondParentScore();
        }

        @Override
        public InnerScore<Score_> getScore() {
            return innerIndividual.getScore();
        }

        @Override
        public int compareTo(Individual<Solution_, Score_> o) {
            return innerIndividual.compareTo(o);
        }

        public double getFitness() {
            return fitness;
        }

        public void setFitness(double fitness) {
            this.fitness = fitness;
        }
    }
}
