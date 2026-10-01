package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.bavet.AbstractBavetNodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Scorer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Represents Constraint Streams' network of nodes, specific to a particular session.
 * Nodes only used by disabled constraints have already been removed.
 *
 */
@NullMarked
public final class ConstraintStreamsBavetNodeNetwork extends AbstractBavetNodeNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintStreamsBavetNodeNetwork.class);

    public static ConstraintStreamsBavetNodeNetwork of(List<AbstractNode> nodeList,
            Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            Map<BavetConstraint<?>, Scorer<?>> constraintToScorerMap, Function<AbstractNode, Propagator> propagatorFunction,
            @Nullable InnerConstraintProfiler constraintProfiler, boolean scoreDirectorDerived) {
        var layeredNodes = AbstractBavetNodeNetwork.buildLayeredNodes(nodeList);
        return new ConstraintStreamsBavetNodeNetwork(declaredClassToNodeMap, constraintToScorerMap, layeredNodes,
                propagatorFunction, constraintProfiler, scoreDirectorDerived);
    }

    public static final ConstraintStreamsBavetNodeNetwork EMPTY =
            new ConstraintStreamsBavetNodeNetwork(Map.of(), Map.of(), new AbstractNode[0][0], AbstractNode::getPropagator, null,
                    true);

    private final Map<BavetConstraint<?>, Scorer<?>> constraintToScorerMap;
    private final @Nullable InnerConstraintProfiler constraintProfiler;
    private final boolean scoreDirectorDerived;
    private boolean printedInactiveConstraints = false;

    /**
     * @param declaredClassToNodeMap starting nodes, one for each class used in the constraints;
     *        root nodes, layer index 0.
     * @param layeredNodes nodes grouped first by their layer, then by their index within the layer;
     *        propagation needs to happen in this order.
     * @param propagatorFunction function to get the propagator for a given node
     */
    private ConstraintStreamsBavetNodeNetwork(Map<Class<?>, List<AbstractRootNode<?>>> declaredClassToNodeMap,
            Map<BavetConstraint<?>, Scorer<?>> constraintToScorerMap, AbstractNode[][] layeredNodes,
            Function<AbstractNode, Propagator> propagatorFunction, @Nullable InnerConstraintProfiler constraintProfiler,
            boolean scoreDirectorDerived) {
        super(declaredClassToNodeMap, layeredNodes, propagatorFunction);
        this.constraintToScorerMap = constraintToScorerMap;
        this.constraintProfiler = constraintProfiler;
        this.scoreDirectorDerived = scoreDirectorDerived;
    }

    @Override
    public void settle() {
        super.settle();
        var loggingLevel = Level.DEBUG; // Makes sure the check and the logging always operate on the same level.
        if (!LOGGER.isEnabledForLevel(loggingLevel)) {
            return;
        }
        if (!scoreDirectorDerived && !printedInactiveConstraints && isActivationCheckComplete()) {
            printedInactiveConstraints = true;
            var substring = constraintToScorerMap.entrySet().stream()
                    .filter(entry -> !entry.getValue().isActive())
                    .map(entry -> "  Constraint (%s) with weight set to (%s).".formatted(entry.getKey().getConstraintRef(),
                            entry.getValue().getWeight()))
                    .collect(Collectors.joining(System.lineSeparator()));
            if (substring.isEmpty()) {
                return;
            }
            LOGGER.atLevel(loggingLevel).log("""
                    Constraints deactivated due to being useless in the given working solution:
                    %s""".formatted(substring));
        }
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
