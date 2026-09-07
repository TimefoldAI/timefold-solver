package ai.timefold.solver.core.testdomain.shadow.no_inconsistent_field;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataDependencyNoInconsistentFieldEntity {
    @PlanningId
    String id;

    @PlanningListVariable
    List<TestdataDependencyNoInconsistentFieldValue> values;

    @PlanningPinToIndex
    int pinnedToIndex;

    LocalDateTime startTime;

    public TestdataDependencyNoInconsistentFieldEntity() {
        this(null, LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC));
    }

    public TestdataDependencyNoInconsistentFieldEntity(String id) {
        this(id, LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC));
    }

    public TestdataDependencyNoInconsistentFieldEntity(String id, LocalDateTime startTime) {
        this.id = id;
        this.startTime = startTime;
        this.values = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TestdataDependencyNoInconsistentFieldValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataDependencyNoInconsistentFieldValue> values) {
        this.values = values;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getPinnedToIndex() {
        return pinnedToIndex;
    }

    public void setPinnedToIndex(int pinnedToIndex) {
        this.pinnedToIndex = pinnedToIndex;
    }

    @Override
    public String toString() {
        return "TestdataPredecessorEntity{id=%s, values=%s, startTime=%s}".formatted(id, values, startTime);
    }
}
