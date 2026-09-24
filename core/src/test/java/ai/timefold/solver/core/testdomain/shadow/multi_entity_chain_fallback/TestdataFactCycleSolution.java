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
public class TestdataFactCycleSolution {

    public static SolutionDescriptor<TestdataFactCycleSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataFactCycleSolution.class,
                TestdataFactCycleVehicle.class, TestdataFactCycleVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataFactCycleSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataFactCycleVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataFactCycleVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataFactCycleVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataFactCycleVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataFactCycleVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFactCycleVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
