package ai.timefold.solver.core.impl.move;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamSession;
import ai.timefold.solver.core.impl.move.streams.MoveIterable;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MoveStreamsBasedMoveRepository<Solution_>
        implements MoveRepository<Solution_> {

    private final DefaultMoveStreamFactory<Solution_> moveStreamFactory;
    private final MoveProducer<Solution_> moveProducer;
    private final boolean random;

    private @Nullable DefaultMoveStreamSession<Solution_> moveStreamSession;
    private @Nullable MoveIterable<Solution_> moveIterable;
    private @Nullable Random workingRandom;

    public MoveStreamsBasedMoveRepository(DefaultMoveStreamFactory<Solution_> moveStreamFactory,
            MoveProducer<Solution_> moveProducer, boolean random) {
        this.moveStreamFactory = Objects.requireNonNull(moveStreamFactory);
        this.moveProducer = Objects.requireNonNull(moveProducer);
        this.random = random;
    }

    @Override
    public boolean isNeverEnding() {
        return random;
    }

    @Override
    public void initialize(Solution_ workingSolution, SupplyManager supplyManager) {
        if (moveStreamSession != null) {
            throw new IllegalStateException("Impossible state: move repository initialized twice.");
        }
        moveStreamSession = moveStreamFactory.createSession(workingSolution, supplyManager);
        moveStreamFactory.getSolutionDescriptor().visitAll(workingSolution, moveStreamSession::insert);
        moveStreamSession.settle();
        moveIterable = moveProducer.getMoveIterable(moveStreamSession);
    }

    public void insert(Object planningEntityOrProblemFact) {
        Objects.requireNonNull(moveStreamSession).insert(planningEntityOrProblemFact);
    }

    public void update(Object planningEntityOrProblemFact) {
        Objects.requireNonNull(moveStreamSession).update(planningEntityOrProblemFact);
    }

    public void retract(Object planningEntityOrProblemFact) {
        Objects.requireNonNull(moveStreamSession).retract(planningEntityOrProblemFact);
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
        Objects.requireNonNull(moveStreamSession).settle();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        if (moveStreamSession != null) {
            moveStreamSession.close();
            moveStreamSession = null;
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
