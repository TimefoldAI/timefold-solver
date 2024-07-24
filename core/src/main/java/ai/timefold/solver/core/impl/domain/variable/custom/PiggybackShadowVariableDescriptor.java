package ai.timefold.solver.core.impl.domain.variable.custom;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadingUpdateShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class PiggybackShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private final MemberAccessor memberAccessor;
    private ShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    public PiggybackShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor memberAccessor) {
        super(ordinal, entityDescriptor, memberAccessor);
        // The member accessor is required for further processing when using CascadingUpdateShadowVariable
        this.memberAccessor = memberAccessor;
    }

    private boolean isCustomShadowVariable() {
        return shadowVariableDescriptor != null
                && CustomShadowVariableDescriptor.class.isAssignableFrom(shadowVariableDescriptor.getClass());
    }

    private boolean isCascadingUpdateShadowVariable() {
        return shadowVariableDescriptor != null
                && CascadingUpdateShadowVariableDescriptor.class.isAssignableFrom(shadowVariableDescriptor.getClass());
    }

    public String getShadowVariableName() {
        return memberAccessor.getAnnotation(PiggybackShadowVariable.class).shadowVariableName();
    }

    public MemberAccessor getMemberAccessor() {
        return memberAccessor;
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        linkShadowSources(descriptorPolicy);
    }

    private void linkShadowSources(DescriptorPolicy descriptorPolicy) {

        var piggybackShadowVariable = memberAccessor.getAnnotation(PiggybackShadowVariable.class);
        EntityDescriptor<Solution_> shadowEntityDescriptor;
        var shadowEntityClass = piggybackShadowVariable.shadowEntityClass();
        if (shadowEntityClass.equals(PiggybackShadowVariable.NullEntityClass.class)) {
            shadowEntityDescriptor = entityDescriptor;
        } else {
            shadowEntityDescriptor = entityDescriptor.getSolutionDescriptor().findEntityDescriptor(shadowEntityClass);
            if (shadowEntityDescriptor == null) {
                throw new IllegalArgumentException(
                        """
                                The entityClass (%s) has a @%s annotated property (%s) with a shadowEntityClass (%s) which is not a valid planning entity.
                                Maybe check the annotations of the class (%s).
                                Maybe add the class (%s) among planning entities in the solver configuration."""
                                .formatted(entityDescriptor.getEntityClass(), PiggybackShadowVariable.class.getSimpleName(),
                                        memberAccessor.getName(), shadowEntityClass, shadowEntityClass,
                                        shadowEntityClass));
            }
        }
        var shadowVariableName = piggybackShadowVariable.shadowVariableName();
        var uncastShadowVariableDescriptor = shadowEntityDescriptor.getVariableDescriptor(shadowVariableName);
        if (uncastShadowVariableDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            The entityClass (%s) has a @%s annotated property (%s) with shadowVariableName (%s) which is not a valid planning variable on entityClass (%s).
                            %s"""
                            .formatted(entityDescriptor.getEntityClass(), PiggybackShadowVariable.class.getSimpleName(),
                                    memberAccessor.getName(), shadowVariableName,
                                    shadowEntityDescriptor.getEntityClass(),
                                    shadowEntityDescriptor.buildInvalidVariableNameExceptionMessage(shadowVariableName)));
        }
        shadowVariableDescriptor = (ShadowVariableDescriptor<Solution_>) uncastShadowVariableDescriptor;
        if (!isCustomShadowVariable() && !isCascadingUpdateShadowVariable()) {
            throw new IllegalArgumentException(
                    "The entityClass (%s) has a @%s annotated property (%s) with refVariable (%s) that lacks @%s or @%s annotations."
                            .formatted(entityDescriptor.getEntityClass(), PiggybackShadowVariable.class.getSimpleName(),
                                    memberAccessor.getName(),
                                    uncastShadowVariableDescriptor.getSimpleEntityAndVariableName(),
                                    ShadowVariable.class.getSimpleName(), CascadingUpdateShadowVariable.class.getSimpleName()));
        }
        if (isCascadingUpdateShadowVariable() && shadowEntityDescriptor != entityDescriptor) {
            throw new IllegalArgumentException(
                    """
                            The entityClass (%s) has a @%s annotated property (%s) with a shadowEntityClass (%s), but it cannot be set when the source shadow variable is @%s.
                            Maybe remove the property shadowEntityClass and ensure the shadow variable %s is defined on %s."""
                            .formatted(entityDescriptor.getEntityClass(), PiggybackShadowVariable.class.getSimpleName(),
                                    memberAccessor.getName(), shadowEntityClass,
                                    CascadingUpdateShadowVariable.class.getSimpleName(),
                                    piggybackShadowVariable.shadowVariableName(), entityDescriptor.getEntityClass()));
        }
        shadowVariableDescriptor.registerSinkVariableDescriptor(this);
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.singletonList(shadowVariableDescriptor);
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        return shadowVariableDescriptor.getVariableListenerClasses();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Demand<?> getProvidedDemand() {
        throw new UnsupportedOperationException("Custom shadow variable cannot be demanded.");
    }

    @Override
    public boolean hasVariableListener() {
        return false;
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        throw new UnsupportedOperationException(
                "The piggybackShadowVariableDescriptor (%s) cannot build a variable listener.".formatted(this));
    }
}
