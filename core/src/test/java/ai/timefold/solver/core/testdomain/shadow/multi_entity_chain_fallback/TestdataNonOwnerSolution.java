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
public class TestdataNonOwnerSolution {

    public static SolutionDescriptor<TestdataNonOwnerSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataNonOwnerSolution.class,
                TestdataNonOwnerVehicle.class, TestdataNonOwnerVisit.class, TestdataNonOwnerDepot.class);
    }

    public static PlanningSolutionMetaModel<TestdataNonOwnerSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataNonOwnerVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataNonOwnerVisit> visits;

    @PlanningEntityCollectionProperty
    List<TestdataNonOwnerDepot> depots;

    @PlanningScore
    SimpleScore score;

    public List<TestdataNonOwnerVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataNonOwnerVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataNonOwnerVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataNonOwnerVisit> visits) {
        this.visits = visits;
    }

    public List<TestdataNonOwnerDepot> getDepots() {
        return depots;
    }

    public void setDepots(List<TestdataNonOwnerDepot> depots) {
        this.depots = depots;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
