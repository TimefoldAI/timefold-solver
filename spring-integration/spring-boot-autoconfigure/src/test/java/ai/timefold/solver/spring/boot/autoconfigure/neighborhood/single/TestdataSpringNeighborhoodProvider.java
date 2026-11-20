package ai.timefold.solver.spring.boot.autoconfigure.neighborhood.single;

import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution;

public class TestdataSpringNeighborhoodProvider implements NeighborhoodProvider<TestdataSpringSolution> {

    @Override
    public Neighborhood defineNeighborhood(NeighborhoodBuilder<TestdataSpringSolution> builder) {
        var variable = builder.getSolutionMetaModel().genuineEntity(TestdataSpringEntity.class).basicVariable();
        return builder.add(new ChangeMoveProvider<>(variable)).build();
    }

}
