package ai.timefold.solver.core.testdomain.shadow.sibling;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public abstract class AbstractTestdataDeclarativeSiblingValue extends TestdataObject {
    AbstractTestdataDeclarativeSiblingValue previous;

    protected AbstractTestdataDeclarativeSiblingValue() {
        super();
    }

    protected AbstractTestdataDeclarativeSiblingValue(String code) {
        super(code);
    }

    @PreviousElementShadowVariable(sourceVariableName = "values")
    public AbstractTestdataDeclarativeSiblingValue getPrevious() {
        return previous;
    }

    public void setPrevious(AbstractTestdataDeclarativeSiblingValue previous) {
        this.previous = previous;
    }
}
