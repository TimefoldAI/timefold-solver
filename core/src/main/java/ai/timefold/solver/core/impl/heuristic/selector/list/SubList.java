package ai.timefold.solver.core.impl.heuristic.selector.list;

import ai.timefold.solver.core.preview.api.move.Rebaser;

public record SubList(Object entity, int fromIndex, int length) {

    public int getToIndex() {
        return fromIndex + length;
    }

    public SubList rebase(Rebaser rebaser) {
        return new SubList(rebaser.rebase(entity), fromIndex, length);
    }

    @Override
    public String toString() {
        return entity + "[" + fromIndex + ".." + getToIndex() + "]";
    }
}
