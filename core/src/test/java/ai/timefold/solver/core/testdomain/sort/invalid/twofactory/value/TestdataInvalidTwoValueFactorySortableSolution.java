package ai.timefold.solver.core.testdomain.sort.invalid.twofactory.value;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningSolution
public class TestdataInvalidTwoValueFactorySortableSolution {

    private List<TestdataSortableValue> valueList;
    private List<TestdataInvalidTwoValueFactorySortableEntity> entityList;
    private HardSoftScore score;

    @ValueRangeProvider(id = "valueRange")
    @PlanningEntityCollectionProperty
    public List<TestdataSortableValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataSortableValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataInvalidTwoValueFactorySortableEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInvalidTwoValueFactorySortableEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataInvalidTwoValueFactorySortableEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
