package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.testdata.domain.declarative.invalid.TestdataInvalidDeclarativeEntity;
import ai.timefold.solver.core.impl.testdata.domain.declarative.invalid.TestdataInvalidDeclarativeSolution;
import ai.timefold.solver.core.impl.testdata.domain.declarative.invalid.TestdataInvalidDeclarativeValue;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RootVariableSourceTest {
    PlanningSolutionMetaModel<TestdataInvalidDeclarativeSolution> planningSolutionMetaModel;

    PlanningEntityMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeEntity> entityMetaModel;
    PlanningListVariableMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeEntity, List<TestdataInvalidDeclarativeValue>> listVariableMetaModel;

    PlanningEntityMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue> shadowEntityMetaModel;
    ShadowVariableMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue, TestdataInvalidDeclarativeValue> previousElementMetaModel;
    ShadowVariableMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue, TestdataInvalidDeclarativeValue> shadowVariableMetaModel;
    ShadowVariableMetaModel<TestdataInvalidDeclarativeSolution, TestdataInvalidDeclarativeValue, TestdataInvalidDeclarativeValue> dependencyMetaModel;

    MemberAccessorFactory memberAccessorFactory;
    DescriptorPolicy descriptorPolicy;

    @BeforeEach
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void setUp() {
        planningSolutionMetaModel = mock(PlanningSolutionMetaModel.class);

        entityMetaModel = mock(PlanningEntityMetaModel.class);

        listVariableMetaModel = mock(PlanningListVariableMetaModel.class);
        when(listVariableMetaModel.name()).thenReturn("values");
        when(listVariableMetaModel.type()).thenReturn((Class) List.class);

        when(entityMetaModel.type()).thenReturn(TestdataInvalidDeclarativeEntity.class);
        when(entityMetaModel.variables()).thenReturn(List.of(listVariableMetaModel));
        when(entityMetaModel.variable("values")).thenReturn((PlanningListVariableMetaModel) listVariableMetaModel);

        shadowEntityMetaModel = mock(PlanningEntityMetaModel.class);

        previousElementMetaModel = mock(ShadowVariableMetaModel.class);
        when(previousElementMetaModel.name()).thenReturn("previous");
        when(previousElementMetaModel.type()).thenReturn(TestdataInvalidDeclarativeValue.class);

        shadowVariableMetaModel = mock(ShadowVariableMetaModel.class);
        when(shadowVariableMetaModel.name()).thenReturn("shadow");
        when(shadowVariableMetaModel.type()).thenReturn(TestdataInvalidDeclarativeValue.class);

        dependencyMetaModel = mock(ShadowVariableMetaModel.class);
        when(dependencyMetaModel.name()).thenReturn("dependency");
        when(dependencyMetaModel.type()).thenReturn(TestdataInvalidDeclarativeValue.class);

        when(shadowEntityMetaModel.type()).thenReturn(TestdataInvalidDeclarativeValue.class);
        when(shadowEntityMetaModel.variables())
                .thenReturn(List.of(previousElementMetaModel, shadowVariableMetaModel, dependencyMetaModel));
        when(shadowEntityMetaModel.variable("previous")).thenReturn((ShadowVariableMetaModel) previousElementMetaModel);
        when(shadowEntityMetaModel.variable("shadow")).thenReturn((ShadowVariableMetaModel) shadowVariableMetaModel);
        when(shadowEntityMetaModel.variable("dependency")).thenReturn((ShadowVariableMetaModel) dependencyMetaModel);

        when(planningSolutionMetaModel.type()).thenReturn(TestdataInvalidDeclarativeSolution.class);
        when(planningSolutionMetaModel.entities()).thenReturn(List.of(entityMetaModel, shadowEntityMetaModel));
        when(planningSolutionMetaModel.entity(TestdataInvalidDeclarativeEntity.class)).thenReturn(entityMetaModel);
        when(planningSolutionMetaModel.entity(TestdataInvalidDeclarativeValue.class)).thenReturn(shadowEntityMetaModel);

        memberAccessorFactory = new MemberAccessorFactory();
        descriptorPolicy = new DescriptorPolicy();
    }

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
                memberAccessorFactory,
                descriptorPolicy);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.isDeclarative()).isFalse();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isNull();

        var sourceVisitor = mock(Consumer.class);
        var entity = new TestdataInvalidDeclarativeValue("v1");
        source.targetEntityFunctionStartingFromVariableEntity().accept(entity, sourceVisitor);

        verify(sourceVisitor).accept(entity);
        verifyNoMoreInteractions(sourceVisitor);

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
                memberAccessorFactory,
                descriptorPolicy);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.isDeclarative()).isTrue();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var sourceVisitor = mock(Consumer.class);
        var entity = new TestdataInvalidDeclarativeValue("v1");
        source.targetEntityFunctionStartingFromVariableEntity().accept(entity, sourceVisitor);

        verify(sourceVisitor).accept(entity);
        verifyNoMoreInteractions(sourceVisitor);

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
                memberAccessorFactory,
                descriptorPolicy);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.isDeclarative()).isTrue();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var sourceVisitor = mock(Consumer.class);
        var group = new TestdataInvalidDeclarativeValue("group");
        var v1 = new TestdataInvalidDeclarativeValue("v1");
        var v2 = new TestdataInvalidDeclarativeValue("v2");

        group.setGroup(List.of(v1, v2));
        source.targetEntityFunctionStartingFromVariableEntity().accept(group, sourceVisitor);

        verify(sourceVisitor).accept(group);
        verifyNoMoreInteractions(sourceVisitor);

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
                memberAccessorFactory,
                descriptorPolicy);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.isDeclarative()).isFalse();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isNull();

        var sourceVisitor = mock(Consumer.class);
        var group = new TestdataInvalidDeclarativeValue("group");
        var v1 = new TestdataInvalidDeclarativeValue("v1");
        var v2 = new TestdataInvalidDeclarativeValue("v2");

        group.setGroup(List.of(v1, v2));
        source.targetEntityFunctionStartingFromVariableEntity().accept(group, sourceVisitor);

        verify(sourceVisitor).accept(group);
        verifyNoMoreInteractions(sourceVisitor);

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
                memberAccessorFactory,
                descriptorPolicy);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(1);
        var source = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(source);
        assertThat(source.variableMetaModel()).isEqualTo(dependencyMetaModel);
        assertThat(source.isTopLevel()).isTrue();
        assertThat(source.isDeclarative()).isTrue();
        assertThat(source.targetVariableMetamodel()).isEqualTo(shadowVariableMetaModel);
        assertThat(source.downstreamDeclarativeVariableMetamodel()).isEqualTo(dependencyMetaModel);

        var sourceVisitor = mock(Consumer.class);
        var root = new TestdataInvalidDeclarativeValue("fact");
        var fact = new TestdataInvalidDeclarativeValue("fact");
        var v1 = new TestdataInvalidDeclarativeValue("v1");
        var v2 = new TestdataInvalidDeclarativeValue("v2");

        root.setFact(fact);
        fact.setGroup(List.of(v1, v2));
        source.targetEntityFunctionStartingFromVariableEntity().accept(root, sourceVisitor);

        verify(sourceVisitor).accept(root);
        verifyNoMoreInteractions(sourceVisitor);

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
                memberAccessorFactory,
                descriptorPolicy);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(2);
        var previousSource = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(previousSource);
        assertThat(previousSource.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(previousSource.isTopLevel()).isTrue();
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

        var sourceVisitor = mock(Consumer.class);
        var previousElement = new TestdataInvalidDeclarativeValue("previous");
        var currentElement = new TestdataInvalidDeclarativeValue("current");
        currentElement.setPrevious(previousElement);

        previousSource.targetEntityFunctionStartingFromVariableEntity().accept(currentElement, sourceVisitor);

        verify(sourceVisitor).accept(previousElement);
        verifyNoMoreInteractions(sourceVisitor);

        var rootVisitor = mock(Consumer.class);
        rootVariableSource.valueEntityFunction().accept(currentElement, rootVisitor);
        verify(rootVisitor).accept(previousElement);
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
                memberAccessorFactory,
                descriptorPolicy);

        assertThat(rootVariableSource.rootEntity()).isEqualTo(TestdataInvalidDeclarativeValue.class);
        assertThat(rootVariableSource.variableSourceReferences()).hasSize(2);
        var previousSource = rootVariableSource.variableSourceReferences().get(0);

        assertEmptyChainToVariableEntity(previousSource);
        assertThat(previousSource.variableMetaModel()).isEqualTo(previousElementMetaModel);
        assertThat(previousSource.isTopLevel()).isTrue();
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

        var sourceVisitor = mock(Consumer.class);
        var previousElement = new TestdataInvalidDeclarativeValue("previous");
        var currentElement = new TestdataInvalidDeclarativeValue("current");
        var group = new TestdataInvalidDeclarativeValue("group");

        currentElement.setPrevious(previousElement);
        group.setGroup(List.of(currentElement));

        previousSource.targetEntityFunctionStartingFromVariableEntity().accept(currentElement, sourceVisitor);

        verify(sourceVisitor).accept(previousElement);
        verifyNoMoreInteractions(sourceVisitor);

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
                memberAccessorFactory,
                descriptorPolicy))
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
                memberAccessorFactory,
                descriptorPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (previous.previous)" +
                        " starting from root entity class (TestdataInvalidDeclarativeValue)" +
                        " accesses a non-declarative shadow variable (previous)" +
                        " not from the root entity or collection.");
    }

    @Test
    void invalidPathUsingBuiltinShadowAfterFact() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "fact.previous",
                memberAccessorFactory,
                descriptorPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (fact.previous)" +
                        " starting from root entity class (TestdataInvalidDeclarativeValue)" +
                        " accesses a non-declarative shadow variable (previous)" +
                        " not from the root entity or collection.");
    }

    @Test
    void invalidPathUsingGroupAfterGroup() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "group[].group[].previous",
                memberAccessorFactory,
                descriptorPolicy))
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
                memberAccessorFactory,
                descriptorPolicy))
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
                memberAccessorFactory,
                descriptorPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (fact)" +
                        " starting from root entity class (TestdataInvalidDeclarativeValue)" +
                        " does not reference any variables.");
    }

    @Test
    void invalidPathMultipleFactsInARow() {
        assertThatCode(() -> RootVariableSource.from(
                planningSolutionMetaModel,
                TestdataInvalidDeclarativeValue.class,
                "shadow",
                "fact.fact.dependency",
                memberAccessorFactory,
                descriptorPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The source path (fact.fact.dependency)" +
                        " starting from root entity (TestdataInvalidDeclarativeValue)" +
                        " referencing multiple facts (fact, fact)" +
                        " in a row.");
    }
}
