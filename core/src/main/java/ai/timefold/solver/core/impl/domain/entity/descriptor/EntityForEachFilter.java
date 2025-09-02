package ai.timefold.solver.core.impl.domain.entity.descriptor;

import java.util.Collection;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariablesInconsistent;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class EntityForEachFilter {
    private final Predicate<Object> assignedAndConsistentPredicate;

    // null when there are no declarative shadow variables,
    // since a for each node with a null filter is treated as an always true filter
    @Nullable
    private final Predicate<Object> consistentPredicate;

    EntityForEachFilter(EntityDescriptor<?> entityDescriptor) {
        var solutionDescriptor = entityDescriptor.getSolutionDescriptor();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        var declarativeShadowVariables = getDeclarativeShadowVariables(entityDescriptor);
        var shadowVariableInconsistentDescriptor = entityDescriptor.getShadowVariablesInconsistentDescriptor();

        if (!declarativeShadowVariables.isEmpty() && shadowVariableInconsistentDescriptor == null) {
            // TODO: Remove this when supplier is present
            throw new IllegalStateException(
                    "Entity class %s has declarative shadow variables (%s) but does not have a @%s member."
                            .formatted(entityDescriptor.getEntityClass().getCanonicalName(),
                                    declarativeShadowVariables.stream().map(VariableDescriptor::getVariableName).toList(),
                                    ShadowVariablesInconsistent.class.getSimpleName()));
        }

        var assignedPredicate = getAssignedPredicate(entityDescriptor, listVariableDescriptor);
        if (declarativeShadowVariables.isEmpty()) {
            assignedAndConsistentPredicate = assignedPredicate;
            consistentPredicate = null; // everything is always consistent
        } else {
            consistentPredicate = entity -> Boolean.FALSE.equals(shadowVariableInconsistentDescriptor.getValue(entity));
            assignedAndConsistentPredicate = assignedPredicate.and(consistentPredicate);
        }
    }

    public Predicate<Object> getAssignedAndConsistentPredicate() {
        return assignedAndConsistentPredicate;
    }

    @Nullable
    public Predicate<Object> getConsistentPredicate() {
        return consistentPredicate;
    }

    private static Collection<? extends ShadowVariableDescriptor>
            getDeclarativeShadowVariables(EntityDescriptor<?> entityDescriptor) {
        return entityDescriptor.getShadowVariableDescriptors()
                .stream()
                .filter(shadowVariableDescriptor -> shadowVariableDescriptor instanceof DeclarativeShadowVariableDescriptor<?>)
                .toList();
    }

    private static Predicate<Object> getAssignedPredicate(EntityDescriptor<?> entityDescriptor,
            ListVariableDescriptor<?> listVariableDescriptor) {
        var isListVariableValue =
                listVariableDescriptor != null && listVariableDescriptor.acceptsValueType(entityDescriptor.getEntityClass());
        if (isListVariableValue) {
            var listInverseVariable = listVariableDescriptor.getInverseRelationShadowVariableDescriptor();
            if (listInverseVariable != null) {
                return entity -> entityDescriptor.hasNoNullVariables(entity) && listInverseVariable.getValue(entity) != null;
            } else {
                return ignored -> {
                    throw new IllegalStateException("""
                            Impossible state: assigned predicate for list variable value should not be used
                            when there no inverse relation shadow variable descriptor
                            """);
                };
            }
        } else {
            return entityDescriptor::hasNoNullVariables;
        }
    }
}
