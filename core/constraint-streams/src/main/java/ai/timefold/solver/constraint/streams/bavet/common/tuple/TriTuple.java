package ai.timefold.solver.constraint.streams.bavet.common.tuple;

public final class TriTuple<A, B, C> extends AbstractTuple {

    // Only a tuple's origin node may modify a fact.
    public A factA;
    public B factB;
    public C factC;

    public TriTuple(A factA, B factB, C factC, int storeSize) {
        super(storeSize);
        this.factA = factA;
        this.factB = factB;
        this.factC = factC;
    }

    public boolean updateIfDifferent(A newFactA, B newFactB, C newFactC) {
        boolean different = false;
        if (factA != newFactA) {
            factA = newFactA;
            different = true;
        }
        if (factB != newFactB) {
            factB = newFactB;
            different = true;
        }
        if (factC != newFactC) {
            factC = newFactC;
            different = true;
        }
        return different;
    }

    @Override
    public String toString() {
        return "{" + factA + ", " + factB + ", " + factC + "}";
    }

}
