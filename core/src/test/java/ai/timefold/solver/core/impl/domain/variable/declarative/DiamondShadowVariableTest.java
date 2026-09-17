package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.diamond.TestdataDiamondEntity;
import ai.timefold.solver.core.testdomain.shadow.diamond.TestdataDiamondSolution;
import ai.timefold.solver.core.testdomain.shadow.diamond.TestdataDiamondValue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiamondShadowVariableTest {

    @Test
    void testUpdateOnce() {
        var entity = Mockito.spy(new TestdataDiamondEntity("A"));
        var value = new TestdataDiamondValue("1", 10, 5);

        var solution = new TestdataDiamondSolution("Solution", List.of(entity), List.of(value));

        var solutionMetaModel = TestdataDiamondSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataDiamondEntity.class)
                .basicVariable("value", TestdataDiamondValue.class);
        var context = MoveTester.build(solutionMetaModel)
                .using(solution);

        // Building the graph queues every node, and setting the solution marks them on top of that.
        verify(entity, Mockito.times(1)).durationSupplier();
        verify(entity, Mockito.times(1)).endTimeSupplier();

        Mockito.reset(entity);
        context.execute(Moves.change(variableMetaModel, entity, value));

        // endTime is both marked by the move and reached from duration.
        verify(entity, Mockito.times(1)).durationSupplier();
        verify(entity, Mockito.times(1)).endTimeSupplier();

        assertThat(entity.getDuration()).isEqualTo(5);
        assertThat(entity.getEndTime()).isEqualTo(15);
    }

}
