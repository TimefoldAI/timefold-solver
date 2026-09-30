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
public class TestdataMultiEntityChainLoopSolution {

    public static SolutionDescriptor<TestdataMultiEntityChainLoopSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityChainLoopSolution.class,
                TestdataMultiEntityChainLoopVehicle.class, TestdataMultiEntityChainLoopVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataMultiEntityChainLoopSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    // The vehicles are their own value range, so previousVehicle can chain any two of them.
    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainLoopVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainLoopVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataMultiEntityChainLoopVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataMultiEntityChainLoopVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataMultiEntityChainLoopVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainLoopVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
