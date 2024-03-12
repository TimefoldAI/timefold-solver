package ai.timefold.solver.core.impl.heuristic.selector.list;

public record UnassignedLocation() implements ElementLocation {

    public static final UnassignedLocation INSTANCE = new UnassignedLocation();

}
