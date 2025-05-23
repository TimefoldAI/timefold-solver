package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
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
    private boolean chained;

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
     * Sourced on a basic genuine planning variable, the shadow type is a Collection (such as List or Set).
     * Sourced on a list or chained planning variable, the shadow variable type is a single instance.
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
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has an @" + InverseRelationShadowVariable.class.getSimpleName()
                    + " annotated property (" + variableMemberAccessor.getName()
                    + ") with a sourceClass (" + sourceClass
                    + ") which is not a valid planning entity."
                    + "\nMaybe check the annotations of the class (" + sourceClass + ")."
                    + "\nMaybe add the class (" + sourceClass
                    + ") among planning entities in the solver configuration.");
        }
        String sourceVariableName = shadowVariableAnnotation.sourceVariableName();
        // TODO can we getGenuineVariableDescriptor()?
        sourceVariableDescriptor = sourceEntityDescriptor.getVariableDescriptor(sourceVariableName);
        if (sourceVariableDescriptor == null) {
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has an @" + InverseRelationShadowVariable.class.getSimpleName()
                    + " annotated property (" + variableMemberAccessor.getName()
                    + ") with sourceVariableName (" + sourceVariableName
                    + ") which is not a valid planning variable on entityClass ("
                    + sourceEntityDescriptor.getEntityClass() + ").\n"
                    + sourceEntityDescriptor.buildInvalidVariableNameExceptionMessage(sourceVariableName));
        }
        chained = sourceVariableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor &&
                basicVariableDescriptor.isChained();
        boolean list = sourceVariableDescriptor.isListVariable();
        if (singleton) {
            if (!chained && !list) {
                throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                        + ") has an @" + InverseRelationShadowVariable.class.getSimpleName()
                        + " annotated property (" + variableMemberAccessor.getName()
                        + ") which does not return a " + Collection.class.getSimpleName()
                        + " with sourceVariableName (" + sourceVariableName
                        + ") which is neither a list variable @" + PlanningListVariable.class.getSimpleName()
                        + " nor a chained variable @" + PlanningVariable.class.getSimpleName()
                        + "(graphType=" + PlanningVariableGraphType.CHAINED + ")."
                        + " Only list and chained variables support a singleton inverse.");
            }
        } else {
            if (chained || list) {
                throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                        + ") has an @" + InverseRelationShadowVariable.class.getSimpleName()
                        + " annotated property (" + variableMemberAccessor.getName()
                        + ") which returns a " + Collection.class.getSimpleName()
                        + " (" + variablePropertyType
                        + ") with sourceVariableName (" + sourceVariableName
                        + ") which is a" + (chained
                                ? " chained variable @" + PlanningVariable.class.getSimpleName()
                                        + "(graphType=" + PlanningVariableGraphType.CHAINED
                                        + "). A chained variable supports only a singleton inverse."
                                : " list variable @" + PlanningListVariable.class.getSimpleName()
                                        + ". A list variable supports only a singleton inverse."));
            }
        }
        sourceVariableDescriptor.registerSinkVariableDescriptor(this);
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.singletonList(sourceVariableDescriptor);
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        if (singleton) {
            if (chained) {
                return Collections.singleton(SingletonInverseVariableListener.class);
            } else {
                throw new UnsupportedOperationException("Impossible state: Handled by %s."
                        .formatted(ListVariableStateSupply.class.getSimpleName()));
            }
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
            if (chained) {
                return new SingletonInverseVariableDemand<>(sourceVariableDescriptor);
            } else {
                throw new UnsupportedOperationException("Impossible state: Handled by %s."
                        .formatted(ListVariableStateSupply.class.getSimpleName()));
            }
        } else {
            return new CollectionInverseVariableDemand<>(sourceVariableDescriptor);
        }
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        return new VariableListenerWithSources<>(buildVariableListener(), sourceVariableDescriptor).toCollection();
    }

    private AbstractVariableListener<Solution_, Object> buildVariableListener() {
        if (singleton) {
            if (chained) {
                return new SingletonInverseVariableListener<>(this, sourceVariableDescriptor);
            } else {
                throw new UnsupportedOperationException("Impossible state: Handled by %s."
                        .formatted(ListVariableStateSupply.class.getSimpleName()));
            }
        } else {
            return new CollectionInverseVariableListener<>(this, sourceVariableDescriptor);
        }
    }

    @Override
    public boolean isListVariableSource() {
        return sourceVariableDescriptor instanceof ListVariableDescriptor<Solution_>;
    }
}
