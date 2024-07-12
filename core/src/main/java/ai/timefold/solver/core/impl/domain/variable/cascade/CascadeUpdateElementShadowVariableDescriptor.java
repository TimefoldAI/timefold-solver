package ai.timefold.solver.core.impl.domain.variable.cascade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.CascadeUpdateElementShadowVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class CascadeUpdateElementShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private final List<VariableDescriptor<Solution_>> sourceVariableDescriptorList = new ArrayList<>();
    private ShadowVariableDescriptor<Solution_> sourceShadowVariableDescriptor;
    private ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;
    private MemberAccessor listenerUpdateMethod;

    public CascadeUpdateElementShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        this.sourceShadowVariableDescriptor = entityDescriptor.getShadowVariableDescriptor(variableMemberAccessor.getName());

        var inverseRelationShadowDescriptor = entityDescriptor.getShadowVariableDescriptors().stream()
                .filter(variableDescriptor -> InverseRelationShadowVariableDescriptor.class
                        .isAssignableFrom(variableDescriptor.getClass()))
                .findFirst()
                .orElse(null);
        if (inverseRelationShadowDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            The entityClass (%s) has an @%s annotated property (%s), but has no @InverseRelationShadowVariable shadow variable defined.
                            Maybe add a new shadow variable @InverseRelationShadowVariable to the entity %s."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadeUpdateElementShadowVariable.class.getSimpleName(), variableMemberAccessor.getName(),
                                    entityDescriptor.getEntityClass()));
        }
        sourceVariableDescriptorList.add(inverseRelationShadowDescriptor);

        var previousElementShadowDescriptor = entityDescriptor.getShadowVariableDescriptors().stream()
                .filter(variableDescriptor -> PreviousElementShadowVariableDescriptor.class
                        .isAssignableFrom(variableDescriptor.getClass()))
                .findFirst()
                .orElse(null);
        if (previousElementShadowDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            The entityClass (%s) has an @%s annotated property (%s), but has no @PreviousElementShadowVariable shadow variable defined.
                            Maybe add a new shadow variable @PreviousElementShadowVariable to the entity %s."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadeUpdateElementShadowVariable.class.getSimpleName(), variableMemberAccessor.getName(),
                                    entityDescriptor.getEntityClass()));
        }
        sourceVariableDescriptorList.add(previousElementShadowDescriptor);

        nextElementShadowVariableDescriptor = entityDescriptor.getShadowVariableDescriptors().stream()
                .filter(variableDescriptor -> NextElementShadowVariableDescriptor.class
                        .isAssignableFrom(variableDescriptor.getClass()))
                .findFirst()
                .orElse(null);
        if (nextElementShadowVariableDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            The entityClass (%s) has an @%s annotated property (%s), but has no @NextElementShadowVariable shadow variable defined.
                            Maybe add a new shadow variable @NextElementShadowVariable to the entity %s."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadeUpdateElementShadowVariable.class.getSimpleName(), variableMemberAccessor.getName(),
                                    entityDescriptor.getEntityClass()));
        }

        CascadeUpdateElementShadowVariable shadowVariableAnnotation =
                variableMemberAccessor.getAnnotation(CascadeUpdateElementShadowVariable.class);
        var sourceMethodName = shadowVariableAnnotation.sourceMethodName();
        var sourceMethodMember = ConfigUtils.getDeclaredMembers(entityDescriptor.getEntityClass())
                .stream()
                .filter(member -> member.getName().equals(sourceMethodName))
                .findFirst()
                .orElse(null);
        if (sourceMethodMember == null) {
            throw new IllegalArgumentException(
                    "The entityClass (%s) has an @%s annotated property (%s) with sourceMethodName (%s), but the method has not been found in the entityClass."
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadeUpdateElementShadowVariable.class.getSimpleName(), sourceMethodName,
                                    entityDescriptor.getEntityClass()));
        }
        listenerUpdateMethod = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(sourceMethodMember,
                MemberAccessorFactory.MemberAccessorType.REGULAR_METHOD, null, descriptorPolicy.getDomainAccessType());
        inverseRelationShadowDescriptor.registerSinkVariableDescriptor(this);
        previousElementShadowDescriptor.registerSinkVariableDescriptor(this);
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.unmodifiableList(sourceVariableDescriptorList);
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        return Collections.singleton(CascadeUpdateVariableListener.class);
    }

    @Override
    public Demand<?> getProvidedDemand() {
        throw new UnsupportedOperationException("Cascade update element shadow variable cannot be demanded.");
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        return List.of(new VariableListenerWithSources<>(
                new CascadeUpdateVariableListener<>(sourceShadowVariableDescriptor, nextElementShadowVariableDescriptor,
                        listenerUpdateMethod),
                sourceVariableDescriptorList));
    }
}
