package ai.timefold.solver.core.testdomain.shadow.sibling;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

/**
 * Overrides {@link #getPrevious()}, so its source metamodel carries this class as its entity.
 */
@PlanningEntity
public class TestdataDeclarativeSiblingFirstValue extends AbstractTestdataDeclarativeSiblingValue {
    String previousCode;

    public TestdataDeclarativeSiblingFirstValue() {
        super();
    }

    public TestdataDeclarativeSiblingFirstValue(String code) {
        super(code);
    }

    @Override
    public AbstractTestdataDeclarativeSiblingValue getPrevious() {
        return previous;
    }

    @ShadowVariable(supplierName = "previousCodeSupplier")
    public String getPreviousCode() {
        return previousCode;
    }

    public void setPreviousCode(String previousCode) {
        this.previousCode = previousCode;
    }

    @ShadowSources("previous")
    public String previousCodeSupplier() {
        return previous == null ? "" : previous.getCode();
    }
}
