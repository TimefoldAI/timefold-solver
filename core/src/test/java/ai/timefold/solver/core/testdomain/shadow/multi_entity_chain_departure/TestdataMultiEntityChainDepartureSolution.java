package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_departure;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataMultiEntityChainDepartureSolution {

    public static SolutionDescriptor<TestdataMultiEntityChainDepartureSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntityChainDepartureSolution.class,
                TestdataMultiEntityChainDepartureVehicle.class, TestdataMultiEntityChainDepartureVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataMultiEntityChainDepartureSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    List<Integer> departureTimes;

    @PlanningEntityCollectionProperty
    List<TestdataMultiEntityChainDepartureVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataMultiEntityChainDepartureVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<Integer> getDepartureTimes() {
        return departureTimes;
    }

    public void setDepartureTimes(List<Integer> departureTimes) {
        this.departureTimes = departureTimes;
    }

    public List<TestdataMultiEntityChainDepartureVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataMultiEntityChainDepartureVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataMultiEntityChainDepartureVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainDepartureVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
