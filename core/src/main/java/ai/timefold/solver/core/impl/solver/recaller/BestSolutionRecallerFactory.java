package ai.timefold.solver.core.impl.solver.recaller;

import ai.timefold.solver.core.config.solver.EnvironmentMode;

public class BestSolutionRecallerFactory {

    public static BestSolutionRecallerFactory create() {
        return new BestSolutionRecallerFactory();
    }

    public <Solution_> BestSolutionRecaller<Solution_> buildBestSolutionRecaller(EnvironmentMode environmentMode) {
        var bestSolutionRecaller = new BestSolutionRecaller<Solution_>();
        bestSolutionRecaller.enableAssertions(environmentMode);
        return bestSolutionRecaller;
    }
}
