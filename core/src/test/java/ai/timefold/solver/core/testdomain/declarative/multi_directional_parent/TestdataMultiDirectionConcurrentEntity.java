package ai.timefold.solver.core.testdomain.declarative.multi_directional_parent;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataMultiDirectionConcurrentEntity {
    String id;

    @PlanningListVariable
    List<TestdataMultiDirectionConcurrentValue> values;

    public TestdataMultiDirectionConcurrentEntity() {
        values = new ArrayList<>();
    }

    public TestdataMultiDirectionConcurrentEntity(String id) {
        this.id = id;
        values = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TestdataMultiDirectionConcurrentValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataMultiDirectionConcurrentValue> values) {
        this.values = values;
    }

    public void updateValueShadows() {
        TestdataMultiDirectionConcurrentValue previousVisit = null;
        for (var visit : values) {
            visit.setEntity(this);
            visit.setPreviousValue(previousVisit);
            if (previousVisit != null) {
                previousVisit.setNextValue(visit);
            }
            previousVisit = visit;
        }
        if (previousVisit != null) {
            previousVisit.setNextValue(null);
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
