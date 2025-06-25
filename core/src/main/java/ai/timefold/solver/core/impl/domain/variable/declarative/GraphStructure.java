package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GraphStructure {
    /**
     * A graph structure that only accepts the empty graph.
     */
    EMPTY,

    /**
     * A graph structure without dynamic edges. The topological order
     * of such a graph is fixed, since edges are neither added nor removed.
     */
    NO_DYNAMIC_EDGES,

    /**
     * A graph structure where there is at most
     * one directional parent for each graph node, and
     * no indirect parents.
     * For example, when the only input variable from
     * a different entity is previous. This allows us
     * to use a successor function to find affected entities.
     * Since there is at most a single parent node, such a graph
     * cannot be looped.
     */
    SINGLE_DIRECTIONAL_PARENT,

    /**
     * A graph structure that accepts all graphs.
     */
    ARBITRARY;

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStructure.class);

    public record GraphStructureAndDirection(GraphStructure structure,
            @Nullable VariableMetaModel<?, ?, ?> parentMetaModel,
            @Nullable ParentVariableType direction) {
    }

    public static <Solution_> GraphStructureAndDirection determineGraphStructure(
            SolutionDescriptor<Solution_> solutionDescriptor,
            Object... entities) {
        var declarativeShadowVariableDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();
        if (declarativeShadowVariableDescriptors.isEmpty()) {
            return new GraphStructureAndDirection(EMPTY, null, null);
        }
        var multipleDeclarativeEntityClasses = declarativeShadowVariableDescriptors.stream()
                .map(variable -> variable.getEntityDescriptor().getEntityClass())
                .distinct().count() > 1;

        var rootVariableSources = declarativeShadowVariableDescriptors.stream()
                .flatMap(descriptor -> Arrays.stream(descriptor.getSources()))
                .toList();
        ParentVariableType directionalType = null;
        VariableMetaModel<?, ?, ?> parentMetaModel = null;
        for (var variableSource : rootVariableSources) {
            var parentVariableType = variableSource.parentVariableType();
            LOGGER.trace("{} has parentVariableType {}", variableSource, parentVariableType);
            switch (parentVariableType) {
                case GROUP -> {
                    var groupMemberCount = new MutableInt(0);
                    for (var entity : entities) {
                        if (variableSource.rootEntity().isInstance(entity)) {
                            variableSource.valueEntityFunction().accept(entity, fromEntity -> {
                                groupMemberCount.increment();
                            });
                        }
                    }
                    if (groupMemberCount.intValue() != 0) {
                        return new GraphStructureAndDirection(ARBITRARY, null, null);
                    }
                    // The group variable is unused/always empty
                }
                case INDIRECT, INVERSE -> {
                    return new GraphStructureAndDirection(ARBITRARY, null, null);
                }
                case NEXT, PREVIOUS, CHAINED_NEXT, CHAINED_PREVIOUS -> {
                    if (parentMetaModel == null) {
                        parentMetaModel = variableSource.variableSourceReferences().get(0).variableMetaModel();
                        directionalType = parentVariableType;
                    } else if (!parentMetaModel.equals(variableSource.variableSourceReferences().get(0).variableMetaModel())) {
                        return new GraphStructureAndDirection(ARBITRARY, null, null);
                    }
                }
                case NO_PARENT -> {
                    // Do nothing
                }
            }
        }

        if (directionalType == null) {
            return new GraphStructureAndDirection(NO_DYNAMIC_EDGES, null, null);
        } else {
            // Cannot use a single successor function if there are multiple entity classes
            return multipleDeclarativeEntityClasses ? new GraphStructureAndDirection(ARBITRARY, null, null)
                    : new GraphStructureAndDirection(SINGLE_DIRECTIONAL_PARENT, parentMetaModel, directionalType);
        }
    }
}
