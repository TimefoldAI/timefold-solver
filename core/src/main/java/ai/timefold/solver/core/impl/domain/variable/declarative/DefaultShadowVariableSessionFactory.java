package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSessionFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultShadowVariableSessionFactory<Solution_> implements ShadowVariableSessionFactory {
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final SupplyManager supplyManager;
    private final IntFunction<TopologicalOrderGraph> graphCreator;

    public DefaultShadowVariableSessionFactory(
            SolutionDescriptor<Solution_> solutionDescriptor,
            InnerScoreDirector<Solution_, ?> scoreDirector, SupplyManager supplyManager,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        this.solutionDescriptor = solutionDescriptor;
        this.scoreDirector = scoreDirector;
        this.supplyManager = supplyManager;
        this.graphCreator = graphCreator;
    }

    static <Solution_> void visitGraph(
            SolutionDescriptor<Solution_> solutionDescriptor,
            VariableReferenceGraph<Solution_> variableReferenceGraph, Object[] entities,
            IntFunction<TopologicalOrderGraph> graphCreator) {
        var declarativeShadowVariableDescriptors = solutionDescriptor.getDeclarativeShadowVariableDescriptors();
        var variableIdToUpdater = new HashMap<VariableId, VariableUpdaterInfo>();

        // Maps a variable id to it source aliases;
        // For instance, "previousVisit.startTime" is a source alias of "startTime"
        // One way to view this concept is "previousVisit.startTime" is a pointer
        // to "startTime" of some visit, and thus alias it.
        Map<VariableId, Set<VariableSourceReference>> declarativeShadowVariableToAliasMap = new HashMap<>();

        // Create graph node for each entity/declarative shadow variable pair
        for (var entity : entities) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (entityClass.isInstance(entity)) {
                    var variableId = new VariableId(entityClass, declarativeShadowVariableDescriptor.getVariableName());
                    var updater = variableIdToUpdater.computeIfAbsent(variableId, ignored -> new VariableUpdaterInfo(
                            entityClass,
                            declarativeShadowVariableDescriptor.getVariableName(),
                            declarativeShadowVariableDescriptor,
                            declarativeShadowVariableDescriptor.getEntityDescriptor().getInvalidityMarkerVariableDescriptor(),
                            declarativeShadowVariableDescriptor.getMemberAccessor(),
                            declarativeShadowVariableDescriptor.getCalculator()::executeGetter));
                    variableReferenceGraph.addVariableReferenceEntity(
                            variableId,
                            entity,
                            updater);
                    for (var sourceRoot : declarativeShadowVariableDescriptor.getSources()) {
                        for (var source : sourceRoot.variableSourceReferences()) {
                            if (source.downstreamDeclarativeVariable() != null) {
                                declarativeShadowVariableToAliasMap
                                        .computeIfAbsent(source.downstreamDeclarativeVariable(),
                                                ignored -> new LinkedHashSet<>())
                                        .add(source);
                            }
                        }
                    }
                }
            }
        }

        // Create variable listeners for each declarative shadow variable descriptor
        for (var declarativeShadowVariable : declarativeShadowVariableDescriptors) {
            var fromEntityClass = declarativeShadowVariable.getEntityDescriptor().getEntityClass();
            var fromVariableName = declarativeShadowVariable.getVariableName();
            final var fromVariableId = new VariableId(fromEntityClass, fromVariableName);
            variableReferenceGraph.addShadowVariable(variableIdToUpdater.get(fromVariableId));

            for (var source : declarativeShadowVariable.getSources()) {
                for (var sourcePart : source.variableSourceReferences()) {
                    var toEntityClass = sourcePart.entityClass();
                    var toVariableName = sourcePart.variableName();
                    var toVariableId = new VariableId(toEntityClass, toVariableName);

                    // declarative variables have edges to each other in the graph,
                    // and will mark dependent variable as changed.
                    // non-declarative variables are not in the graph and must have their
                    // own processor
                    if (!sourcePart.isDeclarative()) {
                        variableReferenceGraph.addAfterProcessor(toVariableId, (graph, entity) -> {
                            // Exploits the fact the source entity and the target entity must be the same,
                            // since non-declarative variables can only be accessed from the root entity
                            // i.e. paths like "otherVisit.previous"
                            // or "visitGroup[].otherVisit.previous" are not allowed,
                            // but paths like "previous" or
                            // "visitGroup[].previous" are.
                            // Without this invariant, an inverse set must be calculated
                            // and maintained,
                            // and this code is complicated enough.
                            graph.markChanged(graph.lookup(fromVariableId, entity));
                        });
                    }
                }
            }

            for (var alias : declarativeShadowVariableToAliasMap.getOrDefault(fromVariableId, Collections.emptySet())) {
                var toVariableId = alias.targetVariableId();
                var sourceVariableId = new VariableId(alias.entityClass(), alias.variableName());

                if (!alias.isDeclarative() && alias.affectGraphEdges()) {
                    // Exploit the same fact as above
                    variableReferenceGraph.addBeforeProcessor(sourceVariableId, (graph, toEntity) -> {
                        alias.targetEntityFunctionStartingFromVariableEntity().accept(toEntity, fromEntity -> {
                            graph.removeEdge(
                                    (EntityVariableOrFactReference<Solution_>) graph.lookup(fromVariableId, fromEntity),
                                    (EntityVariableOrFactReference<Solution_>) graph.lookup(toVariableId, toEntity));
                        });
                    });
                    variableReferenceGraph.addAfterProcessor(sourceVariableId, (graph, toEntity) -> {
                        alias.targetEntityFunctionStartingFromVariableEntity().accept(toEntity, fromEntity -> {
                            graph.addEdge((EntityVariableOrFactReference<Solution_>) graph.lookup(fromVariableId, fromEntity),
                                    (EntityVariableOrFactReference<Solution_>) graph.lookup(toVariableId, toEntity));
                        });
                    });
                }
                // Note: it is impossible to have a declarative variable affect graph edges,
                // since accessing a declarative variable from another declarative variable is prohibited.
            }
        }

        // Create the fixed edges in the graph
        for (var entity : entities) {
            for (var declarativeShadowVariableDescriptor : declarativeShadowVariableDescriptors) {
                var entityClass = declarativeShadowVariableDescriptor.getEntityDescriptor().getEntityClass();
                if (entityClass.isInstance(entity)) {
                    var toVariableId = new VariableId(entityClass, declarativeShadowVariableDescriptor.getVariableName());
                    for (var sourceRoot : declarativeShadowVariableDescriptor.getSources()) {
                        for (var source : sourceRoot.variableSourceReferences()) {
                            if (source.isTopLevel() && source.isDeclarative()) {
                                var fromEntityClass = source.entityClass();
                                var fromVariableName = source.variableName();
                                var fromVariableId = new VariableId(fromEntityClass, fromVariableName);

                                ((BiConsumer<Object, Consumer>) (BiConsumer) sourceRoot.valueEntityFunction())
                                        .accept(entity, fromEntity -> {
                                            variableReferenceGraph.addFixedEdge(
                                                    (EntityVariableOrFactReference<Solution_>) variableReferenceGraph
                                                            .lookup(fromVariableId, fromEntity),
                                                    (EntityVariableOrFactReference<Solution_>) variableReferenceGraph
                                                            .lookup(toVariableId, entity));
                                        });
                                break;
                            }
                        }
                    }
                }
            }
        }
        variableReferenceGraph.createGraph(graphCreator);
    }

    public DefaultShadowVariableSession<Solution_> forSolution(Solution_ solution) {
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        return forEntities(entities.toArray());
    }

    @Override
    public DefaultShadowVariableSession<Solution_> forEntities(Object... entities) {
        var variableReferenceGraph = new VariableReferenceGraph<>(ChangedVariableNotifier.of(scoreDirector));

        visitGraph(solutionDescriptor, variableReferenceGraph, entities, graphCreator);

        return new DefaultShadowVariableSession<>(variableReferenceGraph);
    }
}
