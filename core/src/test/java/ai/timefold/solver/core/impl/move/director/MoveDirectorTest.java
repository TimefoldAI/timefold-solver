package ai.timefold.solver.core.impl.move.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultBasicVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class MoveDirectorTest {

    @Test
    void readBasicVariable() {
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .<TestdataValue> basicVariable("value");

        var mockScoreDirector = mock(AbstractScoreDirector.class);
        var moveDirector = new MoveDirector<TestdataSolution>(mockScoreDirector);
        var expectedValue = new TestdataValue("value");
        var actualValue = moveDirector.getValue(variableMetaModel, new TestdataEntity("A", expectedValue));
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test
    void changeVariable() {
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .<TestdataValue> basicVariable("value");
        var variableDescriptor =
                ((DefaultBasicVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue>) variableMetaModel)
                        .variableDescriptor();

        var mockScoreDirector = (AbstractScoreDirector<TestdataSolution, ?, ?>) mock(AbstractScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector);
        var originalValue = new TestdataValue("value");
        var entity = new TestdataEntity("A", originalValue);
        var newValue = new TestdataValue("newValue");
        moveDirector.changeVariable(variableMetaModel, entity, newValue);

        assertThat(entity.getValue()).isEqualTo(newValue);
        verify(mockScoreDirector).beforeVariableChanged(variableDescriptor, entity);
        verify(mockScoreDirector).afterVariableChanged(variableDescriptor, entity);
        reset(mockScoreDirector);

        moveDirector.undo();
        assertThat(entity.getValue()).isEqualTo(originalValue);
        verify(mockScoreDirector).beforeVariableChanged(variableDescriptor, entity);
        verify(mockScoreDirector).afterVariableChanged(variableDescriptor, entity);
    }

    @Test
    void readListVariable() {
        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .<TestdataListValue> listVariable("valueList");

        var mockScoreDirector = (AbstractScoreDirector<TestdataListSolution, ?, ?>) mock(AbstractScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector);
        var expectedValue1 = new TestdataListValue("value1");
        var expectedValue2 = new TestdataListValue("value2");

        var entity = TestdataListEntity.createWithValues("A", expectedValue1, expectedValue2);
        var actualValue1 = moveDirector.getValueAtIndex(variableMetaModel, entity, 0);
        assertThat(actualValue1).isEqualTo(expectedValue1);

        var expectedLocation = ElementLocation.of(entity, 1);
        var supplyMock = mock(ListVariableStateSupply.class);
        when(supplyMock.getLocationInList(eq(expectedValue2)))
                .thenReturn(expectedLocation);
        when(mockScoreDirector.getListVariableStateSupply(any()))
                .thenReturn(supplyMock);
        var actualPosition = moveDirector.getPositionOf(variableMetaModel, expectedValue2);
        assertThat(actualPosition).isEqualTo(expectedLocation);
    }

    @Test
    void moveValueInList() {
        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .<TestdataListValue> listVariable("valueList");
        var variableDescriptor =
                ((DefaultListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue>) variableMetaModel)
                        .variableDescriptor();

        var expectedValue1 = new TestdataListValue("value1");
        var expectedValue2 = new TestdataListValue("value2");
        var expectedValue3 = new TestdataListValue("value3");
        var entity = TestdataListEntity.createWithValues("A", expectedValue1, expectedValue2, expectedValue3);

        var mockScoreDirector = (AbstractScoreDirector<TestdataListSolution, ?, ?>) mock(AbstractScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector);

        // Swap between second and last position.
        moveDirector.moveValueInList(variableMetaModel, entity, 1, 2);
        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue3, expectedValue2);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
        reset(mockScoreDirector);

        moveDirector.undo();
        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue2, expectedValue3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
        reset(mockScoreDirector);

        // Do the same in reverse.
        moveDirector.moveValueInList(variableMetaModel, entity, 2, 1);
        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue3, expectedValue2);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
        reset(mockScoreDirector);

        moveDirector.undo();
        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue2, expectedValue3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
    }

    @Test
    void moveValueBetweenLists() {
        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .<TestdataListValue> listVariable("valueList");
        var variableDescriptor =
                ((DefaultListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue>) variableMetaModel)
                        .variableDescriptor();

        var expectedValueA1 = new TestdataListValue("valueA1");
        var expectedValueA2 = new TestdataListValue("valueA2");
        var expectedValueA3 = new TestdataListValue("valueA3");
        var entityA = TestdataListEntity.createWithValues("A", expectedValueA1, expectedValueA2, expectedValueA3);
        var expectedValueB1 = new TestdataListValue("valueB1");
        var expectedValueB2 = new TestdataListValue("valueB2");
        var expectedValueB3 = new TestdataListValue("valueB3");
        var entityB = TestdataListEntity.createWithValues("B", expectedValueB1, expectedValueB2, expectedValueB3);

        var mockScoreDirector = (AbstractScoreDirector<TestdataListSolution, ?, ?>) mock(AbstractScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector);

        // Swap between second and last position.
        moveDirector.moveValueBetweenLists(variableMetaModel, entityA, 1, entityB, 2);
        assertThat(entityA.getValueList()).containsExactly(expectedValueA1, expectedValueA3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityA, 1, 2);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityA, 1, 1);
        assertThat(entityB.getValueList()).containsExactly(expectedValueB1, expectedValueB2, expectedValueA2, expectedValueB3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityB, 2, 2);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityB, 2, 3);
        reset(mockScoreDirector);

        moveDirector.undo();
        assertThat(entityA.getValueList()).containsExactly(expectedValueA1, expectedValueA2, expectedValueA3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityB, 2, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityB, 2, 2);
        assertThat(entityB.getValueList()).containsExactly(expectedValueB1, expectedValueB2, expectedValueB3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityA, 1, 1);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityA, 1, 2);
    }

    @Test
    void rebase() {
        var mockScoreDirector = mock(AbstractScoreDirector.class);
        when(mockScoreDirector.lookUpWorkingObject(any(TestdataValue.class)))
                .thenAnswer(invocation -> {
                    var value = (TestdataValue) invocation.getArgument(0);
                    return new TestdataValue(value.getCode());
                });
        var moveDirector = new MoveDirector<TestdataSolution>(mockScoreDirector);

        var expectedValue = new TestdataValue("value");
        var actualValue = moveDirector.rebase(expectedValue);
        assertSoftly(softly -> {
            softly.assertThat(actualValue)
                    .isNotSameAs(expectedValue);
            softly.assertThat(actualValue.getCode())
                    .isEqualTo(expectedValue.getCode());
        });
    }

    @Test
    void updateShadowVariables() {
        var mockScoreDirector = mock(AbstractScoreDirector.class);
        var moveDirector = new MoveDirector<TestdataSolution>(mockScoreDirector);

        moveDirector.updateShadowVariables();
        verify(mockScoreDirector).triggerVariableListeners();
    }

}
