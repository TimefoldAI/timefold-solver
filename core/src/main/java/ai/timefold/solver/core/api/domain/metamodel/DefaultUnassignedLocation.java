package ai.timefold.solver.core.api.domain.metamodel;

import java.util.function.Supplier;

record DefaultUnassignedLocation() implements UnassignedLocation {

    public static final DefaultUnassignedLocation INSTANCE = new DefaultUnassignedLocation();

    @Override
    public LocationInList ensureAssigned(Supplier<String> messageSupplier) {
        throw new IllegalStateException(messageSupplier.get());
    }

    @Override
    public String toString() {
        return "UnassignedLocation";
    }

}
