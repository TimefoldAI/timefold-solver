package ai.timefold.solver.quarkus.testdomain.neighborhood;

import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusEntity;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

public class TestdataQuarkusNeighborhoodProvider implements NeighborhoodProvider<TestdataQuarkusSolution> {

    @Override
    public Neighborhood defineNeighborhood(NeighborhoodBuilder<TestdataQuarkusSolution> builder) {
        var variable = builder.getSolutionMetaModel().genuineEntity(TestdataQuarkusEntity.class).basicVariable();
        return builder.add(new ChangeMoveProvider<>(variable)).build();
    }

}
