package ai.timefold.solver.constraint.streams.bavet.common.tuple;

public final class QuadTuple<A, B, C, D> extends AbstractTuple {

    // Only a tuple's origin node may modify a fact.
    public A factA;
    public B factB;
    public C factC;
    public D factD;

    public QuadTuple(A factA, B factB, C factC, D factD, int storeSize) {
        super(storeSize);
        this.factA = factA;
        this.factB = factB;
        this.factC = factC;
        this.factD = factD;
    }

    public boolean updateIfDifferent(A newFactA, B newFactB, C newFactC, D newFactD) {
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
        if (factD != newFactD) {
            factD = newFactD;
            different = true;
        }
        return different;
    }

    @Override
    public String toString() {
        return "{" + factA + ", " + factB + ", " + factC + ", " + factD + "}";
    }

}
