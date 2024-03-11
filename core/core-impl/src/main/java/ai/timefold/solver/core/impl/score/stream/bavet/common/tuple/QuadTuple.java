package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

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

    @Override
    public String toString() {
        return "{" + factA + ", " + factB + ", " + factC + ", " + factD + "}";
    }

}
