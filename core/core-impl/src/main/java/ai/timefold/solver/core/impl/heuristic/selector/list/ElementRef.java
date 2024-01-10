package ai.timefold.solver.core.impl.heuristic.selector.list;

/**
 * Points to a list variable position specified by an entity and an index.
 */
public record ElementRef(Object entity, int index) {

    @Override
    public String toString() {
        return entity + "[" + index + "]";
    }

}
