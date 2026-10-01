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

record PickGreatestValueMoveProvider(
        PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variableMetaModel)
        implements
            MoveProvider<TestdataSolution> {

    @Override
    public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> factory) {
        var entities = factory.forEach(TestdataEntity.class, false).asCachedDataset();
        var values = factory.forEach(TestdataValue.class, false).asCachedDataset();
        return factory.buildMoveStream((session, random) -> {
            var entityInstance = session.getInstance(entities);
            var valueInstance = session.getInstance(values);
            return new Iterator<>() {
                private final Iterator<TestdataEntity> entityIterator = entityInstance.iterator(random);

                @Override
                public boolean hasNext() {
                    return entityIterator.hasNext();
                }

                @Override
                public Move<TestdataSolution> next() {
                    var entity = entityIterator.next();
                    // A scoring rule not expressible as a join predicate: pick the value with the greatest code.
                    TestdataValue best = null;
                    var valueIterator = valueInstance.exhaustiveIterator(random);
                    while (valueIterator.hasNext()) {
                        var value = Objects.requireNonNull(valueIterator.next());
                        if (best == null || value.getCode().compareTo(best.getCode()) > 0) {
                            best = value;
                        }
                    }
                    return Moves.change(variableMetaModel, entity, Objects.requireNonNull(best));
                }
            };
        });
    }

}
