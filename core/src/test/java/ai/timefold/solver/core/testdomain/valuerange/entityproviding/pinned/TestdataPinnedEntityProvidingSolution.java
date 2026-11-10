package ai.timefold.solver.core.testdomain.valuerange.entityproviding.pinned;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataPinnedEntityProvidingSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataPinnedEntityProvidingSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataPinnedEntityProvidingSolution.class,
                TestdataPinnedEntityProvidingEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataPinnedEntityProvidingSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    private List<TestdataPinnedEntityProvidingEntity> entityList;
    private SimpleScore score;

    public TestdataPinnedEntityProvidingSolution() {
        // Required for cloning
    }

    public TestdataPinnedEntityProvidingSolution(String code, List<TestdataPinnedEntityProvidingEntity> entityList) {
        super(code);
        this.entityList = entityList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPinnedEntityProvidingEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPinnedEntityProvidingEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    @ProblemFactCollectionProperty
    public Collection<TestdataValue> getProblemFacts() {
        var valueSet = new LinkedHashSet<TestdataValue>();
        for (var entity : entityList) {
            valueSet.addAll(entity.getValueRange());
        }
        return valueSet;
    }

}
