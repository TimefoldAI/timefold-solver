package ai.timefold.solver.core.impl.score.stream.common.uni;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamPrecomputeTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamTestExtension;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValue;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

@ExtendWith(ConstraintStreamTestExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public abstract class AbstractUniConstraintStreamPrecomputeTest extends AbstractConstraintStreamTest
        implements ConstraintStreamPrecomputeTest {
    protected AbstractUniConstraintStreamPrecomputeTest(ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @Override
    @TestTemplate
    public void filter_0_changed() {
        var solution = TestdataLavishSolution.generateSolution();
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        solution.getEntityGroupList().add(entityGroup);
        var entity1 = Mockito.spy(new TestdataLavishEntity("MyEntity 1", entityGroup, solution.getFirstValue()));
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup, solution.getFirstValue());
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                solution.getFirstValue());
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.precompute(data -> data.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() == entityGroup))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        Mockito.reset(entity1);
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));
        Mockito.verify(entity1, Mockito.atLeastOnce()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity1);
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(new TestdataLavishValue());
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));
        Mockito.verify(entity1, Mockito.never()).getEntityGroup();

        // Incrementally update a fact
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity3));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1),
                assertMatch(entity2),
                assertMatch(entity3));
    }

    private <T> void assertPrecompute(TestdataLavishSolution solution,
            List<T> expectedValues,
            Function<PrecomputeFactory, UniConstraintStream<T>> entityStreamSupplier) {
        var scoreDirector =
                buildScoreDirector(factory -> factory.precompute(entityStreamSupplier)
                        .ifExists(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        for (var entity : solution.getEntityList()) {
            scoreDirector.beforeVariableChanged(entity, "value");
            entity.setValue(solution.getFirstValue());
            scoreDirector.afterVariableChanged(entity, "value");
        }

        assertScore(scoreDirector, expectedValues.stream()
                .map(AbstractConstraintStreamTest::assertMatch)
                .toArray(AssertableMatch[]::new));
    }

    @Override
    @TestTemplate
    public void ifExists() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityWithGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .ifExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                TestdataLavishEntity::getEntityGroup, Function.identity())));
    }

    @Override
    @TestTemplate
    public void ifNotExists() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityWithoutGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .ifNotExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                TestdataLavishEntity::getEntityGroup, Function.identity())));
    }

    @Override
    @TestTemplate
    public void groupBy() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .groupBy(TestdataLavishEntity::getEntityGroup));
    }

    @Override
    @TestTemplate
    public void flatten() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecomputeBi(solution, List.of(new Pair<>(entityWithoutGroup, entityWithoutGroup),
                new Pair<>(entityWithGroup, entityWithGroup)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .flatten(List::of));
    }

    private <A, B> void assertPrecomputeBi(TestdataLavishSolution solution, List<Pair<A, B>> expectedValues,
            Function<PrecomputeFactory, BiConstraintStream<A, B>> entityStreamSupplier) {
        var scoreDirector =
                buildScoreDirector(factory -> factory.precompute(entityStreamSupplier)
                        .ifExists(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        for (var entity : solution.getEntityList()) {
            scoreDirector.beforeVariableChanged(entity, "value");
            entity.setValue(solution.getFirstValue());
            scoreDirector.afterVariableChanged(entity, "value");
        }

        assertScore(scoreDirector, expectedValues.stream()
                .map(pair -> new Object[] { pair.key(), pair.value() })
                .map(AbstractConstraintStreamTest::assertMatch)
                .toArray(AssertableMatch[]::new));
    }

    @Override
    @TestTemplate
    public void flattenNewInstances() {
        // Needed since Integers use a cache of instances that we don't want to accidentally use
        record ValueHolder(int value) {
        }

        var solution = TestdataLavishSolution.generateEmptySolution();
        var entity1 = new TestdataLavishEntity();
        entity1.setIntegerProperty(1);
        var entity2 = new TestdataLavishEntity();
        entity2.setIntegerProperty(2);
        solution.getEntityList().addAll(List.of(entity1, entity2));
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecomputeBi(solution, List.of(
                new Pair<>(entity1, new ValueHolder(1)),
                new Pair<>(entity2, new ValueHolder(2))),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .flatten(entity -> List.of(new ValueHolder(entity.getIntegerProperty()))));
    }

    @Override
    @TestTemplate
    public void flattenLast() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityWithoutGroup, entityWithGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList));
    }

    @Override
    @TestTemplate
    public void flattenLastNewInstances() {
        // Needed since Integers use a cache of instances that we don't want to accidentally use
        record ValueHolder(int value) {
        }

        var solution = TestdataLavishSolution.generateEmptySolution();
        var entity1 = new TestdataLavishEntity();
        entity1.setIntegerProperty(1);
        var entity2 = new TestdataLavishEntity();
        entity2.setIntegerProperty(2);
        solution.getEntityList().addAll(List.of(entity1, entity2));
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(new ValueHolder(1), new ValueHolder(2)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList
                                .stream()
                                .map(entity -> new ValueHolder(entity.getIntegerProperty()))
                                .toList()));
    }

    @Override
    @TestTemplate
    public void map() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup1 = new TestdataLavishEntity();
        var entityWithGroup2 = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup1.setEntityGroup(entityGroup);
        entityWithGroup2.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup1, entityWithGroup2));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityGroup, entityGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .map(TestdataLavishEntity::getEntityGroup));
    }

    @Override
    @TestTemplate
    public void concat() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityWithoutGroup, entityWithGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() == null)
                        .concat(pf.forEachUnfiltered(TestdataLavishEntity.class)
                                .filter(entity -> entity.getEntityGroup() != null)));
    }

    @Override
    @TestTemplate
    public void distinct() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup1 = new TestdataLavishEntity();
        var entityWithGroup2 = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup1.setEntityGroup(entityGroup);
        entityWithGroup2.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup1, entityWithGroup2));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .map(TestdataLavishEntity::getEntityGroup)
                        .distinct());
    }

    @Override
    @TestTemplate
    public void complement() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup1 = new TestdataLavishEntity();
        var entityWithGroup2 = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup1.setEntityGroup(entityGroup);
        entityWithGroup2.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup1, entityWithGroup2));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(entityWithGroup1, entityWithGroup2, entityWithoutGroup),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .complement(TestdataLavishEntity.class));
    }
}
