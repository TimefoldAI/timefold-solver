package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_watched;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataMultiEntityChainWatchedSolution {

    public static SolutionDescriptor<TestdataMultiEntityChainWatchedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityChainWatchedSolution.class,
                TestdataMultiEntityChainWatchedVehicle.class, TestdataMultiEntityChainWatchedVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataMultiEntityChainWatchedSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataMultiEntityChainWatchedVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainWatchedVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataMultiEntityChainWatchedVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataMultiEntityChainWatchedVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataMultiEntityChainWatchedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainWatchedVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
