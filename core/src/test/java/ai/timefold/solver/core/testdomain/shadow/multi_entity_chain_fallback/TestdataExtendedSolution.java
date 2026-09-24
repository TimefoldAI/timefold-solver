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
public class TestdataExtendedSolution {

    public static SolutionDescriptor<TestdataExtendedSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataExtendedSolution.class,
                TestdataExtendedVehicle.class, TestdataExtendedVisit.class, TestdataExtendedPriorityVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataExtendedSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataExtendedVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataExtendedVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataExtendedVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataExtendedVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataExtendedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataExtendedVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
