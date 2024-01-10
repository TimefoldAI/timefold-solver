package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptUtils.getMultiEntityBetweenPredicate;
import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptUtils.getMultiEntitySuccessorFunction;
import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt.KOptUtils.getSuccessorFunction;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonListInverseVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v6, v1,
                        v2, v3,
                        v4, v5),
                List.of(v1, v3,
                        v2, v5,
                        v4, v6));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7);

        var variableDescriptorSpy = Mockito.spy(variableDescriptor);
        var entityDescriptor = Mockito.mock(EntityDescriptor.class);
        Mockito.when(variableDescriptorSpy.getEntityDescriptor()).thenReturn(entityDescriptor);
        Mockito.when(entityDescriptor.extractFirstUnpinnedIndex(e1)).thenReturn(1);
        Mockito.when(entityDescriptor.isMovable(null, e1)).thenReturn(true);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                variableDescriptorSpy,
                List.of(v7, v2,
                        v3, v4,
                        v5, v6),
                List.of(v2, v4,
                        v3, v6,
                        v5, v7));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6);
        TestdataListEntity destinationE1 = TestdataListEntity.createWithValues("e1", destinationV1, destinationV2,
                destinationV3, destinationV4, destinationV5, destinationV6);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        SupplyManager supplyManager = Mockito.mock(SupplyManager.class);
        SingletonInverseVariableSupply inverseVariableSupply = Mockito.mock(SingletonInverseVariableSupply.class);

        when(destinationScoreDirector.getSupplyManager()).thenReturn(supplyManager);
        when(supplyManager.demand(Mockito.any(SingletonListInverseVariableDemand.class))).thenReturn(inverseVariableSupply);
        when(inverseVariableSupply.getInverseSingleton(destinationE1.getValueList().get(0))).thenReturn(destinationE1);

        KOptListMove<TestdataListSolution> rebasedMove = kOptListMove.rebase(destinationScoreDirector);
        assertThat(rebasedMove.isMoveDoable(destinationScoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = rebasedMove.doMove(destinationScoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v6);
        TestdataListEntity e2 = TestdataListEntity.createWithValues("e2", v4, v5);

        TestdataListEntity destinationE1 =
                TestdataListEntity.createWithValues("e1", destinationV1, destinationV2, destinationV3, destinationV6);
        TestdataListEntity destinationE2 = TestdataListEntity.createWithValues("e2", destinationV4, destinationV5);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        SupplyManager supplyManager = Mockito.mock(SupplyManager.class);
        SingletonInverseVariableSupply inverseVariableSupply = Mockito.mock(SingletonInverseVariableSupply.class);

        when(destinationScoreDirector.getSupplyManager()).thenReturn(supplyManager);
        when(supplyManager.demand(Mockito.any(SingletonListInverseVariableDemand.class))).thenReturn(inverseVariableSupply);
        when(inverseVariableSupply.getInverseSingleton(destinationE1.getValueList().get(0))).thenReturn(destinationE1);
        when(inverseVariableSupply.getInverseSingleton(destinationE2.getValueList().get(0))).thenReturn(destinationE2);

        KOptListMove<TestdataListSolution> rebasedMove = kOptListMove.rebase(destinationScoreDirector);

        assertThat(rebasedMove.isMoveDoable(destinationScoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = rebasedMove.doMove(destinationScoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4);
        TestdataListEntity e2 = TestdataListEntity.createWithValues("e2", v5, v6, v7, v8);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v2, v3,
                        v6, v7),
                List.of(v2, v6,
                        v3, v7));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v6);
        TestdataListEntity e2 = TestdataListEntity.createWithValues("e2", v4, v5);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v6, v1,
                        v2, v3,
                        v4, v5),
                List.of(v1, v3,
                        v2, v5,
                        v4, v6));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v6, v7);
        TestdataListEntity e2 = TestdataListEntity.createWithValues("e2", v4, v5);

        var variableDescriptorSpy = Mockito.spy(variableDescriptor);
        var entityDescriptor = Mockito.mock(EntityDescriptor.class);
        Mockito.when(variableDescriptorSpy.getEntityDescriptor()).thenReturn(entityDescriptor);
        Mockito.when(entityDescriptor.extractFirstUnpinnedIndex(e1)).thenReturn(1);
        Mockito.when(entityDescriptor.isMovable(null, e1)).thenReturn(true);
        Mockito.when(entityDescriptor.isMovable(null, e2)).thenReturn(true);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                variableDescriptorSpy,
                List.of(v7, v2,
                        v3, v6,
                        v4, v5),
                List.of(v2, v6,
                        v3, v5,
                        v4, v7));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4);
        TestdataListEntity e2 = TestdataListEntity.createWithValues("e2", v5, v6, v7, v8);
        TestdataListEntity e3 = TestdataListEntity.createWithValues("e3", v9, v10, v11, v12);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
                variableDescriptor,
                List.of(v10, v1,
                        v3, v4,
                        v8, v9),
                List.of(v1, v4,
                        v3, v9,
                        v8, v10));

        assertThat(kOptListMove.isMoveDoable(scoreDirector)).isTrue();
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);
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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);

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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);

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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);

        // Note: using only endpoints work (removing v4, v7, v8, v11) from the above list works
        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);

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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);

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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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
        AbstractMove<TestdataListSolution> undoMove = kOptListMove.doMove(scoreDirector);

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
        TestdataListEntity e1 = TestdataListEntity.createWithValues("e1", v1, v2, v3, v4, v5, v6, v7, v8);

        KOptListMove<TestdataListSolution> kOptListMove = fromRemovedAndAddedEdges(scoreDirector,
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

        TestdataListValue[] pickedValues = removedEdgeList.toArray(TestdataListValue[]::new);

        IndexVariableSupply indexVariableSupply =
                scoreDirector.getSupplyManager().demand(new IndexVariableDemand<>(listVariableDescriptor));

        SingletonInverseVariableSupply inverseVariableSupply =
                scoreDirector.getSupplyManager().demand(new SingletonListInverseVariableDemand<>(listVariableDescriptor));

        Function<TestdataListValue, TestdataListValue> successorFunction =
                getSuccessorFunction(listVariableDescriptorSpy, inverseVariableSupply, indexVariableSupply);

        for (int i = 0; i < removedEdgeList.size(); i += 2) {
            if (successorFunction.apply(removedEdgeList.get(i)) != removedEdgeList.get(i + 1)
                    && successorFunction.apply(removedEdgeList.get(i + 1)) != removedEdgeList.get(i)) {
                throw new IllegalArgumentException("removedEdgeList (" + removedEdgeList + ") contains an invalid edge ((" +
                        removedEdgeList.get(i) + ", " + removedEdgeList.get(i + 1) + ")).");
            }
        }

        TestdataListValue[] tourArray = new TestdataListValue[removedEdgeList.size() + 1];
        int[] incl = new int[removedEdgeList.size() + 1];
        for (int i = 0; i < removedEdgeList.size(); i += 2) {
            tourArray[i + 1] = removedEdgeList.get(i);
            tourArray[i + 2] = removedEdgeList.get(i + 1);
            int addedEdgeIndex = identityIndexOf(addedEdgeList, removedEdgeList.get(i));

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

        KOptDescriptor<TestdataListValue> descriptor = new KOptDescriptor<>(tourArray,
                incl,
                getMultiEntitySuccessorFunction(pickedValues,
                        listVariableDescriptorSpy,
                        inverseVariableSupply,
                        indexVariableSupply),
                getMultiEntityBetweenPredicate(pickedValues,
                        listVariableDescriptorSpy,
                        inverseVariableSupply,
                        indexVariableSupply));
        return descriptor.getKOptListMove(listVariableDescriptorSpy, indexVariableSupply, inverseVariableSupply);
    }

    private static int identityIndexOf(List<TestdataListValue> sourceList, TestdataListValue query) {
        for (int i = 0; i < sourceList.size(); i++) {
            if (sourceList.get(i) == query) {
                return i;
            }
        }
        return -1;
    }

}
