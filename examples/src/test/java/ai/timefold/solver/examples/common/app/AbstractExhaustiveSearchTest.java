package ai.timefold.solver.examples.common.app;

import java.util.Arrays;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractExhaustiveSearchTest<Solution_>
        extends AbstractPhaseTest<Solution_, ExhaustiveSearchType> {

    @Override
    protected Stream<ExhaustiveSearchType> solverFactoryParams() {
        return Stream.of(ExhaustiveSearchType.values());
    }

    @Override
    protected SolverFactory<Solution_> buildSolverFactory(
            CommonApp<Solution_> commonApp,
            ExhaustiveSearchType exhaustiveSearchType) {
        String solverConfigResource = commonApp.getSolverConfigResource();
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource);
        solverConfig.setTerminationConfig(new TerminationConfig());
        ExhaustiveSearchPhaseConfig exhaustiveSearchPhaseConfig = new ExhaustiveSearchPhaseConfig();
        exhaustiveSearchPhaseConfig.setExhaustiveSearchType(exhaustiveSearchType);
        solverConfig.setPhaseConfigList(Arrays.asList(exhaustiveSearchPhaseConfig));
        return SolverFactory.create(solverConfig);
    }
}
