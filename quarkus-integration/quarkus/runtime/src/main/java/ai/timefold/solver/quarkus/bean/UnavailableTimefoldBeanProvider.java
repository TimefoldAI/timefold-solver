package ai.timefold.solver.quarkus.bean;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;

import io.quarkus.arc.DefaultBean;

/**
 * Throws an exception if an application tries to inject beans and the Timefold Quarkus extension is skipped
 * due to missing domain classes.
 */
public class UnavailableTimefoldBeanProvider {

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolverFactory<Solution_> solverFactory() {
        throw createException(SolverFactory.class);
    }

    @DefaultBean
    @Dependent
    @Produces
    <Solution_> SolverManager<Solution_> solverManager() {
        throw createException(SolverManager.class);
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
