package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveRunnerTest {

    // T014: Basic unit tests for build() validation
    @Test
    void buildWithNullSolutionClass() {
        assertThatNullPointerException()
                .isThrownBy(() -> MoveRunner.build(null, TestdataEntity.class))
                .withMessageContaining("solutionClass");
    }

    @Test
    void buildWithEmptyEntityClasses() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MoveRunner.build(TestdataSolution.class))
                .withMessageContaining("entityClasses must not be empty");
    }

    @Test
    void buildWithNullEntityClasses() {
        assertThatNullPointerException()
                .isThrownBy(() -> MoveRunner.build(TestdataSolution.class, (Class<?>[]) null))
                .withMessageContaining("entityClasses");
    }

    @Test
    void buildSucceeds() {
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            assertThat(runner).isNotNull();
        }
    }

    // T015: Unit tests for using() validation
    @Test
    void usingWithNullSolution() {
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            assertThatNullPointerException()
                    .isThrownBy(() -> runner.using(null))
                    .withMessageContaining("solution");
        }
    }

    @Test
    void usingSucceeds() {
        var solution = TestdataSolution.generateSolution(2, 3);
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            assertThat(context).isNotNull();
        }
    }

    // T016: Unit tests for execute(move) with simple custom move
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
        Move<TestdataSolution> swapMove = new Move<>() {
            @Override
            public void execute(MutableSolutionView<TestdataSolution> view) {
                var val1 = view.getValue(variableMetaModel, entity1);
                var val2 = view.getValue(variableMetaModel, entity2);

                view.changeVariable(variableMetaModel, entity1, val2);
                view.changeVariable(variableMetaModel, entity2, val1);
            }
        };

        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(swapMove);
        }

        // Verify the swap occurred
        assertThat(entity1.getValue()).isEqualTo(value2);
        assertThat(entity2.getValue()).isEqualTo(value1);
    }

    @Test
    void executeWithNullMove() {
        var solution = TestdataSolution.generateSolution(2, 2);
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            assertThatNullPointerException()
                    .isThrownBy(() -> context.execute(null))
                    .withMessageContaining("move");
        }
    }

    // T017: Unit tests for closed MoveRunner IllegalStateException
    @Test
    void usingAfterClose() {
        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        runner.close();

        var solution = TestdataSolution.generateSolution(2, 2);
        assertThatThrownBy(() -> runner.using(solution))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    // T018: Unit tests for resource cleanup verification
    @Test
    void closeIsIdempotent() {
        var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class);
        runner.close();
        // Second close should not throw
        runner.close();
    }

    @Test
    void tryWithResourcesAutoClose() {
        var solution = TestdataSolution.generateSolution(2, 2);

        MoveRunner<TestdataSolution> runner;
        try (var r = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            runner = r;
            var context = r.using(solution);
            assertThat(context).isNotNull();
        }

        // After try-with-resources, runner should be closed
        assertThatThrownBy(() -> runner.using(solution))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    // T029: Unit tests for executeTemporarily() with move execution and undo verification
    @Test
    void executeTemporarilyWithUndo() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        Move<TestdataSolution> swapMove = new Move<>() {
            @Override
            public void execute(MutableSolutionView<TestdataSolution> view) {
                var val1 = view.getValue(variableMetaModel, entity1);
                var val2 = view.getValue(variableMetaModel, entity2);
                view.changeVariable(variableMetaModel, entity1, val2);
                view.changeVariable(variableMetaModel, entity2, val1);
            }
        };

        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);

            context.executeTemporarily(swapMove, view -> {
                // During temporary execution, values should be swapped
                assertThat(entity1.getValue()).isEqualTo(originalValue2);
                assertThat(entity2.getValue()).isEqualTo(originalValue1);
            });
        }

        // After executeTemporarily, values should be restored
        assertThat(entity1.getValue()).isEqualTo(originalValue1);
        assertThat(entity2.getValue()).isEqualTo(originalValue2);
    }

    // T030: Unit tests for executeTemporarily() with null move and null callback validation
    @Test
    void executeTemporarilyWithNullMove() {
        var solution = TestdataSolution.generateSolution(2, 2);
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            assertThatNullPointerException()
                    .isThrownBy(() -> context.executeTemporarily(null, view -> {
                    }))
                    .withMessageContaining("move");
        }
    }

    @Test
    void executeTemporarilyWithNullCallback() {
        var solution = TestdataSolution.generateSolution(2, 2);
        Move<TestdataSolution> dummyMove = view -> {
        };

        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            assertThatNullPointerException()
                    .isThrownBy(() -> context.executeTemporarily(dummyMove, null))
                    .withMessageContaining("assertions");
        }
    }

    // T031: Unit tests for executeTemporarily() with complex move affecting multiple entities
    @Test
    void executeTemporarilyWithComplexMove() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entities = solution.getEntityList();
        var originalValues = entities.stream()
                .map(TestdataEntity::getValue)
                .toList();

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        // Move that rotates values across all entities
        Move<TestdataSolution> rotateMove = new Move<>() {
            @Override
            public void execute(MutableSolutionView<TestdataSolution> view) {
                var values = entities.stream()
                        .map(e -> view.getValue(variableMetaModel, e))
                        .toList();

                for (int i = 0; i < entities.size(); i++) {
                    var nextValue = values.get((i + 1) % values.size());
                    view.changeVariable(variableMetaModel, entities.get(i), nextValue);
                }
            }
        };

        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);

            context.executeTemporarily(rotateMove, view -> {
                // Verify rotation occurred
                for (int i = 0; i < entities.size(); i++) {
                    var expectedValue = originalValues.get((i + 1) % originalValues.size());
                    assertThat(entities.get(i).getValue()).isEqualTo(expectedValue);
                }
            });
        }

        // Verify complete restoration
        for (int i = 0; i < entities.size(); i++) {
            assertThat(entities.get(i).getValue()).isEqualTo(originalValues.get(i));
        }
    }

    // T032: Unit tests for execute(move, exceptionHandler) with Exception handling and suppression
    @Test
    void executeWithExceptionHandler() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var exceptionList = new java.util.ArrayList<Exception>();

        Move<TestdataSolution> failingMove = view -> {
            throw new RuntimeException("Test exception");
        };

        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            // Should not throw - exception is handled
            context.execute(failingMove, exceptionList::add);
        }

        assertThat(exceptionList).hasSize(1);
        assertThat(exceptionList.get(0))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");
    }

    @Test
    void executeWithNullExceptionHandler() {
        var solution = TestdataSolution.generateSolution(2, 2);
        Move<TestdataSolution> dummyMove = view -> {
        };

        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            assertThatNullPointerException()
                    .isThrownBy(() -> context.execute(dummyMove, null))
                    .withMessageContaining("exceptionHandler");
        }
    }

    // T033: Unit tests for execute(move, exceptionHandler) with Error propagation
    @Test
    void executeWithErrorPropagation() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var exceptionList = new java.util.ArrayList<Exception>();

        Move<TestdataSolution> failingMove = view -> {
            throw new OutOfMemoryError("Test error");
        };

        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            // Error should propagate, not be handled
            assertThatThrownBy(() -> context.execute(failingMove, exceptionList::add))
                    .isInstanceOf(OutOfMemoryError.class)
                    .hasMessage("Test error");
        }

        // Exception handler should not be called for Errors
        assertThat(exceptionList).isEmpty();
    }
}
