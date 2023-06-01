package ai.timefold.solver.constraint.streams.bavet.common.tuple;

public final class BiTuple<A, B> extends AbstractTuple {

    // Only a tuple's origin node may modify a fact.
    public A factA;
    public B factB;

    public BiTuple(A factA, B factB, int storeSize) {
        super(storeSize);
        this.factA = factA;
        this.factB = factB;
    }

    public boolean updateIfDifferent(A newFactA, B newFactB) {
        boolean different = false;
        if (factA != newFactA) {
            factA = newFactA;
            different = true;
        }
        if (factB != newFactB) {
            factB = newFactB;
            different = true;
        }
        return different;
    }

    @Override
    public String toString() {
        return "{" + factA + ", " + factB + "}";
    }

}
