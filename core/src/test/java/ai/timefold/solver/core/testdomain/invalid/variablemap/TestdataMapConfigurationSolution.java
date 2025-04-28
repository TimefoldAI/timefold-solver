package ai.timefold.solver.core.testdomain.invalid.variablemap;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.FIELD)
public class TestdataMapConfigurationSolution {

    public static SolutionDescriptor<TestdataMapConfigurationSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMapConfigurationSolution.class);
    }

    private DummyMapConstraintConfiguration configuration;

    @PlanningEntityCollectionProperty
    private List<TestdataEntity> entityList;

    @PlanningScore
    private SimpleScore score;

    public DummyMapConstraintConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(DummyMapConstraintConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<TestdataEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
