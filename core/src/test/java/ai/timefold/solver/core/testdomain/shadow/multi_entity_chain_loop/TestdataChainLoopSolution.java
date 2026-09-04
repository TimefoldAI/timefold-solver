package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataChainLoopSolution {

    public static SolutionDescriptor<TestdataChainLoopSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataChainLoopSolution.class,
                TestdataChainLoopVehicle.class, TestdataChainLoopVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataChainLoopSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    // The vehicles are their own value range, so previousVehicle can chain any two of them.
    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataChainLoopVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataChainLoopVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataChainLoopVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataChainLoopVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataChainLoopVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataChainLoopVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
