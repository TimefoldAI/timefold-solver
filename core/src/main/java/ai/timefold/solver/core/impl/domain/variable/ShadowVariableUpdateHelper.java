package ai.timefold.solver.core.impl.domain.variable;

import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.BASIC;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.CASCADING_UPDATE;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.CUSTOM_LISTENER;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.DECLARATIVE;
import static ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy.DISABLED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultShadowVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.custom.CustomShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ChangedVariableNotifier;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSessionFactory;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableReferenceGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableReferenceGraphBuilder;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType;
import ai.timefold.solver.core.impl.domain.variable.listener.support.VariableListenerSupport;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;

/**
 * Utility class for updating shadow variables at entity level.
 */
public final class ShadowVariableUpdateHelper<Solution_> {

    private static final EnumSet<ShadowVariableType> SUPPORTED_TYPES =
            EnumSet.of(BASIC, CUSTOM_LISTENER, CASCADING_UPDATE, DECLARATIVE);

    public static <Solution_> ShadowVariableUpdateHelper<Solution_> create() {
        return new ShadowVariableUpdateHelper<>(SUPPORTED_TYPES);
    }

    // Testing purposes
    static <Solution_> ShadowVariableUpdateHelper<Solution_> create(ShadowVariableType... supportedTypes) {
        var typesSet = EnumSet.noneOf(ShadowVariableType.class);
        typesSet.addAll(Arrays.asList(supportedTypes));
        return new ShadowVariableUpdateHelper<>(typesSet);
    }

    private final EnumSet<ShadowVariableType> supportedShadowVariableTypes;

    private ShadowVariableUpdateHelper(EnumSet<ShadowVariableType> supportedShadowVariableTypes) {
        this.supportedShadowVariableTypes = supportedShadowVariableTypes;
    }

