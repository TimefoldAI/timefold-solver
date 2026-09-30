package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_extended;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataMultiEntityChainExtendedSolution {

    public static SolutionDescriptor<TestdataMultiEntityChainExtendedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityChainExtendedSolution.class,
                TestdataMultiEntityChainExtendedVehicle.class, TestdataMultiEntityChainExtendedVisit.class,
                TestdataMultiEntityChainExtendedPriorityVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataMultiEntityChainExtendedSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataMultiEntityChainExtendedVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainExtendedVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataMultiEntityChainExtendedVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataMultiEntityChainExtendedVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataMultiEntityChainExtendedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainExtendedVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
