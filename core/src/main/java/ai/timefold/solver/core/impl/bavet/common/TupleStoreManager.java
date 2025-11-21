package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;

final class TupleStoreManager<Stream_ extends BavetStream>
        implements InOutTupleStorePositionTracker {

    private final AbstractNodeBuildHelper<Stream_> buildHelper;
    private final Stream_ leftParentSource;
    private final Stream_ rightParentSource;
    private int effectiveOutputStoreSize;
    private int finalOutputStoreSize = -1;

    public TupleStoreManager(AbstractNodeBuildHelper<Stream_> buildHelper, Stream_ thisStream, Stream_ leftParentSource,
                             Stream_ rightParentSource) {
        this.buildHelper = buildHelper;
        this.leftParentSource = leftParentSource;
        this.rightParentSource = rightParentSource;
        this.effectiveOutputStoreSize = buildHelper.extractTupleStoreSize(thisStream);
    }

    @Override
    public int reserveNextLeft() {
        return buildHelper.reserveTupleStoreIndex(leftParentSource);
    }

    @Override
    public int reserveNextRight() {
        return buildHelper.reserveTupleStoreIndex(rightParentSource);
    }

    @Override
    public int reserveNextOut() {
        if (finalOutputStoreSize != -1) {
            throw new IllegalStateException("The final output store size has already been computed.");
        }
        return effectiveOutputStoreSize++;
    }

    @Override
    public int computeStoreSize() {
        if (finalOutputStoreSize == -1) {
            finalOutputStoreSize = effectiveOutputStoreSize;
        }
        return finalOutputStoreSize;
    }
}
