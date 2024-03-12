package ai.timefold.solver.core.impl.testdata.domain.solutionproperties.autodiscover;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.GETTER)
public class TestdataAutoDiscoverGetterOverrideSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataAutoDiscoverGetterOverrideSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(
                TestdataAutoDiscoverGetterOverrideSolution.class, TestdataEntity.class);
    }

    private TestdataObject singleProblemFactField;
    private List<TestdataValue> problemFactListField;
    private List<String> listProblemFactField;

    private List<TestdataEntity> entityListField;
    private TestdataEntity otherEntityField;

    private SimpleScore score;

    public TestdataAutoDiscoverGetterOverrideSolution() {
    }

    public TestdataAutoDiscoverGetterOverrideSolution(String code) {
        super(code);
    }

    public TestdataAutoDiscoverGetterOverrideSolution(String code, TestdataObject singleProblemFact,
            List<TestdataValue> problemFactList, List<TestdataEntity> entityList,
            TestdataEntity otherEntity, List<String> listFact) {
        super(code);
        this.singleProblemFactField = singleProblemFact;
        this.problemFactListField = problemFactList;
        this.entityListField = entityList;
        this.otherEntityField = otherEntity;
        this.listProblemFactField = listFact;
    }

    public TestdataObject getSingleProblemFact() {
        return singleProblemFactField;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<TestdataValue> getProblemFactList() {
        return problemFactListField;
    }

    @ProblemFactProperty // would have been autodiscovered as @ProblemFactCollectionProperty
    public List<String> getListProblemFact() {
        return listProblemFactField;
    }

    public List<TestdataEntity> getEntityList() {
        return entityListField;
    }

    public TestdataEntity getOtherEntity() {
        return otherEntityField;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
