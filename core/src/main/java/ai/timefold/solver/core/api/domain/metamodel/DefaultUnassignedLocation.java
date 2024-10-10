package ai.timefold.solver.core.api.domain.metamodel;

record DefaultUnassignedLocation() implements UnassignedLocation {

    public static final DefaultUnassignedLocation INSTANCE = new DefaultUnassignedLocation();

    @Override
    public String toString() {
        return "UnassignedLocation";
    }
}
