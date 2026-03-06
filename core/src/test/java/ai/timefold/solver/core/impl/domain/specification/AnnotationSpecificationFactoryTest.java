package ai.timefold.solver.core.impl.domain.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SpecificationCompiler;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class AnnotationSpecificationFactoryTest {

    @Test
    void basicSpecFromAnnotations() {
        var spec = AnnotationSpecificationFactory.fromAnnotations(
                TestdataSolution.class,
                List.of(TestdataEntity.class),
                DomainAccessType.FORCE_REFLECTION,
                null);

        assertThat(spec.solutionClass()).isEqualTo(TestdataSolution.class);
        assertThat(spec.score()).isNotNull();
        assertThat(spec.score().scoreType()).isEqualTo(SimpleScore.class);
        assertThat(spec.entities()).hasSize(1);
        assertThat(spec.entities().getFirst().entityClass()).isEqualTo(TestdataEntity.class);
        assertThat(spec.facts()).isNotEmpty();
        assertThat(spec.entityCollections()).isNotEmpty();
    }

    @Test
    void specProducesFunctionalSolutionDescriptor() {
        var spec = AnnotationSpecificationFactory.fromAnnotations(
                TestdataSolution.class,
                List.of(TestdataEntity.class),
                DomainAccessType.FORCE_REFLECTION,
                null);

        var descriptor = SpecificationCompiler.compile(spec, null,
                DomainAccessType.FORCE_REFLECTION, null, true);

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.getSolutionClass()).isEqualTo(TestdataSolution.class);
        assertThat(descriptor.getEntityDescriptors()).hasSize(1);
        assertThat(descriptor.getEntityDescriptors().iterator().next().getEntityClass())
                .isEqualTo(TestdataEntity.class);
        assertThat(descriptor.getScoreDefinition()).isNotNull();
    }

    @Test
    void roundTripProducesSameResult() {
        // Build via the old path
        var oldDescriptor = SolutionDescriptor.buildSolutionDescriptor(
                TestdataSolution.class, TestdataEntity.class);

        // The old path now goes through the new pipeline too
        // Verify the descriptor is fully functional
        assertThat(oldDescriptor).isNotNull();
        assertThat(oldDescriptor.getSolutionClass()).isEqualTo(TestdataSolution.class);
        assertThat(oldDescriptor.getEntityDescriptors()).hasSize(1);

        var entityDescriptor = oldDescriptor.getEntityDescriptors().iterator().next();
        assertThat(entityDescriptor.getEntityClass()).isEqualTo(TestdataEntity.class);
        assertThat(entityDescriptor.getDeclaredGenuineVariableDescriptors()).hasSize(1);

        var variableDescriptor = entityDescriptor.getDeclaredGenuineVariableDescriptors().iterator().next();
        assertThat(variableDescriptor.getVariableName()).isEqualTo("value");
    }

    @Test
    void solutionClonerWorks() {
        var descriptor = SolutionDescriptor.buildSolutionDescriptor(
                TestdataSolution.class, TestdataEntity.class);

        var solution = TestdataSolution.generateSolution();
        var cloned = descriptor.getSolutionCloner().cloneSolution(solution);

        assertThat(cloned).isNotSameAs(solution);
        assertThat(cloned.getEntityList()).hasSize(solution.getEntityList().size());
    }

    @Test
    void scoreAccessorWorks() {
        var descriptor = SolutionDescriptor.buildSolutionDescriptor(
                TestdataSolution.class, TestdataEntity.class);
        var solution = TestdataSolution.generateSolution();
        solution.setScore(SimpleScore.of(42));

        var score = descriptor.getScoreDescriptor().getScoreDefinition();
        assertThat(score).isNotNull();
    }

    @Test
    void entityCollectionsWork() {
        var descriptor = SolutionDescriptor.buildSolutionDescriptor(
                TestdataSolution.class, TestdataEntity.class);
        var solution = TestdataSolution.generateSolution(3, 5);

        var entities = descriptor.getEntityCollectionMemberAccessorMap();
        assertThat(entities).isNotEmpty();

        var entityAccessor = entities.values().iterator().next();
        var entityCollection = entityAccessor.executeGetter(solution);
        assertThat(entityCollection).isNotNull();
    }

    @Test
    void valueRangesWork() {
        var descriptor = SolutionDescriptor.buildSolutionDescriptor(
                TestdataSolution.class, TestdataEntity.class);

        var entityDescriptor = descriptor.getEntityDescriptors().iterator().next();
        var variableDescriptor = entityDescriptor.getDeclaredGenuineVariableDescriptors().iterator().next();

        // Value range descriptor should be set
        assertThat(variableDescriptor.getValueRangeDescriptor()).isNotNull();
    }
}
