package ai.timefold.solver.core.preview.api.neighborhood.test;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

// Exercises MoveStreamFactory.register()/buildMoveStream(), for neighborhoods not expressible as a two-dataset
// join (here: a scoring rule picking a value by an arbitrary rule instead of a join predicate).
@NullMarked
class IteratorBasedMoveProviderTest {

    @Test
    void buildMoveStream_customScoringRuleNotExpressibleAsAJoin() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var solution = TestdataSolution.generateSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().get(0);
        var lastValue = solution.getValueList().get(1);

        var context = NeighborhoodTester.build(new PickLastValueMoveProvider(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, e1, lastValue),
                Moves.change(variableMetaModel, e2, lastValue));
        context.producesNoneOf(
                Moves.change(variableMetaModel, e1, firstValue),
                Moves.change(variableMetaModel, e2, firstValue));
    }

    private record PickLastValueMoveProvider(
            PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variableMetaModel)
            implements
                MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> factory) {
            var entities = factory.register(factory.forEach(TestdataEntity.class, false));
            var values = factory.register(factory.forEach(TestdataValue.class, false));
            return factory.buildMoveStream((session, random) -> {
                var entityInstance = session.getInstance(entities);
                var valueInstance = session.getInstance(values);
                return new Iterator<>() {
                    private final Iterator<TestdataEntity> entityIterator = entityInstance.randomIterator(random);

                    @Override
                    public boolean hasNext() {
                        return entityIterator.hasNext();
                    }

                    @Override
                    public Move<TestdataSolution> next() {
                        var entity = entityIterator.next();
                        // A scoring rule not expressible as a join predicate: pick the last value seen.
                        TestdataValue lastSeen = null;
                        var valueIterator = valueInstance.iterator();
                        while (valueIterator.hasNext()) {
                            lastSeen = valueIterator.next();
                        }
                        return Moves.change(variableMetaModel, entity, Objects.requireNonNull(lastSeen));
                    }
                };
            });
        }

    }

}
