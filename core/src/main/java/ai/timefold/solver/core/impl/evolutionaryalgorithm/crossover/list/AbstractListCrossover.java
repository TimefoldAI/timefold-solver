package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.list;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract sealed class AbstractListCrossover<Solution_, Score_ extends Score<Score_>>
        implements CrossoverStrategy<Solution_, Score_>
        permits ListOXCrossover, ListRXCrossover {

    final Phase<Solution_> localSearchPhase;
    final @Nullable Phase<Solution_> refinementPhase;
    final double inheritanceRate;

    AbstractListCrossover(Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
            double inheritanceRate) {
        this.localSearchPhase = localSearchPhase;
        this.refinementPhase = refinementPhase;
        this.inheritanceRate = inheritanceRate;
    }

    /**
     * Applies a best-insertion method for all values given by {@code chromosome}.
     */
    static <Solution_, Score_ extends Score<Score_>> void applyBestFit(InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel,
            ValueRangeManager<Solution_> valueRangeManager, ChromosomeEntry[] chromosome, Set<Object> excludeValuesSet) {
        for (var entry : chromosome) {
            var rebasedValue = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(entry.value()));
            applyBestFit(scoreDirector, listVariableStateSupply, listVariableMetaModel, listVariableDescriptor,
                    valueRangeManager, rebasedValue, excludeValuesSet);
        }
    }

    public static <Solution_, Score_ extends Score<Score_>> void applyBestFit(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel,
            ListVariableDescriptor<Solution_> listVariableDescriptor, ValueRangeManager<Solution_> valueRangeManager,
            Object value, Set<Object> excludeValuesSet) {
        if (excludeValuesSet.contains(value) || listVariableStateSupply.isPinned(value)
                || listVariableStateSupply.isAssigned(value)) {
            return;
        }
        var bestScore = scoreDirector.calculateScore().raw();
        var reachableEntities = valueRangeManager.getReachableValues(listVariableDescriptor).extractEntitiesAsList(value);
        MoveDescriptor<Solution_, Score_> bestMove = null;
        for (var entity : reachableEntities) {
            var entityMove = applyBestFit(scoreDirector, listVariableMetaModel, listVariableDescriptor, entity, value);
            if (bestMove == null || entityMove.score().raw().compareTo(bestMove.score().raw()) > 0) {
                bestMove = entityMove;
            }
        }
        if (bestMove != null && (!listVariableDescriptor.allowsUnassignedValues()
                || (bestMove.score().raw().compareTo(bestScore) > 0))) {
            // If the model accepts unassigned values and the move does not improve the best score,
            // we leave it unassigned
            scoreDirector.getMoveDirector().execute(bestMove.move(), true);
        }
    }

    private static <Solution_, Score_ extends Score<Score_>> MoveDescriptor<Solution_, Score_> applyBestFit(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel,
            ListVariableDescriptor<Solution_> listVariableDescriptor, Object entity, Object valueToAssign) {
        MoveDescriptor<Solution_, Score_> bestMoveDescriptor = null;
        var size = listVariableDescriptor.getListSize(entity);
        var startPos = size == 0 ? 0 : listVariableDescriptor.getFirstUnpinnedIndex(entity);
        for (var i = startPos; i <= size; i++) {
            var descriptor = testMove(scoreDirector, listVariableMetaModel, entity, valueToAssign, i);
            if (bestMoveDescriptor == null
                    || descriptor.score().raw().compareTo(bestMoveDescriptor.score().raw()) > 0) {
                bestMoveDescriptor = descriptor;
            }
        }
        return Objects.requireNonNull(bestMoveDescriptor);
    }

    private static <Solution_, Score_ extends Score<Score_>> MoveDescriptor<Solution_, Score_> testMove(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Object, Object> planningListVariableMetaModel, Object entity,
            Object valueToAssign, int pos) {
        var move = Moves.assign(planningListVariableMetaModel, valueToAssign, entity, pos);
        var moveScore = scoreDirector.executeTemporaryMove(move, false);
        return new MoveDescriptor<>(move, moveScore, moveScore.raw().isFeasible());
    }

    private record MoveDescriptor<Solution_, Score_ extends Score<Score_>>(Move<Solution_> move, InnerScore<Score_> score,
            boolean feasible) {
    }
}
