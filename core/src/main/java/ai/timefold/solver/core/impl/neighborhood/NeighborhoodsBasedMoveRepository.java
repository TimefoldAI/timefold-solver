package ai.timefold.solver.core.impl.neighborhood;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.neighborhood.selection.DefaultMoveSelectionContext;
import ai.timefold.solver.core.impl.neighborhood.selection.DefaultMoveSource;
import ai.timefold.solver.core.impl.neighborhood.selection.StrategyUnionMoveIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.InnerMoveStream;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSource;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class NeighborhoodsBasedMoveRepository<Solution_> implements MoveRepository<Solution_> {

    private final DefaultMoveStreamFactory<Solution_> moveStreamFactory;
    private final List<DefaultMoveSource<Solution_>> moveSourceList;
    private final List<InnerMoveStream<Solution_>> moveStreamList;
    private final Supplier<MoveSelectionStrategy<Solution_>> strategyFactory;

    private @Nullable DefaultNeighborhoodSession<Solution_> neighborhoodSession;
    private @Nullable List<MoveIterable<Solution_>> moveIterableList;
    private final DefaultMoveSelectionContext<Solution_> phaseContext;
    private final DefaultMoveSelectionContext<Solution_> stepContext;
    private @Nullable RandomGenerator workingRandom;
    // A new instance for each phase run, so that no state of an earlier run reaches a custom strategy.
    private @Nullable MoveSelectionStrategy<Solution_> strategy;
    private int stepIndex = -1;

    /**
     * @param moveProviderMap ID to move provider, in registration order
     * @param strategyFactory gives a new strategy for each phase run, with its custom properties applied
     */
    public NeighborhoodsBasedMoveRepository(DefaultMoveStreamFactory<Solution_> moveStreamFactory,
            SequencedMap<String, MoveProvider<Solution_>> moveProviderMap,
            Supplier<MoveSelectionStrategy<Solution_>> strategyFactory) {
        this.moveStreamFactory = Objects.requireNonNull(moveStreamFactory);
        var sourceList = new ArrayList<DefaultMoveSource<Solution_>>(moveProviderMap.size());
        var streamList = new ArrayList<InnerMoveStream<Solution_>>(moveProviderMap.size());
        for (var entry : moveProviderMap.entrySet()) {
            sourceList.add(new DefaultMoveSource<>(sourceList.size(), entry.getKey(), entry.getValue()));
            streamList.add((InnerMoveStream<Solution_>) entry.getValue().build(moveStreamFactory));
        }
        this.moveSourceList = List.copyOf(sourceList);
        this.moveStreamList = List.copyOf(streamList);
        this.strategyFactory = Objects.requireNonNull(strategyFactory);
        List<MoveSource<Solution_>> publicMoveSourceList = List.copyOf(moveSourceList);
        this.phaseContext = new DefaultMoveSelectionContext<>(publicMoveSourceList);
        this.stepContext = new DefaultMoveSelectionContext<>(publicMoveSourceList);
    }

    @Override
    public boolean isNeverEnding() {
        return true;
    }

    public int getNeighborhoodCount() {
        return moveStreamList.size();
    }

    @Override
    public void initialize(SessionContext<Solution_> context) {
        if (neighborhoodSession != null) {
            throw new IllegalStateException("Impossible state: move repository initialized twice.");
        }
        neighborhoodSession = moveStreamFactory.createSession(context);
        moveStreamFactory.getSolutionDescriptor().visitAll(context.workingSolution(), neighborhoodSession::insert);
        neighborhoodSession.settle();
        moveIterableList = moveStreamList.stream().map(m -> m.getMoveIterable(neighborhoodSession)).toList();
    }

    public void insert(Object planningEntityOrProblemFact) {
        neighborhoodSession.insert(planningEntityOrProblemFact);
    }

    public void update(Object planningEntityOrProblemFact) {
        neighborhoodSession.update(planningEntityOrProblemFact);
    }

    public void retract(Object planningEntityOrProblemFact) {
        neighborhoodSession.retract(planningEntityOrProblemFact);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // No need to do anything.
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        workingRandom = phaseScope.getWorkingRandom().moveIteratorUsage();
        phaseScope.getScoreDirector().setMoveRepository(this);
        phaseContext.setRandom(workingRandom);
        strategy = strategyFactory.get();
        phaseContext.setSolution(currentSolution(phaseScope));
        strategy.phaseStarted(phaseContext);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        neighborhoodSession.settle(); // In tests, the stepEnded() might not have been called. Do it here as well, just in case.
        stepIndex = stepScope.getStepIndex();
        stepContext.setSolution(currentSolution(stepScope.getPhaseScope()));
        strategy.stepStarted(stepContext);
    }

    /**
     * Temporary moves also set the score field of the working solution; set it back to the last known score.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <Solution_> Solution_ currentSolution(AbstractPhaseScope<Solution_> phaseScope) {
        var scoreDirector = phaseScope.getScoreDirector();
        var solution = scoreDirector.getWorkingSolution();
        var knownScore = phaseScope.getLastCompletedStepScope().getScore();
        scoreDirector.getSolutionDescriptor().setScore(solution, (Score) knownScore.raw());
        return solution;
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        neighborhoodSession.settle(); // The step will have made changes to the working memory; settle it again.
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        neighborhoodSession = null;
        moveIterableList = null;
        workingRandom = null;
        strategy = null;
        phaseContext.setRandom(null);
        phaseScope.getScoreDirector().setMoveRepository(null);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // No need to do anything.
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new StrategyUnionMoveIterator<>(strategy, moveSourceList, moveIterableList, workingRandom, stepIndex);
    }

}
