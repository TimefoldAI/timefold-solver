package ai.timefold.solver.core.impl.domain.common.accessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.invalid.gettersettervisibility.TestdataDifferentGetterSetterVisibilityEntity;

import org.junit.jupiter.api.Test;

class ReflectionBeanPropertyMemberAccessorTest {

    @Test
    void methodAnnotatedEntity() throws NoSuchMethodException {
        ReflectionBeanPropertyMemberAccessor memberAccessor = new ReflectionBeanPropertyMemberAccessor(
                TestdataEntity.class.getMethod("getValue"));
        assertThat(memberAccessor.getName()).isEqualTo("value");
        assertThat(memberAccessor.getType()).isEqualTo(TestdataValue.class);
        assertThat(memberAccessor.getAnnotation(PlanningVariable.class)).isNotNull();

        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataEntity e1 = new TestdataEntity("e1", v1);
        assertThat(memberAccessor.executeGetter(e1)).isSameAs(v1);
        memberAccessor.executeSetter(e1, v2);
        assertThat(e1.getValue()).isSameAs(v2);
    }

    @Test
    void getterSetterVisibilityDoesNotMatch() {
        assertThatCode(() -> new ReflectionBeanPropertyMemberAccessor(
                TestdataDifferentGetterSetterVisibilityEntity.class.getDeclaredMethod("getValue1")))
                .hasMessageContainingAll("getter method (getValue1)",
                        "has access modifier (public)",
                        "not match the setter method's (setValue1)",
                        "access modifier (private)",
                        "on class (TestdataDifferentGetterSetterVisibilityEntity)");
        assertThatCode(() -> new ReflectionBeanPropertyMemberAccessor(
                TestdataDifferentGetterSetterVisibilityEntity.class.getDeclaredMethod("getValue2")))
                .hasMessageContainingAll("getter method (getValue2)",
                        "has access modifier (package-private)",
                        "not match the setter method's (setValue2)",
                        "access modifier (protected)",
                        "on class (TestdataDifferentGetterSetterVisibilityEntity)");
    }

}
