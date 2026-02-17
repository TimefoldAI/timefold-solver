package ai.timefold.solver.core.impl.domain.common.accessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.TestdataEntityProvidingWithParameterEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.TestdataEntityProvidingWithParameterSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.inheritance.TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.inheritance.TestdataEntityProvidingOnlyBaseAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.parameter.inheritance.TestdataEntityProvidingOnlyBaseAnnotatedSolution;

import org.junit.jupiter.api.Test;

class ReflectionMethodExtendedMemberAccessorTest {

    @Test
    void methodAnnotatedEntity() throws NoSuchMethodException {
        var memberAccessor = new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class));
        assertThat(memberAccessor.getName()).isEqualTo("getValueRange");
        assertThat(memberAccessor.getType()).isEqualTo(List.class);
        assertThat(memberAccessor.getAnnotation(ValueRangeProvider.class)).isNotNull();
        assertThat(memberAccessor.getGetterMethodParameterType()).isEqualTo(TestdataEntityProvidingWithParameterSolution.class);

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntityProvidingWithParameterEntity("e1", List.of(v1, v2), v1);
        var s1 = new TestdataEntityProvidingWithParameterSolution("s1");

        assertThat(memberAccessor.executeGetter(e1, s1)).isEqualTo(List.of(v1, v2));
        e1.setValueRange(List.of(v2));
        assertThat(memberAccessor.executeGetter(e1, s1)).isEqualTo(List.of(v2));
    }

    @Test
    void methodAnnotatedEntityAndInheritance() throws NoSuchMethodException {
        var member = new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingOnlyBaseAnnotatedChildEntity.class.getMethod("getValueList",
                        TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution.class));
        assertMemberWithInheritance(member, TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution.class,
                "getValueList");
        var otherMember = new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingOnlyBaseAnnotatedChildEntity.class.getMethod("getOtherValueList",
                        TestdataEntityProvidingOnlyBaseAnnotatedSolution.class));
        assertMemberWithInheritance(otherMember, TestdataEntityProvidingOnlyBaseAnnotatedSolution.class, "getOtherValueList");
    }

    void assertMemberWithInheritance(ReflectionMethodExtendedMemberAccessor member, Class<?> solutionClass, String memberName) {

        assertThat(member.getName()).isEqualTo(memberName);
        assertThat(member.getType()).isEqualTo(List.class);
        assertThat(member.getAnnotation(ValueRangeProvider.class)).isNotNull();
        assertThat(member.getGetterMethodParameterType()).isEqualTo(solutionClass);

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntityProvidingOnlyBaseAnnotatedChildEntity("e1", v1);
        var s1 = new TestdataEntityProvidingEntityProvidingOnlyBaseAnnotatedExtendedSolution("s1");
        s1.setValueList(List.of(v1, v2));

        assertThat(member.executeGetter(e1, s1)).isEqualTo(List.of(v1, v2));
        s1.setValueList(List.of(v2));
        assertThat(member.executeGetter(e1, s1)).isEqualTo(List.of(v2));
    }

    @Test
    void forbiddenEntityReadWithoutParameter() {
        assertThatCode(() -> new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class))
                .executeGetter(
                        new TestdataEntityProvidingWithParameterEntity()))
                .hasMessageContainingAll(
                        "Impossible state: the method executeGetter(Object) without parameter is not supported.");
        assertThatCode(() -> new ReflectionMethodExtendedMemberAccessor(
                TestdataEntityProvidingWithParameterEntity.class.getMethod("getValueRange",
                        TestdataEntityProvidingWithParameterSolution.class))
                .getGetterFunction())
                .hasMessageContainingAll(
                        "Impossible state: the method getGetterFunction() is not supported.");
    }
}
