package ai.timefold.solver.core.testdomain.shadow.fact_group_source;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;

@PlanningSolution
public class TestdataFactGroupSourceSolution {

    @PlanningEntityCollectionProperty
    List<TestdataFactGroupSourceVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataFactGroupSourceVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataFactGroupSourceVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataFactGroupSourceVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataFactGroupSourceVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFactGroupSourceVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
