package ai.timefold.solver.examples.vehiclerouting.domain.location;

import ai.timefold.solver.examples.common.persistence.jackson.AbstractKeyDeserializer;
import ai.timefold.solver.examples.vehiclerouting.persistence.VehicleRoutingSolutionFileIO;

/**
 * @see VehicleRoutingSolutionFileIO
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
