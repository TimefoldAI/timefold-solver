package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.InnerMoveStream;
import ai.timefold.solver.core.impl.neighborhood.stream.MoveIterable;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class NeighborhoodsBasedMoveRepository<Solution_>
        implements MoveRepository<Solution_> {

    private final DefaultMoveStreamFactory<Solution_> moveStreamFactory;
    private final List<InnerMoveStream<Solution_>> moveStreamList;
    private final boolean random;

    private @Nullable DefaultNeighborhoodSession<Solution_> neighborhoodSession;
    private @Nullable List<MoveIterable<Solution_>> moveIterableList;
    private @Nullable Random workingRandom;

    public NeighborhoodsBasedMoveRepository(DefaultMoveStreamFactory<Solution_> moveStreamFactory,
            List<MoveProvider<Solution_>> neighborhood, boolean random) {
        this.moveStreamFactory = Objects.requireNonNull(moveStreamFactory);
        this.moveStreamList = Objects.requireNonNull(neighborhood).stream()
                .map(d -> (InnerMoveStream<Solution_>) d.build(moveStreamFactory))
                .toList();
        this.random = random;
    }

    @Override
    public boolean isNeverEnding() {
        return random;
    }

    @Override
    public void initialize(SessionContext<Solution_> context) {
        if (neighborhoodSession != null) {
            throw new IllegalStateException("Impossible state: move repository initialized twice.");
        }
        neighborhoodSession = moveStreamFactory.createSession(context);
        moveStreamFactory.getSolutionDescriptor().visitAll(context.workingSolution(), neighborhoodSession::insert);
        neighborhoodSession.settle();
        moveIterableList = moveStreamList.stream()
                .map(m -> m.getMoveIterable(neighborhoodSession))
                .toList();
    }

    public void insert(Object planningEntityOrProblemFact) {
        Objects.requireNonNull(neighborhoodSession).insert(planningEntityOrProblemFact);
    }

    public void update(Object planningEntityOrProblemFact) {
        Objects.requireNonNull(neighborhoodSession).update(planningEntityOrProblemFact);
    }

    public void retract(Object planningEntityOrProblemFact) {
        Objects.requireNonNull(neighborhoodSession).retract(planningEntityOrProblemFact);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // No need to do anything.
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        workingRandom = phaseScope.getWorkingRandom();
        phaseScope.getScoreDirector().setMoveRepository(this);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // No need to do anything.
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
        phaseScope.getScoreDirector().setMoveRepository(null);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // No need to do anything.
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (random) {
            return new RandomOrderNeighborhoodIterator<>(moveIterableList, Objects.requireNonNull(workingRandom));
        } else {
            return new OriginalOrderNeighborhoodIterator<>(moveIterableList);
        }
    }

}
