package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class DefaultShadowVariableSessionFactory<Solution_> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultShadowVariableSessionFactory.class);
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final IntFunction<TopologicalOrderGraph> graphCreator;

    public DefaultShadowVariableSessionFactory(
            SolutionDescriptor<Solution_> solutionDescriptor,
            InnerScoreDirector<Solution_, ?> scoreDirector,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        this.solutionDescriptor = solutionDescriptor;
        this.scoreDirector = scoreDirector;
        this.graphCreator = graphCreator;
    }

    private record MissingEntity(Object sourceEntity,
            Object missingReferredEntity,
            DeclarativeShadowVariableDescriptor<?> referringShadowVariable,
            RootVariableSource<?, ?> referredVariableSource) {
        String getMessage() {
            return """
                    The entity's (%s) shadow variable (%s) refers to a declarative shadow variable on a non-given entity (%s)
                    variable via the source path (%s).
                    """
                    .formatted(sourceEntity, referringShadowVariable.getVariableName(),
                            missingReferredEntity,
                            referredVariableSource.variablePath());
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof MissingEntity that))
                return false;
            return missingReferredEntity == that.missingReferredEntity;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(missingReferredEntity);
        }
    }

    public record GraphDescriptor<Solution_>(ConsistencyTracker<Solution_> consistencyTracker,
            SolutionDescriptor<Solution_> solutionDescriptor,
            boolean ignoreInconsistentSolutions,
            VariableReferenceGraphBuilder<Solution_> variableReferenceGraphBuilder,
            Object[] entities, IntFunction<TopologicalOrderGraph> graphCreator) {

        public GraphDescriptor(SolutionDescriptor<Solution_> solutionDescriptor,
                ChangedVariableNotifier<Solution_> changedVariableNotifier,
                Object... entities) {
            this(new ConsistencyTracker<>(), solutionDescriptor, !solutionDescriptor.hasAnyShadowVariablesInconsistentMember(),
                    new VariableReferenceGraphBuilder<>(changedVariableNotifier),
                    entities, DefaultTopologicalOrderGraph::new);
        }

        public GraphDescriptor<Solution_> withGraphCreator(IntFunction<TopologicalOrderGraph> graphCreator) {
            return new GraphDescriptor<>(consistencyTracker, solutionDescriptor, ignoreInconsistentSolutions,
                    variableReferenceGraphBuilder, entities, graphCreator);
        }

        public GraphDescriptor<Solution_> withConsistencyTracker(ConsistencyTracker<Solution_> consistencyTracker) {
            return new GraphDescriptor<>(consistencyTracker, solutionDescriptor, ignoreInconsistentSolutions,
                    variableReferenceGraphBuilder, entities, graphCreator);
        }

        public GraphDescriptor<Solution_> withIgnoreInconsistentSolutions(boolean ignoreInconsistentSolutions) {
            return new GraphDescriptor<>(consistencyTracker, solutionDescriptor, ignoreInconsistentSolutions,
                    variableReferenceGraphBuilder, entities, graphCreator);
        }

        public GraphDescriptor<Solution_> assertingNoReferencedMissingEntities() {
            var entitySet = CollectionUtils.newIdentityHashSet(entities.length);
            entitySet.addAll(Arrays.asList(entities));
            var missingEntitySet = new HashSet<MissingEntity>();

            var declarativeShadowDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();

            do {
                // iterate when new entities are discovered, so
                // we can include any missing entities they reference in the error message
                missingEntitySet.stream()
                        .map(MissingEntity::missingReferredEntity)
                        .forEach(entitySet::add);
            } while (addDiscoveredEntities(declarativeShadowDescriptors, entitySet, missingEntitySet));

            if (missingEntitySet.isEmpty()) {
                return this;
            }

            var LIMIT = 5;
            throw new IllegalArgumentException("""
                    Found referenced entities that were not given:

                    %s
                    %s
                    When ConstraintVerifier.verifyThat().given(...) or
                    %s.updateShadowVariables(solutionClass, ...) is used,
                    all referenced entities must be passed in as arguments.
                    Maybe add the missing entities as arguments?
                    """.formatted(missingEntitySet.stream()
                    .map(MissingEntity::getMessage)
                    .sorted()
                    .limit(LIMIT)
                    .collect(Collectors.joining("  - ", "  - ", "")),
                    (missingEntitySet.size() > LIMIT) ? // Comments to force formatter to not put the conditional on one line
                            "(%d more...)%n".formatted(missingEntitySet.size() - LIMIT) : //
                            "", //
                    SolutionManager.class.getSimpleName()));
        }

        private boolean addDiscoveredEntities(List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowDescriptors,
                Set<Object> entitySet, HashSet<MissingEntity> missingEntitySet) {
            var originalMissingCount = missingEntitySet.size();
            for (var entity : entitySet) {
                for (var shadowDescriptor : declarativeShadowDescriptors) {
                    if (!shadowDescriptor.getEntityDescriptor().getEntityClass().isAssignableFrom(entity.getClass())) {
                        continue;
                    }
                    shadowDescriptor.visitAllReferencedEntities(entity, (source, maybeMissingEntity) -> {
                        if (!entitySet.contains(maybeMissingEntity)) {
                            missingEntitySet.add(new MissingEntity(entity, maybeMissingEntity,
                                    shadowDescriptor, source));
                        }
                    });
                }
            }
            return missingEntitySet.size() != originalMissingCount;
        }

        public ChangedVariableNotifier<Solution_> changedVariableNotifier() {
            return variableReferenceGraphBuilder.changedVariableNotifier;
        }
    }

    public static <Solution_> VariableReferenceGraph buildGraph(GraphDescriptor<Solution_> graphDescriptor) {
        var graphStructureAndDirection = GraphStructure.determineGraphStructure(graphDescriptor.solutionDescriptor(),
                graphDescriptor.entities());
        LOGGER.trace("Shadow variable graph structure: {}", graphStructureAndDirection);
        return buildGraphForStructureAndDirection(graphStructureAndDirection, graphDescriptor);
    }

    static <Solution_> VariableReferenceGraph buildGraphForStructureAndDirection(
            GraphStructure.GraphStructureAndDirection graphStructureAndDirection, GraphDescriptor<Solution_> graphDescriptor) {
        return switch (graphStructureAndDirection.structure()) {
            case EMPTY -> EmptyVariableReferenceGraph.INSTANCE;
            case SINGLE_DIRECTIONAL_PARENT -> {
                var scoreDirector =
                        graphDescriptor.variableReferenceGraphBuilder().changedVariableNotifier.innerScoreDirector();
                if (scoreDirector == null) {
                    yield buildArbitraryGraph(graphDescriptor);
                }
                yield buildSingleDirectionalParentGraph(graphDescriptor, graphStructureAndDirection);
            }
            case ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE, NO_DYNAMIC_EDGES, ARBITRARY ->
                buildArbitraryGraph(graphDescriptor);
        };
    }

    static <Solution_> VariableReferenceGraph buildSingleDirectionalParentGraph(
            GraphDescriptor<Solution_> graphDescriptor, GraphStructure.GraphStructureAndDirection graphStructureAndDirection) {
        var forward = switch (Objects.requireNonNull(graphStructureAndDirection.direction())) {
            case PREVIOUS -> true;
            case NEXT -> false;
            default -> throw new IllegalStateException(
                    "Impossible state: expected parentVariableType to be previous or next but was %s."
                            .formatted(graphStructureAndDirection.direction()));
        };

        var declarativeShadowVariables = graphDescriptor.solutionDescriptor().getDeclarativeShadowVariableDescriptors();
        var sortedDeclarativeVariables = topologicallySortedDeclarativeShadowVariables(declarativeShadowVariables);
        var listVariableDescriptor = Objects.requireNonNull(graphDescriptor.solutionDescriptor().getListVariableDescriptor());
        var listVariableState = Objects.requireNonNull(graphDescriptor.changedVariableNotifier().innerScoreDirector())
                .getListVariableState(listVariableDescriptor);
        var canTerminateEarly = hasNoNonDeclarativeSourcesFromParent(declarativeShadowVariables);
        return new SingleDirectionalParentVariableReferenceGraph<>(graphDescriptor.consistencyTracker(),
                sortedDeclarativeVariables, listVariableState, forward, graphDescriptor.changedVariableNotifier(),
                canTerminateEarly, graphDescriptor.entities());
    }

    private static <Solution_> boolean hasNoNonDeclarativeSourcesFromParent(
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariables) {
        for (var declarativeShadowVariable : declarativeShadowVariables) {
            for (var source : declarativeShadowVariable.getSources()) {
                for (var sourceReference : source.variableSourceReferences()) {
                    if (!sourceReference.isTopLevel() && !sourceReference.isDeclarative()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static <Solution_> List<DeclarativeShadowVariableDescriptor<Solution_>>
            topologicallySortedDeclarativeShadowVariables(
                    List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariables) {
        Map<String, Integer> nameToIndex = new LinkedHashMap<>();
        for (var declarativeShadowVariable : declarativeShadowVariables) {
            nameToIndex.put(declarativeShadowVariable.getVariableName(), nameToIndex.size());
        }
        var graph = new DefaultTopologicalOrderGraph(nameToIndex.size());
        for (var declarativeShadowVariable : declarativeShadowVariables) {
            var toIndex = nameToIndex.get(declarativeShadowVariable.getVariableName());
            var visited = new HashSet<Integer>();
            for (var source : declarativeShadowVariable.getSources()) {
                var variableReferences = source.variableSourceReferences();
                if (source.parentVariableType() != ParentVariableType.NO_PARENT) {
                    // We only look at direct usage; if we also added
                    // edges for groups/directional, we will end up creating a cycle
                    // which makes all topological orders valid
                    continue;
                }
                var variableReference = variableReferences.get(0);
                var sourceDeclarativeVariable = variableReference.downstreamDeclarativeVariableMetamodel();
                if (sourceDeclarativeVariable != null) {
                    var fromIndex = nameToIndex.get(sourceDeclarativeVariable.name());
                    if (visited.add(fromIndex)) {
                        graph.addEdge(fromIndex, toIndex);
                    }
                }
            }
        }
        graph.commitChanges(new BitSet());
        var sortedDeclarativeVariables = new ArrayList<>(declarativeShadowVariables);
        sortedDeclarativeVariables.sort(Comparator.<DeclarativeShadowVariableDescriptor<Solution_>> comparingInt(
                variable -> graph.getTopologicalOrder(nameToIndex.get(variable.getVariableName())))
                .thenComparing(VariableDescriptor::getVariableName));
        return sortedDeclarativeVariables;
    }

    private static <Solution_> VariableReferenceGraph buildArbitraryGraph(GraphDescriptor<Solution_> graphDescriptor) {
        var declarativeShadowVariableDescriptors =
                graphDescriptor.solutionDescriptor().getDeclarativeShadowVariableDescriptors();
        // Create one graph node per entity/declarative shadow variable pair;
        // the graph builder merges the nodes of an entity where the edges allow it.
        // Maps a variable id to its source aliases;
        // For instance, "previousVisit.startTime" is a source alias of "startTime"
        // One way to view this concept is "previousVisit.startTime" is a pointer
        // to "startTime" of some visit, and thus alias it.
        var declarativeShadowVariableToAliasMap = createGraphNodes(
                graphDescriptor,
                declarativeShadowVariableDescriptors);
        return buildVariableReferenceGraph(graphDescriptor, declarativeShadowVariableDescriptors,
                declarativeShadowVariableToAliasMap);
    }

    private static <Solution_> VariableReferenceGraph buildVariableReferenceGraph(
            GraphDescriptor<Solution_> graphDescriptor,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> declarativeShadowVariableToAliasMap) {
        // Create variable processors for each declarative shadow variable descriptor
        for (var declarativeShadowVariable : declarativeShadowVariableDescriptors) {
            var fromVariableId = declarativeShadowVariable.getVariableMetaModel();
            createSourceChangeProcessors(graphDescriptor, declarativeShadowVariable, fromVariableId);
            var aliasSet = declarativeShadowVariableToAliasMap.get(fromVariableId);
            if (aliasSet != null) {
                createAliasToVariableChangeProcessors(graphDescriptor.variableReferenceGraphBuilder(), aliasSet,
                        fromVariableId);
            }
        }

        // Create the fixed edges in the graph
        createFixedVariableRelationEdges(graphDescriptor.variableReferenceGraphBuilder(), graphDescriptor.entities(),
                declarativeShadowVariableDescriptors);
        return graphDescriptor.variableReferenceGraphBuilder().build(graphDescriptor.graphCreator(),
                graphDescriptor.ignoreInconsistentSolutions());
    }

    private static <Solution_> Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> createGraphNodes(
            GraphDescriptor<Solution_> graphDescriptor,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors) {
        var result = new HashMap<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>>();
        var nextGroupId = 0;
        // One shared updater per non-null alignment key, created by the first
        // member entity in iteration order; all members update through it.
        var alignmentKeyToSharedUpdater =
                new HashMap<DeclarativeShadowVariableDescriptor<Solution_>, Map<Object, VariableUpdaterInfo<Solution_>>>();
        var shadowToAlignmentKeyToAlignedEntities =
                new HashMap<DeclarativeShadowVariableDescriptor<Solution_>, Map<Object, List<Object>>>();
        for (var entity : graphDescriptor.entities()) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (entityClass.isInstance(entity)) {
                    var variableId = declarativeShadowVariableDescriptor.getVariableMetaModel();
                    var updater = new VariableUpdaterInfo<>(
                            variableId,
                            nextGroupId++,
                            declarativeShadowVariableDescriptor,
                            graphDescriptor.consistencyTracker().getDeclarativeEntityConsistencyState(
                                    declarativeShadowVariableDescriptor.getEntityDescriptor()),
                            declarativeShadowVariableDescriptor.getMemberAccessor(),
                            declarativeShadowVariableDescriptor.getCalculator());
                    var alignmentKeyMap = declarativeShadowVariableDescriptor.getAlignmentKeyMap();
                    if (alignmentKeyMap != null) {
                        var alignmentKeyToAlignedEntities =
                                shadowToAlignmentKeyToAlignedEntities.computeIfAbsent(declarativeShadowVariableDescriptor,
                                        ignored -> calculateAllAlignmentGroupsForVariable(graphDescriptor,
                                                declarativeShadowVariableDescriptor));
                        var alignmentKey = alignmentKeyMap.apply(entity);
                        if (alignmentKey != null) {
                            var sharedUpdaterMap =
                                    alignmentKeyToSharedUpdater.computeIfAbsent(declarativeShadowVariableDescriptor,
                                            k -> new HashMap<>());
                            var sharedUpdater = sharedUpdaterMap.get(alignmentKey);
                            if (sharedUpdater == null) {
                                var alignmentMembers = alignmentKeyToAlignedEntities.get(alignmentKey);
                                if (alignmentMembers.size() > 1) {
                                    sharedUpdater = updater.withGroupEntities(alignmentMembers.toArray());
                                    sharedUpdaterMap.put(alignmentKey, sharedUpdater);
                                } else {
                                    // no other variables share this alignment key, so use the original
                                    // updater without group entities to allow the node to be merged with other
                                    // nodes on the same entity
                                    sharedUpdater = updater;
                                }
                            }
                            updater = sharedUpdater;
                        }
                    }
                    graphDescriptor.variableReferenceGraphBuilder.addVariableReferenceEntity(entity, updater);
                    for (var sourceRoot : declarativeShadowVariableDescriptor.getSources()) {
                        for (var source : sourceRoot.variableSourceReferences()) {
                            if (source.downstreamDeclarativeVariableMetamodel() != null) {
                                result.computeIfAbsent(source.downstreamDeclarativeVariableMetamodel(),
                                        ignored -> new LinkedHashSet<>())
                                        .add(source);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static <Solution_> Map<Object, List<Object>> calculateAllAlignmentGroupsForVariable(
            GraphDescriptor<Solution_> graphDescriptor,
            DeclarativeShadowVariableDescriptor<Solution_> declarativeShadowVariableDescriptor) {
        var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
        var alignmentKeyMap = Objects.requireNonNull(declarativeShadowVariableDescriptor.getAlignmentKeyMap());
        var out = new HashMap<Object, List<Object>>();
        for (var entity : graphDescriptor.entities()) {
            if (entityClass.isInstance(entity)) {
                var alignmentKey = alignmentKeyMap.apply(entity);
                if (alignmentKey != null) {
                    out.computeIfAbsent(alignmentKey, ignored -> new ArrayList<>()).add(entity);
                }
            }
        }
        return out;
    }

    private static <Solution_> void createSourceChangeProcessors(
            GraphDescriptor<Solution_> graphDescriptor,
            DeclarativeShadowVariableDescriptor<Solution_> declarativeShadowVariable,
            VariableMetaModel<Solution_, ?, ?> fromVariableId) {
        var fromVariableOrdinal = AbstractVariableReferenceGraph.globalOrdinalOf(fromVariableId);
        for (var source : declarativeShadowVariable.getSources()) {
            if (source.parentVariableType() == ParentVariableType.LIST_ELEMENT) {
                createListElementSourceProcessors(graphDescriptor, source, fromVariableId);
                continue;
            }
            var parentVariableList = new ArrayList<VariableSourceReference>();
            var parentInverseFunctionList = new ArrayList<Function<Object, Collection<Object>>>();
            var parentIsOnRootEntity = false;
            for (var sourcePart : source.variableSourceReferences()) {
                var toVariableId = sourcePart.variableMetaModel();

                // declarative variables have edges to each other in the graph,
                // and will mark dependent variable as changed.
                // non-declarative variables are not in the graph and must have their
                // own processor
                if (!sourcePart.isDeclarative()) {
                    if (sourcePart.onRootEntity()) {
                        // No need for inverse set; source and target entity are the same.
                        graphDescriptor.variableReferenceGraphBuilder()
                                .addAfterProcessor(GraphChangeType.NO_CHANGE, toVariableId,
                                        (graph, entity) -> {
                                            var changed = graph.lookupOrNull(fromVariableOrdinal, entity);
                                            if (changed != null) {
                                                graph.markChanged(changed);
                                            }
                                        });
                        parentInverseFunctionList.add(Collections::singletonList);
                        parentIsOnRootEntity = true;
                    } else {
                        Function<Object, Collection<Object>> inverseFunction;

                        if (parentVariableList.isEmpty()) {
                            // Need to create an inverse set from source to target
                            var inverseMap = new IdentityHashMap<Object, List<Object>>();

                            var visitor = source.getEntityVisitor(sourcePart.chainFromRootEntityToVariableEntity());
                            for (var rootEntity : graphDescriptor.entities()) {
                                if (declarativeShadowVariable.getEntityDescriptor().getEntityClass().isInstance(rootEntity)) {
                                    visitor.accept(rootEntity, shadowEntity -> inverseMap
                                            .computeIfAbsent(shadowEntity, ignored -> new ArrayList<>())
                                            .add(rootEntity));
                                }
                            }
                            inverseFunction = entity -> inverseMap.getOrDefault(entity, Collections.emptyList());
                        } else {
                            var parentIndex = parentVariableList.size() - 1;
                            var parentVariable = parentVariableList.get(parentIndex).variableMetaModel();
                            var inverseSupply = graphDescriptor.variableReferenceGraphBuilder().changedVariableNotifier
                                    .getCollectionInverseVariableState(parentVariable);

                            if (parentIsOnRootEntity) {
                                inverseFunction = (Function) inverseSupply::getInverseCollection;
                            } else {
                                inverseFunction = entity -> {
                                    var inverses = inverseSupply.getInverseCollection(entity);
                                    var parentInverseFunction = parentInverseFunctionList.get(parentIndex);
                                    var out = new ArrayList<>(inverses.size());
                                    for (var inverse : inverses) {
                                        out.addAll(parentInverseFunction.apply(inverse));
                                    }
                                    return out;
                                };
                            }
                        }

                        graphDescriptor.variableReferenceGraphBuilder()
                                .addAfterProcessor(GraphChangeType.NO_CHANGE, toVariableId,
                                        (graph, entity) -> {
                                            for (var item : inverseFunction.apply(entity)) {
                                                var changed = graph.lookupOrNull(fromVariableOrdinal, item);
                                                if (changed != null) {
                                                    graph.markChanged(changed);
                                                }
                                            }
                                        });
                        parentInverseFunctionList.add(inverseFunction);
                        parentIsOnRootEntity = false;
                    }
                }
                parentVariableList.add(sourcePart);
            }
        }
    }

    private static <Solution_> void createListElementSourceProcessors(
            GraphDescriptor<Solution_> graphDescriptor,
            RootVariableSource<?, ?> source,
            VariableMetaModel<Solution_, ?, ?> fromVariableId) {
        var fromVariableOrdinal = AbstractVariableReferenceGraph.globalOrdinalOf(fromVariableId);
        var listVariableId = Objects.requireNonNull(source.listVariableMetaModel());
        // Mark the target variable changed whenever its list variable changes,
        // since its dependency set (and possibly its value) changes with the list's contents.
        // This is also triggered for each entity when the graph is constructed,
        // guaranteeing an initial computation even for entities with an empty list.
        graphDescriptor.variableReferenceGraphBuilder()
                .addAfterProcessor(GraphChangeType.NO_CHANGE, listVariableId,
                        (graph, entity) -> {
                            var changed = graph.lookupOrNull(fromVariableOrdinal, entity);
                            if (changed != null) {
                                graph.markChanged(changed);
                            }
                        });
        // Maintain the fan-in edges (element.sourceVariable -> entity.targetVariable)
        // when the list variable changes.
        var elementSource = source.variableSourceReferences().get(0);
        graphDescriptor.variableReferenceGraphBuilder()
                .addListElementSourceLocator(listVariableId,
                        new ListElementSourceLocator(elementSource.variableMetaModel(), fromVariableId,
                                elementSource.chainFromRootEntityToVariableEntity()));
    }

    private static <Solution_> void createAliasToVariableChangeProcessors(
            VariableReferenceGraphBuilder<Solution_> variableReferenceGraphBuilder, Set<VariableSourceReference> aliasSet,
            VariableMetaModel<Solution_, ?, ?> fromVariableId) {
        var fromVariableOrdinal = AbstractVariableReferenceGraph.globalOrdinalOf(fromVariableId);
        for (var alias : aliasSet) {
            var toVariableId = alias.targetVariableMetamodel();
            var toVariableOrdinal = AbstractVariableReferenceGraph.globalOrdinalOf(toVariableId);
            var sourceVariableId = alias.variableMetaModel();

            if (!alias.isDeclarative() && alias.affectGraphEdges()) {
                // Exploit the same fact as above
                variableReferenceGraphBuilder.addBeforeProcessor(GraphChangeType.REMOVE_EDGE, sourceVariableId,
                        (graph, toEntity) -> {
                            // from/to can be null in extended models
                            // ex: previous is used as a source, but only an extended class
                            // has the to variable
                            var to = graph.lookupOrNull(toVariableOrdinal, toEntity);
                            if (to == null) {
                                return;
                            }
                            var fromEntity = alias.targetEntityFunctionStartingFromVariableEntity()
                                    .apply(toEntity);
                            if (fromEntity == null) {
                                return;
                            }
                            var from = graph.lookupOrNull(fromVariableOrdinal, fromEntity);
                            if (from == null) {
                                return;
                            }
                            graph.removeEdge(from, to);
                        });
                variableReferenceGraphBuilder.addAfterProcessor(GraphChangeType.ADD_EDGE, sourceVariableId,
                        (graph, toEntity) -> {
                            var to = graph.lookupOrNull(toVariableOrdinal, toEntity);
                            if (to == null) {
                                return;
                            }
                            var fromEntity = alias.findTargetEntity(toEntity);
                            if (fromEntity == null) {
                                return;
                            }
                            var from = graph.lookupOrNull(fromVariableOrdinal, fromEntity);
                            if (from == null) {
                                return;
                            }
                            graph.addEdge(from, to);
                        });
            }
            // Note: it is impossible to have a declarative variable affect graph edges,
            // since accessing a declarative variable from another declarative variable is prohibited.
        }
    }

    private static <Solution_> void createFixedVariableRelationEdges(
            VariableReferenceGraphBuilder<Solution_> variableReferenceGraphBuilder,
            Object[] entities,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors) {
        for (var entity : entities) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (!entityClass.isInstance(entity)) {
                    continue;
                }
                var toVariableId = declarativeShadowVariableDescriptor.getVariableMetaModel();
                var to = variableReferenceGraphBuilder.lookupOrError(toVariableId, entity);
                for (var sourceRoot : declarativeShadowVariableDescriptor.getSources()) {
                    for (var source : sourceRoot.variableSourceReferences()) {
                        if (source.isTopLevel() && source.isDeclarative()) {
                            var fromVariableId = source.variableMetaModel();
                            // Edges sourced from a list variable's elements reflect its contents when
                            // the graph is built and are removed/added as it changes during solving,
                            // so they must not be treated as fixed by the fixed-loop fail-fast.
                            var isListElementSource = sourceRoot.parentVariableType() == ParentVariableType.LIST_ELEMENT;
                            sourceRoot.valueEntityFunction()
                                    .accept(entity, fromEntity -> {
                                        var from = variableReferenceGraphBuilder.lookupOrError(fromVariableId, fromEntity);
                                        if (isListElementSource) {
                                            variableReferenceGraphBuilder.addInitialDynamicEdge(from, to);
                                        } else {
                                            variableReferenceGraphBuilder.addFixedEdge(from, to);
                                        }
                                    });
                            break;
                        }
                    }
                }
            }
        }
    }

    public DefaultShadowVariableSession<Solution_> forSolution(ConsistencyTracker<Solution_> consistencyTracker,
            Solution_ solution,
            boolean ignoreInconsistentSolutions) {
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        return forEntities(consistencyTracker, ignoreInconsistentSolutions, entities.toArray());
    }

    public DefaultShadowVariableSession<Solution_> forEntities(ConsistencyTracker<Solution_> consistencyTracker,
            boolean ignoreInconsistentSolutions,
            Object... entities) {
        var graph = buildGraph(
                new GraphDescriptor<>(solutionDescriptor, ChangedVariableNotifier.of(scoreDirector), entities)
                        .withConsistencyTracker(consistencyTracker)
                        .withGraphCreator(graphCreator)
                        .withIgnoreInconsistentSolutions(ignoreInconsistentSolutions));
        return new DefaultShadowVariableSession<>(graph);
    }
}
