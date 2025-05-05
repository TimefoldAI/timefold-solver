package ai.timefold.solver.core.impl.domain.variable;

import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.BASIC;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.CASCADING_UPDATE;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.CUSTOM_LISTENER;
import static ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType.DECLARATIVE;
import static ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy.DISABLED;

import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultShadowVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.custom.CustomShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ChangedVariableNotifier;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultShadowVariableSessionFactory;
import ai.timefold.solver.core.impl.domain.variable.declarative.DefaultTopologicalOrderGraph;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableReferenceGraph;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.support.ShadowVariableType;
import ai.timefold.solver.core.impl.domain.variable.listener.support.VariableListenerSupport;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;

import org.jspecify.annotations.Nullable;

/**
 * Utility class for updating shadow variables at entity level.
 */
public final class ShadowVariableUpdateHelper {

    public static ShadowVariableUpdateHelper create() {
        return new ShadowVariableUpdateHelper(List.of(BASIC, CUSTOM_LISTENER, CASCADING_UPDATE, DECLARATIVE));
    }

    // Testing purposes
    @SuppressWarnings("ProtectedMemberInFinalClass")
    protected static ShadowVariableUpdateHelper create(ShadowVariableType... supportedTypes) {
        return new ShadowVariableUpdateHelper(Arrays.asList(supportedTypes));
    }

    private final List<ShadowVariableType> supportedShadowVariableTypes;

    private ShadowVariableUpdateHelper(List<ShadowVariableType> supportedShadowVariableTypes) {
        this.supportedShadowVariableTypes = supportedShadowVariableTypes;
    }

