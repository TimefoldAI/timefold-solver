package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;

/**
 * Handles the saving and restoring of the working solution state for solutions using
 * {@link BasicVariableDescriptor basic planning variables}.
 * <p>
 * {@link SolutionStateManager#saveSolutionState} captures a snapshot of the current working solution by recording
 * the value of every basic planning variable for every planning entity.
 * An entity descriptor may declare multiple basic variables; each is captured as a separate {@link BasicValueState}
 * entry keyed by its position in the entity's ordered list of basic variable descriptors given by
 * {@link EntityDescriptor#getBasicVariableDescriptorList()}.
 * <p>
 * {@link #restoreSolutionState} revert the working solution back to a previously saved snapshot by re-applying the
 * saved variable values via change moves.
 * <p>
 * When saving from an {@link Individual},
 * {@link ChromosomeEntry#index()} is interpreted as the index of the variable descriptor within the entity's list of
 * basic variable descriptors.
 */
@NullMarked
public final class BasicSolutionStateManager<Solution_, Score_ extends Score<Score_>>
        implements SolutionStateManager<Solution_, Score_, BasicSolutionState<Solution_, Score_>> {

    @Override
    public BasicSolutionState<Solution_, Score_> saveSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector,
            boolean saveAssigned) {
        var solution = scoreDirector.getWorkingSolution();
        var entityValueList = new ArrayList<BasicValueState>();
        for (var entityDescriptor : scoreDirector.getSolutionDescriptor().getGenuineEntityDescriptors()) {
            var basicVarDescriptors = entityDescriptor.getBasicVariableDescriptorList();
            if (basicVarDescriptors.isEmpty()) {
                continue;
            }
            for (var entity : entityDescriptor.extractEntities(solution)) {
                for (var i = 0; i < basicVarDescriptors.size(); i++) {
                    var variableDescriptor = basicVarDescriptors.get(i);
                    var value = saveAssigned || scoreDirector.getMoveDirector().isPinned(entityDescriptor, entity)
                            ? variableDescriptor.getValue(entity)
                            : null;
                    entityValueList.add(new BasicValueState(entity, value, i));
                }
            }
        }
        return new BasicSolutionState<>(solution, Collections.unmodifiableList(entityValueList),
                scoreDirector.calculateScore());
    }

    @Override
    public BasicSolutionState<Solution_, Score_> saveSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector,
            Individual<Solution_, Score_> individual) {
        var chromosome = individual.getChromosome();
        if (chromosome.length == 0) {
            return new BasicSolutionState<>(individual.getSolution(), Collections.emptyList(), individual.getScore());
        }
        // ChromosomeEntry.index encodes the variable descriptor index within the entity's basic variable list.
        var entityValueList = Arrays.stream(chromosome)
                .map(entry -> new BasicValueState(entry.entity(), entry.value(), entry.index()))
                .toList();
        return new BasicSolutionState<>(individual.getSolution(), entityValueList, individual.getScore());
    }

    @Override
    public void restoreSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector,
            BasicSolutionState<Solution_, Score_> stateToRestore) {
        if (stateToRestore.stateList().isEmpty()) {
            return;
        }
        var stateList = stateToRestore.stateList();
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var needRebase = stateToRestore.getSolution() != scoreDirector.getWorkingSolution();
        var moveList = new ArrayList<Move<Solution_>>(stateList.size());
        EntityDescriptor<Solution_> entityDescriptor = null;
        for (var stateEntry : stateList) {
            if (entityDescriptor == null || entityDescriptor.getEntityClass() != stateEntry.entity().getClass()) {
                entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(stateEntry.entity().getClass());
            }
            var basicVarDescriptors = entityDescriptor.getBasicVariableDescriptorList();
            if (basicVarDescriptors.isEmpty()) {
                continue;
            }
            var variableDescriptor = basicVarDescriptors.get(stateEntry.index());
            var rebasedEntity = stateEntry.entity();
            var rebasedValue = stateEntry.value();
            if (needRebase) {
                rebasedEntity = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(stateEntry.entity()));
                rebasedValue = stateEntry.value() != null
                        ? Objects.requireNonNull(scoreDirector.lookUpWorkingObject(stateEntry.value()))
                        : null;
            }
            moveList.add(Moves.change(variableDescriptor.getVariableMetaModel(), rebasedEntity, rebasedValue));
        }
        if (!moveList.isEmpty()) {
            var compositeMove = Moves.compose(moveList);
            scoreDirector.getMoveDirector().execute(compositeMove);
        }
    }
}
