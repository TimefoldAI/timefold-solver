package ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.metrics.InputMetricsAware;
import ai.timefold.solver.service.definition.api.metrics.OutputMetricsAware;
import ai.timefold.solver.service.quarkus.deployment.defaults.EmptyModelInputMetrics;
import ai.timefold.solver.service.quarkus.deployment.defaults.EmptyModelOutputMetrics;

@PlanningSolution
public class TestdataSolution
        implements ModelInput, ModelOutput, SolverModel<SimpleScore>, InputMetricsAware<EmptyModelInputMetrics>,
        OutputMetricsAware<EmptyModelOutputMetrics> {

    private List<String> valueList;
    private List<TestdataEntity> entityList;

    private SimpleScore score;
    private ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides;

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    @Override
    public EmptyModelInputMetrics getInputMetrics() {
        return null;
    }

    @Override
    public EmptyModelOutputMetrics getOutputMetrics() {
        return null;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    @Override
    public ConstraintWeightOverrides<SimpleScore> getConstraintWeightOverrides() {
        return this.constraintWeightOverrides;
    }

    public void setConstraintWeightOverrides(ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }
}
