package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableLooped;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedBaseValue;
import ai.timefold.solver.core.testdomain.declarative.extended.TestdataDeclarativeExtendedSubclassValue;
import ai.timefold.solver.core.testdomain.declarative.invalid.TestdataInvalidDeclarativeEntity;
import ai.timefold.solver.core.testdomain.declarative.invalid.TestdataInvalidDeclarativeSolution;
import ai.timefold.solver.core.testdomain.declarative.invalid.TestdataInvalidDeclarativeValue;

import org.junit.jupiter.api.Test;

class RootVariableSourceTest {

    private static final MemberAccessorFactory DEFAULT_MEMBER_ACCESSOR_FACTORY = new MemberAccessorFactory();
    private static final DescriptorPolicy DEFAULT_DESCRIPTOR_POLICY = new DescriptorPolicy();

    private final PlanningSolutionMetaModel<TestdataInvalidDeclarativeSolution> planningSolutionMetaModel =
            SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                    TestdataInvalidDeclarativeSolution.class, TestdataInvalidDeclarativeEntity.class,
                    TestdataInvalidDeclarativeValue.class)
                    .getMetaModel();
    private final PlanningEntityMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue> shadowEntityMetaModel =
            planningSolutionMetaModel.entity(TestdataInvalidDeclarativeValue.class);
    private final ShadowVariableMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue, TestdataInvalidDeclarativeValue> previousElementMetaModel =
            shadowEntityMetaModel.shadowVariable("previous");
    private final ShadowVariableMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue, TestdataInvalidDeclarativeValue> shadowVariableMetaModel =
            shadowEntityMetaModel.shadowVariable("shadow");
    private final ShadowVariableMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue, TestdataInvalidDeclarativeValue> dependencyMetaModel =
            shadowEntityMetaModel.shadowVariable("dependency");

    private void assertChainToVariableEntity(VariableSourceReference variableSourceReference, String... expectedNames) {
        var chain = variableSourceReference.chainToVariableEntity();
        assertThat(chain).hasSize(expectedNames.length);

        for (var i = 0; i < chain.size(); i++) {
            assertThat(chain.get(i).getName()).isEqualTo(expectedNames[i]);
        }
    }

    private void assertEmptyChainToVariableEntity(VariableSourceReference variableSourceReference) {
        var chain = variableSourceReference.chainToVariableEntity();
        assertThat(chain).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void pathUsingBuiltinShadow() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "previous",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.onRootEntity()).isTrue();
        assertThat(source.isDeclarative()).isFalse();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isNull();

        var entity = new TestdataInvalidDeclarativeValue("v1");
        var result = source.targetEntityFunctionStartingFromVariableEntity().apply(entity);

        assertThat(result).isSameAs(entity);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(entity, rootVisitor);
        verify(rootVisitor).accept(entity);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pathUsingDeclarativeShadow() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "dependency",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.onRootEntity()).isTrue();
        assertThat(source.isDeclarative()).isTrue();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var entity = new TestdataInvalidDeclarativeValue("v1");
        var result = source.targetEntityFunctionStartingFromVariableEntity().apply(entity);
        assertThat(result).isSameAs(entity);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(entity, rootVisitor);
        verify(rootVisitor).accept(entity);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pathUsingDeclarativeShadowAfterGroup() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "group[].dependency",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.onRootEntity()).isFalse();
        assertThat(source.isDeclarative()).isTrue();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var group = new TestdataInvalidDeclarativeValue("group");
        var v1 = new TestdataInvalidDeclarativeValue("v1");
        var v2 = new TestdataInvalidDeclarativeValue("v2");

        group.setGroup(List.of(v1, v2));
        var result = source.targetEntityFunctionStartingFromVariableEntity().apply(group);
        assertThat(result).isSameAs(group);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(group, rootVisitor);
        verify(rootVisitor).accept(v1);
        verify(rootVisitor).accept(v2);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pathUsingBuiltinShadowAfterGroup() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "group[].previous",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.onRootEntity()).isFalse();
        assertThat(source.isDeclarative()).isFalse();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isNull();

        var group = new TestdataInvalidDeclarativeValue("group");
        var v1 = new TestdataInvalidDeclarativeValue("v1");
        var v2 = new TestdataInvalidDeclarativeValue("v2");

        group.setGroup(List.of(v1, v2));
        var result = source.targetEntityFunctionStartingFromVariableEntity().apply(group);
        assertThat(result).isSameAs(group);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(group, rootVisitor);
        verify(rootVisitor).accept(v1);
        verify(rootVisitor).accept(v2);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pathUsingDeclarativeShadowAfterGroupAfterFact() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "fact.group[].dependency",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.onRootEntity()).isFalse();
        assertThat(source.isDeclarative()).isTrue();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var root = new TestdataInvalidDeclarativeValue("fact");
        var fact = new TestdataInvalidDeclarativeValue("fact");
        var v1 = new TestdataInvalidDeclarativeValue("v1");
        var v2 = new TestdataInvalidDeclarativeValue("v2");

        root.setFact(fact);
        fact.setGroup(List.of(v1, v2));
        var result = source.targetEntityFunctionStartingFromVariableEntity().apply(root);
        assertThat(result).isSameAs(root);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(root, rootVisitor);
        verify(rootVisitor).accept(v1);
        verify(rootVisitor).accept(v2);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pathUsingDeclarativeShadowAfterBuiltinShadow() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "previous.dependency",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(2);
        var previousSource = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(previousSource);
        assertThat(previousSource.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(previousSource.isTopLevel()).isTrue();
        assertThat(previousSource.onRootEntity()).isTrue();
        assertThat(previousSource.isDeclarative()).isFalse();
        assertThat(previousSource.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(previousSource.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var dependencySource = rootVariableSource.variableSourceReferences().get(1);

        assertChainToVariableEntity(dependencySource, "previous");
        assertThat(dependencySource.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(dependencySource.isTopLevel()).isFalse();
        assertThat(dependencySource.isDeclarative()).isTrue();
        assertThat(dependencySource.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(dependencySource.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var previousElement = new TestdataInvalidDeclarativeValue("previous");
        var currentElement = new TestdataInvalidDeclarativeValue("current");
        currentElement.setPrevious(previousElement);

        var result = previousSource.targetEntityFunctionStartingFromVariableEntity().apply(currentElement);
        assertThat(result).isSameAs(previousElement);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(currentElement, rootVisitor);
        verify(rootVisitor).accept(previousElement);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    void pathUsingBuiltinShadowAfterFact() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "fact.previous",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var previousSource = rootVariableSource.variableSourceReferences().get(0);

        assertChainToVariableEntity(previousSource, "fact");
        assertThat(previousSource.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(previousSource.onRootEntity()).isFalse();
        assertThat(previousSource.isTopLevel()).isTrue();
        assertThat(previousSource.isDeclarative()).isFalse();
        assertThat(previousSource.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(previousSource.downstreamDeclarativeVariableMetamodel()).isNull();

        var previousElement = new TestdataInvalidDeclarativeValue("previous");
        var factElement = new TestdataInvalidDeclarativeValue("fact");
        var currentElement = new TestdataInvalidDeclarativeValue("current");

        factElement.setPrevious(previousElement);
        currentElement.setFact(factElement);

        var result = previousSource.targetEntityFunctionStartingFromVariableEntity().apply(factElement);
        assertThat(result).isSameAs(factElement);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(currentElement, rootVisitor);
        verify(rootVisitor).accept(factElement);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pathUsingDeclarativeShadowAfterBuiltinShadowAfterGroup() {
        var rootVariableSource = RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "group[].previous.dependency",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(2);
        var previousSource = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(previousSource);
        assertThat(previousSource.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(previousSource.isTopLevel()).isTrue();
        assertThat(previousSource.onRootEntity()).isFalse();
        assertThat(previousSource.isDeclarative()).isFalse();
        assertThat(previousSource.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(previousSource.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var dependencySource = rootVariableSource.variableSourceReferences().get(1);

        assertChainToVariableEntity(dependencySource, "previous");
        assertThat(dependencySource.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(dependencySource.isTopLevel()).isFalse();
        assertThat(dependencySource.onRootEntity()).isFalse();
        assertThat(dependencySource.isDeclarative()).isTrue();
        assertThat(dependencySource.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(dependencySource.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var previousElement = new TestdataInvalidDeclarativeValue("previous");
        var currentElement = new TestdataInvalidDeclarativeValue("current");
        var group = new TestdataInvalidDeclarativeValue("group");

        currentElement.setPrevious(previousElement);
        group.setGroup(List.of(currentElement));

        var result = previousSource.targetEntityFunctionStartingFromVariableEntity().apply(currentElement);
        assertThat(result).isSameAs(previousElement);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(group, rootVisitor);
        verify(rootVisitor).accept(previousElement);
        verifyNoMoreInteractions(rootVisitor);
    }

    @Test
    void invalidPathMissingProperty() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "missing",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (missing)" +
                        " starting from root class (TestdataInvalidDeclarativeValue)" +
                        " references a member (missing)" +
                        " on class (TestdataInvalidDeclarativeValue)" +
                        " that does not exist.");
    }

    @Test
    void invalidPathUsingBuiltinShadowAfterBuiltinShadow() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "previous.previous",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (previous.previous)" +
                        " starting from root entity class (TestdataInvalidDeclarativeValue)" +
                        " accesses a non-declarative shadow variable (previous)" +
                        " after another non-declarative shadow variable (previous).");
    }

    @Test
    void invalidPathUsingGroupAfterGroup() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "group[].group[].previous",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (group[].group[].previous)" +
                        " starting from root class (TestdataInvalidDeclarativeValue)" +
                        " accesses a collection (group[])" +
                        " after another collection (group), which is not allowed.");
    }

    @Test
    void invalidPathUsingDeclarativeAfterDeclarative() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "dependency.dependency",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (dependency.dependency)" +
                        " starting from root entity class (TestdataInvalidDeclarativeValue)" +
                        " accesses a declarative shadow variable (dependency)" +
                        " from another declarative shadow variable (dependency).");
    }

    @Test
    void invalidPathNoVariables() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "fact",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (fact)" +
                        " starting from root entity class (TestdataInvalidDeclarativeValue)" +
                        " does not reference any variables.");
    }

    @Test
    void invalidPathEndOnFact() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "previous.fact",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (previous.fact)" +
                        " starting from root entity class (TestdataInvalidDeclarativeValue)" +
                        " does not end on a variable.");
    }

    @Test
    void invalidPathMultipleFactsInARow() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "fact.fact.dependency",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (fact.fact.dependency)" +
                        " starting from root entity (TestdataInvalidDeclarativeValue)" +
                        " referencing multiple facts (fact, fact)" +
                        " in a row.");
    }

    @Test
    void preferGetterWhenFieldTheSameType() {
        record TestClass(String name) {
            public String getName() {
                return name;
            }
        }

        var member = RootVariableSource.getMember(TestClass.class, "name", TestClass.class, "name");
        assertThat(member).isInstanceOf(Method.class);
    }

    @Test
    void preferGetterWhenGetterIsMoreSpecificThanField() {
        record TestClass(TestdataDeclarativeExtendedBaseValue value) {
            public TestdataDeclarativeExtendedSubclassValue getValue() {
                return (TestdataDeclarativeExtendedSubclassValue) value;
            }
        }

        var member = RootVariableSource.getMember(TestClass.class, "value", TestClass.class, "value");
        assertThat(member).isInstanceOf(Method.class);
    }

    @Test
    void preferGetterWhenGetterIsNotCovariantWithField() {
        record TestClass(String value) {
            public Integer getValue() {
                return 1;
            }
        }

        var member = RootVariableSource.getMember(TestClass.class, "value", TestClass.class, "value");
        assertThat(member).isInstanceOf(Method.class);
    }

    @Test
    void preferFieldWhenFieldMoreSpecificThanGetter() {
        record TestClass(TestdataDeclarativeExtendedSubclassValue value) {
            public TestdataDeclarativeExtendedBaseValue getValue() {
                return value;
            }
        }

        var member = RootVariableSource.getMember(TestClass.class, "value", TestClass.class, "value");
        assertThat(member).isInstanceOf(Field.class);
    }

    @Test
    void useFieldIfNoGetter() {
        record TestClass(String value) {
        }

        var member = RootVariableSource.getMember(TestClass.class, "value", TestClass.class, "value");
        assertThat(member).isInstanceOf(Field.class);
    }

    @Test
    void useGetterIfNoField() {
        record TestClass() {
            String getValue() {
                return "value";
            }
        }

        var member = RootVariableSource.getMember(TestClass.class, "value", TestClass.class, "value");
        assertThat(member).isInstanceOf(Method.class);
    }

    @Test
    void errorIfNoMember() {
        record TestClass() {
        }

        record RootClass(TestClass inner) {
        }

        assertThatCode(() -> RootVariableSource.getMember(RootClass.class, "inner.value", TestClass.class, "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("The source path (inner.value)",
                        "starting from root class (RootClass)",
                        "references a member (value)",
                        "on class (TestClass)",
                        "that does not exist.");
    }

    @Test
    void errorIfShadowVariableLoopedReferenced() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "isLooped",
                DEFAULT_MEMBER_ACCESSOR_FACTORY,
                DEFAULT_DESCRIPTOR_POLICY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("The source path (isLooped)",
                        "starting from root class (" + TestdataInvalidDeclarativeValue.class.getCanonicalName() + ")",
                        "accesses a @" + ShadowVariableLooped.class.getSimpleName() + " property",
                        "(isLooped)",
                        "Maybe remove the source path (isLooped) from the @" + ShadowSources.class.getSimpleName());
    }

    @Test
    void isVariableIsTrueForVariableOnEntity() {
        var metaModel = SolutionDescriptor.buildSolutionDescriptor(TestdataSolution.class, TestdataEntity.class).getMetaModel();
        assertThat(RootVariableSource.isVariable(metaModel, TestdataEntity.class, "value")).isTrue();
    }

    @Test
    void isVariableIsFalseForFactOnEntity() {
        var metaModel = SolutionDescriptor.buildSolutionDescriptor(TestdataSolution.class, TestdataEntity.class).getMetaModel();
        assertThat(RootVariableSource.isVariable(metaModel, TestdataEntity.class, "code")).isFalse();
    }

    @Test
    void isVariableIsFalseForFactClass() {
        var metaModel = SolutionDescriptor.buildSolutionDescriptor(TestdataSolution.class, TestdataEntity.class).getMetaModel();
        assertThat(RootVariableSource.isVariable(metaModel, TestdataObject.class, "code")).isFalse();
    }
}
