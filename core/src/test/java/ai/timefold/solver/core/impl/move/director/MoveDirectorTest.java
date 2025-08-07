package ai.timefold.solver.core.impl.move.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListAssignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.ListRuinRecreateMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.cascade.single.TestdataSingleCascadingEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.cascade.single.TestdataSingleCascadingEntity;
import ai.timefold.solver.core.testdomain.cascade.single.TestdataSingleCascadingSolution;
import ai.timefold.solver.core.testdomain.cascade.single.TestdataSingleCascadingValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullEntity;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullMultiSwapListMove;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullSolution;
import ai.timefold.solver.core.testdomain.shadow.full.TestdataShadowedFullValue;

import org.junit.jupiter.api.Test;

class MoveDirectorTest {

    @Test
    void readBasicVariable() {
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .<TestdataValue> planningVariable("value");

        var mockScoreDirector = mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<TestdataSolution, SimpleScore>(mockScoreDirector);
        var expectedValue = new TestdataValue("value");
        var actualValue = moveDirector.getValue(variableMetaModel, new TestdataEntity("A", expectedValue));
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test
    void changeVariable() {
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .<TestdataValue> planningVariable("value");
        var variableDescriptor =
                ((DefaultPlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue>) variableMetaModel)
                        .variableDescriptor();

        var originalValue = new TestdataValue("value");
        var entity = new TestdataEntity("A", originalValue);
        var newValue = new TestdataValue("newValue");

        // Run a change and undo it.
        var mockScoreDirector = (InnerScoreDirector<TestdataSolution, ?>) mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector).ephemeral();
        moveDirector.changeVariable(variableMetaModel, entity, newValue);
        assertThat(entity.getValue()).isEqualTo(newValue);
        verify(mockScoreDirector).beforeVariableChanged(variableDescriptor, entity);
        verify(mockScoreDirector).afterVariableChanged(variableDescriptor, entity);
        reset(mockScoreDirector);
        moveDirector.close();

        assertThat(entity.getValue()).isEqualTo(originalValue);
        verify(mockScoreDirector).beforeVariableChanged(variableDescriptor, entity);
        verify(mockScoreDirector).afterVariableChanged(variableDescriptor, entity);
    }

    @Test
    void readListVariable() {
        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .<TestdataListValue> planningListVariable("valueList");

        var mockScoreDirector = (InnerScoreDirector<TestdataListSolution, ?>) mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector);
        var expectedValue1 = new TestdataListValue("value1");
        var expectedValue2 = new TestdataListValue("value2");

        var entity = TestdataListEntity.createWithValues("A", expectedValue1, expectedValue2);
        var actualValue1 = moveDirector.getValueAtIndex(variableMetaModel, entity, 0);
        assertThat(actualValue1).isEqualTo(expectedValue1);

