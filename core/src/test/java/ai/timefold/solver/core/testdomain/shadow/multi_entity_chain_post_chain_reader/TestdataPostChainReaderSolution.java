package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_post_chain_reader;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataPostChainReaderSolution {

    public static SolutionDescriptor<TestdataPostChainReaderSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataPostChainReaderSolution.class,
                TestdataPostChainReaderVehicle.class, TestdataPostChainReaderVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataPostChainReaderSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataPostChainReaderVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataPostChainReaderVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataPostChainReaderVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataPostChainReaderVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataPostChainReaderVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataPostChainReaderVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
