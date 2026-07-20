package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual;

import java.util.ArrayList;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Default representation of an individual for basic planning variables.
 * <p>
 * Each {@link ChromosomeEntry} in the chromosome represents one (entity, variable) pair.
 * {@link ChromosomeEntry#index()} is the position of the variable descriptor within
 * {@link EntityDescriptor#getBasicVariableDescriptorList()}.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
@NullMarked
public final class BasicVariableIndividual<Solution_, Score_ extends Score<Score_>>
        extends AbstractIndividual<Solution_, Score_> {

    private final ChromosomeEntry[] chromosome;

    public BasicVariableIndividual(InnerScoreDirector<Solution_, Score_> scoreDirector, Solution_ solution,
            InnerScore<Score_> score, @Nullable InnerScore<Score_> firstParentScore,
            @Nullable InnerScore<Score_> secondParentScore) {
        super(solution, score, firstParentScore, secondParentScore);
        this.chromosome = load(scoreDirector.getSolutionDescriptor(), solution);
    }

    private BasicVariableIndividual(Solution_ solution, InnerScore<Score_> score,
            InnerScore<Score_> firstParentScore, InnerScore<Score_> secondParentScore,
            ChromosomeEntry[] chromosome) {
        super(solution, score, firstParentScore, secondParentScore);
        this.chromosome = chromosome;
    }

    private static <Solution_> ChromosomeEntry[] load(SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution) {
        var chromosomeList = new ArrayList<ChromosomeEntry>();
        for (var entityDescriptor : solutionDescriptor.getGenuineEntityDescriptors()) {
            var basicVarDescriptors = entityDescriptor.getBasicVariableDescriptorList();
            if (basicVarDescriptors.isEmpty()) {
                continue;
            }
            for (var entity : entityDescriptor.extractEntities(solution)) {
                for (var i = 0; i < basicVarDescriptors.size(); i++) {
                    chromosomeList.add(new ChromosomeEntry(entity, basicVarDescriptors.get(i).getValue(entity), i));
                }
            }
        }
        return chromosomeList.toArray(ChromosomeEntry[]::new);
    }

    @Override
    public ChromosomeEntry[] getChromosome() {
        return chromosome;
    }

    @Override
    public int size() {
        return chromosome.length;
    }

    @Override
    public double diff(Individual<Solution_, Score_> otherIndividual) {
        var other = (BasicVariableIndividual<Solution_, Score_>) otherIndividual;
        var total = chromosome.length;
        if (total == 0) {
            return 0.0;
        }
        var diff = 0;
        for (var i = 0; i < total; i++) {
            if (!Objects.equals(chromosome[i].value(), other.chromosome[i].value())) {
                diff++;
            }
        }
        return (double) diff / (double) total;
    }

    @Override
    public Individual<Solution_, Score_> clone(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var newSolution = scoreDirector.cloneSolution(solution);
        var newChromosome = load(scoreDirector.getSolutionDescriptor(), newSolution);
        return new BasicVariableIndividual<>(newSolution, score, firstParentScore, secondParentScore, newChromosome);
    }
}