        var expectedLocation = ElementPosition.of(entity, 1);
        var supplyMock = mock(ListVariableStateSupply.class);
        when(supplyMock.getElementPosition(expectedValue2))
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
                .<TestdataListValue> planningListVariable("valueList");
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue>) variableMetaModel)
                        .variableDescriptor();

        var expectedValue1 = new TestdataListValue("value1");
        var expectedValue2 = new TestdataListValue("value2");
        var expectedValue3 = new TestdataListValue("value3");
        var entity = TestdataListEntity.createWithValues("A", expectedValue1, expectedValue2, expectedValue3);

        // Swap between second and last position.
        var mockScoreDirector = (InnerScoreDirector<TestdataListSolution, ?>) mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector).ephemeral();
        moveDirector.moveValueInList(variableMetaModel, entity, 1, 2);
        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue3, expectedValue2);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
        reset(mockScoreDirector);
        moveDirector.close(); // Undo it.

        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue2, expectedValue3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
        reset(mockScoreDirector);

        // Do the same in reverse.
        moveDirector = new MoveDirector<>(mockScoreDirector).ephemeral();
        moveDirector.moveValueInList(variableMetaModel, entity, 2, 1);
        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue3, expectedValue2);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
        reset(mockScoreDirector);
        moveDirector.close();

        assertThat(entity.getValueList()).containsExactly(expectedValue1, expectedValue2, expectedValue3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entity, 1, 3);
    }

    @Test
    void moveValueBetweenLists() {
        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor()
                .getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .<TestdataListValue> planningListVariable("valueList");
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<TestdataListSolution, TestdataListEntity, TestdataListValue>) variableMetaModel)
                        .variableDescriptor();

        var expectedValueA1 = new TestdataListValue("valueA1");
        var expectedValueA2 = new TestdataListValue("valueA2");
        var expectedValueA3 = new TestdataListValue("valueA3");
        var entityA = TestdataListEntity.createWithValues("A", expectedValueA1, expectedValueA2, expectedValueA3);
        var expectedValueB1 = new TestdataListValue("valueB1");
        var expectedValueB2 = new TestdataListValue("valueB2");
        var expectedValueB3 = new TestdataListValue("valueB3");
        var entityB = TestdataListEntity.createWithValues("B", expectedValueB1, expectedValueB2, expectedValueB3);

        // Swap between second and last position.
        var mockScoreDirector = (InnerScoreDirector<TestdataListSolution, ?>) mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(mockScoreDirector).ephemeral();
        moveDirector.moveValueBetweenLists(variableMetaModel, entityA, 1, entityB, 2);
        assertThat(entityA.getValueList()).containsExactly(expectedValueA1, expectedValueA3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityA, 1, 2);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityA, 1, 1);
        assertThat(entityB.getValueList()).containsExactly(expectedValueB1, expectedValueB2, expectedValueA2,
                expectedValueB3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityB, 2, 2);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityB, 2, 3);
        reset(mockScoreDirector);
        moveDirector.close(); // Undo it.

        assertThat(entityA.getValueList()).containsExactly(expectedValueA1, expectedValueA2, expectedValueA3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityB, 2, 3);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityB, 2, 2);
        assertThat(entityB.getValueList()).containsExactly(expectedValueB1, expectedValueB2, expectedValueB3);
        verify(mockScoreDirector).beforeListVariableChanged(variableDescriptor, entityA, 1, 1);
        verify(mockScoreDirector).afterListVariableChanged(variableDescriptor, entityA, 1, 2);
    }

    @Test
    void rebase() {
        var mockScoreDirector = mock(InnerScoreDirector.class);
        when(mockScoreDirector.lookUpWorkingObject(any(TestdataValue.class)))
                .thenAnswer(invocation -> {
                    var value = (TestdataValue) invocation.getArgument(0);
                    return new TestdataValue(value.getCode());
                });
        var moveDirector = new MoveDirector<TestdataSolution, SimpleScore>(mockScoreDirector);

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
    void undoNestedPhaseMove() {
        var innerScoreDirector = (InnerScoreDirector<TestdataListSolution, SimpleScore>) mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var listVariableStateSupply = mock(ListVariableStateSupply.class);
        var listVariableDescriptor = mock(ListVariableDescriptor.class);
        var supplyManager = mock(SupplyManager.class);
        var ruinRecreateConstructionHeuristicPhaseBuilder = mock(RuinRecreateConstructionHeuristicPhaseBuilder.class);
        var constructionHeuristicPhase = mock(RuinRecreateConstructionHeuristicPhase.class);

        // The objective is to simulate the reassignment of v1 from e1 to e2
        // The R&R move analyzes only e1 initially,
        // since it is impossible to know that v1 will be assigned to e2 during the nested CH phase
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var e1 = new TestdataListEntity("e1", v1);
        var e2 = new TestdataListEntity("e2", v2, v1);
        var s1 = new TestdataListSolution();
        s1.setEntityList(List.of(e1, e2));
        s1.setValueList(List.of(v1, v2));
        when(innerScoreDirector.getWorkingSolution()).thenReturn(s1);
        when(innerScoreDirector.isDerived()).thenReturn(false);
        when(innerScoreDirector.getSupplyManager()).thenReturn(supplyManager);
        when(supplyManager.demand(any())).thenReturn(listVariableStateSupply);
        // 1 - v1 is on e1 list
        // 2 - v1 moves to e2 list
        when(listVariableStateSupply.getElementPosition(any()))
                .thenReturn(ElementPosition.of(e1, 0), ElementPosition.of(e2, 1));
        when(listVariableStateSupply.getSourceVariableDescriptor()).thenReturn(listVariableDescriptor);
        when(listVariableDescriptor.getFirstUnpinnedIndex(any())).thenReturn(0);
        when(listVariableDescriptor.getListSize(any())).thenReturn(1);
        when(listVariableDescriptor.getValue(any())).thenReturn(e1.getValueList(), e2.getValueList());
        // Ignore the nested phase but simulates v1 moving to e2
        when(ruinRecreateConstructionHeuristicPhaseBuilder.withElementsToRecreate(any()))
                .thenReturn(ruinRecreateConstructionHeuristicPhaseBuilder);
        when(ruinRecreateConstructionHeuristicPhaseBuilder.withElementsToRuin(any()))
                .thenReturn(ruinRecreateConstructionHeuristicPhaseBuilder);
        when(ruinRecreateConstructionHeuristicPhaseBuilder.ensureThreadSafe(any()))
                .thenReturn(ruinRecreateConstructionHeuristicPhaseBuilder);
        when(ruinRecreateConstructionHeuristicPhaseBuilder.build())
                .thenReturn(constructionHeuristicPhase);
        when(constructionHeuristicPhase.getMissingUpdatedElementsMap()).thenReturn(Map.of(e2, List.of(v2)));

        var ephemeralMoveDirector = moveDirector.ephemeral();
        var scoreDirector = ephemeralMoveDirector.getScoreDirector();
        var move = new ListRuinRecreateMove<TestdataListSolution>(listVariableDescriptor,
                ruinRecreateConstructionHeuristicPhaseBuilder, new SolverScope<>(), List.of(v1), Set.of(e1));
        move.doMoveOnly(scoreDirector);
        var undoMove = (RecordedUndoMove<TestdataListSolution>) ephemeralMoveDirector.createUndoMove();
        // e1 must be analyzed at the beginning of the move execution
        assertThat(undoMove.variableChangeActionList().stream().anyMatch(action -> {
            if (action instanceof ListVariableBeforeChangeAction<?, ?, ?> beforeChangeAction) {
                return beforeChangeAction.entity() == e1 && beforeChangeAction.fromIndex() == 0
                        && beforeChangeAction.toIndex() == 1 && beforeChangeAction.oldValue().size() == 1
                        && beforeChangeAction.oldValue().get(0).equals(v1);
            }
            return false;
        })).isTrue();
        // e2 is not analyzed at the beginning of move execution,
        // but it must have a before list change event to restore the original elements.
        assertThat(undoMove.variableChangeActionList().stream().anyMatch(action -> {
            if (action instanceof ListVariableBeforeChangeAction<?, ?, ?> beforeChangeAction) {
                return beforeChangeAction.entity() == e2 && beforeChangeAction.fromIndex() == 0
                        && beforeChangeAction.toIndex() == 1 && beforeChangeAction.oldValue().size() == 1
                        && beforeChangeAction.oldValue().get(0).equals(v2);
            }
            return false;
        })).isTrue();
        ephemeralMoveDirector.close();
    }

    @Test
    void testSolverScopeNestedPhase() {
        var innerScoreDirector = (InnerScoreDirector<TestdataSolution, SimpleScore>) mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(innerScoreDirector);
        var genuineVariableDescriptor = mock(GenuineVariableDescriptor.class);
        var supplyManager = mock(SupplyManager.class);
        var ruinRecreateConstructionHeuristicPhaseBuilder = mock(RuinRecreateConstructionHeuristicPhaseBuilder.class);
        var constructionHeuristicPhase = mock(RuinRecreateConstructionHeuristicPhase.class);
        var mainSolverScope = new SolverScope<TestdataSolution>();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);
        var s1 = new TestdataSolution();
        s1.setEntityList(List.of(e1, e2));
        s1.setValueList(List.of(v1, v2));
        when(innerScoreDirector.getWorkingSolution()).thenReturn(s1);
        when(innerScoreDirector.isDerived()).thenReturn(false);
        when(innerScoreDirector.getSupplyManager()).thenReturn(supplyManager);
        when(ruinRecreateConstructionHeuristicPhaseBuilder.withElementsToRecreate(any()))
                .thenReturn(ruinRecreateConstructionHeuristicPhaseBuilder);
        when(ruinRecreateConstructionHeuristicPhaseBuilder.ensureThreadSafe(any()))
                .thenReturn(ruinRecreateConstructionHeuristicPhaseBuilder);
        when(ruinRecreateConstructionHeuristicPhaseBuilder.build())
                .thenReturn(constructionHeuristicPhase);
        var ephemeralMoveDirector = moveDirector.ephemeral();
        var scoreDirector = ephemeralMoveDirector.getScoreDirector();

        var move = new RuinRecreateMove<TestdataSolution>(genuineVariableDescriptor,
                ruinRecreateConstructionHeuristicPhaseBuilder, mainSolverScope, List.of(v1), Set.of(e1));
        move.doMoveOnly(scoreDirector);
        // Not using the main solver scope
        verify(constructionHeuristicPhase, times(0)).solve(mainSolverScope);
        // Uses a new instance of SolverScope
        verify(constructionHeuristicPhase, times(1)).solve(any());
        ephemeralMoveDirector.close();
    }

    @Test
    void variableListenersAreTriggeredWhenSolutionIsConsistent() {
        var solutionDescriptor = TestdataShadowedFullSolution.buildSolutionDescriptor();
        var scoreCalculator = new TestdataShadowedFullEasyScoreCalculator();
        var easyScoreDirectorFactory = new EasyScoreDirectorFactory<>(solutionDescriptor, scoreCalculator);
        var innerScoreDirector = easyScoreDirectorFactory.buildScoreDirector();
        var moveDirector = new MoveDirector<>(innerScoreDirector);

        var entityA = new TestdataShadowedFullEntity("Entity A");
        var entityB = new TestdataShadowedFullEntity("Entity B");

        var valueA = new TestdataShadowedFullValue("Value A");
        var valueB = new TestdataShadowedFullValue("Value B");
        var valueC = new TestdataShadowedFullValue("Value C");
        var valueD = new TestdataShadowedFullValue("Value D");
        var valueE = new TestdataShadowedFullValue("Value E");
        var valueF = new TestdataShadowedFullValue("Value F");

        entityA.getValueList().addAll(List.of(valueA, valueB, valueC));
        valueA.updateShadows(entityA, 0);
        valueB.updateShadows(entityA, 1);
        valueC.updateShadows(entityA, 2);

        entityB.getValueList().addAll(List.of(valueD, valueE, valueF));
        valueD.updateShadows(entityB, 0);
        valueE.updateShadows(entityB, 1);
        valueF.updateShadows(entityB, 2);

        var workingSolution = new TestdataShadowedFullSolution();
        workingSolution.setCode("Solution");
        workingSolution.setEntityList(List.of(entityA, entityB));
        workingSolution.setValueList(List.of(valueA, valueB, valueC, valueD, valueE, valueF));

        innerScoreDirector.setWorkingSolution(workingSolution);

        var ephemeralMoveDirector = moveDirector.ephemeral();
        var scoreDirector = ephemeralMoveDirector.getScoreDirector();
        var move = new TestdataShadowedFullMultiSwapListMove(entityA, entityB,
                List.of(
                        List.of(valueE, valueF),
                        List.of(valueA, valueB, valueC, valueD)),
                List.of(
                        List.of(valueA, valueB, valueC, valueD),
                        List.of(valueE, valueF)));
        move.doMoveOnly(scoreDirector);
        scoreDirector.triggerVariableListeners();
        ephemeralMoveDirector.close();
    }

    @Test
    void undoCascadingUpdateShadowVariable() {
        var solutionDescriptor = TestdataSingleCascadingSolution.buildSolutionDescriptor();
        var scoreCalculator = new TestdataSingleCascadingEasyScoreCalculator();
        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(solutionDescriptor, scoreCalculator);
        var innerScoreDirector = scoreDirectorFactory.buildScoreDirector();
        var moveDirector = new MoveDirector<>(innerScoreDirector);

        var entityA = new TestdataSingleCascadingEntity("Entity A");
        var valueA = new TestdataSingleCascadingValue(0);
        var valueB = new TestdataSingleCascadingValue(1);
        var workingSolution = new TestdataSingleCascadingSolution();
        workingSolution.setEntityList(List.of(entityA));
        workingSolution.setValueList(List.of(valueA, valueB));

        innerScoreDirector.setWorkingSolution(workingSolution);
        assertThat(workingSolution.getValueList()).map(TestdataSingleCascadingValue::getCascadeValue).allMatch(Objects::isNull);

        var ephemeralMoveDirector = moveDirector.ephemeral();
        var scoreDirector = ephemeralMoveDirector.getScoreDirector();
        var move = new ListAssignMove<>(TestdataSingleCascadingEntity.buildVariableDescriptorForValueList(), valueA,
                entityA, 0);
        move.doMoveOnly(scoreDirector);
        assertThat(valueA.getCascadeValue()).isNotNull();
        ephemeralMoveDirector.close();

        // After the move is undone, the cascade value must be reset
        assertThat(valueA.getCascadeValue()).isNull();
    }

}
