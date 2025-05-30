package ai.timefold.solver.core.testdomain.multientity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataMultiEntitySolution extends TestdataObject {

    public static SolutionDescriptor<TestdataMultiEntitySolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataMultiEntitySolution.class,
                TestdataLeadEntity.class, TestdataHerdEntity.class);
    }

    public static TestdataMultiEntitySolution generateUninitializedSolution(int entityListSize, int valueListSize) {
        var solution = new TestdataMultiEntitySolution();
        var valueList = new ArrayList<TestdataValue>(valueListSize);
        for (int i = 0; i < valueListSize; i++) {
            valueList.add(new TestdataValue("Generated Value " + i));
        }
        solution.setValueList(valueList);
        var entityLeadList = new ArrayList<TestdataLeadEntity>(entityListSize);
        var entityHerdList = new ArrayList<TestdataHerdEntity>(entityListSize);
        for (int i = 0; i < entityListSize; i++) {
            entityLeadList.add(new TestdataLeadEntity("LeadEntity " + i));
            entityHerdList.add(new TestdataHerdEntity("HerdEntity " + i));
        }
        solution.setLeadEntityList(entityLeadList);
        solution.setHerdEntityList(entityHerdList);
        return solution;
    }

    private List<TestdataValue> valueList;
    private List<TestdataLeadEntity> leadEntityList;
    private List<TestdataHerdEntity> herdEntityList;

    private SimpleScore score;

    public TestdataMultiEntitySolution() {
    }

    public TestdataMultiEntitySolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "leadEntityRange")
    public List<TestdataLeadEntity> getLeadEntityList() {
        return leadEntityList;
    }

    public void setLeadEntityList(List<TestdataLeadEntity> leadEntityList) {
        this.leadEntityList = leadEntityList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataHerdEntity> getHerdEntityList() {
        return herdEntityList;
    }

    public void setHerdEntityList(List<TestdataHerdEntity> herdEntityList) {
        this.herdEntityList = herdEntityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
