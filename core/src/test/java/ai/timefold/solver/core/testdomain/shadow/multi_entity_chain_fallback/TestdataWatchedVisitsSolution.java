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
public class TestdataWatchedVisitsSolution {

    public static SolutionDescriptor<TestdataWatchedVisitsSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataWatchedVisitsSolution.class,
                TestdataWatchedVisitsVehicle.class, TestdataWatchedVisitsVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataWatchedVisitsSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataWatchedVisitsVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataWatchedVisitsVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataWatchedVisitsVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataWatchedVisitsVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataWatchedVisitsVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataWatchedVisitsVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
