package ai.timefold.solver.core.impl.testdata.domain.chained.shadow;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataShadowingChainedEntity extends TestdataObject implements TestdataShadowingChainedObject {

    public static EntityDescriptor<TestdataShadowingChainedSolution> buildEntityDescriptor() {
        return TestdataShadowingChainedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataShadowingChainedEntity.class);
    }

    private TestdataShadowingChainedObject chainedObject;

    // Shadow variables
    private TestdataShadowingChainedEntity nextEntity;
    private TestdataShadowingChainedAnchor anchor;

    public TestdataShadowingChainedEntity() {
    }

    public TestdataShadowingChainedEntity(String code) {
        super(code);
    }

    public TestdataShadowingChainedEntity(String code, TestdataShadowingChainedObject chainedObject) {
        this(code);
        this.chainedObject = chainedObject;
    }

    @PlanningVariable(valueRangeProviderRefs = { "chainedAnchorRange",
            "chainedEntityRange" }, graphType = PlanningVariableGraphType.CHAINED)
    public TestdataShadowingChainedObject getChainedObject() {
        return chainedObject;
    }

    public void setChainedObject(TestdataShadowingChainedObject chainedObject) {
        this.chainedObject = chainedObject;
    }

    @Override
    public TestdataShadowingChainedEntity getNextEntity() {
        return nextEntity;
    }

    @Override
    public void setNextEntity(TestdataShadowingChainedEntity nextEntity) {
        this.nextEntity = nextEntity;
    }

    @AnchorShadowVariable(sourceVariableName = "chainedObject")
    public TestdataShadowingChainedAnchor getAnchor() {
        return anchor;
    }

    public void setAnchor(TestdataShadowingChainedAnchor anchor) {
        this.anchor = anchor;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
