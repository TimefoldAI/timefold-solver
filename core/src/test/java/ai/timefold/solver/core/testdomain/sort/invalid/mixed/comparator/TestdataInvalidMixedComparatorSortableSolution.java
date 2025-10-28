package ai.timefold.solver.core.testdomain.sort.invalid.mixed.comparator;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.testdomain.common.TestdataSortableValue;

@PlanningSolution
public class TestdataInvalidMixedComparatorSortableSolution {

    private List<TestdataSortableValue> valueList;
    private List<TestdataInvalidMixedComparatorSortableEntity> entityList;
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
    public List<TestdataInvalidMixedComparatorSortableEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataInvalidMixedComparatorSortableEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public void removeEntity(TestdataInvalidMixedComparatorSortableEntity entity) {
        this.entityList = entityList.stream()
                .filter(e -> e != entity)
                .toList();
    }
}
