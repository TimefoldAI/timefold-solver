package ai.timefold.solver.enterprise.multithreaded;

import java.util.concurrent.ThreadFactory;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.enterprise.MultithreadedSolvingEnterpriseService;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.solver.termination.Termination;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class MultiThreadedFactory implements MultithreadedSolvingEnterpriseService {

    @Override
    public <Solution_> ConstructionHeuristicDecider<Solution_> buildConstructionHeuristic(int moveThreadCount,
            Termination<Solution_> termination, ConstructionHeuristicForager<Solution_> forager,
            EnvironmentMode environmentMode, HeuristicConfigPolicy<Solution_> configPolicy) {
        Integer moveThreadBufferSize = configPolicy.getMoveThreadBufferSize();
        if (moveThreadBufferSize == null) {
            // TODO Verify this is a good default by more meticulous benchmarking on multiple machines and JDK's
            // If it's too low, move threads will need to wait on the buffer, which hurts performance
            // If it's too high, more moves are selected that aren't foraged
            moveThreadBufferSize = 10;
        }
        ThreadFactory threadFactory = configPolicy.buildThreadFactory(ChildThreadType.MOVE_THREAD);
        int selectedMoveBufferSize = moveThreadCount * moveThreadBufferSize;
        MultiThreadedConstructionHeuristicDecider<Solution_> multiThreadedDecider =
                new MultiThreadedConstructionHeuristicDecider<>(configPolicy.getLogIndentation(), termination, forager,
                        threadFactory, moveThreadCount, selectedMoveBufferSize);
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            multiThreadedDecider.setAssertStepScoreFromScratch(true);
        }
        if (environmentMode.isIntrusiveFastAsserted()) {
            multiThreadedDecider.setAssertExpectedStepScore(true);
            multiThreadedDecider.setAssertShadowVariablesAreNotStaleAfterStep(true);
        }
        return multiThreadedDecider;
    }

    @Override
    public <Solution_> LocalSearchDecider<Solution_> buildLocalSearch(int moveThreadCount, Termination<Solution_> termination,
            MoveSelector<Solution_> moveSelector, Acceptor<Solution_> acceptor, LocalSearchForager<Solution_> forager,
            EnvironmentMode environmentMode, HeuristicConfigPolicy<Solution_> configPolicy) {
        Integer moveThreadBufferSize = configPolicy.getMoveThreadBufferSize();
        if (moveThreadBufferSize == null) {
            // TODO Verify this is a good default by more meticulous benchmarking on multiple machines and JDK's
            // If it's too low, move threads will need to wait on the buffer, which hurts performance
            // If it's too high, more moves are selected that aren't foraged
            moveThreadBufferSize = 10;
        }
        ThreadFactory threadFactory = configPolicy.buildThreadFactory(ChildThreadType.MOVE_THREAD);
        int selectedMoveBufferSize = moveThreadCount * moveThreadBufferSize;
        MultiThreadedLocalSearchDecider<Solution_> multiThreadedDecider = new MultiThreadedLocalSearchDecider<>(
                configPolicy.getLogIndentation(), termination, moveSelector, acceptor, forager,
                threadFactory, moveThreadCount, selectedMoveBufferSize);
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            multiThreadedDecider.setAssertStepScoreFromScratch(true);
        }
        if (environmentMode.isIntrusiveFastAsserted()) {
            multiThreadedDecider.setAssertExpectedStepScore(true);
            multiThreadedDecider.setAssertShadowVariablesAreNotStaleAfterStep(true);
        }
        return multiThreadedDecider;
    }

}
