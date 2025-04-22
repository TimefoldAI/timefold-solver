package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultShadowVariableSessionFactory<Solution_> {
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

    public static <Solution_> void visitGraph(
            SolutionDescriptor<Solution_> solutionDescriptor,
            VariableReferenceGraph<Solution_> variableReferenceGraph, Object[] entities,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        var declarativeShadowVariableDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();
        var variableIdToUpdater = new HashMap<VariableMetaModel<?, ?, ?>, VariableUpdaterInfo>();

        // Maps a variable id to it source aliases;
        // For instance, "previousVisit.startTime" is a source alias of "startTime"
        // One way to view this concept is "previousVisit.startTime" is a pointer
        // to "startTime" of some visit, and thus alias it.
        Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> declarativeShadowVariableToAliasMap = new HashMap<>();

        // Create graph node for each entity/declarative shadow variable pair
        createGraphNodes(variableReferenceGraph, entities, declarativeShadowVariableDescriptors, variableIdToUpdater,
                declarativeShadowVariableToAliasMap);

        // Create variable processors for each declarative shadow variable descriptor
        for (var declarativeShadowVariable : declarativeShadowVariableDescriptors) {
            final var fromVariableId = declarativeShadowVariable.getVariableMetaModel();
            createSourceChangeProcessors(variableReferenceGraph, declarativeShadowVariable, fromVariableId);
            createAliasToVariableChangeProcessors(variableReferenceGraph, declarativeShadowVariableToAliasMap, fromVariableId);
        }

        // Create the fixed edges in the graph
        createFixedVariableRelationEdges(variableReferenceGraph, entities, declarativeShadowVariableDescriptors);
        variableReferenceGraph.createGraph(graphCreator);
    }

    private static <Solution_> void createGraphNodes(VariableReferenceGraph<Solution_> graph, Object[] entities,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            Map<VariableMetaModel<?, ?, ?>, VariableUpdaterInfo> variableIdToUpdater,
            Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> declarativeShadowVariableToAliasMap) {
        for (var entity : entities) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (entityClass.isInstance(entity)) {
                    var variableId = declarativeShadowVariableDescriptor.getVariableMetaModel();
                    var updater = variableIdToUpdater.computeIfAbsent(variableId, ignored -> new VariableUpdaterInfo(
                            declarativeShadowVariableDescriptor,
                            declarativeShadowVariableDescriptor.getEntityDescriptor().getShadowVariableLoopedDescriptor(),
                            declarativeShadowVariableDescriptor.getMemberAccessor(),
                            declarativeShadowVariableDescriptor.getCalculator()::executeGetter));
                    graph.addVariableReferenceEntity(
                            variableId,
                            entity,
                            updater);
                    for (var sourceRoot : declarativeShadowVariableDescriptor.getSources()) {
                        for (var source : sourceRoot.variableSourceReferences()) {
                            if (source.downstreamDeclarativeVariableMetamodel() != null) {
                                declarativeShadowVariableToAliasMap
                                        .computeIfAbsent(source.downstreamDeclarativeVariableMetamodel(),
                                                ignored -> new LinkedHashSet<>())
                                        .add(source);
                            }
                        }
                    }
                }
            }
        }
    }

    private static <Solution_> void createSourceChangeProcessors(VariableReferenceGraph<Solution_> variableReferenceGraph,
            DeclarativeShadowVariableDescriptor<Solution_> declarativeShadowVariable,
            VariableMetaModel<Solution_, ?, ?> fromVariableId) {
        for (var source : declarativeShadowVariable.getSources()) {
            for (var sourcePart : source.variableSourceReferences()) {
                var toVariableId = sourcePart.variableMetaModel();

                // declarative variables have edges to each other in the graph,
                // and will mark dependent variable as changed.
                // non-declarative variables are not in the graph and must have their
                // own processor
                if (!sourcePart.isDeclarative()) {
                    variableReferenceGraph.addAfterProcessor(toVariableId, (graph, entity) ->
                    // Exploits the fact the source entity and the target entity must be the same,
                    // since non-declarative variables can only be accessed from the root entity
                    // i.e. paths like "otherVisit.previous"
                    // or "visitGroup[].otherVisit.previous" are not allowed,
                    // but paths like "previous" or
                    // "visitGroup[].previous" are.
                    // Without this invariant, an inverse set must be calculated
                    // and maintained,
                    // and this code is complicated enough.
                    graph.markChanged(graph.lookup(fromVariableId, entity)));
                }
            }
        }
    }

    private static <Solution_> void createAliasToVariableChangeProcessors(
            VariableReferenceGraph<Solution_> variableReferenceGraph,
            Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> declarativeShadowVariableToAliasMap,
            VariableMetaModel<Solution_, ?, ?> fromVariableId) {
        for (var alias : declarativeShadowVariableToAliasMap.getOrDefault(fromVariableId, Collections.emptySet())) {
            var toVariableId = alias.targetVariableMetamodel();
            var sourceVariableId = alias.variableMetaModel();

            if (!alias.isDeclarative() && alias.affectGraphEdges()) {
                // Exploit the same fact as above
                variableReferenceGraph.addBeforeProcessor(sourceVariableId,
                        (graph, toEntity) -> alias.targetEntityFunctionStartingFromVariableEntity().accept(toEntity,
                                fromEntity -> graph.removeEdge(
                                        graph.lookup(fromVariableId, fromEntity),
                                        graph.lookup(toVariableId, toEntity))));
                variableReferenceGraph.addAfterProcessor(sourceVariableId,
                        (graph, toEntity) -> alias.targetEntityFunctionStartingFromVariableEntity().accept(toEntity,
                                fromEntity -> graph.addEdge(graph.lookup(fromVariableId, fromEntity),
                                        graph.lookup(toVariableId, toEntity))));
            }
            // Note: it is impossible to have a declarative variable affect graph edges,
            // since accessing a declarative variable from another declarative variable is prohibited.
        }
    }

    private static <Solution_> void createFixedVariableRelationEdges(VariableReferenceGraph<Solution_> variableReferenceGraph,
            Object[] entities,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors) {
        for (var entity : entities) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (entityClass.isInstance(entity)) {
                    var toVariableId = declarativeShadowVariableDescriptor.getVariableMetaModel();
                    for (var sourceRoot : declarativeShadowVariableDescriptor.getSources()) {
                        for (var source : sourceRoot.variableSourceReferences()) {
                            if (source.isTopLevel() && source.isDeclarative()) {
                                var fromVariableId = source.variableMetaModel();

                                sourceRoot.valueEntityFunction()
                                        .accept(entity, fromEntity -> variableReferenceGraph.addFixedEdge(
                                                variableReferenceGraph
                                                        .lookup(fromVariableId, fromEntity),
                                                variableReferenceGraph
                                                        .lookup(toVariableId, entity)));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public DefaultShadowVariableSession<Solution_> forSolution(Solution_ solution) {
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        return forEntities(entities.toArray());
    }

    public DefaultShadowVariableSession<Solution_> forEntities(Object... entities) {
        var variableReferenceGraph = new VariableReferenceGraph<>(ChangedVariableNotifier.of(scoreDirector));

        visitGraph(solutionDescriptor, variableReferenceGraph, entities, graphCreator);

        return new DefaultShadowVariableSession<>(variableReferenceGraph);
    }
}
