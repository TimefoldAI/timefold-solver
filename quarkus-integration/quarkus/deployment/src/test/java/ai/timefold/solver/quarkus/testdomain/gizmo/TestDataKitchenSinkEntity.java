package ai.timefold.solver.quarkus.testdomain.gizmo;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;

/*
 *  Should have one of every annotation, even annotations that
 *  don't make sense on an entity, to make sure everything works
 *  a-ok.
 */
@PlanningEntity
public class TestDataKitchenSinkEntity {

    private String groupId;
    private Integer intVariable;

    @ShadowVariable(supplierName = "copyStringVariable")
    private String declarativeShadowVariable;

    @ShadowVariablesInconsistent
    private boolean inconsistent;

    @PlanningVariable(valueRangeProviderRefs = { "names" })
    private String stringVariable;

    private boolean pinned;

    @PlanningVariable(valueRangeProviderRefs = { "ints" })
    public Integer getIntVariable() {
        return intVariable;
    }

    public void setIntVariable(Integer val) {
        intVariable = val;
    }

    public Integer testGetIntVariable() {
        return intVariable;
    }

    public String testGetStringVariable() {
        return stringVariable;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDeclarativeShadowVariable() {
        return declarativeShadowVariable;
    }

    public void setDeclarativeShadowVariable(String declarativeShadowVariable) {
        this.declarativeShadowVariable = declarativeShadowVariable;
    }

    public boolean isInconsistent() {
        return inconsistent;
    }

    public void setInconsistent(boolean inconsistent) {
        this.inconsistent = inconsistent;
    }

    public String getStringVariable() {
        return stringVariable;
    }

    public void setStringVariable(String stringVariable) {
        this.stringVariable = stringVariable;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    @ShadowSources(value = "stringVariable", alignmentKey = "groupId")
    public String copyStringVariable() {
        return stringVariable;
    }

    @PlanningPin
    public boolean isPinned() {
        return pinned;
    }

    @ValueRangeProvider(id = "ints")
    public List<Integer> myIntValueRange() {
        return Collections.singletonList(1);
    }

    @ValueRangeProvider(id = "names")
    public List<String> myStringValueRange() {
        return Collections.singletonList("A");
    }

}
