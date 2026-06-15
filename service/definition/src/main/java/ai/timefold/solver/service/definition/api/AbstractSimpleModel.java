package ai.timefold.solver.service.definition.api;

import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.service.definition.api.metrics.InputMetricsAware;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.metrics.OutputMetricsAware;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents a simplified model with the default score type {@link HardMediumSoftScore} acting as the {@link ModelInput},
 * {@link SolverModel} and {@link ModelOutput}.
 * <p>
 * If any of a different score type, a separated model input from the model output and the solver model is required, implement
 * the {@link ModelInput}, {@link ModelOutput} and {@link SolverModel} interfaces directly.
 * <p>
 * If {@link ModelInputMetrics} or {@link ModelOutputMetrics}
 * collecting is required,
 * implement {@link InputMetricsAware} or
 * {@link OutputMetricsAware} interfaces.
 */
@PlanningSolution
public abstract class AbstractSimpleModel implements ModelInput, SolverModel<HardMediumSoftScore>, ModelOutput {

    @JsonIgnore
    @PlanningScore
    private HardMediumSoftScore score = null;

    @Override
    public HardMediumSoftScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }
}
