package ai.timefold.solver.core.impl.score.stream.common.bi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamPrecomputeTest;
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
                        .asConstraint(TEST_CONSTRAINT_ID));

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
                        .asConstraint(TEST_CONSTRAINT_ID));

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
                        .asConstraint(TEST_CONSTRAINT_ID));

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
                        .asConstraint(TEST_CONSTRAINT_ID));

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

    record Expected<A, B>(A a, B b, Object... indicted) {
        Expected<A, B> addIndicted(Object indictedObject) {
            for (var object : indicted) {
                if (object == indictedObject) {
                    return this;
                }
            }
            var newIndictments = Arrays.copyOf(indicted, indicted.length + 1);
            newIndictments[indicted.length] = indictedObject;
            return new Expected<>(a, b, newIndictments);
        }
    }

    <A, B> Expected<A, B> expect(A a, B b, Object... indicted) {
        return new Expected<>(a, b, indicted);
    }

    private <A, B> void assertPrecompute(TestdataLavishSolution solution,
            List<Expected<A, B>> expectedValues,
            Function<PrecomputeFactory, BiConstraintStream<A, B>> entityStreamSupplier) {
        expectedValues = new ArrayList<>(expectedValues);
        var scoreDirector =
                buildScoreDirector(factory -> factory.precompute(entityStreamSupplier)
                        .ifExists(TestdataLavishEntity.class)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_ID));

        // From scratch
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector);

        for (var entity : solution.getEntityList()) {
            scoreDirector.beforeVariableChanged(entity, "value");
            entity.setValue(solution.getFirstValue());
            scoreDirector.afterVariableChanged(entity, "value");
            var listIterator = expectedValues.listIterator();
            while (listIterator.hasNext()) {
                var expected = listIterator.next();
                listIterator.set(expected.addIndicted(entity));
            }
        }

        assertScore(scoreDirector, expectedValues.stream()
                .map(expected -> assertMatch(expected.a, expected.b)
                        .withIndictedObjects(expected.indicted))
                .toArray(AssertableMatch[]::new));
    }

    @Override
    @TestTemplate
    public void ifExists() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        entityWithoutGroup.setCode("A");
        var entityWithGroup = new TestdataLavishEntity();
        entityWithGroup.setCode("B");
        var entityGroup = new TestdataLavishEntityGroup();
        entityGroup.setCode("C");
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        var value = new TestdataLavishValue();
        value.setCode("D");
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(expect(entityWithGroup, value, entityWithGroup, value, entityGroup)),
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

        assertPrecompute(solution, List.of(expect(entityWithoutGroup, value, entityWithoutGroup, value)),
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

        assertPrecompute(solution, List.of(expect(entityGroup, 1L)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .groupBy(TestdataLavishEntity::getEntityGroup, ConstraintCollectors.count()));
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(expect(entityWithoutGroup, entityWithoutGroup),
                expect(entityWithGroup, entityWithoutGroup)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .flatten(List::of));
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(expect(entity1, new ValueHolder(entity1.getIntegerProperty())),
                expect(entity2, new ValueHolder(entity2.getIntegerProperty()))),
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
        var value = new TestdataLavishValue();
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(expect(entityWithoutGroup, value, entityWithoutGroup, value),
                expect(entityWithGroup, value, entityWithGroup, value)),
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

        assertPrecompute(solution, List.of(expect(new ValueHolder(1), value, entity1, value),
                expect(new ValueHolder(2), value, entity2, value)),
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

        assertPrecompute(solution, List.of(expect(entityGroup, value, entityWithGroup1, value),
                expect(entityGroup, value, entityWithGroup2, value)),
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
        entityWithoutGroup.setCode("A");
        var entityWithGroup = new TestdataLavishEntity();
        entityWithGroup.setCode("B");
        var entityGroup = new TestdataLavishEntityGroup();
        entityGroup.setCode("C");
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        var value = new TestdataLavishValue();
        value.setCode("D");
        solution.getValueList().add(value);

        assertPrecompute(solution, List.of(
                expect(entityWithoutGroup, value, entityWithoutGroup, value),
                expect(entityWithGroup, value, entityWithGroup, value)),
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

        assertPrecompute(solution, List.of(expect(entityGroup, value, entityWithGroup1, entityWithGroup2, value)),
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
                expect(entityWithGroup1, value, entityWithGroup1, value),
                expect(entityWithGroup2, value, entityWithGroup2, value),
                expect(entityWithoutGroup, null, entityWithoutGroup)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue) -> entity.getEntityGroup() != null)
                        .complement(TestdataLavishEntity.class));
    }
}
