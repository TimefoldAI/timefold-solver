package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataMultiEntityChainSolution {

    public static SolutionDescriptor<TestdataMultiEntityChainSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityChainSolution.class,
                TestdataMultiEntityChainVehicle.class, TestdataMultiEntityChainVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataMultiEntityChainSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataMultiEntityChainVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataMultiEntityChainVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataMultiEntityChainVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataMultiEntityChainVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
