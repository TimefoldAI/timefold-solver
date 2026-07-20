package ai.timefold.solver.core.impl.evolutionaryalgorithm.swapstar;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

/**
 * Implementation of the SWAP* method described in the article:
 * <p>
 * Hybrid Genetic Search for the CVRP: Open-Source Implementation and SWAP* Neighborhood by Thibaut Vidal
 * <p>
 * The author explains
 * that the method involves selecting the best swap move between two planning values from different planning entities.
 * Instead of being applied in place, the swap move allows each planning value to be positioned differently,
 * resembling a change move instead.
 * <p>
 * The original implementation uses a geometric calculation based on polar sectors
 * to apply the move only to overlapping routes.
 * Conversely, the author mentions the option of using other strategies to locate nearby planning entities,
 * such as distance.
 * Therefore,
 * the proposed approach uses the Nearby feature
 * and evaluates only the three closest planning entities for a given source.
 * 
 * @param <Solution_> the solution type
 */
public final class ListSwapStarPhase<Solution_> extends AbstractPhase<Solution_> {

    private final EntitySelector<Solution_> originalEntitySelector;
    private final EntitySelector<Solution_> innerEntitySelector;

    private ListVariableDescriptor<Solution_> listVariableDescriptor;

    protected ListSwapStarPhase(Builder<Solution_> builder) {
        super(builder);
        this.originalEntitySelector = builder.originalEntitySelector;
        this.innerEntitySelector = builder.innerEntitySelector;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public PhaseType getPhaseType() {
        return PhaseType.LOCAL_SEARCH;
    }

    @Override
    public IntFunction<EventProducerId> getEventProducerIdSupplier() {
        return EventProducerId::localSearch;
    }

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        phaseStarted(phaseScope);
        var originalEntityIterator = originalEntitySelector.iterator();
        var lastUpdateEntityMap = new LastUpdateVersionMap();
        BestMoveMap<Solution_, ?> bestMoveMap = new BestMoveMap<>((int) solverScope.getProblemSizeStatistics().entityCount());
        while (originalEntityIterator.hasNext()) {
            if (phaseTermination.isPhaseTerminated(phaseScope)) {
                break;
            }
            var sourceEntity = originalEntityIterator.next();
            if (listVariableDescriptor.getListSize(sourceEntity) == 0) {
                continue;
            }
            for (Object otherEntity : innerEntitySelector) {
                var sourceEntityVersion = lastUpdateEntityMap.getVersion(sourceEntity);
                var otherEntityVersion = lastUpdateEntityMap.getVersion(otherEntity);
                if (listVariableDescriptor.getListSize(otherEntity) == 0
                        || (otherEntityVersion > -1 && sourceEntityVersion >= otherEntityVersion)) {
                    continue;
                }
                swapStar(phaseScope, lastUpdateEntityMap, bestMoveMap, sourceEntity, otherEntity);
            }
        }
        phaseEnded(phaseScope);
    }

    private <Score_ extends Score<Score_>> void swapStar(LocalSearchPhaseScope<Solution_> phaseScope,
            LastUpdateVersionMap lastUpdateVersionMap,
            BestMoveMap<Solution_, Score_> bestMoveMap, Object sourceEntity, Object otherEntity) {
        var stepIndex = phaseScope.getNextStepIndex();
        var solverScope = phaseScope.getSolverScope();
        // Compute the best three moves for both entities
        findThreeBestLocations(solverScope, bestMoveMap, sourceEntity, otherEntity);

        // Evaluate all 9 composite move combinations from the three best locations of each entity.
        // bestPairScore starts as null so that only composite (swap) moves are considered
        var sourceBestMoveLocation = bestMoveMap.getBestMoveLocation(sourceEntity, otherEntity);
        var otherBestMoveLocation = bestMoveMap.getBestMoveLocation(otherEntity, sourceEntity);
        MoveDescriptor<Solution_, Score_> bestPairScore = null;
        for (var i = 0; i < 3; i++) {
            if (sourceBestMoveLocation.bestMoves[i] == null) {
                continue;
            }
            for (var j = 0; j < 3; j++) {
                if (otherBestMoveLocation.bestMoves[j] == null) {
                    continue;
                }
                var compositeMoveDescriptor = computeCompositeBestLocation(solverScope, sourceBestMoveLocation.bestMoves[i],
                        otherBestMoveLocation.bestMoves[j]);
                if (bestPairScore == null || compositeMoveDescriptor.score().compareTo(bestPairScore.score()) > 0) {
                    bestPairScore = compositeMoveDescriptor;
                }
            }
        }

        // Apply the best score
        if (bestPairScore != null && bestPairScore.score().compareTo(solverScope.<Score_> getBestScore()) > 0) {
            solverScope.<Score_> getScoreDirector().getMoveDirector().execute(bestPairScore.move(), true);
            var step = new LocalSearchStepScope<>(phaseScope, stepIndex);
            step.setStep(bestPairScore.move());
            step.setScore(bestPairScore.score());
            solverScope.getSolver().getBestSolutionRecaller().processWorkingSolutionDuringMove(bestPairScore.score(), step);
            solverScope.addMoveEvaluationCount(1L);
            phaseScope.setLastCompletedStepScope(step);
            lastUpdateVersionMap.updateVersion(sourceEntity, otherEntity);
        }
    }

