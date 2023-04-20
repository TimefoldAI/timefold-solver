package ai.timefold.solver.examples.common.app;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractConstructionHeuristicTest<Solution_>
        extends AbstractPhaseTest<Solution_, ConstructionHeuristicType> {

    protected Predicate<ConstructionHeuristicType> includeConstructionHeuristicType() {
        return constructionHeuristicType -> true;
    }

    @Override
    protected Stream<ConstructionHeuristicType> solverFactoryParams() {
        return Stream.of(ConstructionHeuristicType.values()).filter(includeConstructionHeuristicType());
    }

    @Override
    protected SolverFactory<Solution_> buildSolverFactory(CommonApp<Solution_> commonApp,
            ConstructionHeuristicType constructionHeuristicType) {
        String solverConfigResource = commonApp.getSolverConfigResource();
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource);
        solverConfig.setTerminationConfig(new TerminationConfig());
        ConstructionHeuristicPhaseConfig constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        constructionHeuristicPhaseConfig.setConstructionHeuristicType(constructionHeuristicType);
        solverConfig.setPhaseConfigList(Arrays.asList(constructionHeuristicPhaseConfig));
        return SolverFactory.create(solverConfig);
    }
}
