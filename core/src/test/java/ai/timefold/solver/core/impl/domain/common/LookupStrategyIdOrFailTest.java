package ai.timefold.solver.core.impl.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectEnum;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectIntegerId;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectMultipleIds;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectNoId;
import ai.timefold.solver.core.testdomain.clone.lookup.TestdataObjectPrimitiveIntId;

import org.junit.jupiter.api.Test;

class LookupStrategyIdOrFailTest extends AbstractLookupTest {

    public LookupStrategyIdOrFailTest() {
        super(LookUpStrategyType.PLANNING_ID_OR_FAIL_FAST);
    }

    @Test
    void addRemoveWithIntegerId() {
        var object = new TestdataObjectIntegerId(0);
        lookUpManager.addWorkingObject(object);
        lookUpManager.removeWorkingObject(object);
        // The removed object cannot be looked up
        assertThatThrownBy(() -> lookUpManager.lookUpWorkingObject(object))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("externalObject")
                .hasMessageContaining("no known workingObject");
    }

    @Test
    void addRemoveWithPrimitiveIntId() {
        var object = new TestdataObjectPrimitiveIntId(0);
        lookUpManager.addWorkingObject(object);
        lookUpManager.removeWorkingObject(object);
        // The removed object cannot be looked up
        assertThatThrownBy(() -> lookUpManager.lookUpWorkingObject(object))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("externalObject")
                .hasMessageContaining("no known workingObject");
    }

    @Test
    void addRemoveEnum() {
        var object = TestdataObjectEnum.THIRD_VALUE;
        lookUpManager.addWorkingObject(object);
        lookUpManager.removeWorkingObject(object);
    }

    @Test
    void addWithNullId() {
        var object = new TestdataObjectIntegerId(null);
        assertThatIllegalArgumentException().isThrownBy(() -> lookUpManager.addWorkingObject(object));
    }

    @Test
    void removeWithNullId() {
        var object = new TestdataObjectIntegerId(null);
        assertThatIllegalArgumentException().isThrownBy(() -> lookUpManager.removeWorkingObject(object));
    }

    @Test
    void addWithoutId() {
        var object = new TestdataObjectNoId();
        assertThatIllegalArgumentException().isThrownBy(() -> lookUpManager.addWorkingObject(object));
    }

    @Test
    void removeWithoutId() {
        var object = new TestdataObjectNoId();
        assertThatIllegalArgumentException().isThrownBy(() -> lookUpManager.removeWorkingObject(object));
    }

    @Test
    void addSameIdTwice() {
        var object = new TestdataObjectIntegerId(2);
        lookUpManager.addWorkingObject(object);
        assertThatIllegalStateException()
                .isThrownBy(() -> lookUpManager.addWorkingObject(new TestdataObjectIntegerId(2)))
                .withMessageContaining(" have the same planningId ")
                .withMessageContaining(object.toString());
    }

    @Test
    void removeWithoutAdding() {
        var object = new TestdataObjectIntegerId(0);
        assertThatIllegalStateException()
                .isThrownBy(() -> lookUpManager.removeWorkingObject(object))
                .withMessageContaining("differ");
    }

    @Test
    void lookUpWithId() {
        var object = new TestdataObjectIntegerId(1);
        lookUpManager.addWorkingObject(object);
        assertThat(lookUpManager.lookUpWorkingObject(new TestdataObjectIntegerId(1))).isSameAs(object);
    }

    @Test
    void lookUpWithoutId() {
        var object = new TestdataObjectNoId();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> lookUpManager.lookUpWorkingObject(object))
                .withMessageContaining("does not have a @" + PlanningId.class.getSimpleName());
    }

    @Test
    void lookUpWithoutAdding() {
        var object = new TestdataObjectIntegerId(0);
        assertThatThrownBy(() -> lookUpManager.lookUpWorkingObject(object))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("externalObject")
                .hasMessageContaining("no known workingObject");
    }

    @Test
    void addWithTwoIds() {
        var object = new TestdataObjectMultipleIds();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> lookUpManager.addWorkingObject(object))
                .withMessageContaining("3 members")
                .withMessageContaining(PlanningId.class.getSimpleName());
    }

    @Test
    void removeWithTwoIds() {
        var object = new TestdataObjectMultipleIds();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> lookUpManager.removeWorkingObject(object))
                .withMessageContaining("3 members")
                .withMessageContaining(PlanningId.class.getSimpleName());
    }
}
