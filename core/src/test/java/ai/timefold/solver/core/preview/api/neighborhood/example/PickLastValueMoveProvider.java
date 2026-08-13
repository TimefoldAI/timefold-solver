package ai.timefold.solver.core.preview.api.neighborhood.example;

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

record PickLastValueMoveProvider(
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
