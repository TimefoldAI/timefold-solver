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
import java.util.stream.IntStream;

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
            case ARBITRARY_SINGLE_ENTITY_AT_MOST_ONE_DIRECTIONAL_PARENT_TYPE ->
                buildArbitrarySingleEntityGraph(graphDescriptor);
            case NO_DYNAMIC_EDGES, ARBITRARY ->
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
        return graphDescriptor.variableReferenceGraphBuilder().build(graphDescriptor.graphCreator(),
                graphDescriptor.ignoreInconsistentSolutions());
    }

    private record GroupVariableUpdaterInfo<Solution_>(
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedDeclarativeVariableDescriptors,
            List<VariableUpdaterInfo<Solution_>> allUpdaters,
            StaticVariableGroups<Solution_> staticGroups,
            int groupIndex,
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
                    if (rootSource.parentVariableType() == ParentVariableType.LIST_ELEMENT) {
                        // Unlike a fact collection, a list variable's contents change during solving,
                        // so the decision must not depend on its current contents.
                        return staticGroups.getUpdatersForEntity(entity, groupIndex);
                    }
                    if (rootSource.parentVariableType() == ParentVariableType.GROUP && hasMembers(rootSource, entity)) {
                        return staticGroups.getUpdatersForEntity(entity, groupIndex);
                    }
                }
            }
            return allUpdaters;
        }

    }

    private static boolean hasMembers(RootVariableSource<?, ?> groupSource, Object entity) {
        var visitedCount = new MutableInt();
        groupSource.valueEntityFunction().accept(entity, ignored -> visitedCount.increment());
        return visitedCount.intValue() > 0;
    }

    /**
     * The groups of declarative shadow variables that need their own graph node
     * for an entity that uses a group or a list element source.
     * <p>
     * For each entity, a group joins the node before it when nothing outside that node can send an edge into the group:
     * <ul>
     * <li>The group starts only because of its group sources (no list element source, no alignment key).</li>
     * <li>Each of those group sources reads its variable from the members of the group (i.e. "visitGroup[].arrivalTime"),
     * and the group is empty for the entity.</li>
     * <li>Each other source is a variable of the entity itself.</li>
     * <li>The group reads a declarative shadow variable of the node it joins, and none of an earlier node.</li>
     * <li>No source of the node it joins reaches the entity itself to read a variable of the group.
     * Inside one node, that edge would vanish, and with it the fixed loop that it closes.</li>
     * </ul>
     * Then every edge from outside enters the first group of the joined node, and each later group depends on that node,
     * so a loop through the joined node is also a loop through the separate nodes.
     * A separate node for such a group only adds work to every update.
     */
    private static final class StaticVariableGroups<Solution_> {

        private final List<List<VariableUpdaterInfo<Solution_>>> groupIndexToUpdaters;
        private final @Nullable JoinCondition[] groupIndexToJoinCondition;
        private final Map<BitSet, List<List<VariableUpdaterInfo<Solution_>>>> keptSplitsToJoinedGroupUpdaters =
                new HashMap<>();
        private int nextGroupId;

        private StaticVariableGroups(List<List<DeclarativeShadowVariableDescriptor<Solution_>>> groupIndexToVariables,
                boolean[] isSplitOnlyByGroupSources, List<List<VariableUpdaterInfo<Solution_>>> groupIndexToUpdaters) {
            this.groupIndexToUpdaters = groupIndexToUpdaters;
            var groupCount = groupIndexToVariables.size();
            var variableToGroupIndex = new HashMap<VariableMetaModel<?, ?, ?>, Integer>();
            for (var groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                for (var variableDescriptor : groupIndexToVariables.get(groupIndex)) {
                    variableToGroupIndex.put(variableDescriptor.getVariableMetaModel(), groupIndex);
                }
            }
            var groupIndexToBackwardSources = new ArrayList<List<BackwardSource>>(groupCount);
            for (var groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                groupIndexToBackwardSources.add(new ArrayList<>());
            }
            for (var groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                for (var variableDescriptor : groupIndexToVariables.get(groupIndex)) {
                    for (var source : variableDescriptor.getSources()) {
                        var sourceReferences = source.variableSourceReferences();
                        var readReference = sourceReferences.get(sourceReferences.size() - 1);
                        var readGroupIndex = readReference.isDeclarative()
                                ? variableToGroupIndex.get(readReference.variableMetaModel())
                                : null;
                        if (readGroupIndex != null && readGroupIndex > groupIndex) {
                            groupIndexToBackwardSources.get(readGroupIndex).add(new BackwardSource(groupIndex, source));
                        }
                    }
                }
            }
            this.groupIndexToJoinCondition = new JoinCondition[groupCount];
            for (var groupIndex = 1; groupIndex < groupCount; groupIndex++) {
                if (isSplitOnlyByGroupSources[groupIndex]) {
                    groupIndexToJoinCondition[groupIndex] = JoinCondition.of(groupIndex,
                            groupIndexToVariables.get(groupIndex), variableToGroupIndex,
                            groupIndexToBackwardSources.get(groupIndex));
                }
            }
        }

        private List<VariableUpdaterInfo<Solution_>> getUpdatersForEntity(Object entity, int groupIndex) {
            var groupCount = groupIndexToUpdaters.size();
            var keptSplits = new BitSet(groupCount);
            var nodeStartGroupIndex = 0;
            for (var i = 1; i < groupCount; i++) {
                var joinCondition = groupIndexToJoinCondition[i];
                if (joinCondition == null || !joinCondition.allows(entity, nodeStartGroupIndex)) {
                    keptSplits.set(i);
                    nodeStartGroupIndex = i;
                }
            }
            if (keptSplits.cardinality() == groupCount - 1) {
                return groupIndexToUpdaters.get(groupIndex);
            }
            var joinedGroupUpdaters = keptSplitsToJoinedGroupUpdaters.computeIfAbsent(keptSplits, this::joinGroups);
            // Bit 0 is never set, so this counts the kept splits up to and including this group.
            return joinedGroupUpdaters.get(keptSplits.get(0, groupIndex + 1).cardinality());
        }

        private List<List<VariableUpdaterInfo<Solution_>>> joinGroups(BitSet keptSplits) {
            var out = new ArrayList<List<VariableUpdaterInfo<Solution_>>>();
            List<VariableUpdaterInfo<Solution_>> joinedUpdaters = new ArrayList<>();
            var joinedGroupId = nextGroupId;
            nextGroupId++;
            for (var i = 0; i < groupIndexToUpdaters.size(); i++) {
                if (keptSplits.get(i)) {
                    out.add(joinedUpdaters);
                    joinedUpdaters = new ArrayList<>();
                    joinedGroupId = nextGroupId;
                    nextGroupId++;
                }
                for (var updater : groupIndexToUpdaters.get(i)) {
                    joinedUpdaters.add(updater.withGroupId(joinedGroupId));
                }
            }
            out.add(joinedUpdaters);
            return out;
        }

    }

    /**
     * When a group of declarative shadow variables can join the node before it, for one entity.
     *
     * @param groupSources the group sources of the group, which must all be empty for the entity
     * @param lowestOwnSourceGroupIndex the lowest group index of the declarative shadow variables of the entity
     *        that the group reads
     * @param highestEarlierOwnSourceGroupIndex the highest such group index before the group itself
     * @param backwardSources the sources of earlier groups that read a variable of the group
     */
    private record JoinCondition(List<RootVariableSource<?, ?>> groupSources, int lowestOwnSourceGroupIndex,
            int highestEarlierOwnSourceGroupIndex, List<BackwardSource> backwardSources) {

        /**
         * @return null if the group can never join the node before it
         */
        static <Solution_> @Nullable JoinCondition of(int groupIndex,
                List<DeclarativeShadowVariableDescriptor<Solution_>> groupVariables,
                Map<VariableMetaModel<?, ?, ?>, Integer> variableToGroupIndex, List<BackwardSource> backwardSources) {
            var groupSources = new ArrayList<RootVariableSource<?, ?>>();
            var lowestOwnSourceGroupIndex = Integer.MAX_VALUE;
            var highestEarlierOwnSourceGroupIndex = -1;
            for (var variableDescriptor : groupVariables) {
                for (var source : variableDescriptor.getSources()) {
                    var sourceReferences = source.variableSourceReferences();
                    if (sourceReferences.size() != 1) {
                        // The path goes through a variable, so it can reach other entities later.
                        return null;
                    }
                    if (source.parentVariableType() == ParentVariableType.GROUP) {
                        groupSources.add(source);
                        continue;
                    }
                    var sourceReference = sourceReferences.get(0);
                    if (!sourceReference.onRootEntity()) {
                        return null;
                    }
                    if (sourceReference.isDeclarative()) {
                        var sourceGroupIndex = variableToGroupIndex.get(sourceReference.variableMetaModel());
                        if (sourceGroupIndex == null) {
                            return null;
                        }
                        lowestOwnSourceGroupIndex = Math.min(lowestOwnSourceGroupIndex, sourceGroupIndex);
                        if (sourceGroupIndex < groupIndex) {
                            highestEarlierOwnSourceGroupIndex = Math.max(highestEarlierOwnSourceGroupIndex, sourceGroupIndex);
                        }
                    }
                }
            }
            if (highestEarlierOwnSourceGroupIndex < 0) {
                // Nothing connects the group to the node before it.
                return null;
            }
            return new JoinCondition(List.copyOf(groupSources), lowestOwnSourceGroupIndex,
                    highestEarlierOwnSourceGroupIndex, List.copyOf(backwardSources));
        }

        boolean allows(Object entity, int nodeStartGroupIndex) {
            if (lowestOwnSourceGroupIndex < nodeStartGroupIndex || highestEarlierOwnSourceGroupIndex < nodeStartGroupIndex) {
                return false;
            }
            for (var groupSource : groupSources) {
                if (hasMembers(groupSource, entity)) {
                    return false;
                }
            }
            for (var backwardSource : backwardSources) {
                if (backwardSource.groupIndex() >= nodeStartGroupIndex && backwardSource.mayReachItself(entity)) {
                    return false;
                }
            }
            return true;
        }

    }

    /**
     * A source of a variable in an earlier group that reads a declarative shadow variable of a later group.
     *
     * @param groupIndex the group of the variable that has the source
     */
    private record BackwardSource(int groupIndex, RootVariableSource<?, ?> source) {

        boolean mayReachItself(Object entity) {
            return switch (source.parentVariableType()) {
                // A list holds each element once, so the element before or after an element is never that element.
                case PREVIOUS, NEXT -> false;
                // With one variable on the path, only facts lead to it, so the entities that the path reaches never change.
                // A path through another variable, or through a list variable, can reach the entity itself later.
                case GROUP, INDIRECT, INVERSE, VARIABLE, NO_PARENT ->
                    source.variableSourceReferences().size() != 1 || reachesItself(source, entity);
                case LIST_ELEMENT -> true;
            };
        }

        private static boolean reachesItself(RootVariableSource<?, ?> source, Object entity) {
            var reachedCount = new MutableInt();
            source.valueEntityFunction().accept(entity, reachedEntity -> {
                if (reachedEntity == entity) {
                    reachedCount.increment();
                }
            });
            return reachedCount.intValue() > 0;
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
        var groupIndexToSplitOnlyByGroupSources = new ArrayList<Boolean>();
        var groupVariables = new ArrayList<DeclarativeShadowVariableDescriptor<Solution_>>();
        groupIndexToVariables.put(0, groupVariables);
        groupIndexToSplitOnlyByGroupSources.add(false);
        for (var declarativeShadowVariableDescriptor : sortedDeclarativeVariableDescriptors) {
            // If a @ShadowSources has a group source (i.e. "visitGroup[].arrivalTimes")
            // or a list element source (i.e. "values[].endTime"),
            // create a new group since it must wait until all members of that group are processed
            var hasListElementSources = Arrays.stream(declarativeShadowVariableDescriptor.getSources())
                    .anyMatch(rootVariableSource -> rootVariableSource.parentVariableType() == ParentVariableType.LIST_ELEMENT);
            var hasGroupSources = hasListElementSources || Arrays.stream(declarativeShadowVariableDescriptor.getSources())
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
                groupIndexToSplitOnlyByGroupSources
                        .add(!hasListElementSources && !hasAlignmentKey && !previousHasAlignmentKey);
            }
            groupVariables.add(declarativeShadowVariableDescriptor);
        }

        var out = new HashMap<VariableMetaModel<Solution_, ?, ?>, GroupVariableUpdaterInfo<Solution_>>();
        var allUpdaters = new ArrayList<VariableUpdaterInfo<Solution_>>();
        var groupIndexToUpdaters = new ArrayList<List<VariableUpdaterInfo<Solution_>>>();
        var groupedUpdaters =
                new HashMap<DeclarativeShadowVariableDescriptor<Solution_>, Map<Object, VariableUpdaterInfo<Solution_>>>();
        var groupCount = groupIndexToVariables.size();
        var isSplitOnlyByGroupSources = new boolean[groupCount];
        for (var i = 0; i < groupCount; i++) {
            isSplitOnlyByGroupSources[i] = groupIndexToSplitOnlyByGroupSources.get(i);
        }
        var staticGroups = new StaticVariableGroups<Solution_>(
                IntStream.range(0, groupCount).mapToObj(groupIndexToVariables::get).toList(), isSplitOnlyByGroupSources,
                groupIndexToUpdaters);
        var updaterKey = 0;
        for (var entryKey = 0; entryKey < groupCount; entryKey++) {
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
                        declarativeShadowVariableDescriptor.getCalculator());
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
            groupIndexToUpdaters.add(updaters);
            var groupVariableUpdaterInfo =
                    new GroupVariableUpdaterInfo<Solution_>(sortedDeclarativeVariableDescriptors, allUpdaters,
                            staticGroups, entryKey, groupedUpdaters);
            for (var declarativeShadowVariableDescriptor : entryGroupVariables) {
                out.put(declarativeShadowVariableDescriptor.getVariableMetaModel(), groupVariableUpdaterInfo);
            }
            updaterKey++;
        }
        allUpdaters.replaceAll(updater -> updater.withGroupId(groupCount));
        // Merged groups take ids that no other updater uses.
        staticGroups.nextGroupId = Math.max(updaterKey, groupCount + 1);
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
                                declarativeShadowVariableDescriptor.getCalculator())));
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
