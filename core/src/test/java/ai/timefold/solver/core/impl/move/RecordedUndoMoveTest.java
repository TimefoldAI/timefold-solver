package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

import org.junit.jupiter.api.Test;

class RecordedUndoMoveTest {

    @Test
    void executeReplaysActionsInReverseRecordingOrder() {
        var callOrder = new ArrayList<String>();
        var action1 = variableChangeAction("action1", callOrder);
        var action2 = variableChangeAction("action2", callOrder);
        var action3 = variableChangeAction("action3", callOrder);
        var move = new RecordedUndoMove<>(List.of(action1, action2, action3));

        var scoreDirector = mock(VariableDescriptorAwareScoreDirector.class);
        var solutionView = mock(InnerMutableSolutionView.class);
        doAnswer(invocation -> scoreDirector).when(solutionView).getScoreDirector();

        move.execute(solutionView);

        assertThat(callOrder).containsExactly("action3", "action2", "action1");
    }

    @SuppressWarnings("unchecked")
    private ChangeAction<Object> variableChangeAction(String label, List<String> callOrder) {
        var variableDescriptor = mock(VariableDescriptor.class);
        doAnswer(invocation -> callOrder.add(label)).when(variableDescriptor).setValue(any(), any());
        return new VariableChangeAction<>(new Object(), new Object(), variableDescriptor);
    }

}
