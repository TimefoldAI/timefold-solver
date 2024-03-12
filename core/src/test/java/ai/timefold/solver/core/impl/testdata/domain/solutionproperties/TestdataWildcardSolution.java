package ai.timefold.solver.core.impl.testdata.domain.solutionproperties;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

@PlanningSolution
public class TestdataWildcardSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataWildcardSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataWildcardSolution.class, TestdataEntity.class);
    }

    private List<? extends TestdataValue> extendsValueList;
    private List<? super TestdataValue> supersValueList;
    private List<? extends TestdataEntity> extendsEntityList;

    private SimpleScore score;

    public TestdataWildcardSolution() {
    }

    public TestdataWildcardSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<? extends TestdataValue> getExtendsValueList() {
        return extendsValueList;
    }

    public void setExtendsValueList(List<? extends TestdataValue> extendsValueList) {
        this.extendsValueList = extendsValueList;
    }

    @ProblemFactCollectionProperty
    public List<? super TestdataValue> getSupersValueList() {
        return supersValueList;
    }

    public void setSupersValueList(List<? super TestdataValue> supersValueList) {
        this.supersValueList = supersValueList;
    }

    @PlanningEntityCollectionProperty
    public List<? extends TestdataEntity> getExtendsEntityList() {
        return extendsEntityList;
    }

    public void setExtendsEntityList(List<? extends TestdataEntity> extendsEntityList) {
        this.extendsEntityList = extendsEntityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
