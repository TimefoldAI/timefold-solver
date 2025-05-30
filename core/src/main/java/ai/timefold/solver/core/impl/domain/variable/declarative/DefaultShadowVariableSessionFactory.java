package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
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

    @SuppressWarnings("unchecked")
    public static <Solution_> VariableReferenceGraph<Solution_> buildGraph(
            SolutionDescriptor<Solution_> solutionDescriptor,
            VariableReferenceGraphBuilder<Solution_> variableReferenceGraphBuilder, Object[] entities,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        var declarativeShadowVariableDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();
        if (declarativeShadowVariableDescriptors.isEmpty()) {
            return EmptyVariableReferenceGraph.INSTANCE;
        }
        var variableIdToUpdater = new HashMap<VariableMetaModel<?, ?, ?>, VariableUpdaterInfo<Solution_>>();

        // Create graph node for each entity/declarative shadow variable pair.
        // Maps a variable id to it source aliases;
        // For instance, "previousVisit.startTime" is a source alias of "startTime"
        // One way to view this concept is "previousVisit.startTime" is a pointer
        // to "startTime" of some visit, and thus alias it.
        var declarativeShadowVariableToAliasMap = createGraphNodes(variableReferenceGraphBuilder, entities,
                declarativeShadowVariableDescriptors, variableIdToUpdater);

        // Create variable processors for each declarative shadow variable descriptor
        for (var declarativeShadowVariable : declarativeShadowVariableDescriptors) {
            var fromVariableId = declarativeShadowVariable.getVariableMetaModel();
            createSourceChangeProcessors(entities, variableReferenceGraphBuilder, declarativeShadowVariable, fromVariableId);
            var aliasSet = declarativeShadowVariableToAliasMap.get(fromVariableId);
            if (aliasSet != null) {
                createAliasToVariableChangeProcessors(variableReferenceGraphBuilder, aliasSet, fromVariableId);
            }
        }

        // Create the fixed edges in the graph
        createFixedVariableRelationEdges(variableReferenceGraphBuilder, entities, declarativeShadowVariableDescriptors);
        return variableReferenceGraphBuilder.build(graphCreator);
    }

    private static <Solution_> Map<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>> createGraphNodes(
            VariableReferenceGraphBuilder<Solution_> graph, Object[] entities,
            List<DeclarativeShadowVariableDescriptor<Solution_>> declarativeShadowVariableDescriptors,
            Map<VariableMetaModel<?, ?, ?>, VariableUpdaterInfo<Solution_>> variableIdToUpdater) {
        var result = new HashMap<VariableMetaModel<?, ?, ?>, Set<VariableSourceReference>>();
        for (var entity : entities) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (entityClass.isInstance(entity)) {
                    var variableId = declarativeShadowVariableDescriptor.getVariableMetaModel();
                    var updater = variableIdToUpdater.computeIfAbsent(variableId, ignored -> new VariableUpdaterInfo<>(
                            variableId,
                            declarativeShadowVariableDescriptor,
                            declarativeShadowVariableDescriptor.getEntityDescriptor().getShadowVariableLoopedDescriptor(),
                            declarativeShadowVariableDescriptor.getMemberAccessor(),
                            declarativeShadowVariableDescriptor.getCalculator()::executeGetter));
                    graph.addVariableReferenceEntity(entity, updater);
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
            Object[] entities,
            VariableReferenceGraphBuilder<Solution_> variableReferenceGraphBuilder,
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
                    if (sourcePart.onRootEntity()) {
                        // No need for inverse set; source and target entity are the same.
                        variableReferenceGraphBuilder.addAfterProcessor(toVariableId, (graph, entity) -> {
                            var changed = graph.lookupOrNull(fromVariableId, entity);
                            if (changed != null) {
                                graph.markChanged(changed);
                            }
                        });
                    } else {
                        // Need to create an inverse set from source to target
                        var inverseMap = new IdentityHashMap<Object, List<Object>>();
                        var visitor = source.getEntityVisitor(sourcePart.chainToVariableEntity());
                        for (var rootEntity : entities) {
                            if (declarativeShadowVariable.getEntityDescriptor().getEntityClass().isInstance(rootEntity)) {
                                visitor.accept(rootEntity, shadowEntity -> inverseMap
                                        .computeIfAbsent(shadowEntity, ignored -> new ArrayList<>()).add(rootEntity));
                            }
                        }
                        variableReferenceGraphBuilder.addAfterProcessor(toVariableId, (graph, entity) -> {
                            for (var item : inverseMap.getOrDefault(entity, Collections.emptyList())) {
                                var changed = graph.lookupOrNull(fromVariableId, item);
                                if (changed != null) {
                                    graph.markChanged(changed);
                                }
                            }
                        });
                    }
                }
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
                variableReferenceGraphBuilder.addBeforeProcessor(sourceVariableId,
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
                variableReferenceGraphBuilder.addAfterProcessor(sourceVariableId,
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

    public DefaultShadowVariableSession<Solution_> forSolution(Solution_ solution) {
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        return forEntities(entities.toArray());
    }

    public DefaultShadowVariableSession<Solution_> forEntities(Object... entities) {
        var builder = new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.of(scoreDirector));
        var graph = buildGraph(solutionDescriptor, builder, entities, graphCreator);
        return new DefaultShadowVariableSession<>(graph);
    }
}
