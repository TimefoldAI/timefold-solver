package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptUtils.getMultiEntityBetweenPredicate;
import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptUtils.getMultiEntitySuccessorFunction;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KOptListMoveTest {

    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();

    private final InnerScoreDirector<TestdataListSolution, ?> scoreDirector =
            PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

    private final TestdataListValue v1 = new TestdataListValue("1");
    private final TestdataListValue v2 = new TestdataListValue("2");
    private final TestdataListValue v3 = new TestdataListValue("3");
    private final TestdataListValue v4 = new TestdataListValue("4");
    private final TestdataListValue v5 = new TestdataListValue("5");
    private final TestdataListValue v6 = new TestdataListValue("6");
    private final TestdataListValue v7 = new TestdataListValue("7");
    private final TestdataListValue v8 = new TestdataListValue("8");
    private final TestdataListValue v9 = new TestdataListValue("9");
    private final TestdataListValue v10 = new TestdataListValue("10");
    private final TestdataListValue v11 = new TestdataListValue("11");
    private final TestdataListValue v12 = new TestdataListValue("12");

    private final TestdataListValue destinationV1 = new TestdataListValue("1");
    private final TestdataListValue destinationV2 = new TestdataListValue("2");
    private final TestdataListValue destinationV3 = new TestdataListValue("3");
    private final TestdataListValue destinationV4 = new TestdataListValue("4");
    private final TestdataListValue destinationV5 = new TestdataListValue("5");
    private final TestdataListValue destinationV6 = new TestdataListValue("6");
    private final TestdataListValue destinationV7 = new TestdataListValue("7");
    private final TestdataListValue destinationV8 = new TestdataListValue("8");
    private final TestdataListValue destinationV9 = new TestdataListValue("9");
    private final TestdataListValue destinationV10 = new TestdataListValue("10");
    private final TestdataListValue destinationV11 = new TestdataListValue("11");
    private final TestdataListValue destinationV12 = new TestdataListValue("12");

    // TODO: It appears the multi-entity approach does not like kopt-affected-elements;
    // (in particular, I found index variable corruption causing a NPE due to incorrect after
    // listVariableChanged happens around local step 1000 when changes notifications are limited
    // to kopt-affected-elements; as a workaround, the full list is notified currently.
    // (the list notifications that are commented out are the correct + precise
    // notifications; they all pass with the kopt-affected-elements, but solving must of hit an
    // edge case that was not tested before.

    @Test
    void test3Opt() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v6, v1,
                        v2, v3,
                        v4, v5),
                List.of(v1, v3,
                        v2, v5,
                        v4, v6));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v5, v6, v4, v3);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 6);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 6);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 6);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 6);
    }

    @Test
    void test3OptPinned() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7));
        scoreDirector.setWorkingSolution(solution);

        var variableDescriptorSpy = spy(variableDescriptor);
        var entityDescriptor = spy(TestdataListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntity.class));
        when(variableDescriptorSpy.getEntityDescriptor()).thenReturn(entityDescriptor);
        when(variableDescriptorSpy.getFirstUnpinnedIndex(e1)).thenReturn(1);
        when(entityDescriptor.supportsPinning()).thenReturn(true);
        when(entityDescriptor.isMovable(null, e1)).thenReturn(true);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                variableDescriptorSpy,
                List.of(v7, v2,
                        v3, v4,
                        v5, v6),
                List.of(v2, v4,
                        v3, v6,
                        v5, v7));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v6, v7, v5, v4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptorSpy, e1, 1, 7);
        verify(scoreDirector).afterListVariableChanged(variableDescriptorSpy, e1, 1, 7);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptorSpy, e1, 1, 7);
        verify(scoreDirector).afterListVariableChanged(variableDescriptorSpy, e1, 1, 7);
    }

    @Test
    void test3OptRebase() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6);
        var destinationE1 = TestdataListEntity.createWithValues("e1", destinationV1, destinationV2,
                destinationV3, destinationV4, destinationV5, destinationV6);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v6, v1,
                        v2, v3,
                        v4, v5),
                List.of(v1, v3,
                        v2, v5,
                        v4, v6));

        InnerScoreDirector<TestdataListSolution, ?> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { v3, destinationV3 },
                        { v4, destinationV4 },
                        { v5, destinationV5 },
                        { v6, destinationV6 },
                        { e1, destinationE1 },
                });
        var supplyManager = Mockito.mock(SupplyManager.class);
        var inverseVariableSupply = Mockito.mock(ListVariableStateSupply.class);

        when(destinationScoreDirector.getSupplyManager()).thenReturn(supplyManager);
        when(supplyManager.demand(Mockito.any(ListVariableStateDemand.class))).thenReturn(inverseVariableSupply);
        when(inverseVariableSupply.getInverseSingleton(destinationE1.getValueList().get(0))).thenReturn(destinationE1);

        var rebasedMove = kOptListMove.rebase(destinationScoreDirector);
        assertThat(rebasedMove.isMoveDoable(destinationScoreDirector)).isTrue();
        var undoMove = rebasedMove.doMove(destinationScoreDirector);
        assertThat(destinationE1.getValueList()).containsExactly(destinationV1, destinationV2, destinationV5, destinationV6,
                destinationV4, destinationV3);
        verify(destinationScoreDirector).beforeListVariableChanged(variableDescriptor, destinationE1, 0, 6);
        verify(destinationScoreDirector).afterListVariableChanged(variableDescriptor, destinationE1, 0, 6);

        reset(destinationScoreDirector);
        undoMove.doMoveOnly(destinationScoreDirector);
        assertThat(destinationE1.getValueList()).containsExactly(destinationV1, destinationV2, destinationV3, destinationV4,
                destinationV5, destinationV6);
        verify(destinationScoreDirector).beforeListVariableChanged(variableDescriptor, destinationE1, 0, 6);
        verify(destinationScoreDirector).afterListVariableChanged(variableDescriptor, destinationE1, 0, 6);
    }

    @Test
    void testMultiEntity3OptRebase() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v6);
        var e2 = TestdataListEntity.createWithValues("e2", v4, v5);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6));
        scoreDirector.setWorkingSolution(solution);

        var destinationE1 =
                TestdataListEntity.createWithValues("e1", destinationV1, destinationV2, destinationV3, destinationV6);
        var destinationE2 = TestdataListEntity.createWithValues("e2", destinationV4, destinationV5);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v6, v1,
                        v2, v3,
                        v4, v5),
                List.of(v1, v3,
                        v2, v5,
                        v4, v6));

        InnerScoreDirector<TestdataListSolution, ?> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { v3, destinationV3 },
                        { v4, destinationV4 },
                        { v5, destinationV5 },
                        { v6, destinationV6 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                });
        var supplyManager = Mockito.mock(SupplyManager.class);
        var inverseVariableSupply = Mockito.mock(SingletonInverseVariableSupply.class);

        when(destinationScoreDirector.getSupplyManager()).thenReturn(supplyManager);
        when(supplyManager.demand(Mockito.any(ListVariableStateDemand.class))).thenReturn(inverseVariableSupply);
        when(inverseVariableSupply.getInverseSingleton(destinationE1.getValueList().get(0))).thenReturn(destinationE1);
        when(inverseVariableSupply.getInverseSingleton(destinationE2.getValueList().get(0))).thenReturn(destinationE2);

        var rebasedMove = kOptListMove.rebase(destinationScoreDirector);

        assertThat(rebasedMove.isMoveDoable(destinationScoreDirector)).isTrue();
        var undoMove = rebasedMove.doMove(destinationScoreDirector);
        assertThat(destinationE1.getValueList()).containsExactly(destinationV1, destinationV5);
        assertThat(destinationE2.getValueList()).containsExactly(destinationV2, destinationV6, destinationV4, destinationV3);
        verify(destinationScoreDirector).beforeListVariableChanged(variableDescriptor, destinationE1, 0, 4);
        verify(destinationScoreDirector).beforeListVariableChanged(variableDescriptor, destinationE2, 0, 2);
        verify(destinationScoreDirector).afterListVariableChanged(variableDescriptor, destinationE1, 0, 2);
        verify(destinationScoreDirector).afterListVariableChanged(variableDescriptor, destinationE2, 0, 4);

        reset(destinationScoreDirector);
        undoMove.doMoveOnly(destinationScoreDirector);
        assertThat(destinationE1.getValueList()).containsExactly(destinationV1, destinationV2, destinationV3, destinationV6);
        assertThat(destinationE2.getValueList()).containsExactly(destinationV4, destinationV5);
        verify(destinationScoreDirector).beforeListVariableChanged(variableDescriptor, destinationE1, 0, 2);
        verify(destinationScoreDirector).beforeListVariableChanged(variableDescriptor, destinationE2, 0, 4);
        verify(destinationScoreDirector).afterListVariableChanged(variableDescriptor, destinationE1, 0, 4);
        verify(destinationScoreDirector).afterListVariableChanged(variableDescriptor, destinationE2, 0, 2);
    }

    @Test
    void testMultiEntity2Opt() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4);
        var e2 = TestdataListEntity.createWithValues("e2", v5, v6, v7, v8);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v2, v3,
                        v6, v7),
                List.of(v2, v6,
                        v3, v7));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v6, v5, v4);
        assertThat(e2.getValueList()).containsExactly(v3, v7, v8);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 4);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 2);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 5);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 1);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 5);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 3);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4);
        assertThat(e2.getValueList()).containsExactly(v5, v6, v7, v8);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 5);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 1);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 4);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 2);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 5);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 3);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 4);
    }

    @Test
    void testMultiEntity3Opt() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v6);
        var e2 = TestdataListEntity.createWithValues("e2", v4, v5);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v6, v1,
                        v2, v3,
                        v4, v5),
                List.of(v1, v3,
                        v2, v5,
                        v4, v6));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v5);
        assertThat(e2.getValueList()).containsExactly(v2, v6, v4, v3);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 2);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 2);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 4);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v6);
        assertThat(e2.getValueList()).containsExactly(v4, v5);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 2);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 2);
    }

    @Test
    void testMultiEntity3OptPinned() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v6, v7);
        var e2 = TestdataListEntity.createWithValues("e2", v4, v5);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7));
        scoreDirector.setWorkingSolution(solution);

        var variableDescriptorSpy = spy(variableDescriptor);
        var entityDescriptor = spy(TestdataListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataListEntity.class));
        when(variableDescriptorSpy.getEntityDescriptor()).thenReturn(entityDescriptor);
        when(variableDescriptorSpy.getFirstUnpinnedIndex(e1)).thenReturn(1);
        when(entityDescriptor.supportsPinning()).thenReturn(true);
        when(entityDescriptor.isMovable(null, e1)).thenReturn(true);
        when(entityDescriptor.isMovable(null, e2)).thenReturn(true);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                variableDescriptorSpy,
                List.of(v7, v2,
                        v3, v6,
                        v4, v5),
                List.of(v2, v6,
                        v3, v5,
                        v4, v7));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v5);
        assertThat(e2.getValueList()).containsExactly(v3, v7, v4, v6);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptorSpy, e1, 1, 5);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptorSpy, e2, 0, 2);
        verify(scoreDirector).afterListVariableChanged(variableDescriptorSpy, e1, 1, 3);
        verify(scoreDirector).afterListVariableChanged(variableDescriptorSpy, e2, 0, 4);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v6, v7);
        assertThat(e2.getValueList()).containsExactly(v4, v5);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptorSpy, e1, 1, 3);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptorSpy, e2, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptorSpy, e1, 1, 5);
        verify(scoreDirector).afterListVariableChanged(variableDescriptorSpy, e2, 0, 2);
    }

    @Test
    void testMultiEntity4Opt() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4);
        var e2 = TestdataListEntity.createWithValues("e2", v5, v6, v7, v8);
        var e3 = TestdataListEntity.createWithValues("e3", v9, v10, v11, v12);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2, e3));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v2, v3,
                        v7, v8,
                        v9, v10,
                        v11, v12),
                List.of(v11, v3,
                        v12, v7,
                        v2, v9,
                        v10, v8));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v9, v8);
        assertThat(e2.getValueList()).containsExactly(v10, v11, v3, v4);
        assertThat(e3.getValueList()).containsExactly(v5, v6, v7, v12);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 4);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 4);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e3, 0, 3);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 4);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 4);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e3, 0, 3);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e3, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e3, 0, 4);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4);
        assertThat(e2.getValueList()).containsExactly(v5, v6, v7, v8);
        assertThat(e3.getValueList()).containsExactly(v9, v10, v11, v12);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 4);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 4);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e3, 0, 3);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 4);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 4);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e3, 0, 3);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e2, 0, 4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e3, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e2, 0, 4);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e3, 0, 4);
    }

    @Test
    void test3OptLong() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v10, v1,
                        v3, v4,
                        v8, v9),
                List.of(v1, v4,
                        v3, v9,
                        v8, v10));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v9, v10, v8, v7, v6, v5, v4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 10);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 10);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 10);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 10);
    }

    @Test
    void test4Opt() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(
                        v1, v2,
                        v4, v3,
                        v7, v8,
                        v6, v5),
                List.of(
                        v2, v4,
                        v3, v7,
                        v8, v6,
                        v5, v1));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v5, v4, v2, v3, v7, v6, v8);

        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 1, 7);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 1, 7);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 8);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 8);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7, v8);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 1, 7);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 1, 7);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 8);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 8);
    }

    @Test
    void test4OptLong() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v1, v12,
                        v2, v3,
                        v5, v6,
                        v9, v10),
                List.of(
                        v1, v3,
                        v2, v6,
                        v5, v10,
                        v9, v12));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v3, v4, v5, v10, v11, v12, v9, v8, v7, v6, v2);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 12);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 12);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 12);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 12);
    }

    @Test
    void testInverted4OptLong() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12));
        scoreDirector.setWorkingSolution(solution);

        // Note: using only endpoints work (removing v4, v7, v8, v11) from the above list works
        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(
                        v2, v3,
                        v6, v5,
                        v10, v9,
                        v12, v1),
                List.of(
                        v2, v5,
                        v6, v10,
                        v9, v1,
                        v12, v3));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v5, v4, v3, v12, v11, v10, v6, v7, v8, v9);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 5, 12);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 0);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 5);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 5, 12);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 0);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 5);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 12);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 12);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 5, 12);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 0);
        //verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 5);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 5, 12);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 0);
        //verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 5);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 12);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 12);
    }

    @Test
    void testDoubleBridge4Opt() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v8, v1,
                        v4, v5,
                        v2, v3,
                        v6, v7),
                List.of(
                        v1, v4,
                        v6, v3,
                        v5, v8,
                        v7, v2));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v7, v8, v5, v6, v3, v4);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 8);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 8);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7, v8);
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 8);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 8);
    }

    @Test
    void testDoubleBridge4OptLong() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v12, v1,
                        v5, v6,
                        v2, v3,
                        v8, v9),
                List.of(
                        v1, v5,
                        v8, v3,
                        v6, v12,
                        v9, v2));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        var undoMove = kOptListMove.doMove(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v9, v10, v11, v12, v6, v7, v8, v3, v4, v5);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 12);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 12);

        reset(scoreDirector);
        undoMove.doMoveOnly(scoreDirector);

        assertThat(e1.getValueList()).containsExactly(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 12);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 12);
    }

    @Test
    void testIsFeasible() {
        var e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
        scoreDirector.setWorkingSolution(solution);

        var kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(
                        v1, v2,
                        v4, v3,
                        v7, v8,
                        v6, v5),
                List.of(
                        v2, v4,
                        v3, v7,
                        v8, v6,
                        v5, v1));
        // this move create 1 cycle (v1 -> v5 -> v4 -> v2 -> v3 -> v7 -> v6 -> v8 -> v1 -> ...)
        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();

        e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v8, v7, v5, v6);

        kOptListMove = fromRemovedAndAddedEdges(scoreDirector, variableDescriptor,
                List.of(
                        v1, v2,
                        v3, v4,
                        v7, v8,
                        v6, v5),
                List.of(
                        v2, v3,
                        v4, v7,
                        v8, v6,
                        v5, v1));
        // this move create 2 cycles (v2...v3->t2...) and (v4...v5->v7...v6->v1...v8->v4...)
        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isFalse();
    }

    /**
     * Create a sequential or non-sequential k-opt from the supplied pairs of undirected removed and added edges.
     *
     * @param <Solution_>
     * @param scoreDirector
     * @param listVariableDescriptor
     * @param removedEdgeList The edges to remove. For each pair {@code (edgePairs[2*i], edgePairs[2*i+1])},
     *        it must be the case {@code edgePairs[2*i+1]} is either the successor or predecessor of
     *        {@code edgePairs[2*i]}. Additionally, each edge must belong to the given entity's
     *        list variable.
     * @param addedEdgeList The edges to add. Must contain only endpoints specified in the removedEdgeList.
     * @return A new sequential or non-sequential k-opt move with the specified undirected edges removed and added.
     */
    private static <Solution_> KOptListMove<Solution_> fromRemovedAndAddedEdges(
            InnerScoreDirector<Solution_, ?> scoreDirector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            List<TestdataListValue> removedEdgeList,
            List<TestdataListValue> addedEdgeList) {
        return fromRemovedAndAddedEdges(scoreDirector, listVariableDescriptor, listVariableDescriptor, removedEdgeList,
                addedEdgeList);
    }

    private static <Solution_> KOptListMove<Solution_> fromRemovedAndAddedEdges(
            InnerScoreDirector<Solution_, ?> scoreDirector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableDescriptor<Solution_> listVariableDescriptorSpy,
            List<TestdataListValue> removedEdgeList,
            List<TestdataListValue> addedEdgeList) {

        if (addedEdgeList.size() != removedEdgeList.size()) {
            throw new IllegalArgumentException(
                    "addedEdgeList (" + addedEdgeList + ") and removedEdgeList (" + removedEdgeList + ") have the same size");
        }

        if ((addedEdgeList.size() % 2) != 0) {
            throw new IllegalArgumentException(
                    "addedEdgeList and removedEdgeList are invalid: there is an odd number of endpoints.");
        }

        if (!addedEdgeList.containsAll(removedEdgeList)) {
            throw new IllegalArgumentException("addedEdgeList (" + addedEdgeList + ") is invalid; it contains endpoints "
                    + "that are not included in the removedEdgeList (" + removedEdgeList + ").");
        }

        var pickedValues = removedEdgeList.toArray(TestdataListValue[]::new);

        var listVariableDataSupply = scoreDirector.getSupplyManager().demand(listVariableDescriptor.getStateDemand());
        listVariableDataSupply = spy(listVariableDataSupply);
        when(listVariableDataSupply.getSourceVariableDescriptor()).thenReturn(listVariableDescriptorSpy);

        Function<TestdataListValue, TestdataListValue> successorFunction =
                getSuccessorFunction(listVariableDescriptorSpy, listVariableDataSupply);

        for (var i = 0; i < removedEdgeList.size(); i += 2) {
            if (successorFunction.apply(removedEdgeList.get(i)) != removedEdgeList.get(i + 1)
                    && successorFunction.apply(removedEdgeList.get(i + 1)) != removedEdgeList.get(i)) {
                throw new IllegalArgumentException("removedEdgeList (" + removedEdgeList + ") contains an invalid edge ((" +
                        removedEdgeList.get(i) + ", " + removedEdgeList.get(i + 1) + ")).");
            }
        }

        var tourArray = new TestdataListValue[removedEdgeList.size() + 1];
        var incl = new int[removedEdgeList.size() + 1];
        for (var i = 0; i < removedEdgeList.size(); i += 2) {
            tourArray[i + 1] = removedEdgeList.get(i);
            tourArray[i + 2] = removedEdgeList.get(i + 1);
            var addedEdgeIndex = identityIndexOf(addedEdgeList, removedEdgeList.get(i));

            if (addedEdgeIndex % 2 == 0) {
                incl[i + 1] = identityIndexOf(removedEdgeList, addedEdgeList.get(addedEdgeIndex + 1)) + 1;
            } else {
                incl[i + 1] = identityIndexOf(removedEdgeList, addedEdgeList.get(addedEdgeIndex - 1)) + 1;
            }

            addedEdgeIndex = identityIndexOf(addedEdgeList, removedEdgeList.get(i + 1));
            if (addedEdgeIndex % 2 == 0) {
                incl[i + 2] = identityIndexOf(removedEdgeList, addedEdgeList.get(addedEdgeIndex + 1)) + 1;
            } else {
                incl[i + 2] = identityIndexOf(removedEdgeList, addedEdgeList.get(addedEdgeIndex - 1)) + 1;
            }
        }

        var descriptor = new KOptDescriptor<>(tourArray, incl,
                getMultiEntitySuccessorFunction(pickedValues, listVariableDataSupply),
                getMultiEntityBetweenPredicate(pickedValues, listVariableDataSupply));
        return descriptor.getKOptListMove(listVariableDataSupply);
    }

    private static <Node_> Function<Node_, Node_> getSuccessorFunction(ListVariableDescriptor<?> listVariableDescriptor,
            ListVariableStateSupply<?> listVariableStateSupply) {
        return (node) -> {
            var entity = listVariableStateSupply.getInverseSingleton(node);
            var valueList = (List<Node_>) listVariableDescriptor.getValue(entity);
            var index = listVariableStateSupply.getIndex(node);
            if (index == valueList.size() - 1) {
                var firstUnpinnedIndex = listVariableDescriptor.getFirstUnpinnedIndex(entity);
                return valueList.get(firstUnpinnedIndex);
            } else {
                return valueList.get(index + 1);
            }
        };
    }

    private static int identityIndexOf(List<TestdataListValue> sourceList, TestdataListValue query) {
        for (var i = 0; i < sourceList.size(); i++) {
            if (sourceList.get(i) == query) {
                return i;
            }
        }
        return -1;
    }

}
