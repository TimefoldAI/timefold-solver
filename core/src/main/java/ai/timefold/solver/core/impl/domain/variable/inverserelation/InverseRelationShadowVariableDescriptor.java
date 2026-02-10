package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class InverseRelationShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private VariableDescriptor<Solution_> sourceVariableDescriptor;
    private boolean singleton;

    public InverseRelationShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        linkShadowSources(descriptorPolicy);
    }

    /**
     * Sourced on a basic variable, the shadow type is a Collection (such as List or Set).
     * Sourced on a list variable, the shadow variable type is a single instance.
     *
     * @param descriptorPolicy descriptor policy
     */
    private void linkShadowSources(DescriptorPolicy descriptorPolicy) {
        InverseRelationShadowVariable shadowVariableAnnotation = variableMemberAccessor
                .getAnnotation(InverseRelationShadowVariable.class);
        Class<?> variablePropertyType = getVariablePropertyType();
        Class<?> sourceClass;
        if (Collection.class.isAssignableFrom(variablePropertyType)) {
            Type genericType = variableMemberAccessor.getGenericType();
            sourceClass = ConfigUtils
                    .extractGenericTypeParameter("entityClass", entityDescriptor.getEntityClass(), variablePropertyType,
                            genericType, InverseRelationShadowVariable.class, variableMemberAccessor.getName())
                    .orElse(Object.class);
            singleton = false;
        } else {
            sourceClass = variablePropertyType;
            singleton = true;
        }
        EntityDescriptor<Solution_> sourceEntityDescriptor = getEntityDescriptor().getSolutionDescriptor()
                .findEntityDescriptor(sourceClass);
        if (sourceEntityDescriptor == null) {
            throw new IllegalArgumentException("""
                    The entityClass (%s) has an @%s-annotated property (%s) \
                    with a sourceClass (%s) which is not a valid planning entity.
                    Maybe check the annotations of the class (%s).
                    Maybe add the class (%s) among planning entities in the solver configuration."""
                    .formatted(entityDescriptor.getEntityClass(), InverseRelationShadowVariable.class.getSimpleName(),
                            variableMemberAccessor.getName(), sourceClass, sourceClass, sourceClass));
        }
        String sourceVariableName = shadowVariableAnnotation.sourceVariableName();
        // TODO can we getGenuineVariableDescriptor()?
        sourceVariableDescriptor = sourceEntityDescriptor.getVariableDescriptor(sourceVariableName);
        if (sourceVariableDescriptor == null) {
            throw new IllegalStateException("""
                    The entityClass (%s) has an @%s-annotated property (%s) \
                    with sourceVariableName (%s) which is not a valid planning variable on entityClass (%s).
                    %s"""
                    .formatted(entityDescriptor.getEntityClass(), InverseRelationShadowVariable.class.getSimpleName(),
                            variableMemberAccessor.getName(), sourceVariableName, sourceEntityDescriptor.getEntityClass(),
                            sourceEntityDescriptor.buildInvalidVariableNameExceptionMessage(sourceVariableName)));
        }
        boolean list = sourceVariableDescriptor.isListVariable();
        if (singleton) {
            if (!list) {
                throw new IllegalArgumentException("""
                        The entityClass (%s) has an @%s-annotated property (%s) \
                        which does not return a %s with sourceVariableName (%s) which is not a list variable @%s.
                        Only list variable supports a singleton inverse."""
                        .formatted(entityDescriptor.getEntityClass(), InverseRelationShadowVariable.class.getSimpleName(),
                                variableMemberAccessor.getName(), Collection.class.getSimpleName(), sourceVariableName,
                                PlanningListVariable.class.getSimpleName()));
            }
        } else {
            if (list) {
                throw new IllegalArgumentException("""
                        The entityClass (%s) has an @%s-annotated property (%s) \
                        which returns a %s with sourceVariableName (%s) which is a list variable @%s.
                        A list variable supports only a singleton inverse."""
                        .formatted(entityDescriptor.getEntityClass(), InverseRelationShadowVariable.class.getSimpleName(),
                                variableMemberAccessor.getName(), Collection.class.getSimpleName(), sourceVariableName,
                                PlanningListVariable.class.getSimpleName()));
            }
        }
        sourceVariableDescriptor.registerSinkVariableDescriptor(this);
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.singletonList(sourceVariableDescriptor);
    }

    @Override
    public Collection<Class<?>> getVariableListenerClasses() {
        if (singleton) {
            throw new UnsupportedOperationException("Impossible state: Handled by %s."
                    .formatted(ListVariableStateSupply.class.getSimpleName()));
        } else {
            return Collections.singleton(CollectionInverseVariableListener.class);
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Demand<?> getProvidedDemand() {
        if (singleton) {
            throw new UnsupportedOperationException("Impossible state: Handled by %s."
                    .formatted(ListVariableStateSupply.class.getSimpleName()));
        } else {
            return new CollectionInverseVariableDemand<>(sourceVariableDescriptor);
        }
    }

    @Override
    public Iterable<VariableListenerWithSources> buildVariableListeners(SupplyManager supplyManager) {
        return new VariableListenerWithSources<>(buildVariableListener(), sourceVariableDescriptor).toCollection();
    }

    private InnerVariableListener<Solution_, BasicVariableChangeEvent<Object>> buildVariableListener() {
        if (singleton) {
            throw new UnsupportedOperationException("Impossible state: Handled by %s."
                    .formatted(ListVariableStateSupply.class.getSimpleName()));
        } else {
            return new CollectionInverseVariableListener<>(this, sourceVariableDescriptor);
        }
    }

    public boolean isSingleton() {
        return singleton;
    }

    @Override
    public boolean isListVariableSource() {
        return sourceVariableDescriptor instanceof ListVariableDescriptor<Solution_>;
    }
}
