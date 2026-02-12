package ai.timefold.solver.core.testdomain.solutionproperties.autodiscover;

import java.util.List;

import ai.timefold.solver.core.api.domain.autodiscover.AutoDiscoverMemberType;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution(autoDiscoverMemberType = AutoDiscoverMemberType.FIELD)
public class TestdataAutoDiscoverFieldOverrideSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataAutoDiscoverFieldOverrideSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataAutoDiscoverFieldOverrideSolution.class,
                TestdataEntity.class);
    }

    private TestdataObject singleProblemFact;
    @ValueRangeProvider(id = "valueRange")
    private List<TestdataValue> problemFactList;
    @ProblemFactProperty // would have been autodiscovered as @ProblemFactCollectionProperty
    private List<String> listProblemFact;

    private List<TestdataEntity> entityList;
    private TestdataEntity otherEntity;

    private SimpleScore score;

    public TestdataAutoDiscoverFieldOverrideSolution() {
    }

    public TestdataAutoDiscoverFieldOverrideSolution(String code) {
        super(code);
    }

    public TestdataAutoDiscoverFieldOverrideSolution(String code, TestdataObject singleProblemFact,
            List<TestdataValue> problemFactList, List<TestdataEntity> entityList,
            TestdataEntity otherEntity, List<String> listFact) {
        super(code);
        this.singleProblemFact = singleProblemFact;
        this.problemFactList = problemFactList;
        this.entityList = entityList;
        this.otherEntity = otherEntity;
        this.listProblemFact = listFact;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    public TestdataObject getSingleProblemFact() {
        return singleProblemFact;
    }

    public void setSingleProblemFact(TestdataObject singleProblemFact) {
        this.singleProblemFact = singleProblemFact;
    }

    public List<TestdataValue> getProblemFactList() {
        return problemFactList;
    }

    public void setProblemFactList(List<TestdataValue> problemFactList) {
        this.problemFactList = problemFactList;
    }

    public List<String> getListProblemFact() {
        return listProblemFact;
    }

    public void setListProblemFact(List<String> listProblemFact) {
        this.listProblemFact = listProblemFact;
    }

    public List<TestdataEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataEntity> entityList) {
        this.entityList = entityList;
    }

    public TestdataEntity getOtherEntity() {
        return otherEntity;
    }

    public void setOtherEntity(TestdataEntity otherEntity) {
        this.otherEntity = otherEntity;
    }
}
