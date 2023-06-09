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

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class CompositeMoveTest {

    @Test
    void createUndoMove() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());
        DummyMove a = new DummyMove("a");
        DummyMove b = new DummyMove("b");
        DummyMove c = new DummyMove("c");
        CompositeMove<TestdataSolution> move = new CompositeMove<>(a, b, c);
        CompositeMove<TestdataSolution> undoMove = move.doMove(scoreDirector);
        assertAllCodesOfArray(move.getMoves(), "a", "b", "c");
        assertAllCodesOfArray(undoMove.getMoves(), "undo c", "undo b", "undo a");
    }

    @Test
    void createUndoMoveWithNonDoableMove() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());

        DummyMove a = new DummyMove("a");
        DummyMove b = new NotDoableDummyMove("b");
        DummyMove c = new DummyMove("c");
        CompositeMove<TestdataSolution> move = new CompositeMove<>(a, b, c);
        CompositeMove<TestdataSolution> undoMove = move.doMove(scoreDirector);
        assertAllCodesOfArray(move.getMoves(), "a", "b", "c");
        assertAllCodesOfArray(undoMove.getMoves(), "undo c", "undo a");

        a = new NotDoableDummyMove("a");
        b = new DummyMove("b");
        c = new NotDoableDummyMove("c");
        move = new CompositeMove<>(a, b, c);
        undoMove = move.doMove(scoreDirector);
        assertAllCodesOfArray(move.getMoves(), "a", "b", "c");
        assertAllCodesOfArray(undoMove.getMoves(), "undo b");
    }

    @Test
    void doMove() {
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());
        DummyMove a = mock(DummyMove.class);
        when(a.isMoveDoable(any())).thenReturn(true);
        DummyMove b = mock(DummyMove.class);
        when(b.isMoveDoable(any())).thenReturn(true);
        DummyMove c = mock(DummyMove.class);
        when(c.isMoveDoable(any())).thenReturn(true);
        CompositeMove<TestdataSolution> move = new CompositeMove<>(a, b, c);
        move.doMove(scoreDirector);
        verify(a, times(1)).doMove(scoreDirector);
        verify(b, times(1)).doMove(scoreDirector);
        verify(c, times(1)).doMove(scoreDirector);
    }

    @Test
    void rebase() {
        GenuineVariableDescriptor<TestdataSolution> variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();

        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataEntity e1 = new TestdataEntity("e1", v1);
        TestdataEntity e2 = new TestdataEntity("e2", null);
        TestdataEntity e3 = new TestdataEntity("e3", v1);

        TestdataValue destinationV1 = new TestdataValue("v1");
        TestdataValue destinationV2 = new TestdataValue("v2");
        TestdataEntity destinationE1 = new TestdataEntity("e1", destinationV1);
        TestdataEntity destinationE2 = new TestdataEntity("e2", null);
        TestdataEntity destinationE3 = new TestdataEntity("e3", destinationV1);

        ScoreDirector<TestdataSolution> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                });

        ChangeMove<TestdataSolution> a = new ChangeMove<>(variableDescriptor, e1, v2);
        ChangeMove<TestdataSolution> b = new ChangeMove<>(variableDescriptor, e2, v1);
        CompositeMove<TestdataSolution> rebaseMove = new CompositeMove<>(a, b).rebase(destinationScoreDirector);
        Move<TestdataSolution>[] rebasedChildMoves = rebaseMove.getMoves();
        assertThat(rebasedChildMoves).hasSize(2);
        ChangeMove<TestdataSolution> rebasedA = (ChangeMove<TestdataSolution>) rebasedChildMoves[0];
        assertThat(rebasedA.getEntity()).isSameAs(destinationE1);
        assertThat(rebasedA.getToPlanningValue()).isSameAs(destinationV2);
        ChangeMove<TestdataSolution> rebasedB = (ChangeMove<TestdataSolution>) rebasedChildMoves[1];
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
        DummyMove tmpMove = new DummyMove();
        Move<TestdataSolution> move = CompositeMove.buildMove(Collections.singletonList(tmpMove));
        assertThat(move)
                .isInstanceOf(DummyMove.class);

        move = CompositeMove.buildMove(tmpMove);
        assertThat(move)
                .isInstanceOf(DummyMove.class);
    }

    @Test
    void buildTwoElemMove() {
        DummyMove first = new DummyMove();
        NoChangeMove<TestdataSolution> second = new NoChangeMove<>();
        Move<TestdataSolution> move = CompositeMove.buildMove(Arrays.asList(first, second));
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
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());

        DummyMove first = new DummyMove();
        DummyMove second = new DummyMove();
        Move<TestdataSolution> move = CompositeMove.buildMove(first, second);
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
    void equals() {
        DummyMove first = new DummyMove();
        NoChangeMove<TestdataSolution> second = new NoChangeMove<>();
        Move<TestdataSolution> move = CompositeMove.buildMove(Arrays.asList(first, second));
        Move<TestdataSolution> other = CompositeMove.buildMove(first, second);
        assertThat(move).isEqualTo(other);

        move = CompositeMove.buildMove(first, second);
        other = CompositeMove.buildMove(second, first);
        assertThat(move).isNotEqualTo(other);
        assertThat(move).isNotEqualTo(new DummyMove());
        assertThat(move).isEqualTo(move);
    }

    @Test
    void interconnectedChildMoves() {
        TestdataSolution solution = new TestdataSolution("s1");
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataValue v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        TestdataEntity e1 = new TestdataEntity("e1", v1);
        TestdataEntity e2 = new TestdataEntity("e2", v2);
        solution.setEntityList(Arrays.asList(e1, e2));

        GenuineVariableDescriptor<TestdataSolution> variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        SwapMove<TestdataSolution> first = new SwapMove<>(Collections.singletonList(variableDescriptor), e1, e2);
        ChangeMove<TestdataSolution> second = new ChangeMove<>(variableDescriptor, e1, v3);
        Move<TestdataSolution> move = CompositeMove.buildMove(first, second);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);

        ScoreDirector<TestdataSolution> scoreDirector = mockScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor());
        Move<TestdataSolution> undoMove = move.doMove(scoreDirector);

        assertThat(e1.getValue()).isSameAs(v3);
        assertThat(e2.getValue()).isSameAs(v1);

        undoMove.doMove(scoreDirector);

        assertThat(e1.getValue()).isSameAs(v1);
        assertThat(e2.getValue()).isSameAs(v2);
    }

}
