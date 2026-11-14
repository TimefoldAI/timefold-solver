package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Describes how the elements of the planning list variable are represented by one chain node per list entity,
 * see {@link GraphStructure#LIST_CHAIN}.
 * <p>
 * The elements may only be reached through their list:
 * <ul>
 * <li>an element variable reads its own variables, its previous (or next) element's,
 * and its list entity's declarative variables through its inverse, the pre-chain variables;</li>
 * <li>outside of the elements, only the list entity's variables read the elements, through its list,
 * the post-chain variables.</li>
 * </ul>
 * So a chain node's only edges come from the pre-chain variables and go to the post-chain variables
 * of its list entity.
 * Only the element class's sources and the references towards the element class are checked here:
 * the rest of the model is covered by the graph, whatever its structure.
 *
 * @param parentMetaModel the previous (or next) element variable every element variable walks along
 * @param direction {@link ParentVariableType#PREVIOUS} or {@link ParentVariableType#NEXT}
 * @param elementDescriptorList the declarative shadow variables of the elements
 * @param preChainVariableIdList the list entity's declarative variables the elements read through their inverse
 * @param postChainVariableIdList the list entity's declarative variables that read its elements through its list
 */
@NullMarked
record ListChain<Solution_>(
        ListVariableDescriptor<Solution_> listVariableDescriptor,
        VariableMetaModel<?, ?, ?> parentMetaModel,
        ParentVariableType direction,
        List<DeclarativeShadowVariableDescriptor<Solution_>> elementDescriptorList,
        List<VariableMetaModel<?, ?, ?>> preChainVariableIdList,
        List<VariableMetaModel<?, ?, ?>> postChainVariableIdList) {

    /**
     * @return null if the planning list variable's elements cannot be represented by chain nodes
     */
    static <Solution_> @Nullable ListChain<Solution_> of(SolutionDescriptor<Solution_> solutionDescriptor) {
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null) {
            return null;
        }
        var declarativeShadowVariableDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();
        VariableMetaModel<?, ?, ?> parentMetaModel = null;
        ParentVariableType direction = null;
        for (var descriptor : declarativeShadowVariableDescriptors) {
            for (var source : descriptor.getSources()) {
                var parentVariableType = source.parentVariableType();
                if (parentVariableType != ParentVariableType.PREVIOUS && parentVariableType != ParentVariableType.NEXT) {
                    continue;
                }
                var sourceParentMetaModel = source.variableSourceReferences().getFirst().variableMetaModel();
                if (parentMetaModel == null) {
                    parentMetaModel = sourceParentMetaModel;
                    direction = parentVariableType;
                } else if (!parentMetaModel.equals(sourceParentMetaModel) || direction != parentVariableType) {
                    // The chain node walks each list in a single direction.
                    return null;
                }
            }
        }
        if (parentMetaModel == null) {
            return null;
        }
        // The class declaring the directional parent; the elements may be of any
        // subclass of it, as long as none of them declares a declarative variable.
        var elementEntityClass = parentMetaModel.entity().type();
        var listEntityDescriptor = listVariableDescriptor.getEntityDescriptor();
        var listEntityClass = listEntityDescriptor.getEntityClass();
        if (!elementEntityClass.isAssignableFrom(listVariableDescriptor.getElementType())
                || listEntityClass.isAssignableFrom(elementEntityClass)
                || elementEntityClass.isAssignableFrom(listEntityClass)) {
            // The chain node walks the list entity's list and classifies entities with instanceof,
            // so the element class must cover the list's elements and be distinct from the list entity.
            return null;
        }
        if (listEntityDescriptor.getShadowVariableDescriptors().stream()
                .noneMatch(variableDescriptor -> variableDescriptor instanceof DeclarativeShadowVariableDescriptor<?>)) {
            // The graph marks the chain node's entity consistent through the list entity's consistency state,
            // which only exists when the list entity has declarative shadow variables of its own.
            // A model whose only declarative variables are its elements' is covered by the
            // existing structures anyway.
            return null;
        }
        var elementDescriptorList = new ArrayList<DeclarativeShadowVariableDescriptor<Solution_>>();
        var preChainVariableIdSet = new LinkedHashSet<VariableMetaModel<?, ?, ?>>();
        var postChainVariableIdList = new ArrayList<VariableMetaModel<?, ?, ?>>();
        for (var descriptor : declarativeShadowVariableDescriptors) {
            if (descriptor.getAlignmentKeyMap() != null) {
                // The chain node updates one entity at a time, both when it walks a chain
                // and when it recomputes the list entity's post-chain variables,
                // which an alignment key's grouped updater contradicts.
                return null;
            }
            var entityClass = descriptor.getEntityDescriptor().getEntityClass();
            if (entityClass == elementEntityClass) {
                if (!collectElementSources(descriptor, preChainVariableIdSet)) {
                    return null;
                }
                elementDescriptorList.add(descriptor);
            } else if (elementEntityClass.isAssignableFrom(entityClass) || entityClass.isAssignableFrom(elementEntityClass)) {
                // The chain node's walk applies every element updater to every element,
                // so a declarative variable declared elsewhere in the element hierarchy
                // would be applied to elements that do not have it.
                return null;
            } else {
                var isPostChain = isPostChainVariable(descriptor, elementEntityClass);
                if (isPostChain == null) {
                    return null;
                }
                if (isPostChain) {
                    postChainVariableIdList.add(descriptor.getVariableMetaModel());
                }
            }
        }
        return new ListChain<>(listVariableDescriptor, parentMetaModel, Objects.requireNonNull(direction),
                elementDescriptorList, List.copyOf(preChainVariableIdSet), postChainVariableIdList);
    }

    /**
     * Adds the pre-chain variables an element variable reads.
     *
     * @return false if the element variable reads something the chain node cannot follow
     */
    private static <Solution_> boolean collectElementSources(DeclarativeShadowVariableDescriptor<Solution_> descriptor,
            LinkedHashSet<VariableMetaModel<?, ?, ?>> preChainVariableIdSet) {
        for (var source : descriptor.getSources()) {
            var references = source.variableSourceReferences();
            switch (source.parentVariableType()) {
                case PREVIOUS, NEXT -> {
                    // Within the chain, which the walk follows.
                }
                case NO_PARENT -> {
                    // A variable of the element itself; one reached through another (non-declarative) variable,
                    // such as a fact's, would need the elements to be part of the graph.
                    if (references.size() != 1) {
                        return false;
                    }
                }
                case INVERSE -> {
                    // A declarative variable of the list entity, which an edge orders before the chain node.
                    // One that depends on the elements, directly or not, would close a loop
                    // through the chain node, which the build detects.
                    var listEntityReference = references.getLast();
                    if (!listEntityReference.isDeclarative()) {
                        return false;
                    }
                    preChainVariableIdSet.add(listEntityReference.variableMetaModel());
                }
                case VARIABLE, INDIRECT, GROUP, LIST_ELEMENT -> {
                    // An element reached any other way would have to be part of the graph.
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true if the variable reads the elements through its own list,
     *         false if it does not read the elements at all,
     *         null if it reads them some other way, which the chain node cannot follow
     */
    private static <Solution_> @Nullable Boolean isPostChainVariable(
            DeclarativeShadowVariableDescriptor<Solution_> descriptor, Class<?> elementEntityClass) {
        var isPostChain = false;
        for (var source : descriptor.getSources()) {
            if (source.parentVariableType() == ParentVariableType.LIST_ELEMENT) {
                // Only safe when it accesses the list's own elements directly:
                // an entity reached through an element's fact may belong to another list,
                // whose changes would not recompute this variable.
                if (!source.variableSourceReferences().getFirst().chainFromRootEntityToVariableEntity().isEmpty()) {
                    return null;
                }
                isPostChain = true;
                continue;
            }
            // Elements are not part of the graph, so no other source may reach them.
            for (var reference : source.variableSourceReferences()) {
                if (elementEntityClass.isAssignableFrom(reference.variableMetaModel().entity().type())) {
                    return null;
                }
            }
        }
        return isPostChain;
    }

    Class<?> elementEntityClass() {
        return parentMetaModel.entity().type();
    }

    /**
     * @return true if the elements read their previous element, so the walk follows the list order
     */
    boolean isChainInListOrder() {
        return direction == ParentVariableType.PREVIOUS;
    }
}
