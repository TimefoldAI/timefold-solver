package ai.timefold.solver.core.preview.api.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

class MoveRunnerTest {

    @Test
    void buildWithNullSolutionClass() {
        assertThatNullPointerException().isThrownBy(() -> MoveRunner.build(null, TestdataEntity.class))
                .withMessageContaining("solutionClass");
    }

    @Test
    void buildWithEmptyEntityClasses() {
        assertThatIllegalArgumentException().isThrownBy(() -> MoveRunner.build(TestdataSolution.class))
                .withMessageContaining("entityClasses must not be empty");
    }

    @Test
    void buildWithNullEntityClasses() {
        assertThatNullPointerException().isThrownBy(() -> MoveRunner.build(TestdataSolution.class, (Class<?>[]) null))
                .withMessageContaining("entityClasses");
    }

    @Test
    void buildSucceeds() {
        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        assertThat(runner).isNotNull();
    }

    @Test
    void usingWithNullSolution() {
        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        assertThatNullPointerException().isThrownBy(() -> runner.using(null)).withMessageContaining("solution");
    }

    @Test
    void usingSucceeds() {
        var solution = TestdataSolution.generateSolution(2, 3);
        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        var context = runner.using(solution);
        assertThat(context).isNotNull();
    }

    @Test
    void executeSimpleMove() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value1 = entity1.getValue();
        var value2 = entity2.getValue();

        // Get the meta model for the planning variable
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        // Create a simple swap move
        Move<TestdataSolution> swapMove = view -> {
            var val1 = view.getValue(variableMetaModel, entity1);
            var val2 = view.getValue(variableMetaModel, entity2);

            view.changeVariable(variableMetaModel, entity1, val2);
            view.changeVariable(variableMetaModel, entity2, val1);
        };

        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        runner.using(solution).execute(swapMove);

        // Verify the swap occurred
        assertThat(entity1.getValue()).isEqualTo(value2);
        assertThat(entity2.getValue()).isEqualTo(value1);
    }

    @Test
    void executeWithNullMove() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        var context = runner.using(solution);
        assertThatNullPointerException().isThrownBy(() -> context.execute(null)).withMessageContaining("move");
    }

    @Test
    void executeTemporarilyWithUndo() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        Move<TestdataSolution> swapMove = view -> {
            var val1 = view.getValue(variableMetaModel, entity1);
            var val2 = view.getValue(variableMetaModel, entity2);
            view.changeVariable(variableMetaModel, entity1, val2);
            view.changeVariable(variableMetaModel, entity2, val1);
        };

        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        runner.using(solution).executeTemporarily(swapMove, view -> {
            // During temporary execution, values should be swapped
            assertThat(entity1.getValue()).isEqualTo(originalValue2);
            assertThat(entity2.getValue()).isEqualTo(originalValue1);
        });

        // After executeTemporarily, values should be restored
        assertThat(entity1.getValue()).isEqualTo(originalValue1);
        assertThat(entity2.getValue()).isEqualTo(originalValue2);
    }

    @Test
    void executeTemporarilyWithNullMove() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        var context = runner.using(solution);
        assertThatNullPointerException().isThrownBy(() -> context.executeTemporarily(null, view -> {
        })).withMessageContaining("move");
    }

    @Test
    void executeTemporarilyWithNullCallback() {
        var solution = TestdataSolution.generateSolution(2, 2);
        Move<TestdataSolution> dummyMove = view -> {
        };

        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        var context = runner.using(solution);
        assertThatNullPointerException().isThrownBy(() -> context.executeTemporarily(dummyMove, null))
                .withMessageContaining("assertions");
    }

    @Test
    void executeTemporarilyWithComplexMove() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entities = solution.getEntityList();
        var originalValues = entities.stream().map(TestdataEntity::getValue).toList();

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        // Move that rotates values across all entities
        Move<TestdataSolution> rotateMove = view -> {
            var values = entities.stream().map(e -> view.getValue(variableMetaModel, e)).toList();

            for (int i = 0; i < entities.size(); i++) {
                var nextValue = values.get((i + 1) % values.size());
                view.changeVariable(variableMetaModel, entities.get(i), nextValue);
            }
        };

        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        runner.using(solution).executeTemporarily(rotateMove, view -> {
            // Verify rotation occurred
            for (int i = 0; i < entities.size(); i++) {
                var expectedValue = originalValues.get((i + 1) % originalValues.size());
                assertThat(entities.get(i).getValue()).isEqualTo(expectedValue);
            }
        });

        // Verify complete restoration
        for (int i = 0; i < entities.size(); i++) {
            assertThat(entities.get(i).getValue()).isEqualTo(originalValues.get(i));
        }
    }

}
