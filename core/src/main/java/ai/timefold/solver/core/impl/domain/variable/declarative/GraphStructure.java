package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
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
     * cannot be inconsistent.
     */
    SINGLE_DIRECTIONAL_PARENT,

    /**
     * A graph structure that accepts all graphs that only have a single
     * entity that uses declarative shadow variables with all directional
     * parents being the same type.
     */
    ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE,

    /**
     * A graph structure that accepts all graphs.
     */
    ARBITRARY,

    /**
     * A graph structure where a planning list variable's elements are excluded from the graph,
     * which covers the other entity classes with per-variable nodes. Each list entity additionally
     * gets a single block node representing its whole chain of elements, ordered after the entity's
     * pre-chain variables (which the elements read through their inverse) and before its post-chain
     * variables (which read the elements). When the block node is processed, it walks the entity's
     * list from each element whose sources changed, in the direction of
     * {@link GraphStructureAndDirection#direction()}.
     * The list elements the block nodes represent are available via {@link GraphStructureAndDirection#blockedElementClass()}.
     * This decomposition is valid because the elements only read their chain and, through their
     * inverse, declarative variables of their own list entity, and because the other
     * classes only reach the elements through the list variable itself.
     * Built as {@link #ARBITRARY} when there is no score director, or when those nodes would form
     * a dependency loop.
     */
    LIST_ELEMENT_BLOCK;

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStructure.class);

    public record GraphStructureAndDirection(GraphStructure structure,
            @Nullable VariableMetaModel<?, ?, ?> parentMetaModel,
            @Nullable ParentVariableType direction,
            @Nullable Class<?> blockedElementClass) {

        public GraphStructureAndDirection(GraphStructure structure,
                @Nullable VariableMetaModel<?, ?, ?> parentMetaModel,
                @Nullable ParentVariableType direction) {
            this(structure, parentMetaModel, direction, null);
        }
    }

    public static <Solution_> GraphStructureAndDirection determineGraphStructure(
            SolutionDescriptor<Solution_> solutionDescriptor,
            Object... entities) {
        var declarativeShadowVariableDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();
        if (declarativeShadowVariableDescriptors.isEmpty()) {
            return new GraphStructureAndDirection(EMPTY, null, null);
        }

        if (!doEntitiesUseDeclarativeShadowVariables(declarativeShadowVariableDescriptors, entities)) {
            return new GraphStructureAndDirection(EMPTY, null, null);
        }

        var blockAndDirection = determineListElementBlock(solutionDescriptor, declarativeShadowVariableDescriptors);
        if (blockAndDirection != null) {
            return new GraphStructureAndDirection(LIST_ELEMENT_BLOCK, null,
                    blockAndDirection.direction(), blockAndDirection.elementEntityClass());
        }
        return determineGraphStructure(declarativeShadowVariableDescriptors, entities);
    }

    private static <Solution_> GraphStructureAndDirection determineGraphStructure(
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            Object... entities) {
        var multipleDeclarativeEntityClasses = declarativeShadowVariableDescriptors.stream()
                .map(variable -> variable.getEntityDescriptor().getEntityClass())
                .distinct().count() > 1;

        final var arbitraryGraphStructure = new GraphStructureAndDirection(
                multipleDeclarativeEntityClasses ? ARBITRARY : ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE,
                null, null);

        var rootVariableSources = declarativeShadowVariableDescriptors.stream()
                .flatMap(descriptor -> Arrays.stream(descriptor.getSources()))
                .toList();
        ParentVariableType directionalType = null;
        VariableMetaModel<?, ?, ?> parentMetaModel = null;
        var isArbitrary = multipleDeclarativeEntityClasses;
        for (var variableSource : rootVariableSources) {
            var parentVariableType = variableSource.parentVariableType();
            LOGGER.trace("{} has parentVariableType {}", variableSource, parentVariableType);
            switch (parentVariableType) {
                case GROUP -> {
                    var groupMemberCount = new MutableInt(0);
                    for (var entity : entities) {
                        if (variableSource.rootEntity().isInstance(entity)) {
                            variableSource.valueEntityFunction().accept(entity, fromEntity -> groupMemberCount.increment());
                        }
                    }
                    if (groupMemberCount.intValue() != 0) {
                        isArbitrary = true;
                        var groupParentVariableType = variableSource.groupParentVariableType();
                        if (groupParentVariableType != null && groupParentVariableType.isDirectional()) {
                            var groupParentVariableMetamodel =
                                    variableSource.variableSourceReferences().get(0).variableMetaModel();
                            if (parentMetaModel == null) {
                                parentMetaModel = groupParentVariableMetamodel;
                            } else if (!parentMetaModel
                                    .equals(variableSource.variableSourceReferences().get(0).variableMetaModel())) {
                                return new GraphStructureAndDirection(GraphStructure.ARBITRARY, null, null);
                            }
                        }
                    }
                    // The group variable is unused/always empty
                }
                case INDIRECT, INVERSE, VARIABLE, LIST_ELEMENT -> isArbitrary = true;
                case NEXT, PREVIOUS -> {
                    if (parentMetaModel == null) {
                        parentMetaModel = variableSource.variableSourceReferences().get(0).variableMetaModel();
                        directionalType = parentVariableType;
                    } else if (!parentMetaModel.equals(variableSource.variableSourceReferences().get(0).variableMetaModel())) {
                        return new GraphStructureAndDirection(GraphStructure.ARBITRARY, null, null);
                    }
                }
                case NO_PARENT -> {
                    // Do nothing
                }
            }
        }

        if (isArbitrary) {
            return arbitraryGraphStructure;
        }

        if (directionalType == null) {
            return new GraphStructureAndDirection(NO_DYNAMIC_EDGES, null, null);
        } else {
            // Cannot use a single successor function if there are multiple entity classes
            return new GraphStructureAndDirection(SINGLE_DIRECTIONAL_PARENT, parentMetaModel, directionalType);
        }
    }

    private record ListElementBlockAndDirection(Class<?> elementEntityClass, ParentVariableType direction) {
    }

    /**
     * Non-null if every previous/next directional parent among the descriptors' sources agrees
     * on a single source variable and direction, which fixes the block's element entity class
     * and walk direction.
     */
    private static <Solution_> @Nullable ListElementBlockAndDirection findChainDirection(
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors) {
        VariableMetaModel<?, ?, ?> parentMetaModel = null;
        ParentVariableType direction = null;
        Class<?> elementEntityClass = null;
        for (var descriptor : declarativeShadowVariableDescriptors) {
            for (var source : descriptor.getSources()) {
                var parentVariableType = source.parentVariableType();
                if (parentVariableType == ParentVariableType.PREVIOUS || parentVariableType == ParentVariableType.NEXT) {
                    var sourceParentMetaModel = source.variableSourceReferences().getFirst().variableMetaModel();
                    if (parentMetaModel == null) {
                        parentMetaModel = sourceParentMetaModel;
                        direction = parentVariableType;
                        // The class declaring the directional parent; the elements may be of any
                        // subclass of it, as long as none of them declares a declarative variable.
                        elementEntityClass = sourceParentMetaModel.entity().type();
                    } else if (!parentMetaModel.equals(sourceParentMetaModel)
                            || direction != parentVariableType) {
                        // The block node walks each list in a single direction.
                        return null;
                    }
                }
            }
        }
        if (elementEntityClass == null || direction == null) {
            return null;
        }
        return new ListElementBlockAndDirection(elementEntityClass, direction);
    }

    /**
     * Non-null if the planning list variable's elements can be excluded from the variable
     * reference graph and represented by a per-entity block node instead;
     * see {@link GraphStructureAndDirection#blockedElementClass()}.
     * Only the element class's sources and the references towards the element class are
     * checked here: the rest of the model is covered by the graph, whatever its structure.
     */
    private static <Solution_> @Nullable ListElementBlockAndDirection determineListElementBlock(
            SolutionDescriptor<Solution_> solutionDescriptor,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors) {
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null) {
            return null;
        }
        var chainDirection = findChainDirection(declarativeShadowVariableDescriptors);
        if (chainDirection == null) {
            return null;
        }
        var elementEntityClass = chainDirection.elementEntityClass();
        var ownerEntityDescriptor = listVariableDescriptor.getEntityDescriptor();
        var ownerEntityClass = ownerEntityDescriptor.getEntityClass();
        if (!elementEntityClass.isAssignableFrom(listVariableDescriptor.getElementType())
                || ownerEntityClass.isAssignableFrom(elementEntityClass)
                || elementEntityClass.isAssignableFrom(ownerEntityClass)) {
            // The block node walks the list entity's list and classifies entities with instanceof,
            // so the element class must cover the list's elements and be distinct from the list entity.
            return null;
        }
        for (var descriptor : declarativeShadowVariableDescriptors) {
            if (descriptor.getAlignmentKeyMap() != null) {
                // The block node updates one entity at a time, both when it walks a chain
                // and when it recomputes the list entity's post-chain variables,
                // which an alignment key's grouped updater contradicts.
                return null;
            }
            var entityClass = descriptor.getEntityDescriptor().getEntityClass();
            if (entityClass != elementEntityClass
                    && (elementEntityClass.isAssignableFrom(entityClass)
                            || entityClass.isAssignableFrom(elementEntityClass))) {
                // The block node's walk applies every element updater to every element,
                // so a declarative variable declared elsewhere in the element hierarchy
                // would be applied to elements that do not have it.
                return null;
            }
        }
        if (ownerEntityDescriptor.getShadowVariableDescriptors().stream()
                .noneMatch(variableDescriptor -> variableDescriptor instanceof DeclarativeShadowVariableDescriptor<?>)) {
            // The block node tracks its looped status through the list entity's consistency state,
            // which only exists when the list entity has declarative shadow variables of its own.
            // A model whose only declarative variables are its elements' is covered by the
            // existing structures anyway.
            return null;
        }
        for (var descriptor : declarativeShadowVariableDescriptors) {
            var isElementSource = descriptor.getEntityDescriptor().getEntityClass() == elementEntityClass;
            for (var variableSource : descriptor.getSources()) {
                var parentVariableType = variableSource.parentVariableType();
                if (isElementSource) {
                    switch (parentVariableType) {
                        case PREVIOUS, NEXT -> {
                            // Safe: stays within the chain.
                        }
                        case NO_PARENT -> {
                            // Only safe when it does not access a declarative variable
                            // through another (non-declarative) variable,
                            // which would require the elements to be part of the graph.
                            if (variableSource.variableSourceReferences().size() != 1) {
                                return null;
                            }
                        }
                        case INVERSE -> {
                            // Only safe when it targets a declarative variable of the list entity.
                            // One that depends on the elements, directly or not, would close a loop
                            // through the block node, which the build detects.
                            if (variableSource.variableSourceReferences().getFirst()
                                    .downstreamDeclarativeVariableMetamodel() == null) {
                                return null;
                            }
                        }
                        case VARIABLE, INDIRECT, GROUP, LIST_ELEMENT -> {
                            // An element reached any other way would have to be part of the graph.
                            return null;
                        }
                    }
                } else if (parentVariableType == ParentVariableType.LIST_ELEMENT) {
                    // Only safe when it accesses the list's own elements directly:
                    // an entity reached through an element's fact may belong to another list,
                    // whose changes would not recompute this variable.
                    var reference = variableSource.variableSourceReferences().getFirst();
                    if (!reference.chainFromRootEntityToVariableEntity().isEmpty()) {
                        return null;
                    }
                } else {
                    // Elements are not part of the graph, so no other source may reach them.
                    for (var reference : variableSource.variableSourceReferences()) {
                        if (elementEntityClass.isAssignableFrom(reference.variableMetaModel().entity().type())) {
                            return null;
                        }
                    }
                }
            }
        }
        return chainDirection;
    }

    private static <Solution_> boolean doEntitiesUseDeclarativeShadowVariables(
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors, Object... entities) {
        boolean anyDeclarativeEntities = false;
        for (var declarativeShadowVariable : declarativeShadowVariableDescriptors) {
            var entityClass = declarativeShadowVariable.getEntityDescriptor().getEntityClass();
            for (var entity : entities) {
                if (entityClass.isInstance(entity)) {
                    anyDeclarativeEntities = true;
                    break;
                }
                if (anyDeclarativeEntities) {
                    break;
                }
            }
        }
        return anyDeclarativeEntities;
    }
}
