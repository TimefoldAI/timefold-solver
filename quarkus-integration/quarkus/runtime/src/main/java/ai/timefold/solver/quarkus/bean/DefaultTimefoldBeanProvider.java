package ai.timefold.solver.quarkus.bean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Lock;

/**
 * Provider for managed resources of the default solver.
 */
@ApplicationScoped
@Lock(Lock.Type.WRITE)
public class DefaultTimefoldBeanProvider {

    private SolverFactory<?> solverFactory;

    private ConstraintMetaModel constraintMetaModel;

    private SolverManager<?> solverManager;

    private SolutionManager<?, ?> solutionManager;

    @SuppressWarnings("unchecked")
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolverFactory<Solution_> solverFactory(SolverConfig solverConfig) {
        if (solverFactory == null) {
            solverFactory = SolverFactory.create(solverConfig);
        }
        return (SolverFactory<Solution_>) solverFactory;
    }

    @DefaultBean
    @Dependent
    @Produces
    ConstraintMetaModel constraintProviderMetaModel(SolverFactory<?> solverFactory) {
        if (constraintMetaModel == null) {
            // The metamodel is not compatible with Quarkus code recording, thus we need to rebuild it at runtime.
            constraintMetaModel = BeanUtil.buildConstraintMetaModel(solverFactory);
        }
        return constraintMetaModel;
    }

    @SuppressWarnings("unchecked")
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolverManager<Solution_> solverManager(SolverFactory<Solution_> solverFactory,
            SolverManagerConfig solverManagerConfig) {
        if (solverManager == null) {
            solverManager = SolverManager.create(solverFactory, solverManagerConfig);
        }
        return (SolverManager<Solution_>) solverManager;
    }

    // Quarkus-ARC-Weld can't deal with enum pattern generics such as Score<S extends Score<S>>.
    // See https://github.com/quarkusio/quarkus/pull/12137
    //    @DefaultBean
    //    @Dependent
    //    @Produces
    @SuppressWarnings("unchecked")
    <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_> solutionManager(
            SolverFactory<Solution_> solverFactory) {
        if (solutionManager == null) {
            solutionManager = SolutionManager.create(solverFactory);
        }
        return (SolutionManager<Solution_, Score_>) solutionManager;
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, SimpleScore> solutionManager_workaroundSimpleScore(
            SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, SimpleBigDecimalScore> solutionManager_workaroundSimpleBigDecimalScore(
            SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardSoftScore> solutionManager_workaroundHardSoftScore(
            SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardSoftBigDecimalScore> solutionManager_workaroundHardSoftBigDecimalScore(
            SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardMediumSoftScore> solutionManager_workaroundHardMediumSoftScore(
            SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardMediumSoftBigDecimalScore>
            solutionManager_workaroundHardMediumSoftBigDecimalScore(SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, BendableScore> solutionManager_workaroundBendableScore(
            SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, BendableBigDecimalScore> solutionManager_workaroundBendableBigDecimalScore(
            SolverFactory<Solution_> solverFactory) {
        return solutionManager(solverFactory);
    }

}
