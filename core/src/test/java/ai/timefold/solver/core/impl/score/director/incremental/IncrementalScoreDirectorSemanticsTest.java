package ai.timefold.solver.core.impl.score.director.incremental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.AnalyzableIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchRegistry;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorSemanticsTest;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.constraintweightoverrides.TestdataConstraintWeightOverridesSolution;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

final class IncrementalScoreDirectorSemanticsTest extends AbstractScoreDirectorSemanticsTest {

    @Override
    protected ScoreDirectorFactory<TestdataConstraintWeightOverridesSolution, SimpleScore>
            buildScoreDirectorFactoryWithConstraintConfiguration(
                    SolutionDescriptor<TestdataConstraintWeightOverridesSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestdataConstraintWeightOverridesIncrementalScoreCalculator.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataConstraintWeightOverridesSolution, SimpleScore>(
                        scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

    @Override
    protected ScoreDirectorFactory<TestdataPinnedListSolution, SimpleScore> buildScoreDirectorFactoryWithListVariableEntityPin(
            SolutionDescriptor<TestdataPinnedListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestdataPinnedListIncrementalScoreCalculator.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataPinnedListSolution, SimpleScore>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

    @Override
    protected ScoreDirectorFactory<TestdataPinnedWithIndexListSolution, SimpleScore>
            buildScoreDirectorFactoryWithListVariablePinIndex(
                    SolutionDescriptor<TestdataPinnedWithIndexListSolution> solutionDescriptor) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withIncrementalScoreCalculatorClass(TestdataPinnedWithIndexListIncrementalScoreCalculator.class);
        var scoreDirectorFactoryFactory =
                new ScoreDirectorFactoryFactory<TestdataPinnedWithIndexListSolution, SimpleScore>(scoreDirectorFactoryConfig);
        return scoreDirectorFactoryFactory.buildScoreDirectorFactory(EnvironmentMode.PHASE_ASSERT, solutionDescriptor);
    }

    @NullMarked
    public static class TestdataPinnedListIncrementalScoreCalculator
            implements IncrementalScoreCalculator<TestdataPinnedListSolution, SimpleScore> {

        private List<TestdataPinnedListEntity> entityList = Collections.emptyList();

        @Override
        public void resetWorkingSolution(TestdataPinnedListSolution workingSolution) {
            this.entityList = new ArrayList<>(workingSolution.getEntityList());
        }

        @Override
        public void beforeVariableChanged(Object entity, String variableName) {
            // No need to do anything.
        }

        @Override
        public void afterVariableChanged(Object entity, String variableName) {
            // No need to do anything.
        }

        @Override
        public SimpleScore calculateScore() {
            return SimpleScore.of(-entityList.size());
        }
    }

    @NullMarked
    public static class TestdataPinnedWithIndexListIncrementalScoreCalculator
            implements IncrementalScoreCalculator<TestdataPinnedWithIndexListSolution, SimpleScore> {

        private List<TestdataPinnedWithIndexListEntity> entityList = Collections.emptyList();

        @Override
        public void resetWorkingSolution(TestdataPinnedWithIndexListSolution workingSolution) {
            this.entityList = new ArrayList<>(workingSolution.getEntityList());
        }

        @Override
        public void beforeVariableChanged(Object entity, String variableName) {
            // No need to do anything.
        }

        @Override
        public void afterVariableChanged(Object entity, String variableName) {
            // No need to do anything.
        }

        @Override
        public SimpleScore calculateScore() {
            return SimpleScore.of(-entityList.size());
        }
    }

    @NullMarked
    public static class TestdataConstraintWeightOverridesIncrementalScoreCalculator
            implements AnalyzableIncrementalScoreCalculator<TestdataConstraintWeightOverridesSolution, SimpleScore> {

        private @Nullable TestdataConstraintWeightOverridesSolution workingSolution;
        private @Nullable ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry;
        private List<TestdataEntity> entityList = Collections.emptyList();

        @Override
        public void resetWorkingSolution(TestdataConstraintWeightOverridesSolution workingSolution) {
            this.workingSolution = workingSolution;
            this.entityList = new ArrayList<>(workingSolution.getEntityList());
        }

        @Override
        public void beforeVariableChanged(Object entity, String variableName) {
            throw new UnsupportedOperationException(); // Will not be called.
        }

        @Override
        public void afterVariableChanged(Object entity, String variableName) {
            throw new UnsupportedOperationException(); // Will not be called.
        }

        @Override
        public SimpleScore calculateScore() {
            var overrides = workingSolution.getConstraintWeightOverrides();
            var firstWeight = overrides.getConstraintWeight("First weight");
            var score = SimpleScore.of(entityList.size());
            if (constraintMatchRegistry != null && firstWeight != null) {
                constraintMatchRegistry.registerConstraintMatch(ConstraintRef.of("First weight"), score);
                return score.multiply(firstWeight.score());
            }
            return score;
        }

        @Override
        public void enableConstraintMatch(ConstraintMatchRegistry<SimpleScore> constraintMatchRegistry) {
            this.constraintMatchRegistry = constraintMatchRegistry;
        }
    }
}
