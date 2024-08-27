package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

record RuinedLocation(Object ruinedValue, int index) implements Comparable<RuinedLocation> {

    @Override
    public int compareTo(RuinedLocation other) {
        return index - other.index;
    }
}