    @SuppressWarnings("unchecked")
    public void updateShadowVariables(Solution_ solution) {
        var enabledPreviewFeatures = EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES);
        var solutionClass = (Class<Solution_>) solution.getClass();
        var initialSolutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(
                enabledPreviewFeatures, solutionClass);
        var entityClassArray = initialSolutionDescriptor.getAllEntitiesAndProblemFacts(solution)
                .stream()
                .map(Object::getClass)
                .distinct()
                .toArray(Class[]::new);
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(enabledPreviewFeatures, solutionClass,
                entityClassArray);
        try (var scoreDirector = new InternalScoreDirector<>(solutionDescriptor)) {
            // When we have a solution, we can reuse the logic from VariableListenerSupport to update all variable types
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.forceTriggerVariableListeners();
        }
    }

    public void updateShadowVariables(Class<Solution_> solutionClass,
            Object... entities) {
        var entityClassList = Arrays.stream(entities).map(Object::getClass)
                .filter(clazz -> clazz.isAnnotationPresent(PlanningEntity.class))
                .distinct().toList();
        var solutionDescriptor =
                SolutionDescriptor.buildSolutionDescriptor(EnumSet.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES),
                        solutionClass,
                        entityClassList.toArray(new Class<?>[0]));
        var customShadowVariableDescriptorList = solutionDescriptor.getAllShadowVariableDescriptors().stream()
                .filter(CustomShadowVariableDescriptor.class::isInstance)
                .toList();
        if (!customShadowVariableDescriptorList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Custom shadow variable descriptors are not supported (%s)".formatted(customShadowVariableDescriptorList));
        }
        var variableListenerSupport = VariableListenerSupport.create(new InternalScoreDirector<>(solutionDescriptor));
        var missingShadowVariableTypeList = variableListenerSupport.getSupportedShadowVariableTypes().stream()
                .filter(type -> !supportedShadowVariableTypes.contains(type))
                .toList();
        if (!missingShadowVariableTypeList.isEmpty()) {
            throw new IllegalStateException(
                    "Impossible state: The following shadow variable types are not currently supported (%s)."
                            .formatted(missingShadowVariableTypeList));
        }
        // No solution, we trigger all supported events manually
        var session = InternalShadowVariableSession.build(solutionDescriptor,
                new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.empty()), entities);
        // Update all built-in shadow variables
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor == null) {
            session.processBasicVariable(entities);
            session.processChainedVariable(entities);
        } else {
            session.processListVariable(entities);
            session.processCascadingVariable(entities);
        }
        session.processDeclarativeVariables();
    }

    private record InternalShadowVariableSession<Solution_>(SolutionDescriptor<Solution_> solutionDescriptor,
            VariableReferenceGraph graph) {

        public static <Solution_> InternalShadowVariableSession<Solution_> build(
                SolutionDescriptor<Solution_> solutionDescriptor, VariableReferenceGraphBuilder<Solution_> graph,
                Object... entities) {
            return new InternalShadowVariableSession<>(solutionDescriptor,
                    DefaultShadowVariableSessionFactory.buildGraph(solutionDescriptor, graph, entities,
                            DefaultTopologicalOrderGraph::new));
        }

        /**
         * Identify and auto-update {@link InverseRelationShadowVariable inverse shadow variables} of shadow entities.
         * 
         * @param entities the entities to be analyzed
         */
        public void processBasicVariable(Object... entities) {
            var shadowEntityToUpdate = new IdentityHashMap<Object, ShadowEntityVariable>();
            for (var entityWithDescriptor : fetchEntityAndDescriptors(entities)) {
                // Iterate over all basic variables and update the inverse relation field
                for (var variableDescriptor : fetchBasicDescriptors(entityWithDescriptor.entityDescriptor(), false)) {
                    var shadowEntity = variableDescriptor.getValue(entityWithDescriptor.entity());
                    addShadowEntity(entityWithDescriptor, variableDescriptor, shadowEntity, shadowEntityToUpdate);
                }
            }
            shadowEntityToUpdate.forEach((key, value) -> updateShadowVariable(value.variableName(), key, value.values()));
        }

        private void addShadowEntity(EntityWithDescriptor<Solution_> entityWithDescriptor,
                BasicVariableDescriptor<Solution_> variableDescriptor, Object shadowEntity,
                Map<Object, ShadowEntityVariable> shadowEntityToUpdate) {
            // If the planning value is set, we update the inverse element collection
            var descriptor = findShadowVariableDescriptor(shadowEntity.getClass(),
                    InverseRelationShadowVariableDescriptor.class);
            if (descriptor != null) {
                var values = (Collection<Object>) descriptor.getValue(shadowEntity);
                if (values == null) {
                    throw new IllegalStateException(
                            "The entity (%s) has a variable (%s) with value (%s) which has a sourceVariableName variable (%s) which is null."
                                    .formatted(entityWithDescriptor.entity().getClass(),
                                            variableDescriptor.getVariableName(),
                                            shadowEntity, descriptor.getVariableName()));
                }
                if (!values.contains(entityWithDescriptor.entity())) {
                    values.add(entityWithDescriptor.entity());
                }
                shadowEntityToUpdate.putIfAbsent(shadowEntity,
                        new ShadowEntityVariable(descriptor.getVariableName(), values));
            }
        }

        /**
         * Identify and auto-update {@link InverseRelationShadowVariable inverse shadow variables} of shadow entities for
         * chained models.
         *
         * @param entities the entities to be analyzed
         */
        public void processChainedVariable(Object... entities) {
            for (var entityWithDescriptor : fetchEntityAndDescriptors(entities)) {
                // We filter all planning entities and update the inverse shadow variable
                for (var variableDescriptor : fetchBasicDescriptors(entityWithDescriptor.entityDescriptor(), true)) {
                    var shadowEntity = variableDescriptor.getValue(entityWithDescriptor.entity());
                    if (shadowEntity != null) {
                        // If the planning value is set, we update the inverse element
                        updateShadowVariable(shadowEntity.getClass(), InverseRelationShadowVariableDescriptor.class,
                                shadowEntity, entityWithDescriptor.entity());
                    }
                }
            }
        }

        /**
         * Identify and auto-update the following shadow variables of shadow entities:
         * {@link InverseRelationShadowVariable }, {@link PreviousElementShadowVariable}, {@link NextElementShadowVariable},
         * and {@link IndexShadowVariable}.
         * 
         * @param entities the entities to be analyzed
         */
        public void processListVariable(Object... entities) {
            var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
            // We filter all planning entities and update the shadow variables of their planning values. 
            // There is no need to evaluate other variables from the entity, 
            // as we fail fast when variables based on listeners are detected.
            var planningEntityList = Arrays.stream(entities)
                    .filter(entity -> listVariableDescriptor.getEntityDescriptor().getEntityClass()
                            .equals(entity.getClass()))
                    .toList();
            for (var entity : planningEntityList) {
                var values = listVariableDescriptor.getValue(entity);
                if (values.isEmpty()) {
                    continue;
                }
                var entityType = values.get(0).getClass();
                for (var i = 0; i < values.size(); i++) {
                    var shadowEntity = values.get(i);
                    // Inverse relation
                    updateShadowVariable(entityType, InverseRelationShadowVariableDescriptor.class, shadowEntity,
                            entity);
                    // Previous element
                    var previousElement = i > 0 ? values.get(i - 1) : null;
                    updateShadowVariable(entityType, PreviousElementShadowVariableDescriptor.class, shadowEntity,
                            previousElement);
                    // Next element
                    var nextElement = i < values.size() - 1 ? values.get(i + 1) : null;
                    updateShadowVariable(entityType, NextElementShadowVariableDescriptor.class, shadowEntity,
                            nextElement);
                    // Index
                    updateShadowVariable(entityType, IndexShadowVariableDescriptor.class, shadowEntity, i);
                }
            }
        }

        /**
         * Identify and auto-update {@link CascadingUpdateShadowVariable shadow
         * variables} of entities.
         *
         * @param entities the entities to be analyzed
         */
        @SuppressWarnings("unchecked")
        public void processCascadingVariable(Object... entities) {
            var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
            if (listVariableDescriptor != null) {
                for (var entity : entities) {
                    var cascadingVariableDescriptor =
                            findShadowVariableDescriptor(entity.getClass(), CascadingUpdateShadowVariableDescriptor.class);
                    if (cascadingVariableDescriptor != null) {
                        cascadingVariableDescriptor.update(new InternalScoreDirector<>(solutionDescriptor), entity);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private <T> T findShadowVariableDescriptor(Class<?> entityType, Class<T> descriptorType) {
            var valueMetaModel = solutionDescriptor.getMetaModel().entities().stream()
                    .filter(type -> type.type().equals(entityType))
                    .findFirst()
                    .orElse(null);
            if (valueMetaModel == null) {
                return null;
            }
            return (T) valueMetaModel.variables().stream()
                    .filter(ShadowVariableMetaModel.class::isInstance)
                    .map(DefaultShadowVariableMetaModel.class::cast)
                    .map(DefaultShadowVariableMetaModel::variableDescriptor)
                    .filter(descriptorType::isInstance)
                    .findFirst()
                    .orElse(null);
        }

        private void updateShadowVariable(String variableName, Object destination, Object value) {
            var variableDescriptor =
                    solutionDescriptor.getEntityDescriptorStrict(destination.getClass()).getVariableDescriptor(variableName);
            if (solutionDescriptor.getDeclarativeShadowVariableDescriptors().isEmpty() && variableDescriptor != null) {
                variableDescriptor.setValue(destination, value);
            } else if (variableDescriptor != null) {
                var variableMetamodel = solutionDescriptor.getMetaModel().entity(destination.getClass()).variable(variableName);
                graph.beforeVariableChanged(variableMetamodel, destination);
                variableDescriptor.setValue(destination, value);
                graph.afterVariableChanged(variableMetamodel, destination);
            }
        }

        @SuppressWarnings("rawtypes")
        private void updateShadowVariable(Class<?> entityType,
                Class<? extends ShadowVariableDescriptor> descriptorType, Object destination, Object value) {
            var descriptor =
                    findShadowVariableDescriptor(entityType, descriptorType);
            if (descriptor != null) {
                updateShadowVariable(descriptor.getVariableName(), destination, value);
            }
        }

        public void processDeclarativeVariables() {
            if (!solutionDescriptor.getDeclarativeShadowVariableDescriptors().isEmpty()) {
                graph.updateChanged();
            }
        }

        private List<EntityWithDescriptor<Solution_>> fetchEntityAndDescriptors(Object... entities) {
            var descriptorList = new ArrayList<EntityWithDescriptor<Solution_>>();
            var genuineEntityDescriptorCollection = solutionDescriptor.getGenuineEntityDescriptors();
            for (var entity : entities) {
                genuineEntityDescriptorCollection.stream()
                        .filter(variableDescriptor -> variableDescriptor.getEntityClass().equals(entity.getClass()))
                        .findFirst()
                        .ifPresent(genuineEntityDescriptor -> descriptorList
                                .add(new EntityWithDescriptor<>(entity, genuineEntityDescriptor)));
            }
            return descriptorList;
        }

        private List<BasicVariableDescriptor<Solution_>> fetchBasicDescriptors(EntityDescriptor<Solution_> entityDescriptor,
                boolean chained) {
            var descriptorList = new ArrayList<BasicVariableDescriptor<Solution_>>();
            for (var descriptor : entityDescriptor.getDeclaredGenuineVariableDescriptors()) {
                if (descriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                        && ((!chained && !basicVariableDescriptor.isChained())
                                || (chained && basicVariableDescriptor.isChained()))) {
                    descriptorList.add(basicVariableDescriptor);
                }
            }
            return descriptorList;
        }
    }

    private static class InternalScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
            extends AbstractScoreDirectorFactory<Solution_, Score_, InternalScoreDirectorFactory<Solution_, Score_>> {

        public InternalScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
            super(solutionDescriptor);
        }

        @Override
        public AbstractScoreDirector.AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder() {
            throw new UnsupportedOperationException();
        }
    }

    private static class InternalScoreDirector<Solution_, Score_ extends Score<Score_>>
            extends AbstractScoreDirector<Solution_, Score_, InternalScoreDirectorFactory<Solution_, Score_>> {

        public InternalScoreDirector(SolutionDescriptor<Solution_> solutionDescriptor) {
            super(new InternalScoreDirectorFactory<>(solutionDescriptor), false, DISABLED, false);
        }

        @Override
        public void setWorkingSolution(Solution_ workingSolution) {
            super.setWorkingSolution(workingSolution, ignore -> {
            });
        }

        @Override
        public InnerScore<Score_> calculateScore() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Object, Indictment<Score_>> getIndictmentMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean requiresFlushing() {
            throw new UnsupportedOperationException();
        }
    }

    private record ShadowEntityVariable(String variableName, Collection<Object> values) {
    }

    private record EntityWithDescriptor<Solution_>(Object entity, EntityDescriptor<Solution_> entityDescriptor) {
    }
}
