package ai.timefold.solver.core.impl.heuristic;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.Executors;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;

import org.junit.jupiter.api.Test;

/**
 * A {@link HeuristicConfigPolicy} is built on the thread which calls {@code buildSolver()},
 * but partitioned search builds its phases on the thread which calls {@code solve()}.
 * Selector factories must therefore not touch anything which belongs to the building thread.
 */
class HeuristicConfigPolicyThreadHandoffTest {

    @Test
    void buildsSelectorsOnADifferentThreadThanTheConfigPolicy() {
        // Stands for buildSolver(). The policy, and everything in it, belongs to this thread.
        var configPolicy = new HeuristicConfigPolicy.Builder<TestdataEntityProvidingSolution>()
                .withEnvironmentMode(EnvironmentMode.PHASE_ASSERT)
                .withSolutionDescriptor(TestdataEntityProvidingSolution.buildSolutionDescriptor())
                .withClassInstanceCache(ClassInstanceCache.create())
                .build();

        // Stands for solve(), where partitioned search builds its phases.
        // TestdataEntityProvidingSolution takes its value range from the entity,
        // which is what makes the factory ask for a mimic recorder id.
        try (var executor = Executors.newSingleThreadExecutor()) {
            var task = executor.submit(
                    () -> new SwapMoveSelectorFactory<TestdataEntityProvidingSolution>(new SwapMoveSelectorConfig())
                            .buildMoveSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM,
                                    false));
            assertThatCode(task::get).doesNotThrowAnyException();
        }
    }

}
