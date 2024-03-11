package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

public final class BiTuple<A, B> extends AbstractTuple {

    // Only a tuple's origin node may modify a fact.
    public A factA;
    public B factB;

    public BiTuple(A factA, B factB, int storeSize) {
        super(storeSize);
        this.factA = factA;
        this.factB = factB;
    }

    @Override
    public String toString() {
        return "{" + factA + ", " + factB + "}";
    }

}
