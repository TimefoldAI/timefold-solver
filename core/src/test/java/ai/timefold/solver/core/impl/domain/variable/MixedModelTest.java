package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.shadow.mixed.TestdataMixedEntity;
import ai.timefold.solver.core.testdomain.shadow.mixed.TestdataMixedSolution;
import ai.timefold.solver.core.testdomain.shadow.mixed.TestdataMixedValue;

import org.junit.jupiter.api.Test;

class MixedModelTest {

    @Test
    void changingVariableOfParentShouldChangeDependentVariableOfChildren() {
        var problem = new TestdataMixedSolution();

        var entity = new TestdataMixedEntity("a");

        var value1 = new TestdataMixedValue("v1");
        var value2 = new TestdataMixedValue("v2");

        problem.setMixedEntityList(List.of(entity));
        problem.setMixedValueList(List.of(value1, value2));
        problem.setDelayList(List.of(1, 2));

        entity.setValueList(List.of(value1, value2));

        value1.setDelay(1);
        value1.setEntity(entity);

        value2.setDelay(2);
        value2.setPrevious(value1);
        value2.setEntity(entity);
        value2.setPreviousDelay(1);

        var solutionMetaModel = PlanningSolutionMetaModel.of(TestdataMixedSolution.class,
                TestdataMixedEntity.class, TestdataMixedValue.class);
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataMixedValue.class)
                .basicVariable("delay", Integer.class);
        MoveRunner.build(solutionMetaModel)
                .using(problem)
                .executeTemporarily(Moves.change(variableMetaModel, value1, 2),
                        newSolution -> {
                            assertThat(value1.getDelay()).isEqualTo(2);
                            assertThat(value2.getPreviousDelay()).isEqualTo(2);
                        });
    }
}
