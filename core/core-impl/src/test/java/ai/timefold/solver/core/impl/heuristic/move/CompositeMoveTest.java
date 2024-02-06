package ai.timefold.solver.core.impl.heuristic.move;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfArray;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class CompositeMoveTest {

    @Test
    void createUndoMove() {
        var scoreDirector = PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());
        var a = new DummyMove("a");
        var b = new DummyMove("b");
        var c = new DummyMove("c");
        var move = new CompositeMove<>(a, b, c);
        var undoMove = (CompositeMove<TestdataSolution>) move.doMove(scoreDirector);
        assertAllCodesOfArray(move.getMoves(), "a", "b", "c");
        assertAllCodesOfArray(undoMove.getMoves(), "undo c", "undo b", "undo a");
    }

    @Test
    void createUndoMoveWithNonDoableMove() {
        var scoreDirector = PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());

        var a = new DummyMove("a");
        var b = (DummyMove) new NotDoableDummyMove("b");
        var c = new DummyMove("c");
        var move = new CompositeMove<>(a, b, c);
        var undoMove = (CompositeMove<TestdataSolution>) move.doMove(scoreDirector);
        assertAllCodesOfArray(move.getMoves(), "a", "b", "c");
        assertAllCodesOfArray(undoMove.getMoves(), "undo c", "undo a");

        a = new NotDoableDummyMove("a");
        b = new DummyMove("b");
        c = new NotDoableDummyMove("c");
        move = new CompositeMove<>(a, b, c);
        var undoMove2 = move.doMove(scoreDirector);
        assertAllCodesOfArray(move.getMoves(), "a", "b", "c");
        assertThat(undoMove2).isInstanceOf(DummyMove.class); // The only doable move, Composite was stripped.
    }

    @Test
    void doMove() {
        var scoreDirector = PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());
        var a = mock(DummyMove.class);
        when(a.isMoveDoable(any())).thenReturn(true);
        var b = mock(DummyMove.class);
        when(b.isMoveDoable(any())).thenReturn(true);
        var c = mock(DummyMove.class);
        when(c.isMoveDoable(any())).thenReturn(true);
        var move = new CompositeMove<>(a, b, c);
        move.doMove(scoreDirector);
        verify(a, times(1)).doMove(scoreDirector);
        verify(b, times(1)).doMove(scoreDirector);
        verify(c, times(1)).doMove(scoreDirector);
    }

    @Test
    void rebase() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", null);
        var e3 = new TestdataEntity("e3", v1);

        var destinationV1 = new TestdataValue("v1");
        var destinationV2 = new TestdataValue("v2");
        var destinationE1 = new TestdataEntity("e1", destinationV1);
        var destinationE2 = new TestdataEntity("e2", null);
        var destinationE3 = new TestdataEntity("e3", destinationV1);

        ScoreDirector<TestdataSolution> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                });

        var a = new ChangeMove<>(variableDescriptor, e1, v2);
        var b = new ChangeMove<>(variableDescriptor, e2, v1);
        var rebaseMove = new CompositeMove<>(a, b).rebase(destinationScoreDirector);
        var rebasedChildMoves = rebaseMove.getMoves();
        assertThat(rebasedChildMoves).hasSize(2);
        var rebasedA = (ChangeMove<TestdataSolution>) rebasedChildMoves[0];
        assertThat(rebasedA.getEntity()).isSameAs(destinationE1);
        assertThat(rebasedA.getToPlanningValue()).isSameAs(destinationV2);
        var rebasedB = (ChangeMove<TestdataSolution>) rebasedChildMoves[1];
        assertThat(rebasedB.getEntity()).isSameAs(destinationE2);
        assertThat(rebasedB.getToPlanningValue()).isSameAs(destinationV1);
    }

    @Test
    void buildEmptyMove() {
        assertThat(CompositeMove.buildMove(new ArrayList<>()))
                .isInstanceOf(NoChangeMove.class);
        assertThat(CompositeMove.buildMove())
                .isInstanceOf(NoChangeMove.class);
    }

    @Test
    void buildOneElemMove() {
        var tmpMove = new DummyMove();
        var move = CompositeMove.buildMove(Collections.singletonList(tmpMove));
        assertThat(move)
                .isInstanceOf(DummyMove.class);

        move = CompositeMove.buildMove(tmpMove);
        assertThat(move)
                .isInstanceOf(DummyMove.class);
    }

    @Test
    <Solution_> void buildTwoElemMove() {
        Move<Solution_> first = (Move<Solution_>) new DummyMove();
        Move<Solution_> second = NoChangeMove.getInstance();
        var move = CompositeMove.buildMove(first, second);
        assertThat(move)
                .isInstanceOf(CompositeMove.class);
        assertThat(((CompositeMove<TestdataSolution>) move).getMoves()[0])
                .isInstanceOf(DummyMove.class);
        assertThat(((CompositeMove<TestdataSolution>) move).getMoves()[1])
                .isInstanceOf(NoChangeMove.class);

        move = CompositeMove.buildMove(first, second);
        assertThat(move)
                .isInstanceOf(CompositeMove.class);
        assertThat(((CompositeMove<TestdataSolution>) move).getMoves()[0])
                .isInstanceOf(DummyMove.class);
        assertThat(((CompositeMove<TestdataSolution>) move).getMoves()[1])
                .isInstanceOf(NoChangeMove.class);
    }

    @Test
    void isMoveDoable() {
        var scoreDirector = PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());

        var first = new DummyMove();
        var second = new DummyMove();
        var move = CompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        first = new DummyMove();
        second = new NotDoableDummyMove();
        move = CompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        first = new NotDoableDummyMove();
        second = new DummyMove();
        move = CompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isTrue();

        first = new NotDoableDummyMove();
        second = new NotDoableDummyMove();
        move = CompositeMove.buildMove(first, second);
        assertThat(move.isMoveDoable(scoreDirector)).isFalse();
    }

    @Test
    <Solution_> void equals() {
        var first = (Move<Solution_>) new DummyMove();
        var second = (Move<Solution_>) NoChangeMove.getInstance();
        var move = CompositeMove.buildMove(Arrays.asList(first, second));
        var other = CompositeMove.buildMove(first, second);
        assertThat(move).isEqualTo(other);

        move = CompositeMove.buildMove(first, second);
        other = CompositeMove.buildMove(second, first);
        assertThat(move).isNotEqualTo(other);
        assertThat(move).isNotEqualTo(new DummyMove());
        assertThat(move).isEqualTo(move);
    }

    @Test
    void interconnectedChildMoves() {
        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        var e1 = new TestdataEntity("e1", v1);
        var e2 = new TestdataEntity("e2", v2);
        solution.setEntityList(Arrays.asList(e1, e2));

        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        var first = new SwapMove<>(Collections.singletonList(variableDescriptor), e1, e2);
        var second = new ChangeMove<>(variableDescriptor, e1, v3);
        var move = CompositeMove.buildMove(first, second);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);

        var scoreDirector = mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());
        var undoMove = move.doMove(scoreDirector);

        assertThat(e1.getValue()).isSameAs(v3);
        assertThat(e2.getValue()).isSameAs(v1);

        undoMove.doMove(scoreDirector);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);
    }

}
