package ai.timefold.solver.quarkus.bean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
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
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;

import io.quarkus.arc.DefaultBean;

/**
 * Throws an exception if an application tries to inject beans and the OptaPlanner Quarkus extension is skipped
 * due to missing domain classes.
 */
public class UnavailableOptaPlannerBeanProvider {

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolverFactory<Solution_> solverFactory() {
        throw createException(SolverFactory.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_, ProblemId_> SolverManager<Solution_, ProblemId_> solverManager() {
        throw createException(SolverManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, SimpleScore> scoreManager_workaroundSimpleScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, SimpleLongScore> scoreManager_workaroundSimpleLongScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, SimpleBigDecimalScore> scoreManager_workaroundSimpleBigDecimalScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardSoftScore> scoreManager_workaroundHardSoftScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardSoftLongScore> scoreManager_workaroundHardSoftLongScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardSoftBigDecimalScore> scoreManager_workaroundHardSoftBigDecimalScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardMediumSoftScore> scoreManager_workaroundHardMediumSoftScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardMediumSoftLongScore> scoreManager_workaroundHardMediumSoftLongScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, HardMediumSoftBigDecimalScore>
            scoreManager_workaroundHardMediumSoftBigDecimalScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, BendableScore> scoreManager_workaroundBendableScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, BendableLongScore> scoreManager_workaroundBendableLongScore() {
        throw createException(ScoreManager.class);
    }

    @Deprecated(forRemoval = true)
    @DefaultBean
    @Dependent
    @Produces
    <Solution_> ScoreManager<Solution_, BendableBigDecimalScore> scoreManager_workaroundBendableBigDecimalScore() {
        throw createException(ScoreManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, SimpleScore> solutionManager_workaroundSimpleScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, SimpleLongScore> solutionManager_workaroundSimpleLongScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, SimpleBigDecimalScore> solutionManager_workaroundSimpleBigDecimalScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardSoftScore> solutionManager_workaroundHardSoftScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardSoftLongScore> solutionManager_workaroundHardSoftLongScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardSoftBigDecimalScore> solutionManager_workaroundHardSoftBigDecimalScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardMediumSoftScore> solutionManager_workaroundHardMediumSoftScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardMediumSoftLongScore> solutionManager_workaroundHardMediumSoftLongScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, HardMediumSoftBigDecimalScore>
            solutionManager_workaroundHardMediumSoftBigDecimalScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, BendableScore> solutionManager_workaroundBendableScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, BendableLongScore> solutionManager_workaroundBendableLongScore() {
        throw createException(SolutionManager.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolutionManager<Solution_, BendableBigDecimalScore> solutionManager_workaroundBendableBigDecimalScore() {
        throw createException(SolutionManager.class);
    }

    private RuntimeException createException(Class<?> beanClass) {
        return new IllegalStateException("The " + beanClass.getName() + " is not available as there are no @"
                + PlanningSolution.class.getSimpleName() + " or @" + PlanningEntity.class.getSimpleName()
                + " annotated classes."
                + "\nIf your domain classes are located in a dependency of this project, maybe try generating"
                + " the Jandex index by using the jandex-maven-plugin in that dependency, or by adding"
                + "application.properties entries (quarkus.index-dependency.<name>.group-id"
                + " and quarkus.index-dependency.<name>.artifact-id).");
    }
}
