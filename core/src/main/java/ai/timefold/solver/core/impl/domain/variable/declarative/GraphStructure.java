package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    ARBITRARY;

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStructure.class);

    /**
     * When present, the planning list variable's elements are excluded from the variable
     * reference graph, which only covers the other entity classes;
     * the elements are instead updated by a cascade that walks each dirty entity's list
     * in the direction of {@link GraphStructureAndDirection#direction()}.
     * This decomposition is valid because the elements only read their chain and,
     * through their inverse, pre-chain declarative variables of their own list entity,
     * and because the other classes only reach the elements through the list variable itself.
     */
    public record ListElementCascade(Class<?> elementEntityClass) {
    }

    public record GraphStructureAndDirection(GraphStructure structure,
            @Nullable VariableMetaModel<?, ?, ?> parentMetaModel,
            @Nullable ParentVariableType direction,
            @Nullable ListElementCascade elementCascade) {

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

        var elementCascadeAndDirection = determineListElementCascade(solutionDescriptor,
                declarativeShadowVariableDescriptors);
        if (elementCascadeAndDirection != null) {
            var elementEntityClass = elementCascadeAndDirection.elementCascade().elementEntityClass();
            var innerDescriptors = declarativeShadowVariableDescriptors.stream()
                    .filter(descriptor -> !elementEntityClass
                            .isAssignableFrom(descriptor.getEntityDescriptor().getEntityClass()))
                    .toList();
            var innerStructure = determineGraphStructure(innerDescriptors, elementEntityClass, entities);
            return new GraphStructureAndDirection(innerStructure.structure(),
                    elementCascadeAndDirection.parentMetaModel(),
                    elementCascadeAndDirection.direction(),
                    elementCascadeAndDirection.elementCascade());
        }
        return determineGraphStructure(declarativeShadowVariableDescriptors, null, entities);
    }

    private static <Solution_> GraphStructureAndDirection determineGraphStructure(
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            @Nullable Class<?> cascadedElementClass,
            Object... entities) {
        if (declarativeShadowVariableDescriptors.isEmpty()
                || !doEntitiesUseDeclarativeShadowVariables(declarativeShadowVariableDescriptors, entities)) {
            return new GraphStructureAndDirection(EMPTY, null, null);
        }
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
                case INDIRECT, INVERSE, VARIABLE -> isArbitrary = true;
                case LIST_ELEMENT -> {
                    // Under an element cascade, the list's elements are not part of the graph;
                    // a processor recomputes the target variable when the list or its elements change,
                    // so the source does not need any edges.
                    if (cascadedElementClass == null) {
                        isArbitrary = true;
                    }
                }
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

    private record ListElementCascadeAndDirection(ListElementCascade elementCascade,
            VariableMetaModel<?, ?, ?> parentMetaModel,
            ParentVariableType direction) {
    }

    /**
     * Non-null if the planning list variable's elements can be excluded from the variable
     * reference graph and updated by a cascade instead; see {@link ListElementCascade}.
     * Only the element class's sources and the references towards the element class are
     * checked here: the rest of the model is covered by the graph, whatever its structure.
     */
    private static <Solution_> @Nullable ListElementCascadeAndDirection determineListElementCascade(
            SolutionDescriptor<Solution_> solutionDescriptor,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors) {
        var listVariableDescriptorList = solutionDescriptor.getListVariableDescriptorList();
        if (listVariableDescriptorList.size() != 1) {
            // The detection does not match list element sources against a specific list variable,
            // and the wrapper routes every list change event to the cascade;
            // both rely on the elements' list being the model's only list variable,
            // which SolutionDescriptor currently guarantees. Re-audit both before lifting this.
            return null;
        }
        var listVariableDescriptor = listVariableDescriptorList.getFirst();
        // The element class is the entity class of the single previous or next directional parent.
        VariableMetaModel<?, ?, ?> parentMetaModel = null;
        ParentVariableType direction = null;
        Class<?> elementEntityClass = null;
        for (var descriptor : declarativeShadowVariableDescriptors) {
            for (var source : descriptor.getSources()) {
                var parentVariableType = source.parentVariableType();
                if (parentVariableType == ParentVariableType.PREVIOUS || parentVariableType == ParentVariableType.NEXT) {
                    var sourceParentMetaModel = source.variableSourceReferences().get(0).variableMetaModel();
                    if (parentMetaModel == null) {
                        parentMetaModel = sourceParentMetaModel;
                        direction = parentVariableType;
                        // The class declaring the directional parent, so extended element classes are covered.
                        elementEntityClass = sourceParentMetaModel.entity().type();
                    } else if (!parentMetaModel.equals(sourceParentMetaModel)) {
                        return null;
                    }
                }
            }
        }
        if (elementEntityClass == null || direction == null) {
            return null;
        }
        var ownerEntityClass = listVariableDescriptor.getEntityDescriptor().getEntityClass();
        if (!elementEntityClass.isAssignableFrom(listVariableDescriptor.getElementType())
                || ownerEntityClass.isAssignableFrom(elementEntityClass)
                || elementEntityClass.isAssignableFrom(ownerEntityClass)) {
            // The cascade walks the owner's list and classifies entities with instanceof,
            // so the element class must cover the list's elements and be distinct from the owner.
            return null;
        }
        var elementDescriptorList = new ArrayList<DeclarativeShadowVariableDescriptor<Solution_>>();
        var hasNonElementDescriptors = false;
        for (var descriptor : declarativeShadowVariableDescriptors) {
            var entityClass = descriptor.getEntityDescriptor().getEntityClass();
            if (elementEntityClass.isAssignableFrom(entityClass)) {
                elementDescriptorList.add(descriptor);
            } else if (entityClass.isAssignableFrom(elementEntityClass)) {
                // A declarative superclass of the elements would be entangled with the cascade.
                return null;
            } else {
                hasNonElementDescriptors = true;
                if (entityClass == ownerEntityClass && descriptor.getAlignmentKeyMap() != null) {
                    // The cascade recomputes the owner's post-chain variables one entity at a time,
                    // which an alignment key's grouped updater contradicts.
                    return null;
                }
            }
        }
        if (!hasNonElementDescriptors) {
            // A model with only element variables is covered by the existing structures.
            return null;
        }
        var hasElementAlignmentKey = elementDescriptorList.stream()
                .anyMatch(descriptor -> descriptor.getAlignmentKeyMap() != null);
        if (hasElementAlignmentKey) {
            return null;
        }
        var postChainVariableSet = computePostChainVariables(declarativeShadowVariableDescriptors, elementEntityClass);
        for (var descriptor : declarativeShadowVariableDescriptors) {
            var isElementSource = elementEntityClass.isAssignableFrom(descriptor.getEntityDescriptor().getEntityClass());
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
                            // Only safe when it targets a pre-chain declarative variable of the owner:
                            // post-chain variables depend on the chain itself,
                            // and a non-declarative variable change does not trigger a chain walk.
                            var references = variableSource.variableSourceReferences();
                            if (references.size() < 2
                                    || !references.get(1).isDeclarative()
                                    || postChainVariableSet.contains(references.get(1).variableMetaModel())) {
                                return null;
                            }
                        }
                        default -> {
                            return null;
                        }
                    }
                } else if (parentVariableType == ParentVariableType.LIST_ELEMENT) {
                    // Only safe when it accesses the list's own elements directly:
                    // an entity reached through an element's fact may belong to another list,
                    // whose changes would not recompute this variable.
                    var reference = variableSource.variableSourceReferences().get(0);
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
        return new ListElementCascadeAndDirection(new ListElementCascade(elementEntityClass), parentMetaModel, direction);
    }

    /**
     * Classifies the declarative shadow variables of the classes outside the element cascade:
     * a variable is post-chain when it depends on its own entity's list elements,
     * directly through a list element source or transitively through another variable
     * of the same entity.
     * Pre-chain variables can be computed before the entity's chain is walked;
     * post-chain variables must be computed after it.
     */
    static <Solution_> Set<VariableMetaModel<?, ?, ?>> computePostChainVariables(
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            Class<?> elementEntityClass) {
        var nonElementDescriptorList = declarativeShadowVariableDescriptors.stream()
                .filter(descriptor -> !elementEntityClass
                        .isAssignableFrom(descriptor.getEntityDescriptor().getEntityClass()))
                .toList();
        var postChainVariableSet = new LinkedHashSet<VariableMetaModel<?, ?, ?>>();
        var changed = true;
        while (changed) {
            changed = false;
            for (var descriptor : nonElementDescriptorList) {
                var variableMetaModel = descriptor.getVariableMetaModel();
                if (postChainVariableSet.contains(variableMetaModel)) {
                    continue;
                }
                for (var source : descriptor.getSources()) {
                    // A cross-entity source (a fact path or a variable path) is ordered by
                    // the graph's own edges, so it does not propagate post-chain status.
                    var isPostChain = source.parentVariableType() == ParentVariableType.LIST_ELEMENT
                            || (source.parentVariableType() == ParentVariableType.NO_PARENT
                                    && source.variableSourceReferences().stream()
                                            .map(VariableSourceReference::variableMetaModel)
                                            .anyMatch(postChainVariableSet::contains));
                    if (isPostChain) {
                        postChainVariableSet.add(variableMetaModel);
                        changed = true;
                        break;
                    }
                }
            }
        }
        return postChainVariableSet;
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
