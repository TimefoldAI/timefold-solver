package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;

@NullMarked
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
