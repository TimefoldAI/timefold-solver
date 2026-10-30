package ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource;

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

@PlanningSolution
public class TestdataSolution implements SolverModel<SimpleScore>, ModelInput, ModelOutput {

    private List<TestdataVehicle> vehicles;

    private List<TestdataVisit> visits;

    private SimpleScore score;
    private ConstraintWeightOverrides<SimpleScore> constraintWeightOverrides;

    @PlanningEntityCollectionProperty
    public List<TestdataVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataVisit> getVisits() {
        return visits;
    }

    public void setEntityList(List<TestdataVisit> visits) {
        this.visits = visits;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
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
