package ai.timefold.solver.core.impl.score.director;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariablesInconsistent;

final class EntityValidator<Solution_, Score_ extends Score<Score_>> implements Consumer<Object> {
    final InnerScoreDirector<Solution_, Score_> scoreDirector;
    final SolutionDescriptor<Solution_> solutionDescriptor;
    final ListVariableDescriptor<Solution_> listVariableDescriptor;
    final Map<Class<?>, InverseRelationShadowVariableDescriptor<Solution_>[]> entityClassToInverseDescriptors;
    final Map<Object, Set<Object>> valueToInverse;

    public EntityValidator(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        this.scoreDirector = scoreDirector;
        this.solutionDescriptor = scoreDirector.getSolutionDescriptor();
        this.listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        this.entityClassToInverseDescriptors =
                CollectionUtils.newLinkedHashMap(solutionDescriptor.getEntityClassSet().size());
        this.valueToInverse = CollectionUtils.newIdentityHashMap(solutionDescriptor.getGenuineEntityCount(
                scoreDirector.getWorkingSolution()));

        if (scoreDirector.expectShadowVariablesInCorrectState()) {
            Map<Class<?>, VariableDescriptor<Solution_>[]> entityToInverseSourceDescriptors =
                    CollectionUtils.newLinkedHashMap(solutionDescriptor.getEntityClassSet().size());
            final int DEFAULT_INVERSE_SET_SIZE = 16;
            solutionDescriptor.visitAllEntities(scoreDirector.getWorkingSolution(),
                    entity -> {
                        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
                        @SuppressWarnings("unchecked")
                        var inverseSourceDescriptors = entityToInverseSourceDescriptors
                                .computeIfAbsent(entityDescriptor.getEntityClass(), ignored -> {
                                    var out = new ArrayList<VariableDescriptor<Solution_>>();
                                    for (var shadow : solutionDescriptor.getAllShadowVariableDescriptors()) {
                                        if (shadow instanceof InverseRelationShadowVariableDescriptor<Solution_> inverseDescriptor) {
                                            var sourceDescriptor =
                                                    inverseDescriptor.getSourceVariableDescriptorList().get(0);
                                            if (!sourceDescriptor.isListVariable()
                                                    && sourceDescriptor.getEntityDescriptor() == entityDescriptor) {
                                                out.add(sourceDescriptor);
                                            }
                                        }
                                    }
                                    return out.toArray(VariableDescriptor[]::new);
                                });
                        for (var sourceDescriptor : inverseSourceDescriptors) {
                            var value = sourceDescriptor.getValue(entity);
                            valueToInverse
                                    .computeIfAbsent(value,
                                            ignored -> CollectionUtils.newIdentityHashSet(DEFAULT_INVERSE_SET_SIZE))
                                    .add(entity);
                        }
                    });
        }
    }

    @Override
    public void accept(Object entity) {
        var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());

        if (scoreDirector.expectShadowVariablesInCorrectState()) {
            var inconsistentDescriptor = entityDescriptor.getShadowVariablesInconsistentDescriptor();
            if (inconsistentDescriptor != null) {
                if (scoreDirector.expectShadowVariablesInCorrectState()
                        && inconsistentDescriptor.getValue(entity) == null) {
                    throw new IllegalStateException(
                            """
                                    Shadow variables update is disabled, but the entity (%s) has a null @%s annotated field (%s).
                                    Maybe enable shadow variable updates?
                                    """
                                    .formatted(entity, ShadowVariablesInconsistent.class.getSimpleName(),
                                            inconsistentDescriptor.getVariableName()));
                }
            }

            @SuppressWarnings("unchecked")
            var inverseRelationDescriptors =
                    entityClassToInverseDescriptors.computeIfAbsent(entityDescriptor.getEntityClass(),
                            ignored -> entityDescriptor.getShadowVariableDescriptors()
                                    .stream()
                                    .filter(descriptor -> descriptor instanceof InverseRelationShadowVariableDescriptor)
                                    .toArray(InverseRelationShadowVariableDescriptor[]::new));
            for (var inverseRelationDescriptor : inverseRelationDescriptors) {
                assertInverseRelationConsistent(scoreDirector, inverseRelationDescriptor, entity);
            }
        }

