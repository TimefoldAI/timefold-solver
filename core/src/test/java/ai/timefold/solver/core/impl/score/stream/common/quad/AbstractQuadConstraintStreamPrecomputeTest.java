package ai.timefold.solver.core.impl.score.stream.common.quad;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.PrecomputeFactory;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamImplSupport;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamPrecomputeTest;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintStreamTestExtension;
import ai.timefold.solver.core.impl.util.Quadruple;
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
public abstract class AbstractQuadConstraintStreamPrecomputeTest extends AbstractConstraintStreamTest
        implements ConstraintStreamPrecomputeTest {
    protected AbstractQuadConstraintStreamPrecomputeTest(ConstraintStreamImplSupport implSupport) {
        super(implSupport);
    }

    private <A, B, C, D> void assertPrecomputeFilterChanged(
            TriFunction<PrecomputeFactory, TestdataLavishEntityGroup, TestdataLavishValueGroup, QuadConstraintStream<A, B, C, D>> precomputeStream,
            QuadFunction<A, B, C, D, TestdataLavishEntity> entityPicker,
            QuadFunction<TestdataLavishEntity, TestdataLavishValue, TestdataLavishEntityGroup, TestdataLavishValueGroup, Quadruple<A, B, C, D>> inputDataToTuple) {
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
                        .filter((a, b, c, d) -> entityPicker.apply(a, b, c, d).getValue() == value1)
                        .penalize(SimpleScore.ONE)
                        .asConstraint(TEST_CONSTRAINT_NAME));

        // From scratch
        var createMatch =
                (QuadFunction<TestdataLavishEntity, TestdataLavishValue, TestdataLavishEntityGroup, TestdataLavishValueGroup, AssertableMatch>) (
                        entity,
                        value, matchEntityGroup, matchValueGroup) -> {
                    var tuple = inputDataToTuple.apply(entity, value, matchEntityGroup, matchValueGroup);
                    return assertMatch(tuple.a(), tuple.b(), tuple.c(), tuple.d());
                };
        Mockito.reset(entity1);
        scoreDirector.setWorkingSolution(solution);
        assertScore(scoreDirector,
                createMatch.apply(entity1, value1, entityGroup, valueGroup),
                createMatch.apply(entity1, value2, entityGroup, valueGroup),
                createMatch.apply(entity2, value1, entityGroup, valueGroup),
                createMatch.apply(entity2, value2, entityGroup, valueGroup));
        Mockito.verify(entity1, Mockito.atLeastOnce()).getEntityGroup();

        // Incrementally update a variable
        Mockito.reset(entity1);
        scoreDirector.beforeVariableChanged(entity1, "value");
        entity1.setValue(solution.getFirstValue());
        scoreDirector.afterVariableChanged(entity1, "value");
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup, valueGroup),
                createMatch.apply(entity2, value2, entityGroup, valueGroup));
        Mockito.verify(entity1, Mockito.never()).getEntityGroup();

        // Incrementally update a fact
        scoreDirector.beforeProblemPropertyChanged(entity3);
        entity3.setEntityGroup(entityGroup);
        scoreDirector.afterProblemPropertyChanged(entity3);
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup, valueGroup),
                createMatch.apply(entity2, value2, entityGroup, valueGroup),
                createMatch.apply(entity3, value1, entityGroup, valueGroup),
                createMatch.apply(entity3, value2, entityGroup, valueGroup));

        // Remove entity
        scoreDirector.beforeEntityRemoved(entity3);
        solution.getEntityList().remove(entity3);
        scoreDirector.afterEntityRemoved(entity3);
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup, valueGroup),
                createMatch.apply(entity2, value2, entityGroup, valueGroup));

        // Add it back again, to make sure it was properly removed before
        scoreDirector.beforeEntityAdded(entity3);
        solution.getEntityList().add(entity3);
        scoreDirector.afterEntityAdded(entity3);
        assertScore(scoreDirector,
                createMatch.apply(entity2, value1, entityGroup, valueGroup),
                createMatch.apply(entity2, value2, entityGroup, valueGroup),
                createMatch.apply(entity3, value1, entityGroup, valueGroup),
                createMatch.apply(entity3, value2, entityGroup, valueGroup));
    }

    @Override
    @TestTemplate
    public void filter_0_changed() {
        assertPrecomputeFilterChanged(
                (precomputeFactory, entityGroup, valueGroup) -> precomputeFactory.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishEntityGroup.class)
                        .join(TestdataLavishValueGroup.class)
                        .filter((entity, value, matchedEntityGroup, matchedValueGroup) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup
                                && matchedEntityGroup == entityGroup
                                && matchedValueGroup == valueGroup),
                (entity, value, entityGroup, valueGroup) -> entity,
                Quadruple::new);
    }

    @Override
    @TestTemplate
    public void filter_1_changed() {
        assertPrecomputeFilterChanged(
                (precomputeFactory, entityGroup, valueGroup) -> precomputeFactory.forEachUnfiltered(TestdataLavishValue.class)
                        .join(TestdataLavishEntity.class)
                        .join(TestdataLavishEntityGroup.class)
                        .join(TestdataLavishValueGroup.class)
                        .filter((value, entity, matchedEntityGroup, matchedValueGroup) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup
                                && matchedEntityGroup == entityGroup
                                && matchedValueGroup == valueGroup),
                (value, entity, entityGroup, valueGroup) -> entity,
                (entity, value, entityGroup, valueGroup) -> new Quadruple<>(value, entity, entityGroup, valueGroup));
    }

    @Override
    @TestTemplate
    public void filter_2_changed() {
        assertPrecomputeFilterChanged(
                (precomputeFactory, entityGroup, valueGroup) -> precomputeFactory.forEachUnfiltered(TestdataLavishValue.class)
                        .join(TestdataLavishEntityGroup.class)
                        .join(TestdataLavishEntity.class)
                        .join(TestdataLavishValueGroup.class)
                        .filter((value, matchedEntityGroup, entity, matchedValueGroup) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup
                                && matchedEntityGroup == entityGroup
                                && matchedValueGroup == valueGroup),
                (value, entityGroup, entity, valueGroup) -> entity,
                (entity, value, entityGroup, valueGroup) -> new Quadruple<>(value, entityGroup, entity, valueGroup));
    }

    @Override
    @TestTemplate
    public void filter_3_changed() {
        assertPrecomputeFilterChanged(
                (precomputeFactory, entityGroup, valueGroup) -> precomputeFactory.forEachUnfiltered(TestdataLavishValue.class)
                        .join(TestdataLavishEntityGroup.class)
                        .join(TestdataLavishValueGroup.class)
                        .join(TestdataLavishEntity.class)
                        .filter((value, matchedEntityGroup, matchedValueGroup, entity) -> entity.getEntityGroup() == entityGroup
                                && value.getValueGroup() == valueGroup
                                && matchedEntityGroup == entityGroup
                                && matchedValueGroup == valueGroup),
                (value, entityGroup, valueGroup, entity) -> entity,
                (entity, value, entityGroup, valueGroup) -> new Quadruple<>(value, entityGroup, valueGroup, entity));
    }

    private <A, B, C, D> void assertPrecompute(TestdataLavishSolution solution,
            List<Quadruple<A, B, C, D>> expectedValues,
            Function<PrecomputeFactory, QuadConstraintStream<A, B, C, D>> entityStreamSupplier) {
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
                .map(quad -> new Object[] { quad.a(), quad.b(), quad.c(), quad.d() })
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

        assertPrecompute(solution, List.of(new Quadruple<>(entityWithGroup, value, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .ifExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                (a, b, c, d) -> a.getEntityGroup(), Function.identity())));
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

        assertPrecompute(solution, List.of(new Quadruple<>(entityWithoutGroup, value, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .ifNotExists(TestdataLavishEntityGroup.class, Joiners.equal(
                                (a, b, c, d) -> a.getEntityGroup(), Function.identity())));
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

        assertPrecompute(solution, List.of(new Quadruple<>(entityGroup, 1, 1, 1)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .filter(entity -> entity.getEntityGroup() != null)
                        .groupBy(TestdataLavishEntity::getEntityGroup,
                                ConstraintCollectors.count(),
                                ConstraintCollectors.count(),
                                ConstraintCollectors.count()));
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

        assertPrecompute(solution, List.of(new Quadruple<>(entityWithoutGroup, value, value, value),
                new Quadruple<>(entityWithGroup, value, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList)
                        .join(TestdataLavishValue.class)
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

        assertPrecompute(solution, List.of(new Quadruple<>(new ValueHolder(1), value, value, value),
                new Quadruple<>(new ValueHolder(2), value, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .groupBy(ConstraintCollectors.toList())
                        .flattenLast(entityList -> entityList
                                .stream()
                                .map(entity -> new ValueHolder(entity.getIntegerProperty()))
                                .toList())
                        .join(TestdataLavishValue.class)
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

        assertPrecompute(solution, List.of(new Quadruple<>(entityGroup, value, value, value),
                new Quadruple<>(entityGroup, value, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2, joinedValue3) -> entity.getEntityGroup() != null)
                        .map((entity, joinedValue1, joinedValue2, joinedValue3) -> entity.getEntityGroup(),
                                (entity, joinedValue1, joinedValue2, joinedValue3) -> joinedValue1,
                                (entity, joinedValue1, joinedValue2, joinedValue3) -> joinedValue2,
                                (entity, joinedValue1, joinedValue2, joinedValue3) -> joinedValue3));
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
                List.of(new Quadruple<>(entityWithoutGroup, value, value, value),
                        new Quadruple<>(entityWithGroup, value, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2, joinedValue3) -> entity.getEntityGroup() == null)
                        .concat(pf.forEachUnfiltered(TestdataLavishEntity.class)
                                .join(TestdataLavishValue.class)
                                .join(TestdataLavishValue.class)
                                .join(TestdataLavishValue.class)
                                .filter((entity, joinedValue1, joinedValue2,
                                        joinedValue3) -> entity.getEntityGroup() != null)));
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

        assertPrecompute(solution, List.of(new Quadruple<>(entityGroup, value, value, value)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2, joinedValue3) -> entity.getEntityGroup() != null)
                        .map((entity, joinedValue1, joinedValue2, joinedValue3) -> entity.getEntityGroup(),
                                (entity, joinedValue1, joinedValue2, joinedValue3) -> joinedValue1,
                                (entity, joinedValue1, joinedValue2, joinedValue3) -> joinedValue2,
                                (entity, joinedValue1, joinedValue2, joinedValue3) -> joinedValue3)
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
                new Quadruple<>(entityWithGroup1, value, value, value),
                new Quadruple<>(entityWithGroup2, value, value, value),
                new Quadruple<>(entityWithoutGroup, null, null, null)),
                pf -> pf.forEachUnfiltered(TestdataLavishEntity.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .join(TestdataLavishValue.class)
                        .filter((entity, joinedValue1, joinedValue2, joinedValue3) -> entity.getEntityGroup() != null)
                        .complement(TestdataLavishEntity.class));
    }
}
