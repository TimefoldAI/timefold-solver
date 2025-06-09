package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import jakarta.inject.Inject;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.partitionedsearch.partitioner.SolutionPartitioner;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.quarkus.gizmo.TimefoldGizmoBeanFactory;
import ai.timefold.solver.quarkus.testdomain.gizmo.DummyVariableListener;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class TimefoldProcessorGeneratedGizmoSupplierTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.test.flat-class-path", "true")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("ai/timefold/solver/quarkus/gizmoSupplierTestSolverConfig.xml",
                            "solverConfig.xml")
                    .addClasses(
                            TestdataSolution.class,
                            TestdataEntity.class,
                            TestdataBothAnnotatedChildEntity.class,
                            DummyInterfaceEntity.class,
                            DummyAbstractEntity.class,
                            DummyVariableListener.class,
                            DummyChangeMoveFilter.class,
                            DummyConstraintProvider.class,
                            DummyEasyScoreCalculator.class,
                            DummyEntityFilter.class,
                            DummyIncrementalScoreCalculator.class,
                            DummyMoveIteratorFactory.class,
                            DummyMoveListFactory.class,
                            DummySolutionPartitioner.class,
                            DummyValueFilter.class));

    @Inject
    TimefoldGizmoBeanFactory gizmoBeanFactory;

    private void assertFactoryContains(Class<?> clazz) {
        assertThat(gizmoBeanFactory.newInstance(clazz)).isNotNull();
    }

    private void assertFactoryNotContains(Class<?> clazz) {
        assertThat(gizmoBeanFactory.newInstance(clazz)).isNull();
    }

    @Test
    void gizmoFactoryContainClassesReferencedInSolverConfig() {
        assertFactoryContains(DummyChangeMoveFilter.class);
        assertFactoryContains(DummyConstraintProvider.class);
        assertFactoryContains(DummyEasyScoreCalculator.class);
        assertFactoryContains(DummyEntityFilter.class);
        assertFactoryContains(DummyIncrementalScoreCalculator.class);
        assertFactoryContains(DummyMoveIteratorFactory.class);
        assertFactoryContains(DummyMoveListFactory.class);
        assertFactoryContains(DummySolutionPartitioner.class);
        assertFactoryContains(DummyValueFilter.class);
        assertFactoryContains(DummyVariableListener.class);

        assertFactoryNotContains(DummyInterfaceEntity.class);
        assertFactoryNotContains(DummyAbstractEntity.class);
    }

    /* Dummy classes below are referenced from the testSolverConfig.xml used in this test case. */

    @PlanningEntity
    public interface DummyInterfaceEntity {
        @ShadowVariable(variableListenerClass = DummyVariableListener.class,
                sourceEntityClass = TestdataEntity.class, sourceVariableName = "value")
        Integer getLength();

        void setLength(Integer length);
    }

    @PlanningEntity
    public abstract static class DummyAbstractEntity {
        @ShadowVariable(variableListenerClass = DummyVariableListener.class,
                sourceEntityClass = TestdataEntity.class, sourceVariableName = "value")
        abstract Integer getLength();

        abstract void setLength(Integer length);
    }

    public static class DummySolutionPartitioner implements SolutionPartitioner<TestdataSolution> {
        @Override
        public List<TestdataSolution> splitWorkingSolution(ScoreDirector<TestdataSolution> scoreDirector,
                Integer runnablePartThreadLimit) {
            return null;
        }
    }

    public static class DummyEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {
        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            return null;
        }
    }

    public static class DummyIncrementalScoreCalculator
            implements IncrementalScoreCalculator<TestdataSolution, SimpleScore> {
        @Override
        public void resetWorkingSolution(@NonNull TestdataSolution workingSolution) {
            // Ignore
        }

        @Override
        public void beforeEntityAdded(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public void afterEntityAdded(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {
            // Ignore
        }

        @Override
        public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {
            // Ignore
        }

        @Override
        public void beforeEntityRemoved(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public void afterEntityRemoved(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public @NonNull SimpleScore calculateScore() {
            return null;
        }
    }

    public static class DummyConstraintProvider implements ConstraintProvider {
        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[0];
        }
    }

    public static class DummyValueFilter implements SelectionFilter<TestdataSolution, TestdataValue> {
        @Override
        public boolean accept(ScoreDirector<TestdataSolution> scoreDirector, TestdataValue selection) {
            return false;
        }
    }

    public static class DummyEntityFilter implements SelectionFilter<TestdataSolution, TestdataEntity> {
        @Override
        public boolean accept(ScoreDirector<TestdataSolution> scoreDirector, TestdataEntity selection) {
            return false;
        }
    }

    public static class DummyChangeMoveFilter
            implements SelectionFilter<TestdataSolution, ChangeMove<TestdataSolution>> {
        @Override
        public boolean accept(ScoreDirector<TestdataSolution> scoreDirector, ChangeMove<TestdataSolution> selection) {
            return false;
        }
    }

    public static class DummyMoveIteratorFactory implements MoveIteratorFactory<TestdataSolution, DummyMove> {
        @Override
        public long getSize(ScoreDirector<TestdataSolution> scoreDirector) {
            return 0;
        }

        @Override
        public Iterator<DummyMove> createOriginalMoveIterator(ScoreDirector<TestdataSolution> scoreDirector) {
            return null;
        }

        @Override
        public Iterator<DummyMove> createRandomMoveIterator(ScoreDirector<TestdataSolution> scoreDirector,
                Random workingRandom) {
            return null;
        }
    }

    public static class DummyMoveListFactory implements MoveListFactory<TestdataSolution> {
        @Override
        public List<? extends Move<TestdataSolution>> createMoveList(TestdataSolution testdataSolution) {
            return null;
        }
    }

}
