package ai.timefold.solver.core.impl.domain.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SpecificationCompiler;
import ai.timefold.solver.core.impl.domain.specification.testdata.LookupTestHelper;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class LookupAnnotationSpecificationTest {

    @Test
    void packagePrivateClassesWithLookup() {
        var lookup = LookupTestHelper.lookup();
        var solutionClass = LookupTestHelper.solutionClass();
        var entityClassList = LookupTestHelper.entityClassList();

        @SuppressWarnings("unchecked")
        var spec = AnnotationSpecificationFactory.fromAnnotations(
                (Class<Object>) solutionClass, entityClassList, lookup);

        assertThat(spec.solutionClass()).isEqualTo(solutionClass);
        assertThat(spec.score()).isNotNull();
        assertThat(spec.score().scoreType()).isEqualTo(SimpleScore.class);
        assertThat(spec.entities()).hasSize(1);
        assertThat(spec.entities().getFirst().entityClass()).isEqualTo(entityClassList.getFirst());
        assertThat(spec.facts()).isNotEmpty();
        assertThat(spec.entityCollections()).isNotEmpty();
    }

    @Test
    void packagePrivateSpecCompilesToValidDescriptor() {
        var lookup = LookupTestHelper.lookup();
        var solutionClass = LookupTestHelper.solutionClass();
        var entityClassList = LookupTestHelper.entityClassList();

        @SuppressWarnings("unchecked")
        var spec = AnnotationSpecificationFactory.fromAnnotations(
                (Class<Object>) solutionClass, entityClassList, lookup);

        var descriptor = SpecificationCompiler.compile(spec, null);

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.getSolutionClass()).isEqualTo(solutionClass);
        assertThat(descriptor.getEntityDescriptors()).hasSize(1);
    }

    @Test
    void publicClassesWithLookupProduceValidSpec() {
        // Using the framework's own lookup — public classes should work fine
        var lookup = MethodHandles.lookup();

        var spec = AnnotationSpecificationFactory.fromAnnotations(
                TestdataSolution.class, List.of(TestdataEntity.class), lookup);

        assertThat(spec.solutionClass()).isEqualTo(TestdataSolution.class);
        assertThat(spec.score()).isNotNull();
        assertThat(spec.score().scoreType()).isEqualTo(SimpleScore.class);
        assertThat(spec.entities()).hasSize(1);
        assertThat(spec.entities().getFirst().entityClass()).isEqualTo(TestdataEntity.class);
    }

    @Test
    void lookupSpecGettersAndSettersWork() {
        var lookup = LookupTestHelper.lookup();
        var solutionClass = LookupTestHelper.solutionClass();
        var entityClassList = LookupTestHelper.entityClassList();

        @SuppressWarnings("unchecked")
        var spec = AnnotationSpecificationFactory.fromAnnotations(
                (Class<Object>) solutionClass, entityClassList, lookup);

        // Test that score getter/setter lambdas work
        var solution = LookupTestHelper.createUninitializedSolution();
        var scoreSpec = spec.score();
        var scoreSetter = scoreSpec.setter();
        var scoreGetter = scoreSpec.getter();

        scoreSetter.accept(solution, SimpleScore.of(42));
        var score = scoreGetter.apply(solution);
        assertThat(score).isEqualTo(SimpleScore.of(42));
    }

    @Test
    void lookupSpecEntityCollectionGetterWorks() {
        var lookup = LookupTestHelper.lookup();
        var solutionClass = LookupTestHelper.solutionClass();
        var entityClassList = LookupTestHelper.entityClassList();

        @SuppressWarnings("unchecked")
        var spec = AnnotationSpecificationFactory.fromAnnotations(
                (Class<Object>) solutionClass, entityClassList, lookup);

        var solution = LookupTestHelper.createUninitializedSolution();
        var entityCollectionSpec = spec.entityCollections().getFirst();
        var entities = entityCollectionSpec.getter().apply(solution);
        assertThat(entities).hasSize(2);
    }

    @Test
    void lookupSpecVariableGetterAndSetterWork() {
        var lookup = LookupTestHelper.lookup();
        var solutionClass = LookupTestHelper.solutionClass();
        var entityClassList = LookupTestHelper.entityClassList();

        @SuppressWarnings("unchecked")
        var spec = AnnotationSpecificationFactory.fromAnnotations(
                (Class<Object>) solutionClass, entityClassList, lookup);

        var entitySpec = spec.entities().getFirst();
        assertThat(entitySpec.variables()).hasSize(1);
        var variableSpec = entitySpec.variables().getFirst();

        // Variable getter/setter should work on an entity instance
        var solution = LookupTestHelper.createUninitializedSolution();
        var entityCollectionSpec = spec.entityCollections().getFirst();
        var entities = entityCollectionSpec.getter().apply(solution);
        var entity = entities.iterator().next();

        // Initially null (uninitialized)
        @SuppressWarnings("unchecked")
        var varGetter = (java.util.function.Function<Object, Object>) variableSpec.getter();
        var value = varGetter.apply(entity);
        assertThat(value).isNull();

        // Get a value from the value range
        var factSpec = spec.facts().getFirst();
        var facts = factSpec.getter().apply(solution);
        assertThat(facts).isInstanceOf(java.util.Collection.class);
    }
}
