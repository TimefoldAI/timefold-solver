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
public class TestdataMultiEntityChainElementSourcedSolution {

    public static SolutionDescriptor<TestdataMultiEntityChainElementSourcedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityChainElementSourcedSolution.class,
                TestdataMultiEntityChainElementSourcedVehicle.class, TestdataMultiEntityChainElementSourcedVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataMultiEntityChainElementSourcedSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    // The vehicles are their own value range, so previousVehicle can chain any two of them.
    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainElementSourcedVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainElementSourcedVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataMultiEntityChainElementSourcedVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataMultiEntityChainElementSourcedVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataMultiEntityChainElementSourcedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainElementSourcedVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
