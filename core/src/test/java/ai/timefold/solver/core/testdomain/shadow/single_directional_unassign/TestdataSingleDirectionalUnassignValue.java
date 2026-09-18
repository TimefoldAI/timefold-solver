package ai.timefold.solver.core.testdomain.shadow.single_directional_unassign;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A visit whose {@code arrivalTime} is sourced from {@code previous} alone, like
 * {@link ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListValue},
 * so the entity classifies as
 * {@link ai.timefold.solver.core.impl.domain.variable.declarative.GraphStructure#SINGLE_DIRECTIONAL_PARENT}.
 * The owning list variable allows unassigned values, so a visit can be ruined without being recreated.
 */
@PlanningEntity
public class TestdataSingleDirectionalUnassignValue extends TestdataObject {

    int duration;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataSingleDirectionalUnassignValue previous;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataSingleDirectionalUnassignEntity entity;

    @ShadowVariable(supplierName = "arrivalTimeSupplier")
    Integer arrivalTime;

    public TestdataSingleDirectionalUnassignValue() {
    }

    public TestdataSingleDirectionalUnassignValue(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    public int getDuration() {
        return duration;
    }

    public TestdataSingleDirectionalUnassignValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataSingleDirectionalUnassignValue previous) {
        this.previous = previous;
    }

    public TestdataSingleDirectionalUnassignEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleDirectionalUnassignEntity entity) {
        this.entity = entity;
    }

    public Integer getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Integer arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    @ShadowSources({ "entity", "previous.arrivalTime" })
    public Integer arrivalTimeSupplier() {
        if (entity == null) {
            // Unassigned: in no vehicle's route, so it has no arrival time either.
            return null;
        }
        if (previous == null) {
            return 0;
        }
        return previous.arrivalTime + previous.duration;
    }

}
