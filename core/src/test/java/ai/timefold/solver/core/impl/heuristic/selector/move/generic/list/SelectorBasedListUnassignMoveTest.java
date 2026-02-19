package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class SelectorBasedListUnassignMoveTest {

    @Test
    void doMove() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var e1 = new TestdataListEntity("e1", v1, v2, v3);

        var scoreDirector = (InnerScoreDirector<TestdataListSolution, SimpleScore>) mock(InnerScoreDirector.class);
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();

        // Unassign last
        var move = new SelectorBasedListUnassignMove<>(variableDescriptor, e1, 2);
        move.execute(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 3);
        verify(scoreDirector).beforeListVariableElementUnassigned(variableDescriptor, v3);
        verify(scoreDirector).afterListVariableElementUnassigned(variableDescriptor, v3);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 2);
        verifyNoMoreInteractions(scoreDirector);

        // Unassign the rest
        new SelectorBasedListUnassignMove<>(variableDescriptor, e1, 0).execute(scoreDirector);
        new SelectorBasedListUnassignMove<>(variableDescriptor, e1, 0).execute(scoreDirector);
        assertThat(e1.getValueList()).isEmpty();
    }

    @Test
    void undoMove() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var e1 = new TestdataListEntity("e1", v1, v2, v3);

        var scoreDirector = (InnerScoreDirector<TestdataListSolution, SimpleScore>) mock(InnerScoreDirector.class);
        var variableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();

        var move = new SelectorBasedListUnassignMove<>(variableDescriptor, e1, 2);
        move.execute(scoreDirector);
        assertThat(e1.getValueList()).hasSize(2);
        verify(scoreDirector).beforeListVariableElementUnassigned(variableDescriptor, v3);
        verify(scoreDirector).afterListVariableElementUnassigned(variableDescriptor, v3);
    }

    @Test
    void rebase() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var source = new TestdataEntity();
        var destination = new TestdataEntity();
        var destinationScoreDirector = mockRebasingScoreDirector(solutionDescriptor, new Object[][] {
                { source, destination },
        });
        var move = new SelectorBasedListUnassignMove<TestdataSolution>(null, source, 0);
        var rebasedMove = move.rebase(destinationScoreDirector.getMoveDirector());
        assertThat(rebasedMove).isNotSameAs(move);
    }

    @Test
    void toStringTest() {
        var v1 = new TestdataListValue("1");
        var e1 = new TestdataListEntity("E1", v1);

        var variableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();

        assertThat(new SelectorBasedListUnassignMove<>(variableDescriptor, e1, 0)).hasToString("1 {E1[0] -> null}");
    }
}
