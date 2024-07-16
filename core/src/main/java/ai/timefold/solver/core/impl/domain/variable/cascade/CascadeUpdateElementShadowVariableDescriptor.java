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

    private final List<SourceVariable<Solution_>> sourceVariables;
    private final List<VariableDescriptor<Solution_>> sourceVariableDescriptorList = new ArrayList<>();
    private List<ShadowVariableDescriptor<Solution_>> sourceShadowVariableDescriptorList;
    private ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;
    private MemberAccessor listenerUpdateMethod;
    // This flag defines if the shadow variable will generate a listener, which will be notified later by the event system
    private boolean notifiable = true;

    public CascadeUpdateElementShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
        sourceVariables = new ArrayList<>();
        addSourceVariable(entityDescriptor, variableMemberAccessor);
    }

    public void addSourceVariable(EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        sourceVariables.add(new SourceVariable<>(entityDescriptor, variableMemberAccessor));
    }

    public boolean isNotifiable() {
        return notifiable;
    }

    public void setNotifiable(boolean notifiable) {
        this.notifiable = notifiable;
    }

    public String getSourceMethodName() {
        CascadeUpdateElementShadowVariable shadowVariableAnnotation =
                variableMemberAccessor.getAnnotation(CascadeUpdateElementShadowVariable.class);
        return shadowVariableAnnotation.sourceMethodName();
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        sourceShadowVariableDescriptorList = new ArrayList<>();
        for (SourceVariable<Solution_> sourceVariable : sourceVariables) {
            sourceShadowVariableDescriptorList.add(sourceVariable.entityDescriptor()
                    .getShadowVariableDescriptor(sourceVariable.variableMemberAccessor().getName()));
        }
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

        var sourceMethodName = getSourceMethodName();
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
        // There are use cases where the shadow variable is applied to different fields and relies on the same method
        // to update their values. Therefore, only one listener will be generated when multiple descriptors use the same
        // method, and the notifiable flag won't be enabled in such cases.
        if (isNotifiable()) {
            return List.of(new VariableListenerWithSources<>(
                    new CascadeUpdateVariableListener<>(sourceShadowVariableDescriptorList, nextElementShadowVariableDescriptor,
                            listenerUpdateMethod),
                    sourceVariableDescriptorList));
        } else {
            return Collections.emptyList();
        }
    }

    private record SourceVariable<Solution_>(EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {

    }
}
