package ai.timefold.solver.core.impl.domain.common.accessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.invalid.gettersetter.TestdataDifferentGetterSetterVisibilityEntity;
import ai.timefold.solver.core.testdomain.invalid.gettersetter.TestdataInvalidGetterEntity;

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
                .hasMessageContainingAll("getterMethod (getValue1)",
                        "has access modifier (public)",
                        "not match the setterMethod (setValue1)",
                        "access modifier (private)",
                        "on class (ai.timefold.solver.core.testdomain.invalid.gettersetter.TestdataDifferentGetterSetterVisibilityEntity)");
        assertThatCode(() -> new ReflectionBeanPropertyMemberAccessor(
                TestdataDifferentGetterSetterVisibilityEntity.class.getDeclaredMethod("getValue2")))
                .hasMessageContainingAll("getterMethod (getValue2)",
                        "has access modifier (package-private)",
                        "not match the setterMethod (setValue2)",
                        "access modifier (protected)",
                        "on class (ai.timefold.solver.core.testdomain.invalid.gettersetter.TestdataDifferentGetterSetterVisibilityEntity)");
    }

    @Test
    void setterMissing() {
        assertThatCode(() -> new ReflectionBeanPropertyMemberAccessor(
                TestdataInvalidGetterEntity.class.getDeclaredMethod("getValueWithoutSetter")))
                .hasMessageContainingAll("getterMethod (getValueWithoutSetter)",
                        "does not have a matching setterMethod",
                        "on class (ai.timefold.solver.core.testdomain.invalid.gettersetter.TestdataInvalidGetterEntity)");
    }

}
