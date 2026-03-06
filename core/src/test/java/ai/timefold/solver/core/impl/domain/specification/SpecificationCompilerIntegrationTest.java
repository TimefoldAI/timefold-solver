package ai.timefold.solver.core.impl.domain.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SpecificationCompiler;

import org.junit.jupiter.api.Test;

class SpecificationCompilerIntegrationTest {

    // ── POJO domain model (no annotations) ──────────────────────────

    static class NoAnnotationSolution {
        SimpleScore score;
        List<NoAnnotationValue> values = new ArrayList<>();
        List<NoAnnotationEntity> entities = new ArrayList<>();

        SimpleScore getScore() {
            return score;
        }

        void setScore(SimpleScore score) {
            this.score = score;
        }

        List<NoAnnotationValue> getValues() {
            return values;
        }

        List<NoAnnotationEntity> getEntities() {
            return entities;
        }
    }

    static class NoAnnotationEntity {
        String id;
        NoAnnotationValue value;

        NoAnnotationEntity() {
        }

        NoAnnotationEntity(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        NoAnnotationValue getValue() {
            return value;
        }

        void setValue(NoAnnotationValue value) {
            this.value = value;
        }
    }

    static class NoAnnotationValue {
        String code;

        NoAnnotationValue() {
        }

        NoAnnotationValue(String code) {
            this.code = code;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static PlanningSpecification<NoAnnotationSolution> buildSpecification() {
        return PlanningSpecification.of(NoAnnotationSolution.class)
                .score(SimpleScore.class, NoAnnotationSolution::getScore, NoAnnotationSolution::setScore)
                .problemFacts("values", NoAnnotationSolution::getValues)
                .entityCollection("entities", NoAnnotationSolution::getEntities)
                .valueRange("valueRange", NoAnnotationSolution::getValues)
                .entity(NoAnnotationEntity.class, entity -> entity
                        .planningId(NoAnnotationEntity::getId)
                        .variable("value", NoAnnotationValue.class, var -> var
                                .accessors(NoAnnotationEntity::getValue, NoAnnotationEntity::setValue)
                                .valueRange("valueRange")))
                .build();
    }

    private static NoAnnotationSolution generateProblem(int valueCount, int entityCount) {
        var solution = new NoAnnotationSolution();
        for (int i = 0; i < valueCount; i++) {
            solution.getValues().add(new NoAnnotationValue("v" + i));
        }
        for (int i = 0; i < entityCount; i++) {
            solution.getEntities().add(new NoAnnotationEntity("e" + i));
        }
        return solution;
    }

    // ── Tests ────────────────────────────────────────────────────────

    @Test
    void compileSolutionDescriptor() {
        var spec = buildSpecification();
        var solutionDescriptor = SpecificationCompiler.compile(spec, null);

        assertThat(solutionDescriptor).isNotNull();
        assertThat(solutionDescriptor.getSolutionClass()).isEqualTo(NoAnnotationSolution.class);
        assertThat(solutionDescriptor.getEntityDescriptors()).hasSize(1);

        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(NoAnnotationEntity.class);
        assertThat(entityDescriptor.getGenuineVariableDescriptorList()).hasSize(1);
        assertThat(entityDescriptor.getGenuineVariableDescriptor("value")).isNotNull();
    }

    @Test
    void compileAndSolve() {
        var spec = buildSpecification();

        var solverConfig = new SolverConfig()
                .withPlanningSpecification(spec)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withEasyScoreCalculatorClass(DummyNoAnnotationScoreCalculator.class))
                .withPhases(
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withTerminationConfig(new TerminationConfig()
                                        .withStepCountLimit(5)));

        SolverFactory<NoAnnotationSolution> solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        var problem = generateProblem(3, 5);
        var solution = solver.solve(problem);

        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isNotNull();
        // All entities should be initialized (assigned a value)
        for (var entity : solution.getEntities()) {
            assertThat(entity.getValue()).isNotNull();
        }
    }

    @Test
    void solutionDescriptorScoreAccessor() {
        var spec = buildSpecification();
        var solutionDescriptor = SpecificationCompiler.compile(spec, null);

        var solution = new NoAnnotationSolution();
        solution.setScore(SimpleScore.of(42));

        var scoreDescriptor = solutionDescriptor.getScoreDescriptor();
        assertThat(scoreDescriptor).isNotNull();
    }

    @Test
    void entityCollectionAccessor() {
        var spec = buildSpecification();
        var solutionDescriptor = SpecificationCompiler.compile(spec, null);

        var problem = generateProblem(2, 3);
        var entityCollections = solutionDescriptor.getEntityCollectionMemberAccessorMap();
        assertThat(entityCollections).containsKey("entities");

        var accessor = entityCollections.get("entities");
        @SuppressWarnings("unchecked")
        var entities = (List<NoAnnotationEntity>) accessor.executeGetter(problem);
        assertThat(entities).hasSize(3);
    }

    @Test
    void problemFactCollectionAccessor() {
        var spec = buildSpecification();
        var solutionDescriptor = SpecificationCompiler.compile(spec, null);

        var problem = generateProblem(4, 2);
        var factCollections = solutionDescriptor.getProblemFactCollectionMemberAccessorMap();
        assertThat(factCollections).containsKey("values");

        var accessor = factCollections.get("values");
        @SuppressWarnings("unchecked")
        var values = (List<NoAnnotationValue>) accessor.executeGetter(problem);
        assertThat(values).hasSize(4);
    }

    @Test
    void planningIdIsRegistered() {
        var spec = buildSpecification();
        var solutionDescriptor = SpecificationCompiler.compile(spec, null);

        // The entity descriptor should have a planning ID configured.
        // Verify by checking the entity descriptor can look up by planning ID.
        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(NoAnnotationEntity.class);
        assertThat(entityDescriptor).isNotNull();
        // If planningId is registered, the solution descriptor should support lookups.
        assertThat(solutionDescriptor.getPlanningIdAccessor(NoAnnotationEntity.class)).isNotNull();
    }

    // Dummy score calculator for the no-annotation domain
    public static class DummyNoAnnotationScoreCalculator
            implements ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator<NoAnnotationSolution, SimpleScore> {

        @Override
        public SimpleScore calculateScore(NoAnnotationSolution solution) {
            return SimpleScore.of(0);
        }
    }
}
