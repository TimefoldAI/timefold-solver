package ai.timefold.solver.core.impl.domain.entity.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.pinned.solutionargument.TestdataPinFromSolutionEntity;
import ai.timefold.solver.core.testdomain.pinned.solutionargument.TestdataPinFromSolutionInvalidSolution;
import ai.timefold.solver.core.testdomain.pinned.solutionargument.TestdataPinFromSolutionSolution;

import org.junit.jupiter.api.Test;

class PlanningPinSolutionArgumentTest {

    @Test
    void solutionSetterRunsOnceThenReadsCachedBoolean() {
        var solution = solution(10, 5);
        var entity = solution.getEntityList().get(0);
        try (var scoreDirector = scoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            var entityDescriptor = scoreDirector.getSolutionDescriptor()
                    .findEntityDescriptorOrFail(TestdataPinFromSolutionEntity.class);

            assertThat(entity.getSolutionSetterCallCount()).isEqualTo(1);
            assertThat(entity.isPinned()).isTrue();
            assertThat(entityDescriptor.isMovable(solution, entity)).isFalse();
            assertThat(entityDescriptor.isMovable(solution, entity)).isFalse();
            assertThat(entity.getSolutionSetterCallCount()).isEqualTo(1);
        }
    }

    @Test
    void problemChangeOnEntityRefreshesCachedPin() {
        var solution = solution(10, 5);
        var entity = solution.getEntityList().get(0);
        try (var scoreDirector = scoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            assertThat(entity.isPinned()).isTrue();

            var problemChangeDirector = new DefaultProblemChangeDirector<>(scoreDirector);
            problemChangeDirector.changeProblemProperty(entity, workingEntity -> workingEntity.setStartTime(15));

            assertThat(entity.getSolutionSetterCallCount()).isEqualTo(2);
            assertThat(entity.isPinned()).isFalse();
            assertThat(scoreDirector.getSolutionDescriptor()
                    .findEntityDescriptorOrFail(TestdataPinFromSolutionEntity.class)
                    .isMovable(solution, entity)).isTrue();
            assertThat(entity.getSolutionSetterCallCount()).isEqualTo(2);
        }
    }

    @Test
    void problemChangeOnProblemFactRefreshesEveryPin() {
        var solution = solution(10, 5);
        var entity = solution.getEntityList().get(0);
        var value = solution.getValueList().get(0);
        try (var scoreDirector = scoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            assertThat(entity.isPinned()).isTrue();

            var problemChangeDirector = new DefaultProblemChangeDirector<>(scoreDirector);
            problemChangeDirector.changeProblemProperty(value,
                    ignored -> scoreDirector.getWorkingSolution().setPlanningWindowStart(0));

            assertThat(entity.getSolutionSetterCallCount()).isEqualTo(2);
            assertThat(entity.isPinned()).isFalse();
        }
    }

    @Test
    void booleanSetterIsNotCalledWithTheSolution() {
        var solution = TestdataPinnedSolution.generateSolution(1, 1);
        var entity = solution.getEntityList().get(0);
        entity.setPinned(true);
        var solutionDescriptor = TestdataPinnedSolution.buildSolutionDescriptor();
        var scoreDirectorFactory = new EasyScoreDirectorFactory<TestdataPinnedSolution, SimpleScore>(solutionDescriptor,
                ignored -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT);
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            assertThat(entity.isPinned()).isTrue();
            assertThat(solutionDescriptor.findEntityDescriptorOrFail(TestdataPinnedEntity.class)
                    .isMovable(solution, entity)).isFalse();
        }
    }

    @Test
    void setterParameterMustBeTheSolution() {
        assertThatThrownBy(TestdataPinFromSolutionInvalidSolution::buildSolutionDescriptor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("parameter type")
                .hasMessageContaining(String.class.getCanonicalName());
    }

    @Test
    void gizmoAccessorInvokesSolutionSetter() throws NoSuchMethodException {
        var method = TestdataPinFromSolutionEntity.class.getMethod("setPinned", TestdataPinFromSolutionSolution.class);
        var accessor = new MemberAccessorFactory().buildAndCacheMemberAccessor(method,
                MemberAccessorType.VOID_METHOD_WITH_PARAMETER, null, DomainAccessType.FORCE_GIZMO);
        var solution = solution(10, 5);
        var entity = solution.getEntityList().get(0);

        accessor.executeGetter(entity, solution);

        assertThat(entity.getSolutionSetterCallCount()).isEqualTo(1);
        assertThat(entity.isPinned()).isTrue();
    }

    private static TestdataPinFromSolutionSolution solution(int planningWindowStart, int entityStartTime) {
        var solution = new TestdataPinFromSolutionSolution("s");
        solution.setPlanningWindowStart(planningWindowStart);
        var value = new TestdataValue("v1");
        var entity = new TestdataPinFromSolutionEntity("e1", entityStartTime);
        entity.setValue(value);
        solution.setValueList(List.of(value));
        solution.setEntityList(List.of(entity));
        return solution;
    }

    private static AbstractScoreDirector<TestdataPinFromSolutionSolution, SimpleScore, ?> scoreDirector() {
        var solutionDescriptor = TestdataPinFromSolutionSolution.buildSolutionDescriptor();
        var scoreDirectorFactory =
                new EasyScoreDirectorFactory<TestdataPinFromSolutionSolution, SimpleScore>(solutionDescriptor,
                        ignored -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT);
        return scoreDirectorFactory.createScoreDirectorBuilder(EnvironmentMode.PHASE_ASSERT)
                .withLookUpEnabled(true)
                .build();
    }

}
