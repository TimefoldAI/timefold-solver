package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

@PlanningSolution
public class TestdataFactChainSolution {

    public static SolutionDescriptor<TestdataFactChainSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataFactChainSolution.class,
                TestdataFactChainVehicle.class, TestdataFactChainVisit.class);
    }

    public static PlanningSolutionMetaModel<TestdataFactChainSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataFactChainVehicle> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<TestdataFactChainVisit> visits;

    @PlanningScore
    SimpleScore score;

    public List<TestdataFactChainVehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<TestdataFactChainVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<TestdataFactChainVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFactChainVisit> visits) {
        this.visits = visits;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
