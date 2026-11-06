package ai.timefold.solver.core.testdomain.mixed.multientity;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity(comparatorClass = TestdataMixedMultiEntityFirstEntityComparator.class)
public class TestdataMixedMultiEntityFirstEntity extends TestdataObject {

    @PlanningListVariable(valueRangeProviderRefs = "valueRange")
    private List<TestdataValue> valueList;

    private int difficulty;

    public TestdataMixedMultiEntityFirstEntity() {
        // Required for cloner
    }

    public TestdataMixedMultiEntityFirstEntity(String code, int difficulty) {
        super(code);
        valueList = new ArrayList<>();
        this.difficulty = difficulty;
    }

    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}
