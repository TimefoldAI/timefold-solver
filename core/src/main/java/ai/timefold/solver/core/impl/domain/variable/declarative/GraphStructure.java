package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GraphStructure {
    /**
     * A graph structure that only accepts the empty graph.
     */
    EMPTY,

    /**
     * A graph structure without dynamic edges.
     */
    NO_DYNAMIC_EDGES,

    /**
     * A graph structure where there is at most
     * one directional parent for each graph node.
     */
    SINGLE_DIRECTIONAL_PARENT,

    /**
     * A graph structure that accepts all graphs.
     */
    ARBITRARY;

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStructure.class);

    public static <Solution_> GraphStructure determineGraphStructure(SolutionDescriptor<Solution_> solutionDescriptor) {
        var declarativeShadowVariableDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();
        if (declarativeShadowVariableDescriptors.isEmpty()) {
            return EMPTY;
        }
        var multipleDeclarativeEntityClasses = declarativeShadowVariableDescriptors.stream()
                .map(variable -> variable.getEntityDescriptor().getEntityClass())
                .distinct().count() > 1;

        if (multipleDeclarativeEntityClasses) {
            // Inverse might become directional if it has
            // declarative variables; ARBITRARY does optimize
            // the graph to NO_DYNAMIC_EDGES if there are no variable listeners that
            // add/remove edges.
            return ARBITRARY;
        }

        var rootVariableSources = declarativeShadowVariableDescriptors.stream()
                .flatMap(descriptor -> Arrays.stream(descriptor.getSources()))
                .toList();
        ParentVariableType directionalType = null;
        for (var variableSource : rootVariableSources) {
            var parentVariableType = variableSource.parentVariableType();
            LOGGER.debug("{} has parentVariableType {}", variableSource, parentVariableType);
            switch (parentVariableType) {
                case GROUP, INDIRECT_DIRECTIONAL, CHAINED_INVERSE -> {
                    // CHAINED_INVERSE is arbitrary, since we don't have
                    // the concept of "index" to help us identify topological
                    // order without creating a graph.
                    return GraphStructure.ARBITRARY;
                }
                case NEXT, PREVIOUS -> {
                    if (directionalType == null) {
                        directionalType = parentVariableType;
                    } else if (directionalType != parentVariableType) {
                        return GraphStructure.ARBITRARY;
                    }
                }
                case UNDIRECTIONAL -> {
                    // Do nothing
                }
                default -> {
                    throw new IllegalStateException("Unhandled case %s".formatted(variableSource.parentVariableType()));
                }
            }
        }

        if (directionalType == null) {
            return NO_DYNAMIC_EDGES;
        } else {
            return SINGLE_DIRECTIONAL_PARENT;
        }
    }
}
