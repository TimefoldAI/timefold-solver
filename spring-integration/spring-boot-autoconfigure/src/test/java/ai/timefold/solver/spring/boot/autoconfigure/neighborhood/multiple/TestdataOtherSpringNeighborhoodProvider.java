package ai.timefold.solver.spring.boot.autoconfigure.neighborhood.multiple;

import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.basic.domain.TestdataSpringSolution;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestdataOtherSpringNeighborhoodProvider implements NeighborhoodProvider<TestdataSpringSolution> {

    @Override
    public Neighborhood defineNeighborhood(NeighborhoodBuilder<TestdataSpringSolution> builder) {
        var variable = builder.getSolutionMetaModel().genuineEntity(TestdataSpringEntity.class).basicVariable();
        return builder.add(new ChangeMoveProvider<>(variable)).build();
    }

}
