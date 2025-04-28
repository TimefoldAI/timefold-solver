package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;

@NullMarked
record DefaultUnassignedElement() implements UnassignedElement {

    public static final DefaultUnassignedElement INSTANCE = new DefaultUnassignedElement();

    @Override
    public PositionInList ensureAssigned(Supplier<String> messageSupplier) {
        throw new IllegalStateException(messageSupplier.get());
    }

    @Override
    public String toString() {
        return "UnassignedLocation";
    }

}
