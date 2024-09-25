package ai.timefold.solver.quarkus.bean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;

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

    private SolverManager<?, ?> solverManager;

    private SolutionManager<?, ?> solutionManager;

    private ScoreManager<?, ?> scoreManager;

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
            var scoreDirectorFactory = ((DefaultSolverFactory<?>) solverFactory).getScoreDirectorFactory();
            if (scoreDirectorFactory instanceof AbstractConstraintStreamScoreDirectorFactory<?, ?> castScoreDirectorFactory) {
                constraintMetaModel = castScoreDirectorFactory.getConstraintMetaModel();
            } else {
                throw new IllegalStateException(
                        "Cannot provide %s because the score director does not use the Constraint Streams API."
                                .formatted(ConstraintMetaModel.class.getSimpleName()));
            }
        }
        return constraintMetaModel;
    }

    @SuppressWarnings("unchecked")
    @DefaultBean
    @Dependent
    @Produces
    <Solution_, ProblemId_> SolverManager<Solution_, ProblemId_> solverManager(SolverFactory<Solution_> solverFactory,
            SolverManagerConfig solverManagerConfig) {
        if (solverManager == null) {
            solverManager = SolverManager.create(solverFactory, solverManagerConfig);
        }
        return (SolverManager<Solution_, ProblemId_>) solverManager;
    }

    // Quarkus-ARC-Weld can't deal with enum pattern generics such as Score<S extends Score<S>>.
    // See https://github.com/quarkusio/quarkus/pull/12137
    //    @DefaultBean
    //    @Dependent
    //    @Produces
    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true)
    <Solution_, Score_ extends Score<Score_>> ScoreManager<Solution_, Score_>
            scoreManager(SolverFactory<Solution_> solverFactory) {
        if (scoreManager == null) {
            scoreManager = ScoreManager.create(solverFactory);
        }
        return (ScoreManager<Solution_, Score_>) scoreManager;
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, SimpleScore> scoreManager_workaroundSimpleScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, SimpleLongScore> scoreManager_workaroundSimpleLongScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, SimpleBigDecimalScore> scoreManager_workaroundSimpleBigDecimalScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardSoftScore> scoreManager_workaroundHardSoftScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardSoftLongScore> scoreManager_workaroundHardSoftLongScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardSoftBigDecimalScore> scoreManager_workaroundHardSoftBigDecimalScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardMediumSoftScore> scoreManager_workaroundHardMediumSoftScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardMediumSoftLongScore> scoreManager_workaroundHardMediumSoftLongScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardMediumSoftBigDecimalScore> scoreManager_workaroundHardMediumSoftBigDecimalScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, BendableScore> scoreManager_workaroundBendableScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, BendableLongScore> scoreManager_workaroundBendableLongScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, BendableBigDecimalScore> scoreManager_workaroundBendableBigDecimalScore(
            SolverFactory<Solution_> solverFactory) {
        return scoreManager(solverFactory);
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
    <Solution_> SolutionManager<Solution_, SimpleLongScore> solutionManager_workaroundSimpleLongScore(
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
    <Solution_> SolutionManager<Solution_, HardSoftLongScore> solutionManager_workaroundHardSoftLongScore(
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
    <Solution_> SolutionManager<Solution_, HardMediumSoftLongScore> solutionManager_workaroundHardMediumSoftLongScore(
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
    <Solution_> SolutionManager<Solution_, BendableLongScore> solutionManager_workaroundBendableLongScore(
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
