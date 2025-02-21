package ai.timefold.solver.core.impl.testdata.domain.fsr;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

@PlanningSolution
public class TestdataFSRRoutePlan {
    public static SolutionDescriptor<TestdataFSRRoutePlan> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataFSRRoutePlan.class,
                TestdataFSRVehicle.class, TestdataFSRVisit.class);
    }

    @PlanningEntityCollectionProperty
    List<TestdataFSRVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataFSRVisit> visits;

    @PlanningScore
    HardSoftScore score;

    public List<TestdataFSRVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataFSRVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataFSRVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFSRVisit> visits) {
        this.visits = visits;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