    private <Score_ extends Score<Score_>> void findThreeBestLocations(SolverScope<Solution_> solverScope,
            BestMoveMap<Solution_, Score_> bestMoveMap, Object sourceEntity, Object otherEntity) {
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        // Reset before recomputing to avoid stale entries with out-of-bounds indices
        // from previous iterations where list sizes may have been different.
        bestMoveMap.resetBestMoveLocation(sourceEntity, otherEntity);
        bestMoveMap.resetBestMoveLocation(otherEntity, sourceEntity);
        var sourceStartPos = listVariableDescriptor.getFirstUnpinnedIndex(sourceEntity);
        var otherStartPos = listVariableDescriptor.getFirstUnpinnedIndex(otherEntity);

        // Find the best move for each value between both entities
        var sourceEntityListSize = listVariableDescriptor.getListSize(sourceEntity);
        var otherEntityListSize = listVariableDescriptor.getListSize(otherEntity);
        for (var i = sourceStartPos; i < sourceEntityListSize; i++) {
            for (var j = otherStartPos; j <= otherEntityListSize; j++) {
                var listChangeMove =
                        Moves.change(listVariableDescriptor.getVariableMetaModel(), sourceEntity, i, otherEntity, j);
                var moveScore = scoreDirector.getMoveDirector()
                        .executeTemporary(listChangeMove, (score, move) -> score);
                bestMoveMap.updateBestLocation(sourceEntity, i, otherEntity, j, listChangeMove, moveScore);
                solverScope.addMoveEvaluationCount(1L);
                if (j < otherEntityListSize) {
                    var otherListChangeMove =
                            Moves.change(listVariableDescriptor.getVariableMetaModel(), otherEntity, j, sourceEntity, i);
                    var otherMoveScore = scoreDirector.getMoveDirector()
                            .executeTemporary(otherListChangeMove, (score, move) -> score);
                    solverScope.addMoveEvaluationCount(1L);
                    bestMoveMap.updateBestLocation(otherEntity, j, sourceEntity, i, otherListChangeMove, otherMoveScore);
                }
            }
        }
        // One last iteration to compute last position for otherEntity
        for (var j = otherStartPos; j < otherEntityListSize; j++) {
            var otherListChangeMove = Moves.change(listVariableDescriptor.getVariableMetaModel(), otherEntity, j, sourceEntity,
                    sourceEntityListSize);
            var otherMoveScore = scoreDirector.getMoveDirector()
                    .executeTemporary(otherListChangeMove, (score, move) -> score);
            solverScope.addMoveEvaluationCount(1L);
            bestMoveMap.updateBestLocation(otherEntity, j, sourceEntity, sourceEntityListSize, otherListChangeMove,
                    otherMoveScore);
        }
    }

