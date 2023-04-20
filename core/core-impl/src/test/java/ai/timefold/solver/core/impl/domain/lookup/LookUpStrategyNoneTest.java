package ai.timefold.solver.core.impl.domain.lookup;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.impl.testdata.domain.clone.lookup.TestdataObjectIntegerId;
import ai.timefold.solver.core.impl.testdata.domain.clone.lookup.TestdataObjectMultipleIds;
import ai.timefold.solver.core.impl.testdata.domain.clone.lookup.TestdataObjectNoId;
import ai.timefold.solver.core.impl.testdata.domain.clone.lookup.TestdataObjectPrimitiveIntId;

import org.junit.jupiter.api.Test;

class LookUpStrategyNoneTest extends AbstractLookupTest {

    public LookUpStrategyNoneTest() {
        super(LookUpStrategyType.NONE);
    }

    @Test
    void addRemoveWithIntegerId() {
        TestdataObjectIntegerId object = new TestdataObjectIntegerId(0);
        lookUpManager.addWorkingObject(object);
        lookUpManager.removeWorkingObject(object);
    }

    @Test
    void addRemoveWithPrimitiveIntId() {
        TestdataObjectPrimitiveIntId object = new TestdataObjectPrimitiveIntId(0);
        lookUpManager.addWorkingObject(object);
        lookUpManager.removeWorkingObject(object);
    }

    @Test
    void addWithNullId() {
        TestdataObjectIntegerId object = new TestdataObjectIntegerId(null);
        // not checked
        lookUpManager.addWorkingObject(object);
    }

    @Test
    void removeWithNullId() {
        TestdataObjectIntegerId object = new TestdataObjectIntegerId(null);
        // not checked
        lookUpManager.removeWorkingObject(object);
    }

    @Test
    void addWithoutId() {
        TestdataObjectNoId object = new TestdataObjectNoId();
        lookUpManager.addWorkingObject(object);
    }

    @Test
    void removeWithoutId() {
        TestdataObjectNoId object = new TestdataObjectNoId();
        lookUpManager.removeWorkingObject(object);
    }

    @Test
    void addSameIdTwice() {
        TestdataObjectIntegerId object = new TestdataObjectIntegerId(2);
        lookUpManager.addWorkingObject(object);
        // not checked
        lookUpManager.addWorkingObject(new TestdataObjectIntegerId(2));
    }

    @Test
    void removeWithoutAdding() {
        TestdataObjectIntegerId object = new TestdataObjectIntegerId(0);
        // not checked
        lookUpManager.removeWorkingObject(object);
    }

    @Test
    void lookUpWithId() {
        TestdataObjectIntegerId object = new TestdataObjectIntegerId(0);
        lookUpManager.addWorkingObject(object);
        // not allowed
        assertThatIllegalArgumentException()
                .isThrownBy(() -> lookUpManager.lookUpWorkingObject(object))
                .withMessageContaining("cannot be looked up");
    }

    @Test
    void lookUpWithoutId() {
        TestdataObjectNoId object = new TestdataObjectNoId();
        lookUpManager.addWorkingObject(object);
        // not allowed
        assertThatIllegalArgumentException()
                .isThrownBy(() -> lookUpManager.lookUpWorkingObject(object))
                .withMessageContaining("cannot be looked up");
    }

    @Test
    void lookUpWithoutAdding() {
        TestdataObjectIntegerId object = new TestdataObjectIntegerId(0);
        // not allowed
        assertThatIllegalArgumentException()
                .isThrownBy(() -> lookUpManager.lookUpWorkingObject(object))
                .withMessageContaining("cannot be looked up");
    }

    @Test
    void addWithTwoIds() {
        TestdataObjectMultipleIds object = new TestdataObjectMultipleIds();
        // not checked
        lookUpManager.addWorkingObject(object);
    }

    @Test
    void removeWithTwoIds() {
        TestdataObjectMultipleIds object = new TestdataObjectMultipleIds();
        // not checked
        lookUpManager.removeWorkingObject(object);
    }
}
