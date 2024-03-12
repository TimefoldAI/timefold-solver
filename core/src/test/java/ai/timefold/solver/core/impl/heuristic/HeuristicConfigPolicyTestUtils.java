package ai.timefold.solver.core.impl.heuristic;

import java.util.Random;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

public final class HeuristicConfigPolicyTestUtils {

    public static HeuristicConfigPolicy<TestdataSolution> buildHeuristicConfigPolicy() {
        return buildHeuristicConfigPolicy(TestdataSolution.buildSolutionDescriptor());
    }

    public static <Solution_> HeuristicConfigPolicy<Solution_>
            buildHeuristicConfigPolicy(SolutionDescriptor<Solution_> solutionDescriptor) {
        return new HeuristicConfigPolicy.Builder<>(EnvironmentMode.REPRODUCIBLE, null, null, null, null, new Random(), null,
                solutionDescriptor, ClassInstanceCache.create())
                .build();
    }

    private HeuristicConfigPolicyTestUtils() {
    }
}