    private <Score_ extends Score<Score_>> MoveDescriptor<Solution_, Score_> computeCompositeBestLocation(
            SolverScope<Solution_> solverScope, MoveDescriptor<Solution_, Score_> sourceMoveDescriptor,
            MoveDescriptor<Solution_, Score_> otherMoveDescriptor) {
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        var sourceList = listVariableDescriptor.getValue(sourceMoveDescriptor.sourceEntity());
        var otherList = listVariableDescriptor.getValue(otherMoveDescriptor.sourceEntity());

        var unassignSourceMove = Moves.unassign(listVariableDescriptor.getVariableMetaModel(),
                sourceMoveDescriptor.sourceEntity(), sourceMoveDescriptor.i());
        var sourceJ = sourceMoveDescriptor.j();
        if (sourceJ == otherList.size()) {
            sourceJ--;
        }
        var assignSourceMove = Moves.assign(listVariableDescriptor.getVariableMetaModel(),
                sourceList.get(sourceMoveDescriptor.i()), sourceMoveDescriptor.otherEntity, sourceJ);

        var unassignOtherMove = Moves.unassign(listVariableDescriptor.getVariableMetaModel(),
                otherMoveDescriptor.sourceEntity(), otherMoveDescriptor.i());
        var otherJ = otherMoveDescriptor.j();
        if (otherJ == sourceList.size()) {
            otherJ--;
        }
        var assignOtherMove = Moves.assign(listVariableDescriptor.getVariableMetaModel(),
                otherList.get(otherMoveDescriptor.i()), otherMoveDescriptor.otherEntity, otherJ);

        // Unassign both values and reassign them
        var compositeMove = Moves.compose(unassignSourceMove, unassignOtherMove, assignSourceMove, assignOtherMove);
        var moveScore = scoreDirector.getMoveDirector().executeTemporary(compositeMove, (score, move) -> score);
        solverScope.addMoveEvaluationCount(1L);
        return new MoveDescriptor<>(sourceMoveDescriptor.sourceEntity(), -1, otherMoveDescriptor.sourceEntity(), -1,
                compositeMove, moveScore);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        originalEntitySelector.solvingStarted(solverScope);
        innerEntitySelector.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        originalEntitySelector.solvingEnded(solverScope);
        innerEntitySelector.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        originalEntitySelector.phaseStarted(phaseScope);
        innerEntitySelector.phaseStarted(phaseScope);
        this.listVariableDescriptor = phaseScope.getScoreDirector().getSolutionDescriptor().getListVariableDescriptor();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        originalEntitySelector.phaseEnded(phaseScope);
        innerEntitySelector.phaseEnded(phaseScope);
        this.listVariableDescriptor = null;
    }

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final EntitySelector<Solution_> originalEntitySelector;
        private final EntitySelector<Solution_> innerEntitySelector;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                EntitySelector<Solution_> originalEntitySelector, EntitySelector<Solution_> innerEntitySelector) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.originalEntitySelector = originalEntitySelector;
            this.innerEntitySelector = innerEntitySelector;
        }

        @Override
        public ListSwapStarPhase<Solution_> build() {
            return new ListSwapStarPhase<>(this);
        }
    }

    private static class LastUpdateVersionMap {
        private int currentVersion = 0;
        private final Map<Object, Integer> versionMap = new IdentityHashMap<>();

        void updateVersion(Object entity, Object otherEntity) {
            currentVersion++;
            versionMap.put(entity, currentVersion);
            versionMap.put(otherEntity, currentVersion);
        }

        int getVersion(Object entity) {
            var version = versionMap.get(entity);
            if (version == null) {
                versionMap.put(entity, currentVersion);
                return -1;
            }
            return version;
        }
    }

    private static class BestMoveMap<Solution_, Score_ extends Score<Score_>> {

        private final int entitySize;
        private final Map<Object, Map<Object, BestMoveLocation<Solution_, Score_>>> valuesMap;

        BestMoveMap(int entitySize) {
            this.entitySize = entitySize;
            valuesMap = new IdentityHashMap<>(entitySize);
        }

        void updateBestLocation(Object sourceEntity, int i, Object otherEntity, int j, Move<Solution_> move,
                InnerScore<Score_> score) {
            var bestMoveLocation = getBestMoveLocation(sourceEntity, otherEntity);
            bestMoveLocation.updateLocation(sourceEntity, i, otherEntity, j, move, score);
        }

        void resetBestMoveLocation(Object sourceEntity, Object otherEntity) {
            getBestMoveLocation(sourceEntity, otherEntity).reset();
        }

        BestMoveLocation<Solution_, Score_> getBestMoveLocation(Object sourceEntity, Object otherEntity) {
            var sourceMap = valuesMap.get(sourceEntity);
            if (sourceMap == null) {
                sourceMap = new IdentityHashMap<>(entitySize);
                valuesMap.put(sourceEntity, sourceMap);
            }
            var bestMoveLocation = sourceMap.get(otherEntity);
            if (bestMoveLocation == null) {
                bestMoveLocation = new BestMoveLocation<>();
                sourceMap.put(otherEntity, bestMoveLocation);
            }
            return bestMoveLocation;
        }
    }

    private static class BestMoveLocation<Solution_, Score_ extends Score<Score_>> {

        private MoveDescriptor<Solution_, Score_>[] bestMoves = new MoveDescriptor[3];

        void reset() {
            bestMoves[0] = null;
            bestMoves[1] = null;
            bestMoves[2] = null;
        }

        void updateLocation(Object sourceEntity, int i, Object otherEntity, int j, Move<Solution_> move,
                InnerScore<Score_> score) {
            if (bestMoves[2] == null || score.compareTo(bestMoves[2].score()) > 0) {
                bestMoves[0] = bestMoves[1];
                bestMoves[1] = bestMoves[2];
                bestMoves[2] = new MoveDescriptor<>(sourceEntity, i, otherEntity, j, move, score);
            } else if (bestMoves[1] == null || score.compareTo(bestMoves[1].score()) > 0) {
                bestMoves[0] = bestMoves[1];
                bestMoves[1] = new MoveDescriptor<>(sourceEntity, i, otherEntity, j, move, score);
            } else if (bestMoves[0] == null || score.compareTo(bestMoves[0].score()) > 0) {
                bestMoves[0] = new MoveDescriptor<>(sourceEntity, i, otherEntity, j, move, score);
            }
        }
    }

    private record MoveDescriptor<Solution_, Score_ extends Score<Score_>>(Object sourceEntity, int i, Object otherEntity,
            int j, Move<Solution_> move, InnerScore<Score_> score) {
    }
}
