package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.neighborhood.move.InnerMoveStream;
import ai.timefold.solver.core.impl.neighborhood.move.MoveIterable;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class NeighborhoodsBasedMoveRepository<Solution_>
        implements MoveRepository<Solution_> {

    private final DefaultMoveStreamFactory<Solution_> moveStreamFactory;
    private final InnerMoveStream<Solution_> moveStream;
    private final boolean random;

    private @Nullable DefaultNeighborhoodSession<Solution_> neighborhoodSession;
    private @Nullable MoveIterable<Solution_> moveIterable;
    private @Nullable Random workingRandom;

    public NeighborhoodsBasedMoveRepository(DefaultMoveStreamFactory<Solution_> moveStreamFactory,
            InnerMoveStream<Solution_> moveStream, boolean random) {
        this.moveStreamFactory = Objects.requireNonNull(moveStreamFactory);
        this.moveStream = Objects.requireNonNull(moveStream);
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
        moveIterable = moveStream.getMoveIterable(neighborhoodSession);
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
        this.workingRandom = phaseScope.getWorkingRandom();
        phaseScope.getScoreDirector().setMoveRepository(this);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // No need to do anything.
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        Objects.requireNonNull(neighborhoodSession).settle();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        if (neighborhoodSession != null) {
            neighborhoodSession = null;
        }
        phaseScope.getScoreDirector().setMoveRepository(null);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // No need to do anything.
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new NeverendingMoveIterator<>(moveIterable, random ? workingRandom : null);
    }

    @NullMarked
    private static final class NeverendingMoveIterator<Solution_> implements Iterator<Move<Solution_>> {

        private final MoveIterable<Solution_> iterable;
        private final @Nullable Random workingRandom;
        private Iterator<Move<Solution_>> iterator;

        public NeverendingMoveIterator(MoveIterable<Solution_> iterable, @Nullable Random workingRandom) {
            this.iterable = Objects.requireNonNull(iterable);
            this.workingRandom = workingRandom;
            this.iterator = createIterator();
        }

        private Iterator<Move<Solution_>> createIterator() {
            return workingRandom == null ? iterable.iterator() : iterable.iterator(workingRandom);
        }

        @Override
        public boolean hasNext() {
            if (iterator.hasNext()) {
                return true;
            }
            // If exhausted, start all over.
            iterator = createIterator();
            return iterator.hasNext();
        }

        @Override
        public Move<Solution_> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return iterator.next();
        }
    }

}
