package ai.timefold.solver.core.testdomain.mixed.multientity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity(difficultyComparatorClass = TestdataMixedMultiEntityFirstEntityComparator.class)
public class TestdataMixedMultiEntityFirstEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataMixedMultiEntityFirstValue> valueList;

    private int difficulty;

    public TestdataMixedMultiEntityFirstEntity() {
        // Required for cloner
    }

    public TestdataMixedMultiEntityFirstEntity(String code, int difficulty) {
        super(code);
        valueList = new ArrayList<>();
        this.difficulty = difficulty;
    }

    public List<TestdataMixedMultiEntityFirstValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataMixedMultiEntityFirstValue> valueList) {
        this.valueList = valueList;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}