        if (listVariableDescriptor == null) { // Only basic variables.
            if (entityDescriptor.isMovable(scoreDirector.getWorkingSolution(), entity)) {
                return;
            }
            for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
                var basicVariableDescriptor = (BasicVariableDescriptor<Solution_>) variableDescriptor;
                if (basicVariableDescriptor.allowsUnassigned()) {
                    continue;
                }
                var value = basicVariableDescriptor.getValue(entity);
                if (value == null) {
                    throw new IllegalStateException(
                            "The entity (%s) has a variable (%s) pinned to null, even though unassigned values are not allowed."
                                    .formatted(entity, basicVariableDescriptor.getVariableName()));
                }
            }
        }
    }

    void assertInverseRelationConsistent(InnerScoreDirector<Solution_, Score_> scoreDirector,
            InverseRelationShadowVariableDescriptor<Solution_> inverseRelationShadowVariableDescriptor,
            Object entity) {
        var sourceDescriptor = inverseRelationShadowVariableDescriptor.getSourceVariableDescriptorList().get(0);
        if (sourceDescriptor == listVariableDescriptor) {
            // skip, since ExternalizedListInverseVariableProcessor asserts this in setInverseAsserted
            return;
        }

        if (inverseRelationShadowVariableDescriptor.isSingleton()) {
            var inverse = inverseRelationShadowVariableDescriptor.getValue(entity);
            var actualSet = valueToInverse.getOrDefault(entity, Set.of());
            if (!actualSet.isEmpty() && actualSet.size() != 1) {
                throw new IllegalStateException(
                        """
                                Impossible state: The entity (%s) has a singleton @%s has multiple source entities (%s).
                                Verify the consistency of your input solution.
                                """
                                .formatted(entity, InverseRelationShadowVariable.class.getSimpleName(), actualSet));
            }
            if (actualSet.isEmpty() && inverse != null) {
                throw new IllegalStateException(
                        """
                                The entity (%s) has a @%s that points to a source entity (%s) but it has no source entities.
                                Verify the consistency of your input solution.
                                """
                                .formatted(entity, InverseRelationShadowVariable.class.getSimpleName(), inverse));
            }
            if (!actualSet.isEmpty()) {
                var actual = actualSet.iterator().next();
                if (actual != inverse) {
                    throw new IllegalStateException(
                            """
                                    The entity (%s) has a singleton @%s that points to a source entity (%s) whereas its actual source entity is (%s).
                                    Verify the consistency of your input solution.
                                    """
                                    .formatted(entity, InverseRelationShadowVariable.class.getSimpleName(), inverse,
                                            actual));
                }
            }
        } else {
            @SuppressWarnings("unchecked")
            var inverseCollection = (Collection<Object>) inverseRelationShadowVariableDescriptor.getValue(entity);
            if (inverseCollection == null) {
                throw new IllegalStateException("""
                        The entity (%s) has a collection @%s that is null.
                        Verify the consistency of your input solution.
                        """.formatted(entity, InverseRelationShadowVariable.class.getSimpleName()));
            }
            var actualCollection = valueToInverse.getOrDefault(entity, Collections.emptySet());
            if (!inverseCollection.containsAll(actualCollection) || !actualCollection.containsAll(inverseCollection)) {
                throw new IllegalStateException(
                        """
                                The entity (%s) has a collection @%s (%s) that does not match the actual inverse set (%s).
                                Verify the consistency of your input solution.
                                """
                                .formatted(entity, InverseRelationShadowVariable.class.getSimpleName(), inverseCollection,
                                        actualCollection.stream().map(Object::toString).sorted()
                                                .collect(Collectors.joining(", ", "[", "]"))));
            }
        }
    }
}
