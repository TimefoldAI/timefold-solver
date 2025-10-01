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

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
            VariableReferenceGraphBuilder<Solution_> variableReferenceGraphBuilder,
            Object[] entities, IntFunction<TopologicalOrderGraph> graphCreator) {

        public GraphDescriptor(SolutionDescriptor<Solution_> solutionDescriptor,
                ChangedVariableNotifier<Solution_> changedVariableNotifier,
                Object... entities) {
            this(new ConsistencyTracker<>(), solutionDescriptor, new VariableReferenceGraphBuilder<>(changedVariableNotifier),
                    entities, DefaultTopologicalOrderGraph::new);
        }

        public GraphDescriptor<Solution_> withGraphCreator(IntFunction<TopologicalOrderGraph> graphCreator) {
            return new GraphDescriptor<>(consistencyTracker, solutionDescriptor,
                    variableReferenceGraphBuilder, entities, graphCreator);
        }

        public GraphDescriptor<Solution_> withConsistencyTracker(ConsistencyTracker<Solution_> consistencyTracker) {
            return new GraphDescriptor<>(consistencyTracker, solutionDescriptor,
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
            case ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE ->
                buildArbitrarySingleEntityGraph(graphDescriptor);
            case NO_DYNAMIC_EDGES, ARBITRARY ->
                buildArbitraryGraph(graphDescriptor);
        };
    }

    static <Solution_> VariableReferenceGraph buildSingleDirectionalParentGraph(
            GraphDescriptor<Solution_> graphDescriptor, GraphStructure.GraphStructureAndDirection graphStructureAndDirection) {
        var declarativeShadowVariables = graphDescriptor.solutionDescriptor().getDeclarativeShadowVariableDescriptors();
        var sortedDeclarativeVariables = topologicallySortedDeclarativeShadowVariables(declarativeShadowVariables);

        var topologicalSorter =
                getTopologicalSorter(graphDescriptor.solutionDescriptor(),
                        Objects.requireNonNull(graphDescriptor.changedVariableNotifier().innerScoreDirector()),
                        Objects.requireNonNull(graphStructureAndDirection.direction()));

        return new SingleDirectionalParentVariableReferenceGraph<>(graphDescriptor.consistencyTracker(),
                sortedDeclarativeVariables,
                topologicalSorter, graphDescriptor.changedVariableNotifier(), graphDescriptor.entities());
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
                variable -> graph.getTopologicalOrder(nameToIndex.get(variable.getVariableName())).order())
                .thenComparing(VariableDescriptor::getVariableName));
        return sortedDeclarativeVariables;
    }

    private static <Solution_> TopologicalSorter getTopologicalSorter(SolutionDescriptor<Solution_> solutionDescriptor,
            InnerScoreDirector<Solution_, ?> scoreDirector, ParentVariableType parentVariableType) {
        return switch (parentVariableType) {
            case PREVIOUS -> {
                var listStateSupply = scoreDirector.getListVariableStateSupply(solutionDescriptor.getListVariableDescriptor());
                yield new TopologicalSorter(listStateSupply::getNextElement,
                        Comparator.comparingInt(entity -> Objects.requireNonNullElse(listStateSupply.getIndex(entity), 0)),
                        listStateSupply::getInverseSingleton);
            }
            case NEXT -> {
                var listStateSupply = scoreDirector.getListVariableStateSupply(solutionDescriptor.getListVariableDescriptor());
                yield new TopologicalSorter(listStateSupply::getPreviousElement,
                        Comparator.comparingInt(entity -> Objects.requireNonNullElse(listStateSupply.getIndex(entity), 0))
                                .reversed(),
                        listStateSupply::getInverseSingleton);
            }
            default -> throw new IllegalStateException(
                    "Impossible state: expected parentVariableType to be previous or next but was %s."
                            .formatted(parentVariableType));
        };
    }

    private static <Solution_> VariableReferenceGraph buildArbitraryGraph(GraphDescriptor<Solution_> graphDescriptor) {
        var declarativeShadowVariableDescriptors =
                graphDescriptor.solutionDescriptor().getDeclarativeShadowVariableDescriptors();
        var variableIdToUpdater = EntityVariableUpdaterLookup.<Solution_> entityIndependentLookup();

        // Create graph node for each entity/declarative shadow variable pair.
        // Maps a variable id to its source aliases;
        // For instance, "previousVisit.startTime" is a source alias of "startTime"
        // One way to view this concept is "previousVisit.startTime" is a pointer
        // to "startTime" of some visit, and thus alias it.
        var declarativeShadowVariableToAliasMap = createGraphNodes(
                graphDescriptor,
                declarativeShadowVariableDescriptors, variableIdToUpdater);
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
        return graphDescriptor.variableReferenceGraphBuilder().build(graphDescriptor.graphCreator());
    }

    private record GroupVariableUpdaterInfo<Solution_>(
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedDeclarativeVariableDescriptors,
            List<VariableUpdaterInfo<Solution_>> allUpdaters,
            List<VariableUpdaterInfo<Solution_>> groupedUpdaters,
            Map<DeclarativeShadowVariableDescriptor<Solution_>, Map<Object, VariableUpdaterInfo<Solution_>>> variableToEntityToGroupUpdater) {

        public List<VariableUpdaterInfo<Solution_>> getUpdatersForEntityVariable(Object entity,
                DeclarativeShadowVariableDescriptor<Solution_> declarativeShadowVariableDescriptor) {
            if (variableToEntityToGroupUpdater.containsKey(declarativeShadowVariableDescriptor)) {
                var updater = variableToEntityToGroupUpdater.get(declarativeShadowVariableDescriptor).get(entity);
                if (updater != null) {
                    return List.of(updater);
                }
            }
            for (var shadowVariableDescriptor : sortedDeclarativeVariableDescriptors) {
                for (var rootSource : shadowVariableDescriptor.getSources()) {
                    if (rootSource.parentVariableType() == ParentVariableType.GROUP) {
                        var visitedCount = new MutableInt();
                        rootSource.valueEntityFunction().accept(entity, ignored -> visitedCount.increment());
                        if (visitedCount.intValue() > 0) {
                            return groupedUpdaters;
                        }
                    }
                }
            }
            return allUpdaters;
        }

    }

    private static <Solution_> Map<VariableMetaModel<Solution_, ?, ?>, GroupVariableUpdaterInfo<Solution_>>
            getGroupVariableUpdaterInfoMap(
                    ConsistencyTracker<Solution_> consistencyTracker,
                    List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
                    Object[] entities) {
        var sortedDeclarativeVariableDescriptors =
                topologicallySortedDeclarativeShadowVariables(declarativeShadowVariableDescriptors);
        var groupIndexToVariables = new HashMap<Integer, List<DeclarativeShadowVariableDescriptor<Solution_>>>();
        var groupVariables = new ArrayList<DeclarativeShadowVariableDescriptor<Solution_>>();
        groupIndexToVariables.put(0, groupVariables);
        for (var declarativeShadowVariableDescriptor : sortedDeclarativeVariableDescriptors) {
            // If a @ShadowSources has a group source (i.e. "visitGroup[].arrivalTimes"),
            // create a new group since it must wait until all members of that group are processed
            var hasGroupSources = Arrays.stream(declarativeShadowVariableDescriptor.getSources())
                    .anyMatch(rootVariableSource -> rootVariableSource.parentVariableType() == ParentVariableType.GROUP);

            // If a @ShadowSources has an alignment key,
            // create a new group since multiple entities must be updated for this node
            var hasAlignmentKey = declarativeShadowVariableDescriptor.getAlignmentKeyMap() != null;

            // If the previous @ShadowSources has an alignment key,
            // create a new group since we are updating a single entity again
            // NOTE: Can potentially be optimized/share a node if VariableUpdaterInfo
            //       update each group member independently after the alignmentKey
            var previousHasAlignmentKey = !groupVariables.isEmpty() && groupVariables.get(0).getAlignmentKeyMap() != null;

            if (!groupVariables.isEmpty() && (hasGroupSources
                    || hasAlignmentKey
                    || previousHasAlignmentKey)) {
                groupVariables = new ArrayList<>();
                groupIndexToVariables.put(groupIndexToVariables.size(), groupVariables);
            }
            groupVariables.add(declarativeShadowVariableDescriptor);
        }

        var out = new HashMap<VariableMetaModel<Solution_, ?, ?>, GroupVariableUpdaterInfo<Solution_>>();
        var allUpdaters = new ArrayList<VariableUpdaterInfo<Solution_>>();
        var groupedUpdaters =
                new HashMap<DeclarativeShadowVariableDescriptor<Solution_>, Map<Object, VariableUpdaterInfo<Solution_>>>();
        var updaterKey = 0;
        for (var entryKey = 0; entryKey < groupIndexToVariables.size(); entryKey++) {
            var entryGroupVariables = groupIndexToVariables.get(entryKey);
            var updaters = new ArrayList<VariableUpdaterInfo<Solution_>>();
            for (var declarativeShadowVariableDescriptor : entryGroupVariables) {
                var updater = new VariableUpdaterInfo<>(
                        declarativeShadowVariableDescriptor.getVariableMetaModel(),
                        updaterKey,
                        declarativeShadowVariableDescriptor,
                        consistencyTracker.getDeclarativeEntityConsistencyState(
                                declarativeShadowVariableDescriptor.getEntityDescriptor()),
                        declarativeShadowVariableDescriptor.getMemberAccessor(),
                        declarativeShadowVariableDescriptor.getCalculator()::executeGetter);
                if (declarativeShadowVariableDescriptor.getAlignmentKeyMap() != null) {
                    var alignmentKeyFunction = declarativeShadowVariableDescriptor.getAlignmentKeyMap();
                    var alignmentKeyToAlignedEntitiesMap = new HashMap<Object, List<Object>>();
                    for (var entity : entities) {
                        if (declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass().isInstance(entity)) {
                            var alignmentKey = alignmentKeyFunction.apply(entity);
                            alignmentKeyToAlignedEntitiesMap.computeIfAbsent(alignmentKey, k -> new ArrayList<>()).add(entity);
                        }
                    }
                    for (var alignmentGroup : alignmentKeyToAlignedEntitiesMap.entrySet()) {
                        var updaterCopy = updater.withGroupId(updaterKey);
                        if (alignmentGroup.getKey() == null) {
                            updaters.add(updaterCopy);
                            allUpdaters.add(updaterCopy);
                        } else {
                            updaterCopy = updaterCopy.withGroupEntities(alignmentGroup.getValue().toArray(new Object[0]));
                            var variableUpdaterMap = groupedUpdaters.computeIfAbsent(declarativeShadowVariableDescriptor,
                                    ignored -> new IdentityHashMap<>());
                            for (var entity : alignmentGroup.getValue()) {
                                variableUpdaterMap.put(entity, updaterCopy);
                            }
                        }
                        updaterKey++;
                    }
                    updaterKey--; // it will be incremented again at end of the loop
                } else {
                    updaters.add(updater);
                    allUpdaters.add(updater);
                }
            }
            var groupVariableUpdaterInfo =
                    new GroupVariableUpdaterInfo<Solution_>(sortedDeclarativeVariableDescriptors, allUpdaters, updaters,
                            groupedUpdaters);
            for (var declarativeShadowVariableDescriptor : entryGroupVariables) {
                out.put(declarativeShadowVariableDescriptor.getVariableMetaModel(), groupVariableUpdaterInfo);
            }
            updaterKey++;
        }
        allUpdaters.replaceAll(updater -> updater.withGroupId(groupIndexToVariables.size()));
        return out;
    }

    private static <Solution_> VariableReferenceGraph buildArbitrarySingleEntityGraph(
            GraphDescriptor<Solution_> graphDescriptor) {
        var declarativeShadowVariableDescriptors =
                graphDescriptor.solutionDescriptor().getDeclarativeShadowVariableDescriptors();
        // Use a dependent lookup; if an entity does not use groups, then all variables can share the same node.
        // If the entity use groups, then variables must be grouped into their own nodes.
        var alignmentKeyMappers = new HashMap<VariableMetaModel<Solution_, ?, ?>, Function<Object, @Nullable Object>>();
        for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
            if (declarativeShadowVariableDescriptor.getAlignmentKeyMap() != null) {
                alignmentKeyMappers.put(declarativeShadowVariableDescriptor.getVariableMetaModel(),
                        declarativeShadowVariableDescriptor.getAlignmentKeyMap());
            }
        }
        var variableIdToUpdater =
                alignmentKeyMappers.isEmpty() ? EntityVariableUpdaterLookup.<Solution_> entityDependentLookup()
                        : EntityVariableUpdaterLookup.<Solution_> groupedEntityDependentLookup(alignmentKeyMappers::get);

        // Create graph node for each entity/declarative shadow variable group pair.
        // Maps a variable id to the source aliases of all variables in its group;
        // If the variables are (in topological order)
        // arrivalTime, readyTime, serviceStartTime, serviceFinishTime,
        // where serviceStartTime depends on a group of readyTime, then
        // the groups are [arrivalTime, readyTime] and [serviceStartTime, serviceFinishTime]
        // this is because from arrivalTime, you can compute readyTime without knowing either
        // serviceStartTime or serviceFinishTime.
        var variableIdToGroupedUpdater =
                getGroupVariableUpdaterInfoMap(graphDescriptor.consistencyTracker(), declarativeShadowVariableDescriptors,
                        graphDescriptor.entities());
        var declarativeShadowVariableToAliasMap = createGraphNodes(
                graphDescriptor, declarativeShadowVariableDescriptors, variableIdToUpdater,
                (entity, declarativeShadowVariable, variableId) -> variableIdToGroupedUpdater.get(variableId)
                        .getUpdatersForEntityVariable(entity, declarativeShadowVariable));
        return buildVariableReferenceGraph(graphDescriptor, declarativeShadowVariableDescriptors,
                declarativeShadowVariableToAliasMap);
    }

    private static <Solution_> Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> createGraphNodes(
            GraphDescriptor<Solution_> graphDescriptor,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            EntityVariableUpdaterLookup<Solution_> variableIdToUpdaters) {
        return createGraphNodes(graphDescriptor,
                declarativeShadowVariableDescriptors, variableIdToUpdaters,
                (entity, declarativeShadowVariableDescriptor,
                        variableId) -> Collections.singletonList(new VariableUpdaterInfo<>(
                                variableId,
                                variableIdToUpdaters.getNextId(),
                                declarativeShadowVariableDescriptor,
                                graphDescriptor.consistencyTracker().getDeclarativeEntityConsistencyState(
                                        declarativeShadowVariableDescriptor.getEntityDescriptor()),
                                declarativeShadowVariableDescriptor.getMemberAccessor(),
                                declarativeShadowVariableDescriptor.getCalculator()::executeGetter)));
    }

    private static <Solution_> Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> createGraphNodes(
            GraphDescriptor<Solution_> graphDescriptor,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            EntityVariableUpdaterLookup<Solution_> variableIdToUpdaters,
            TriFunction<Object, DeclarativeShadowVariableDescriptor<Solution_>, VariableMetaModel<Solution_, ?, ?>, List<VariableUpdaterInfo<Solution_>>> entityVariableToUpdatersMapper) {
        var result = new HashMap<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>>();
        for (var entity : graphDescriptor.entities()) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (entityClass.isInstance(entity)) {
                    var variableId = declarativeShadowVariableDescriptor.getVariableMetaModel();
                    var updaters = variableIdToUpdaters.computeUpdatersForVariableOnEntity(variableId,
                            entity,
                            () -> entityVariableToUpdatersMapper.apply(entity, declarativeShadowVariableDescriptor,
                                    variableId));
                    graphDescriptor.variableReferenceGraphBuilder.addVariableReferenceEntity(entity, updaters);
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

    private static <Solution_> void createSourceChangeProcessors(
            GraphDescriptor<Solution_> graphDescriptor,
            DeclarativeShadowVariableDescriptor<Solution_> declarativeShadowVariable,
            VariableMetaModel<Solution_, ?, ?> fromVariableId) {
        for (var source : declarativeShadowVariable.getSources()) {
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
                                            var changed = graph.lookupOrNull(fromVariableId, entity);
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
                                    .getCollectionInverseVariableSupply(parentVariable);

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
                                                var changed = graph.lookupOrNull(fromVariableId, item);
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

    private static <Solution_> void createAliasToVariableChangeProcessors(
            VariableReferenceGraphBuilder<Solution_> variableReferenceGraphBuilder, Set<VariableSourceReference> aliasSet,
            VariableMetaModel<Solution_, ?, ?> fromVariableId) {
        for (var alias : aliasSet) {
            var toVariableId = alias.targetVariableMetamodel();
            var sourceVariableId = alias.variableMetaModel();

            if (!alias.isDeclarative() && alias.affectGraphEdges()) {
                // Exploit the same fact as above
                variableReferenceGraphBuilder.addBeforeProcessor(GraphChangeType.REMOVE_EDGE, sourceVariableId,
                        (graph, toEntity) -> {
                            // from/to can be null in extended models
                            // ex: previous is used as a source, but only an extended class
                            // has the to variable
                            var to = graph.lookupOrNull(toVariableId, toEntity);
                            if (to == null) {
                                return;
                            }
                            var fromEntity = alias.targetEntityFunctionStartingFromVariableEntity()
                                    .apply(toEntity);
                            if (fromEntity == null) {
                                return;
                            }
                            var from = graph.lookupOrNull(fromVariableId, fromEntity);
                            if (from == null) {
                                return;
                            }
                            graph.removeEdge(from, to);
                        });
                variableReferenceGraphBuilder.addAfterProcessor(GraphChangeType.ADD_EDGE, sourceVariableId,
                        (graph, toEntity) -> {
                            var to = graph.lookupOrNull(toVariableId, toEntity);
                            if (to == null) {
                                return;
                            }
                            var fromEntity = alias.findTargetEntity(toEntity);
                            if (fromEntity == null) {
                                return;
                            }
                            var from = graph.lookupOrNull(fromVariableId, fromEntity);
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
                            sourceRoot.valueEntityFunction()
                                    .accept(entity, fromEntity -> {
                                        var from = variableReferenceGraphBuilder.lookupOrError(fromVariableId, fromEntity);
                                        variableReferenceGraphBuilder.addFixedEdge(from, to);
                                    });
                            break;
                        }
                    }
                }
            }
        }
    }

    public DefaultShadowVariableSession<Solution_> forSolution(ConsistencyTracker<Solution_> consistencyTracker,
            Solution_ solution) {
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        return forEntities(consistencyTracker, entities.toArray());
    }

    public DefaultShadowVariableSession<Solution_> forEntities(ConsistencyTracker<Solution_> consistencyTracker,
            Object... entities) {
        var graph = buildGraph(
                new GraphDescriptor<>(solutionDescriptor, ChangedVariableNotifier.of(scoreDirector), entities)
                        .withConsistencyTracker(consistencyTracker)
                        .withGraphCreator(graphCreator));
        return new DefaultShadowVariableSession<>(graph);
    }
}