    public <Solution_> void updateShadowVariables(Class<Solution_> solutionClass,
            Object... entities) {
        var entityClassList = Arrays.stream(entities).map(Object::getClass)
                .filter(clazz -> clazz.isAnnotationPresent(PlanningEntity.class))
                .distinct().toList();
        var solutionDescriptor =
                SolutionDescriptor.buildSolutionDescriptor(Set.of(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES), solutionClass,
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
                    "The following shadow variable types are not currently supported (%s)."
                            .formatted(missingShadowVariableTypeList));
        }
        var session = new InternalShadowVariableSession<>(solutionDescriptor,
                new VariableReferenceGraph<>(ChangedVariableNotifier.empty()));
        session.init(entities);
        // Update all built-in shadow variables
        session.processBasicVariable(entities);
        session.processChainedVariable(entities);
        session.processListVariable(entities);
        session.processCascadingVariable(entities);
        session.triggerUpdateShadowVariables();
    }

    private record InternalShadowVariableSession<Solution_>(SolutionDescriptor<Solution_> solutionDescriptor,
            VariableReferenceGraph<Solution_> graph) {

        public void init(Object... entities) {
            if (!solutionDescriptor.getDeclarativeShadowVariableDescriptors().isEmpty()) {
                DefaultShadowVariableSessionFactory.visitGraph(solutionDescriptor, graph, entities,
                        DefaultTopologicalOrderGraph::new);
            }
        }

        /**
         * Identify and auto-update {@link InverseRelationShadowVariable inverse shadow variables} of shadow entities.
         * 
         * @param entities the entities to be analyzed
         */
        public void processBasicVariable(Object... entities) {
            var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
            if (listVariableDescriptor == null) {
                var genuineEntityDescriptorCollection = solutionDescriptor.getGenuineEntityDescriptors();
                var shadowEntityToUpdate =
                        new IdentityHashMap<Object, Pair<String, Collection<Object>>>();
                for (var entity : entities) {
                    // We filter all planning entities and update the inverse shadow variable collection
                    var genuineEntityDescriptor = genuineEntityDescriptorCollection.stream()
                            .filter(variableDescriptor -> variableDescriptor.getEntityClass().equals(entity.getClass()))
                            .findFirst()
                            .orElse(null);
                    if (genuineEntityDescriptor != null) {
                        // Iterate over all basic variables and update the inverse relation field
                        for (var variableDescriptor : genuineEntityDescriptor.getDeclaredGenuineVariableDescriptors()) {
                            if (variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                                    && !basicVariableDescriptor.isChained()) {
                                var value = basicVariableDescriptor.getValue(entity);
                                if (value != null) {
                                    // If the planning value is set, we update the inverse element collection
                                    var descriptor = findShadowVariableDescriptor(value.getClass(),
                                            InverseRelationShadowVariableDescriptor.class);
                                    if (descriptor != null) {
                                        var values = (Collection<Object>) descriptor.getValue(value);
                                        if (values == null) {
                                            throw new IllegalStateException(
                                                    "The entity (%s) has a variable (%s) with value (%s) which has a sourceVariableName variable (%s) which is null."
                                                            .formatted(entity.getClass(),
                                                                    basicVariableDescriptor.getVariableName(),
                                                                    value, descriptor.getVariableName()));
                                        }
                                        if (!values.contains(entity)) {
                                            values.add(entity);
                                        }
                                        shadowEntityToUpdate.putIfAbsent(value,
                                                new Pair<>(descriptor.getVariableName(), values));
                                    }
                                }
                            }
                        }
                    }
                }
                shadowEntityToUpdate.forEach((key, value) -> updateShadowVariable(key, value.key(), value.value()));
            }
        }

        /**
         * Identify and auto-update {@link InverseRelationShadowVariable inverse shadow variables} of shadow entities for
         * chained models.
         *
         * @param entities the entities to be analyzed
         */
        public void processChainedVariable(Object... entities) {
            var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
            if (listVariableDescriptor == null) {
                var genuineEntityDescriptorCollection = solutionDescriptor.getGenuineEntityDescriptors();
                for (var entity : entities) {
                    // We filter all planning entities and update the inverse shadow variable
                    var genuineEntityDescriptor = genuineEntityDescriptorCollection.stream()
                            .filter(variableDescriptor -> variableDescriptor.getEntityClass().equals(entity.getClass()))
                            .findFirst()
                            .orElse(null);
                    if (genuineEntityDescriptor != null) {
                        for (var variableDescriptor : genuineEntityDescriptor.getDeclaredGenuineVariableDescriptors()) {
                            if (variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                                    && basicVariableDescriptor.isChained()) {
                                var value = variableDescriptor.getValue(entity);
                                if (value != null) {
                                    // If the planning value is set, we update the inverse element
                                    var descriptor = findShadowVariableDescriptor(value.getClass(),
                                            InverseRelationShadowVariableDescriptor.class);
                                    if (descriptor != null) {
                                        updateShadowVariable(value, descriptor.getVariableName(), entity);
                                    }
                                }
                            }
                        }
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
            if (listVariableDescriptor != null) {
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
                        var value = values.get(i);
                        // Inverse relation
                        var descriptor =
                                findShadowVariableDescriptor(entityType, InverseRelationShadowVariableDescriptor.class);
                        if (descriptor != null) {
                            updateShadowVariable(value, descriptor.getVariableName(), entity);
                        }
                        // Previous element
                        var previousElementDescriptor =
                                findShadowVariableDescriptor(entityType, PreviousElementShadowVariableDescriptor.class);
                        if (previousElementDescriptor != null) {
                            var previousElement = i > 0 ? values.get(i - 1) : null;
                            updateShadowVariable(value, previousElementDescriptor.getVariableName(), previousElement);
                        }
                        // Next element
                        var nextElementDescriptor =
                                findShadowVariableDescriptor(entityType, NextElementShadowVariableDescriptor.class);
                        if (nextElementDescriptor != null) {
                            var nextElement = i < values.size() - 1 ? values.get(i + 1) : null;
                            updateShadowVariable(value, nextElementDescriptor.getVariableName(), nextElement);
                        }
                        // Index
                        var indexDescriptor =
                                findShadowVariableDescriptor(entityType, IndexShadowVariableDescriptor.class);
                        if (indexDescriptor != null) {
                            updateShadowVariable(value, indexDescriptor.getVariableName(), i);
                        }
                    }
                }
            }
        }

        /**
         * Identify and auto-update {@link ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable shadow
         * variables} of entities.
         *
         * @param entities the entities to be analyzed
         */
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
            var valueMetaModel = solutionDescriptor.getMetaModel().entity(entityType);
            return (T) valueMetaModel.variables().stream()
                    .filter(ShadowVariableMetaModel.class::isInstance)
                    .map(DefaultShadowVariableMetaModel.class::cast)
                    .map(DefaultShadowVariableMetaModel::variableDescriptor)
                    .filter(descriptorType::isInstance)
                    .findFirst()
                    .orElse(null);
        }

        private void updateShadowVariable(Object destination, String variableName, @Nullable Object value) {
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

        public void triggerUpdateShadowVariables() {
            if (!solutionDescriptor.getDeclarativeShadowVariableDescriptors().isEmpty()) {
                graph.updateChanged();
            }
        }
    }

    private static class InternalScoreDirectorFactory<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_>>
            extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_> {

        public InternalScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
            super(solutionDescriptor);
        }

        @Override
        public AbstractScoreDirector.AbstractScoreDirectorBuilder<Solution_, Score_, ?, ?> createScoreDirectorBuilder() {
            throw new UnsupportedOperationException();
        }
    }

    private static class InternalScoreDirector<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_, Factory_>>
            extends AbstractScoreDirector<Solution_, Score_, Factory_> {

        public InternalScoreDirector(SolutionDescriptor<Solution_> solutionDescriptor) {
            super((Factory_) new InternalScoreDirectorFactory<Solution_, Score_, Factory_>(solutionDescriptor), false, DISABLED,
                    false);
        }

        @Override
        public void setWorkingSolution(Solution_ workingSolution) {
            throw new UnsupportedOperationException();
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
}
