package ai.timefold.solver.core.impl.score.stream.common.tri;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamPrecomputeTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamTestExtension;
import ai.timefold.solver.core.impl.util.Triple;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValue;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValueGroup;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

@ExtendWith(ConstraintStreamTestExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public abstract class AbstractTriConstraintStreamPrecomputeTest extends AbstractConstraintStreamTest
        implements ConstraintStreamPrecomputeTest {
    protected AbstractTriConstraintStreamPrecomputeTest(ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    private <A, B, C> void assertPrecomputeFilterChanged(
            TriFunction<PrecomputeFactory, TestdataLavishEntityGroup, TestdataLavishValueGroup, TriConstraintStream<A, B, C>> precomputeStream,
            TriFunction<A, B, C, TestdataLavishEntity> entityPicker,
            TriFunction<TestdataLavishEntity, TestdataLavishValue, TestdataLavishEntityGroup, Triple<A, B, C>> inputDataToTuple) {
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
                buildScoreDirector(factory -> factory.precompute(pf -> precomputeStream.apply(pf, entityGroup, valueGroup))
                        .filter((a, b, c) -> entityPicker.apply(a, b, c).getValue() == value1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        var createMatch =
                (TriFunction<TestdataLavishEntity, TestdataLavishValue, TestdataLavishEntityGroup, AssertableMatch>) (entity,
                        value, matchEntityGroup) -> {
                    var tuple = inputDataToTuple.apply(entity, value, matchEntityGroup);
                    return assertMatch(tuple.a(), tuple.b(), tuple.c());
                };
        Mockito.reset(entity1);
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                createMatch.apply(entity1, value1, entityGroup),
                createMatch.apply(entity1, value2, entityGroup),
                createMatch.apply(entity2, value1, entityGroup),
                createMatch.apply(entity2, value2, entityGroup));
        Mockito.verify(entity1, Mockito.atLeastOnce()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity1);
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(solution.getFirstValue());
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup),
                createMatch.apply(entity2, value2, entityGroup));
        Mockito.verify(entity1, Mockito.never()).getEntityGroup();

        // Incrementally update a fact
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup),
                createMatch.apply(entity2, value2, entityGroup),
                createMatch.apply(entity3, value1, entityGroup),
                createMatch.apply(entity3, value2, entityGroup));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup),
                createMatch.apply(entity2, value2, entityGroup));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup),
                createMatch.apply(entity2, value2, entityGroup),
                createMatch.apply(entity3, value1, entityGroup),
                createMatch.apply(entity3, value2, entityGroup));
    }

    @Override
    @TestTemplate
    public void filter_0_changed() {
        assertPrecomputeFilterChanged(
                (precomputeFactory, entityGroup, valueGroup) -> precomputeFactory.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishEntityGroup.class)
                        .filter((entity, value, matchedEntityGroup) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup
                                && matchedEntityGroup == entityGroup),
                (entity, value, entityGroup) -> entity,
                Triple::new);
    }

    @Override
    @TestTemplate
    public void filter_1_changed() {
        assertPrecomputeFilterChanged(
                (precomputeFactory, entityGroup, valueGroup) -> precomputeFactory.forEachUnfiltered(TestdataLavishValue.class)
                        .join(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class)
                        .filter((value, entity, matchedEntityGroup) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup
                                && matchedEntityGroup == entityGroup),
                (value, entity, entityGroup) -> entity,
                (entity, value, entityGroup) -> new Triple<>(value, entity, entityGroup));
    }

    @Override
    @TestTemplate
    public void filter_2_changed() {
        assertPrecomputeFilterChanged(
                (precomputeFactory, entityGroup, valueGroup) -> precomputeFactory.forEachUnfiltered(TestdataLavishValue.class)
                        .join(TestdataLavishEntityGroup.class)
                        .join(TestdataLavishEntity.class)
                        .filter((value, matchedEntityGroup, entity) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup
                                && matchedEntityGroup == entityGroup),
                (value, entityGroup, entity) -> entity,
                (entity, value, entityGroup) -> new Triple<>(value, entityGroup, entity));
    }

    private <A, B, C> void assertPrecompute(TestdataLavishSolution solution,
            List<Triple<A, B, C>> expectedValues,
            Function<PrecomputeFactory, TriConstraintStream<A, B, C>> entityStreamSupplier) {
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
                .map(triple -> new Object[] { triple.a(), triple.b(), triple.c() })
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

        assertPrecompute(solution, List.of(new Triple<>(entityWithGroup, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .ifExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                (a, b, c) -> a.getEntityGroup(), Function.identity())));
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

        assertPrecompute(solution, List.of(new Triple<>(entityWithoutGroup, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .ifNotExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                (a, b, c) -> a.getEntityGroup(), Function.identity())));
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

        assertPrecompute(solution, List.of(new Triple<>(entityGroup, 1, 1)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .groupBy(TestdataLavishEntity::getEntityGroup,
                                ConstraintCollectors.count(),
                                ConstraintCollectors.count()));
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

        assertPrecompute(solution, List.of(new Triple<>(entityWithoutGroup, entityWithoutGroup, value),
                new Triple<>(entityWithGroup, entityWithoutGroup, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .flatten(List::of)
                        .join(TestdataLavishValue.class));
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

        assertPrecompute(solution, List.of(new Triple<>(entity1, new ValueHolder(entity1.getIntegerProperty()), value),
                new Triple<>(entity2, new ValueHolder(entity2.getIntegerProperty()), value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .flatten(entity -> List.of(new ValueHolder(entity.getIntegerProperty())))
                        .join(TestdataLavishValue.class));
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

        assertPrecompute(solution, List.of(new Triple<>(entityWithoutGroup, value, value),
                new Triple<>(entityWithGroup, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList)
                        .join(TestdataLavishValue.class)
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

        assertPrecompute(solution, List.of(new Triple<>(new ValueHolder(1), value, value),
                new Triple<>(new ValueHolder(2), value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList
                                .stream()
                                .map(entity -> new ValueHolder(entity.getIntegerProperty()))
                                .toList())
                        .join(TestdataLavishValue.class)
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

        assertPrecompute(solution, List.of(new Triple<>(entityGroup, value, value),
                new Triple<>(entityGroup, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2) -> entity.getEntityGroup() != null)
                        .map((entity, joinedValue1, joinedValue2) -> entity.getEntityGroup(),
                                (entity, joinedValue1, joinedValue2) -> joinedValue1,
                                (entity, joinedValue1, joinedValue2) -> joinedValue2));
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

        assertPrecompute(solution,
                List.of(new Triple<>(entityWithoutGroup, value, value), new Triple<>(entityWithGroup, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2) -> entity.getEntityGroup() == null)
                        .concat(pf.forEachUnfiltered(TestdataLavishEntity.class)
                                .join(TestdataLavishValue.class)
                                .join(TestdataLavishValue.class)
                                .filter((entity, joinedValue1, joinedValue2) -> entity.getEntityGroup() != null)));
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

        assertPrecompute(solution, List.of(new Triple<>(entityGroup, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2) -> entity.getEntityGroup() != null)
                        .map((entity, joinedValue1, joinedValue2) -> entity.getEntityGroup(),
                                (entity, joinedValue1, joinedValue2) -> joinedValue1,
                                (entity, joinedValue1, joinedValue2) -> joinedValue2)
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
                new Triple<>(entityWithGroup1, value, value),
                new Triple<>(entityWithGroup2, value, value),
                new Triple<>(entityWithoutGroup, null, null)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2) -> entity.getEntityGroup() != null)
                        .complement(TestdataLavishEntity.class)
                        .distinct());
    }
}
