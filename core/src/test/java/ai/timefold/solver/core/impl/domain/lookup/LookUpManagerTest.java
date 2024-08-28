package ai.timefold.solver.core.impl.domain.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.impl.testdata.domain.clone.lookup.TestdataObjectIntegerId;
import ai.timefold.solver.core.impl.testdata.domain.interface_domain.TestdataInterfaceEntity;
import ai.timefold.solver.core.impl.testdata.domain.interface_domain.TestdataInterfaceValue;

import org.junit.jupiter.api.Test;

class LookUpManagerTest extends AbstractLookupTest {

    public LookUpManagerTest() {
        super(LookUpStrategyType.PLANNING_ID_OR_NONE);
    }

    @Test
    void lookUpNull() {
        assertThat(lookUpManager.<Object> lookUpWorkingObject(null)).isNull();
    }

    @Test
    void resetWorkingObjects() {
        TestdataObjectIntegerId o = new TestdataObjectIntegerId(0);
        TestdataObjectIntegerId p = new TestdataObjectIntegerId(1);
        // The objects should be added during the reset
        lookUpManager.reset();
        for (Object fact : Arrays.asList(o, p)) {
            lookUpManager.addWorkingObject(fact);
        }
        // So it's possible to look up and remove them
        assertThat(lookUpManager.lookUpWorkingObject(new TestdataObjectIntegerId(0))).isSameAs(o);
        assertThat(lookUpManager.lookUpWorkingObject(new TestdataObjectIntegerId(1))).isSameAs(p);
        lookUpManager.removeWorkingObject(o);
        lookUpManager.removeWorkingObject(p);
    }

    public static class InterfaceEntity implements TestdataInterfaceEntity {
        final String id;

        public InterfaceEntity(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public TestdataInterfaceValue getValue() {
            return null;
        }

        @Override
        public void setValue(TestdataInterfaceValue value) {

        }
    }

    @Test
    void lookupInterfaceEntity() {
        var o = new InterfaceEntity("0");
        var p = new InterfaceEntity("1");
        // The objects should be added during the reset
        lookUpManager.reset();
        for (Object fact : Arrays.asList(o, p)) {
            lookUpManager.addWorkingObject(fact);
        }
        // So it's possible to look up and remove them
        assertThat(lookUpManager.lookUpWorkingObject(new InterfaceEntity("0"))).isSameAs(o);
        assertThat(lookUpManager.lookUpWorkingObject(new InterfaceEntity("1"))).isSameAs(p);
        lookUpManager.removeWorkingObject(o);
        lookUpManager.removeWorkingObject(p);
    }

}
