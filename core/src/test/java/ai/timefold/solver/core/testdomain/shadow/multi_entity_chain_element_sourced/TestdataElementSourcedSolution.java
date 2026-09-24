package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataElementSourcedSolution {

    public static SolutionDescriptor<TestdataElementSourcedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataElementSourcedSolution.class,
                TestdataElementSourcedVehicle.class, TestdataElementSourcedVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataElementSourcedSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    // The vehicles are their own value range, so previousVehicle can chain any two of them.
    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataElementSourcedVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataElementSourcedVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataElementSourcedVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataElementSourcedVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataElementSourcedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataElementSourcedVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
