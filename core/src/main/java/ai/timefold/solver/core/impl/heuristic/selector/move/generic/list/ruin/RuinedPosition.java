package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

record RuinedPosition(Object ruinedValue, int index) implements Comparable<RuinedPosition> {

    @Override
    public int compareTo(RuinedPosition other) {
        return Integer.compare(index, other.index);
    }
}
