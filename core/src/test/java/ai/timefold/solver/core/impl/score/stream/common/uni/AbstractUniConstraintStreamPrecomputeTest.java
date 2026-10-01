package ai.timefold.solver.core.impl.score.stream.common.uni;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamPrecomputeTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamTestExtension;
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

    /**
     * A precompute that simply enumerates a problem-fact class must emit one tuple per fact.
     * Removing a problem fact must re-derive the fact-only output
     * and retract the removed fact's tuple.
     * Exercises the cache-invalidation path for fact-derived output.
     */
    @TestTemplate
    void forEachUnfiltered_fact() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var value1 = new TestdataLavishValue();
        var value2 = new TestdataLavishValue();
        var value3 = new TestdataLavishValue();
        solution.getValueList().addAll(List.of(value1, value2, value3));

        var scoreDirector = buildScoreDirector(
                factory -> factory.precompute(pf -> pf.forEachUnfiltered(TestdataLavishValue.class))
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_ID));

        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                assertMatch(value1),
                assertMatch(value2),
                assertMatch(value3));

        scoreDirector.beforeProblemFactRemoved(value3);
        solution.getValueList().remove(value3);
        scoreDirector.afterProblemFactRemoved(value3);

        assertScore(scoreDirector,
                assertMatch(value1),
                assertMatch(value2));
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
                        .asConstraint(TEST_CONSTRAINT_ID));

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

    record ExpectedUni<A>(A a, Object... indicted) {
        ExpectedUni<A> addIndicted(Object indictedObject) {
            for (var object : indicted) {
                if (object == indictedObject) {
                    return this;
                }
            }
            var indictments = Arrays.copyOf(indicted, indicted.length + 1);
            indictments[indicted.length] = indictedObject;
            return new ExpectedUni<>(a, indictments);
        }
    }

    <A> ExpectedUni<A> expect(A a, Object... indicted) {
        return new ExpectedUni<>(a, indicted);
    }

    private <A> void assertPrecompute(TestdataLavishSolution solution,
            List<ExpectedUni<A>> expectedTuples,
            Function<PrecomputeFactory, UniConstraintStream<A>> entityStreamSupplier) {
        expectedTuples = new ArrayList<>(expectedTuples);
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
            var listIterator = expectedTuples.listIterator();
            while (listIterator.hasNext()) {
                var expectedTuple = listIterator.next();
                listIterator.set(expectedTuple.addIndicted(entity));
            }
        }

        assertScore(scoreDirector, expectedTuples.stream()
                .map(expected -> AbstractConstraintStreamTest.assertMatch(expected.a)
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
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(expect(entityWithGroup, entityWithGroup, entityGroup)),
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

        assertPrecompute(solution, List.of(expect(entityWithoutGroup, entityWithoutGroup)),
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

        assertPrecompute(solution, List.of(expect(entityGroup, entityWithGroup)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .groupBy(TestdataLavishEntity::getEntityGroup));
    }

    @Override
    @TestTemplate
    public void flatten() {
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
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecomputeBi(solution, List.of(expectBi(entityWithoutGroup, entityWithoutGroup, entityWithoutGroup),
                expectBi(entityWithGroup, entityWithGroup, entityWithGroup)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .flatten(List::of));
    }

    record ExpectedBi<A, B>(A a, B b, Object... indicted) {
        ExpectedBi<A, B> addIndicted(Object indictedObject) {
            for (var object : indicted) {
                if (object == indictedObject) {
                    return this;
                }
            }
            var indictments = Arrays.copyOf(indicted, indicted.length + 1);
            indictments[indicted.length] = indictedObject;
            return new ExpectedBi<>(a, b, indictments);
        }
    }

    <A, B> ExpectedBi<A, B> expectBi(A a, B b, Object... indicted) {
        return new ExpectedBi<>(a, b, indicted);
    }

    private <A, B> void assertPrecomputeBi(TestdataLavishSolution solution, List<ExpectedBi<A, B>> expectedValues,
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
                .map(expectedBi -> assertMatch(expectedBi.a(), expectedBi.b())
                        .withIndictedObjects(expectedBi.indicted))
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
                expectBi(entity1, new ValueHolder(1), entity1),
                expectBi(entity2, new ValueHolder(2), entity2)),
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

        assertPrecompute(solution,
                List.of(expect(entityWithoutGroup, entityWithoutGroup), expect(entityWithGroup, entityWithGroup)),
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

        assertPrecompute(solution, List.of(expect(new ValueHolder(1), entity1),
                expect(new ValueHolder(2), entity2)),
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

        assertPrecompute(solution, List.of(expect(entityGroup, entityWithGroup1),
                expect(entityGroup, entityWithGroup2)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .map(TestdataLavishEntity::getEntityGroup));
    }

    @Override
    @TestTemplate
    public void concat() {
        var solution = TestdataLavishSolution.generateEmptySolution();
        var entityWithoutGroup = new TestdataLavishEntity();
        entityWithoutGroup.setCode("EntityWithoutGroup");
        var entityWithGroup = new TestdataLavishEntity();
        entityWithGroup.setCode("EntityWithGroup");
        var entityGroup = new TestdataLavishEntityGroup();
        entityGroup.setCode("EntityGroup");
        entityWithGroup.setEntityGroup(entityGroup);
        solution.getEntityList().addAll(List.of(entityWithoutGroup, entityWithGroup));
        solution.getEntityGroupList().add(entityGroup);
        solution.getValueList().add(new TestdataLavishValue());

        assertPrecompute(solution, List.of(expect(entityWithoutGroup, entityWithoutGroup),
                expect(entityWithGroup, entityWithGroup)),
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

        assertPrecompute(solution, List.of(expect(entityGroup, entityWithGroup1, entityWithGroup2)),
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

        assertPrecompute(solution, List.of(expect(entityWithGroup1, entityWithGroup1),
                expect(entityWithGroup2, entityWithGroup2),
                expect(entityWithoutGroup, entityWithoutGroup)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .complement(TestdataLavishEntity.class));
    }
}
