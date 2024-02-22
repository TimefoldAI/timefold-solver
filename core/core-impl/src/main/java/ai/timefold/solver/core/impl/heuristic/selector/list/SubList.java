package ai.timefold.solver.core.impl.heuristic.selector.list;

import ai.timefold.solver.core.api.score.director.ScoreDirector;

public record SubList(Object entity, int fromIndex, int length) {

    public int getToIndex() {
        return fromIndex + length;
    }

    public SubList rebase(ScoreDirector<?> destinationScoreDirector) {
        return new SubList(destinationScoreDirector.lookUpWorkingObject(entity), fromIndex, length);
    }

    @Override
    public String toString() {
        return entity + "[" + fromIndex + ".." + getToIndex() + "]";
    }
}
