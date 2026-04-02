package ai.timefold.solver.core.impl.heuristic.selector.list;

import ai.timefold.solver.core.api.domain.common.Lookup;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record SubList(@Nullable Object entity, int fromIndex, int length) {

    public int getToIndex() {
        return fromIndex + length;
    }

    public SubList rebase(Lookup lookup) {
        return new SubList(lookup.lookUpWorkingObject(entity), fromIndex, length);
    }

    @Override
    public String toString() {
        return "%s[%d..%d]".formatted(entity, fromIndex, getToIndex());
    }

}
