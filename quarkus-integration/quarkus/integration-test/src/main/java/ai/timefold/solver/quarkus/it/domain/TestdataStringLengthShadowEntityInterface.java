package ai.timefold.solver.quarkus.it.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataStringLengthShadowEntityInterface {

    @PlanningVariable(valueRangeProviderRefs = { "valueRange", "valueRangeWithParameter" })
    String getValue();

    void setValue(String value);

    List<String> getValueList();

    @ValueRangeProvider(id = "valueRangeWithParameter")
    default List<String> getValueRangeWithParameter(TestdataStringLengthShadowSolution solution) {
        return solution.getValueList();
    }

    @ValueRangeProvider(id = "valueRange")
    default List<String> getValueRange() {
        return getValueList();
    }
}
