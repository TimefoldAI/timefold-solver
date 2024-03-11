package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractFlattenLastNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;

final class BavetFlattenLastBiConstraintStream<Solution_, A, B, NewB>
        extends BavetAbstractBiConstraintStream<Solution_, A, B> {

    private final Function<B, Iterable<NewB>> mappingFunction;
    private BavetAftBridgeBiConstraintStream<Solution_, A, NewB> flattenLastStream;

    public BavetFlattenLastBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent,
            Function<B, Iterable<NewB>> mappingFunction) {
        super(constraintFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(BavetAftBridgeBiConstraintStream<Solution_, A, NewB> flattenLastStream) {
        this.flattenLastStream = flattenLastStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public boolean guaranteesDistinct() {
        return false;
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        assertEmptyChildStreamList();
        int inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        int outputStoreSize = buildHelper.extractTupleStoreSize(flattenLastStream);
        AbstractFlattenLastNode<BiTuple<A, B>, BiTuple<A, NewB>, B, NewB> node = new FlattenLastBiNode<>(
                inputStoreIndex, mappingFunction,
                buildHelper.getAggregatedTupleLifecycle(flattenLastStream.getChildStreamList()),
                outputStoreSize);
        buildHelper.addNode(node, this);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        BavetFlattenLastBiConstraintStream<?, ?, ?, ?> that = (BavetFlattenLastBiConstraintStream<?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(mappingFunction, that.mappingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunction);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "FlattenLast()";
    }

}
