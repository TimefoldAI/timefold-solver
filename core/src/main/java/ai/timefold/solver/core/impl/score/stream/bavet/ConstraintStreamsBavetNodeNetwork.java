package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.AbstractBavetNodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;
import ai.timefold.solver.core.impl.bavet.common.Propagator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents Constraint Streams's network of nodes, specific to a particular session.
 * Nodes only used by disabled constraints have already been removed.
 *
 */
@NullMarked
public final class ConstraintStreamsBavetNodeNetwork extends AbstractBavetNodeNetwork {

    public static ConstraintStreamsBavetNodeNetwork of(List<AbstractNode> nodeList,
            Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            Function<AbstractNode, Propagator> propagatorFunction, @Nullable InnerConstraintProfiler constraintProfiler) {
        var layeredNodes = AbstractBavetNodeNetwork.buildLayeredNodes(nodeList, propagatorFunction);
        return new ConstraintStreamsBavetNodeNetwork(declaredClassToNodeMap, layeredNodes, constraintProfiler);
    }

    public static final ConstraintStreamsBavetNodeNetwork EMPTY =
            new ConstraintStreamsBavetNodeNetwork(Map.of(), new Propagator[0][0], null);

    private final @Nullable InnerConstraintProfiler constraintProfiler;

    /**
     * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
     *        root nodes, layer index 0.
     * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
     *        propagation needs to happen in this order.
     */
    private ConstraintStreamsBavetNodeNetwork(Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            Propagator[][] layeredNodes, @Nullable InnerConstraintProfiler constraintProfiler) {
        super(declaredClassToNodeMap, layeredNodes);
        this.constraintProfiler = constraintProfiler;
    }

    public @Nullable InnerConstraintProfiler getConstraintProfiler() {
        return constraintProfiler;
    }

    public void summarizeProfileIfPresent() {
        if (constraintProfiler != null) {
            constraintProfiler.summarize();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConstraintStreamsBavetNodeNetwork))
            return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
