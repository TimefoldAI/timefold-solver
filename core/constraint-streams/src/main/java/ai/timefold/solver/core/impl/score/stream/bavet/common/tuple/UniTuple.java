package ai.timefold.solver.core.impl.score.stream.bavet.common.tuple;

public final class UniTuple<A> extends AbstractTuple {

    // Only a tuple's origin node may modify a fact.
    public A factA;

    public UniTuple(A factA, int storeSize) {
        super(storeSize);
        this.factA = factA;
    }

    @Override
    public String toString() {
        return "{" + factA + "}";
    }

}
