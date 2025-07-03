package ai.timefold.solver.core.testdomain.declarative.multi_entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

@PlanningEntity
public class TestdataMultiEntityDependencyEntity {
    @PlanningListVariable
    List<TestdataMultiEntityDependencyValue> values;

    @PlanningVariable
    TestdataMultiEntityDependencyDelay delay;

    @ShadowVariable(supplierName = "readyTimeSupplier")
    LocalDateTime readyTime;

    LocalDateTime startTime;

    public TestdataMultiEntityDependencyEntity() {
        this(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC));
    }

    public TestdataMultiEntityDependencyEntity(LocalDateTime startTime) {
        this.startTime = startTime;
        this.values = new ArrayList<>();
    }

    public List<TestdataMultiEntityDependencyValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataMultiEntityDependencyValue> values) {
        this.values = values;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(LocalDateTime readyTime) {
        this.readyTime = readyTime;
    }

    @ShadowSources("delay")
    public LocalDateTime readyTimeSupplier() {
        if (delay == null) {
            return null;
        }
        return startTime.plusHours(delay.delay());
    }

    @Override
    public String toString() {
        return "TestdataPredecessorEntity{" +
                "values=" + values +
                ", startTime=" + startTime +
                '}';
    }
}
