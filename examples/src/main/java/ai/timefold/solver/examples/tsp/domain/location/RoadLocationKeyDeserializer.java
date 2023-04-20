package ai.timefold.solver.examples.tsp.domain.location;

import ai.timefold.solver.examples.common.persistence.jackson.AbstractKeyDeserializer;
import ai.timefold.solver.examples.tsp.persistence.TspSolutionFileIO;

/**
 * @see TspSolutionFileIO
 */
final class RoadLocationKeyDeserializer extends AbstractKeyDeserializer<RoadLocation> {

    public RoadLocationKeyDeserializer() {
        super(RoadLocation.class);
    }

    @Override
    protected RoadLocation createInstance(long id) {
        return new RoadLocation(id);
    }
}
