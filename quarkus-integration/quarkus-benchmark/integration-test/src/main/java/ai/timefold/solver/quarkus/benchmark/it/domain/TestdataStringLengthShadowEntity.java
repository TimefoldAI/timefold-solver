package ai.timefold.solver.quarkus.benchmark.it.domain;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataStringLengthShadowEntity {

    @PlanningId
    private Long id;

    @PlanningListVariable
    private List<TestdataListValueShadowEntity> values;

    public TestdataStringLengthShadowEntity() {
    }

    public TestdataStringLengthShadowEntity(Long id) {
        this.id = id;
        this.values = new ArrayList<>();
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public Long getId() {
        return id;
    }

    public List<TestdataListValueShadowEntity> getValues() {
        return values;
    }

    public void setValues(List<TestdataListValueShadowEntity> values) {
        this.values = values;
    }

}
