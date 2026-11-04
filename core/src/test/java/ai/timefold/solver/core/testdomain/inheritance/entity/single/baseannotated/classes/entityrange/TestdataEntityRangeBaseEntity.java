package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.entityrange;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataEntityRangeBaseEntity {

    @PlanningId
    private Long id;

    @ValueRangeProvider(id = "valueRange")
    private List<String> valueList;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    public TestdataEntityRangeBaseEntity() {
    }

    public TestdataEntityRangeBaseEntity(long id, List<String> valueList) {
        this.id = id;
        this.valueList = valueList;
    }

    public Long getId() {
        return id;
    }

    public List<String> getValueList() {
        return valueList;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
