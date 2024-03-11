package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

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

    @Override
    public String toString() {
        return "{" + factA + ", " + factB + ", " + factC + "}";
    }

}
