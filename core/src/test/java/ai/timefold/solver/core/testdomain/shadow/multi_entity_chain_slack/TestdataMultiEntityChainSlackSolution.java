package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_slack;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataMultiEntityChainSlackSolution {

    public static SolutionDescriptor<TestdataMultiEntityChainSlackSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityChainSlackSolution.class,
                TestdataMultiEntityChainSlackVehicle.class, TestdataMultiEntityChainSlackVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataMultiEntityChainSlackSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataMultiEntityChainSlackVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainSlackVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataMultiEntityChainSlackVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataMultiEntityChainSlackVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataMultiEntityChainSlackVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainSlackVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
