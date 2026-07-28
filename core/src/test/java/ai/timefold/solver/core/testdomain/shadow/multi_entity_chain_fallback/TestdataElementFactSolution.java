package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataElementFactSolution {

    public static SolutionDescriptor<TestdataElementFactSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataElementFactSolution.class,
                TestdataElementFactVehicle.class, TestdataElementFactVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataElementFactSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataElementFactVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataElementFactVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataElementFactVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataElementFactVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataElementFactVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataElementFactVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
