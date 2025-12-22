package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class MapUniToBiNode<A, NewA, NewB> extends AbstractMapNode<UniTuple<A>, BiTuple<NewA, NewB>> {

    private final Function<A, NewA> mappingFunctionA;
    private final Function<A, NewB> mappingFunctionB;

    public MapUniToBiNode(int mapStoreIndex, Function<A, NewA> mappingFunctionA, Function<A, NewB> mappingFunctionB,
            TupleLifecycle<BiTuple<NewA, NewB>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
    }

    @Override
    protected BiTuple<NewA, NewB> map(UniTuple<A> tuple) {
        var factA = tuple.getA();
        return BiTuple.of(mappingFunctionA.apply(factA), mappingFunctionB.apply(factA), outputStoreSize);
    }

    @Override
    protected void remap(UniTuple<A> inTuple, BiTuple<NewA, NewB> outTuple) {
        var factA = inTuple.getA();
        outTuple.setA(mappingFunctionA.apply(factA));
        outTuple.setB(mappingFunctionB.apply(factA));
    }

}
