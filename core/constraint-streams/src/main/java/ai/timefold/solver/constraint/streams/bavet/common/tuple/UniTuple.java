package ai.timefold.solver.constraint.streams.bavet.common.tuple;

public final class UniTuple<A> extends AbstractTuple {

    // Only a tuple's origin node may modify a fact.
    public A factA;

    public UniTuple(A factA, int storeSize) {
        super(storeSize);
        this.factA = factA;
    }

    public boolean updateIfDifferent(A newFactA) {
        if (factA != newFactA) {
            factA = newFactA;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + factA + "}";
    }

}
