package ai.timefold.solver.core.impl.score.stream.common.bi;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamPrecomputeTest;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValue;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValueGroup;

import org.junit.jupiter.api.TestTemplate;
import org.mockito.Mockito;

public abstract class AbstractBiConstraintStreamPrecomputeTest extends AbstractConstraintStreamTest
        implements ConstraintStreamPrecomputeTest {
    protected AbstractBiConstraintStreamPrecomputeTest(ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    @Override
    @TestTemplate
    public void filter_0_changed() {
        var solution = TestdataLavishSolution.generateSolution();
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        var valueGroup = new TestdataLavishValueGroup("MyValueGroup");
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueGroupList().add(valueGroup);

        var value1 = Mockito.spy(new TestdataLavishValue("MyValue 1", valueGroup));
        solution.getValueList().add(value1);
        var value2 = Mockito.spy(new TestdataLavishValue("MyValue 2", valueGroup));
        solution.getValueList().add(value2);
        var value3 = Mockito.spy(new TestdataLavishValue("MyValue 3", null));
        solution.getValueList().add(value3);

        var entity1 = Mockito.spy(new TestdataLavishEntity("MyEntity 1", entityGroup, value1));
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup, value1);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value1);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.precompute(data -> data.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, value) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup))
                        .filter((entity, value) -> entity.getValue() == value1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        Mockito.reset(entity1);
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, value1),
                assertMatch(entity1, value2),
                assertMatch(entity2, value1),
                assertMatch(entity2, value2));
        Mockito.verify(entity1, Mockito.atLeastOnce()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity1);
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(solution.getFirstValue());
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector,
                assertMatch(entity2, value1),
                assertMatch(entity2, value2));
        Mockito.verify(entity1, Mockito.never()).getEntityGroup();

        // Incrementally update a fact
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                assertMatch(entity2, value1),
                assertMatch(entity2, value2),
                assertMatch(entity3, value1),
                assertMatch(entity3, value2));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatch(entity2, value1),
                assertMatch(entity2, value2));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                assertMatch(entity2, value1),
                assertMatch(entity2, value2),
                assertMatch(entity3, value1),
                assertMatch(entity3, value2));
    }

    @Override
    @TestTemplate
    public void filter_1_changed() {
        var solution = TestdataLavishSolution.generateSolution();
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        var valueGroup = new TestdataLavishValueGroup("MyValueGroup");
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueGroupList().add(valueGroup);

        var value1 = Mockito.spy(new TestdataLavishValue("MyValue 1", valueGroup));
        solution.getValueList().add(value1);
        var value2 = Mockito.spy(new TestdataLavishValue("MyValue 2", valueGroup));
        solution.getValueList().add(value2);
        var value3 = Mockito.spy(new TestdataLavishValue("MyValue 3", null));
        solution.getValueList().add(value3);

        var entity1 = Mockito.spy(new TestdataLavishEntity("MyEntity 1", entityGroup, value1));
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup, value1);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value1);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory.precompute(data -> data.forEachUnfiltered(TestdataLavishValue.class)
                        .join(TestdataLavishEntity.class)
                        .filter((value, entity) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup))
                        .filter((value, entity) -> entity.getValue() == value1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        Mockito.reset(entity1);
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(value1, entity1),
                assertMatch(value2, entity1),
                assertMatch(value1, entity2),
                assertMatch(value2, entity2));
        Mockito.verify(entity1, Mockito.atLeastOnce()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity1);
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(solution.getFirstValue());
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector,
                assertMatch(value1, entity2),
                assertMatch(value2, entity2));
        Mockito.verify(entity1, Mockito.never()).getEntityGroup();

        // Incrementally update a fact
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                assertMatch(value1, entity2),
                assertMatch(value2, entity2),
                assertMatch(value1, entity3),
                assertMatch(value2, entity3));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatch(value1, entity2),
                assertMatch(value2, entity2));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                assertMatch(value1, entity2),
                assertMatch(value2, entity2),
                assertMatch(value1, entity3),
                assertMatch(value2, entity3));
    }

    @TestTemplate
    public void filter_0_changed_forEachUnfilteredUniquePair() {
        var solution = TestdataLavishSolution.generateSolution();
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        var valueGroup = new TestdataLavishValueGroup("MyValueGroup");
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueGroupList().add(valueGroup);

        var value1 = Mockito.spy(new TestdataLavishValue("MyValue 1", valueGroup));
        solution.getValueList().add(value1);
        var value2 = Mockito.spy(new TestdataLavishValue("MyValue 2", valueGroup));
        solution.getValueList().add(value2);
        var value3 = Mockito.spy(new TestdataLavishValue("MyValue 3", null));
        solution.getValueList().add(value3);

        var entity1 = Mockito.spy(new TestdataLavishEntity("MyEntity 1", entityGroup, value1));
        solution.getEntityList().add(entity1);
        var entity2 = new TestdataLavishEntity("MyEntity 2", entityGroup, value1);
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value1);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory
                        .precompute(data -> data.forEachUnfilteredUniquePair(TestdataLavishEntity.class,
                                Joiners.equal(TestdataLavishEntity::getEntityGroup)))
                        .filter((a, b) -> a.getValue() == value1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        Mockito.reset(entity1);
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2));
        Mockito.verify(entity1, Mockito.atLeastOnce()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity1);
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(solution.getFirstValue());
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector);
        Mockito.verify(entity1, Mockito.never()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity1);
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(value1);
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity2));
        Mockito.verify(entity1, Mockito.never()).getEntityGroup();

        // Incrementally update a fact
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2),
                assertMatch(entity1, entity3),
                assertMatch(entity2, entity3));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2),
                assertMatch(entity1, entity3),
                assertMatch(entity2, entity3));
    }

    @TestTemplate
    public void filter_1_changed_forEachUnfilteredUniquePair() {
        var solution = TestdataLavishSolution.generateSolution();
        var entityGroup = new TestdataLavishEntityGroup("MyEntityGroup");
        var valueGroup = new TestdataLavishValueGroup("MyValueGroup");
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueGroupList().add(valueGroup);

        var value1 = Mockito.spy(new TestdataLavishValue("MyValue 1", valueGroup));
        solution.getValueList().add(value1);
        var value2 = Mockito.spy(new TestdataLavishValue("MyValue 2", valueGroup));
        solution.getValueList().add(value2);
        var value3 = Mockito.spy(new TestdataLavishValue("MyValue 3", null));
        solution.getValueList().add(value3);

        var entity1 = new TestdataLavishEntity("MyEntity 1", entityGroup, value1);
        solution.getEntityList().add(entity1);
        var entity2 = Mockito.spy(new TestdataLavishEntity("MyEntity 2", entityGroup, value1));
        solution.getEntityList().add(entity2);
        var entity3 = new TestdataLavishEntity("MyEntity 3", solution.getFirstEntityGroup(),
                value2);
        solution.getEntityList().add(entity3);

        var scoreDirector =
                buildScoreDirector(factory -> factory
                        .precompute(data -> data.forEachUnfilteredUniquePair(TestdataLavishEntity.class,
                                Joiners.equal(TestdataLavishEntity::getEntityGroup)))
                        .filter((a, b) -> b.getValue() == value1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        Mockito.reset(entity2);
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2));
        Mockito.verify(entity2, Mockito.atLeastOnce()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity2);
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(solution.getFirstValue());
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector);
        Mockito.verify(entity2, Mockito.never()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity2);
        scoreDirector.beforeVariableChanged(entity2, "value");
        entity2.setValue(value1);
        scoreDirector.afterVariableChanged(entity2, "value");
        assertScore(scoreDirector,
                assertMatch(entity1, entity2));
        Mockito.verify(entity2, Mockito.never()).getEntityGroup();

        // Incrementally update a fact
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setValue(value1);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2),
                assertMatch(entity1, entity3),
                assertMatch(entity2, entity3));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                assertMatch(entity1, entity2),
                assertMatch(entity1, entity3),
                assertMatch(entity2, entity3));
    }

    private <A, B> void assertPrecompute(TestdataLavishSolution solution,
            List<Pair<A, B>> expectedValues,
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
    public void ifExists() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        var entityWithGroup = new TestdataLavishEntity();
        var entityGroup = new TestdataLavishEntityGroup();
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(entityWithGroup, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .ifExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                (a, b) -> a.getEntityGroup(), Function.identity())));
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

        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(entityWithoutGroup, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .ifNotExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                (a, b) -> a.getEntityGroup(), Function.identity())));
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

        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(entityGroup, 1)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .groupBy(TestdataLavishEntity::getEntityGroup, ConstraintCollectors.count()));
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(entityWithoutGroup, value),
                new Pair<>(entityWithGroup, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList)
                        .join(TestdataLavishValue.class));
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(new ValueHolder(1), value),
                new Pair<>(new ValueHolder(2), value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList
                                .stream()
                                .map(entity -> new ValueHolder(entity.getIntegerProperty()))
                                .toList())
                        .join(TestdataLavishValue.class));
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(entityGroup, value),
                new Pair<>(entityGroup, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue) -> entity.getEntityGroup() != null)
                        .map((entity, joinedValue) -> entity.getEntityGroup(),
                                (entity, joinedValue) -> joinedValue));
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(entityWithoutGroup, value), new Pair<>(entityWithGroup, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue) -> entity.getEntityGroup() == null)
                        .concat(pf.forEachUnfiltered(TestdataLavishEntity.class)
                                .join(TestdataLavishValue.class)
                                .filter((entity, joinedValue) -> entity.getEntityGroup() != null)));
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(new Pair<>(entityGroup, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue) -> entity.getEntityGroup() != null)
                        .map((entity, joinedValue) -> entity.getEntityGroup(),
                                (entity, joinedValue) -> joinedValue)
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(
                new Pair<>(entityWithGroup1, value),
                new Pair<>(entityWithGroup2, value),
                new Pair<>(entityWithoutGroup, null)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue) -> entity.getEntityGroup() != null)
                        .complement(TestdataLavishEntity.class));
    }
}
