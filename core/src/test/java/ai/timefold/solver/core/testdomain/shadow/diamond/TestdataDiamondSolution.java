package ai.timefold.solver.core.testdomain.shadow.diamond;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningSolution
public class TestdataDiamondSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataDiamondSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataDiamondSolution.class, TestdataDiamondEntity.class);
    }

    public static PlanningSolutionMetaModel<TestdataDiamondSolution> buildMetaModel() {
        return buildSolutionDescriptor().getMetaModel();
    }

    @PlanningEntityCollectionProperty
    List<TestdataDiamondEntity> entities;

    @ValueRangeProvider
    List<TestdataDiamondValue> values;

    @PlanningScore
    SimpleScore score;

    public TestdataDiamondSolution() {
    }

    public TestdataDiamondSolution(String code, List<TestdataDiamondEntity> entities, List<TestdataDiamondValue> values) {
        super(code);
        this.entities = entities;
        this.values = values;
    }

    public List<TestdataDiamondEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<TestdataDiamondEntity> entities) {
        this.entities = entities;
    }

    public List<TestdataDiamondValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataDiamondValue> values) {
        this.values = values;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
