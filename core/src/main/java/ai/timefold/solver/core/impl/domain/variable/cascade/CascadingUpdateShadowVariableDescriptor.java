package ai.timefold.solver.core.impl.domain.variable.cascade;

import static java.util.stream.Collectors.joining;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class CascadingUpdateShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private final List<TargetVariable<Solution_>> targetVariables;
    private ListVariableDescriptor<Solution_> sourceListVariable;
    private final List<VariableDescriptor<Solution_>> targetVariableDescriptorList = new ArrayList<>();
    private final Set<ShadowVariableDescriptor<Solution_>> sourceShadowVariableDescriptorSet = new HashSet<>();
    private ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;
    private MemberAccessor targetMethod;
    // This flag defines if the planning variable generates a listener, which will be notified later by the event system
    private boolean notifiable = true;

    public CascadingUpdateShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
        targetVariables = new ArrayList<>();
        addTargetVariable(entityDescriptor, variableMemberAccessor);
    }

    public void addTargetVariable(EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        targetVariables.add(new TargetVariable<>(entityDescriptor, variableMemberAccessor));
    }

    public void setNotifiable(boolean notifiable) {
        this.notifiable = notifiable;
    }

    private List<CascadingUpdateShadowVariable> getDeclaredListeners(MemberAccessor variableMemberAccessor) {
        var declaredListenerList = Arrays.asList(variableMemberAccessor
                .getDeclaredAnnotationsByType(CascadingUpdateShadowVariable.class));
        var targetMethodList = declaredListenerList.stream()
                .map(CascadingUpdateShadowVariable::targetMethodName)
                .distinct()
                .toList();
        if (targetMethodList.size() > 1) {
            throw new IllegalArgumentException(
                    """
                            The entityClass (%s) has multiple @%s in the annotated property (%s), and there are distinct targetMethodName values [%s].
                            Maybe update targetMethodName to use same method in the field %s."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadingUpdateShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    String.join(", ", targetMethodList),
                                    variableMemberAccessor.getName()));
        }
        return declaredListenerList;
    }

    public String getTargetMethodName() {
        return getDeclaredListeners(variableMemberAccessor).get(0).targetMethodName();
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        for (TargetVariable<Solution_> targetVariable : targetVariables) {
            var declaredListenerList =
                    getDeclaredListeners(targetVariable.variableMemberAccessor());
            for (var listener : declaredListenerList) {
                linkVariableDescriptorToSource(listener);
            }
            targetVariableDescriptorList.add(targetVariable.entityDescriptor()
                    .getShadowVariableDescriptor(targetVariable.variableMemberAccessor().getName()));
        }

        // Currently, only one list variable is supported per design.
        // So, we assume that only one list variable can be found in the available entities, or we fail fast otherwise.
        var listVariableDescriptorList = entityDescriptor.getSolutionDescriptor().getEntityDescriptors().stream()
                .flatMap(e -> e.getGenuineVariableDescriptorList().stream())
                .filter(VariableDescriptor::isListVariable)
                .toList();
        if (listVariableDescriptorList.size() > 1) {
            throw new IllegalArgumentException(
                    "The listener @%s does not support models with multiple planning list variables [%s].".formatted(
                            CascadingUpdateShadowVariable.class.getSimpleName(),
                            listVariableDescriptorList.stream().map(
                                    v -> v.getEntityDescriptor().getEntityClass().getSimpleName() + "::" + v.getVariableName())
                                    .collect(joining(", "))));
        }
        sourceListVariable = (ListVariableDescriptor<Solution_>) listVariableDescriptorList.get(0);

        nextElementShadowVariableDescriptor = entityDescriptor.getShadowVariableDescriptors().stream()
                .filter(variableDescriptor -> NextElementShadowVariableDescriptor.class
                        .isAssignableFrom(variableDescriptor.getClass()))
                .findFirst()
                .orElse(null);

        var targetMethodName = getTargetMethodName();
        var allSourceMethodMembers = ConfigUtils.getDeclaredMembers(entityDescriptor.getEntityClass())
                .stream()
                .filter(member -> member.getName().equals(targetMethodName)
                        && member instanceof Method method
                        && method.getParameterCount() == 0)
                .toList();
        if (allSourceMethodMembers.isEmpty()) {
            throw new IllegalArgumentException(
                    "The entity class (%s) has an @%s annotated property (%s), but the method \"%s\" cannot be found."
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadingUpdateShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    targetMethodName));
        }
        targetMethod = descriptorPolicy.getMemberAccessorFactory().buildAndCacheMemberAccessor(allSourceMethodMembers.get(0),
                MemberAccessorFactory.MemberAccessorType.REGULAR_METHOD, null, descriptorPolicy.getDomainAccessType());
    }

    public void linkVariableDescriptorToSource(CascadingUpdateShadowVariable listener) {
        var sourceDescriptor = entityDescriptor.getShadowVariableDescriptor(listener.sourceVariableName());
        if (sourceDescriptor == null) {
            throw new IllegalArgumentException(
                    """
                            The entity class (%s) has an @%s annotated property (%s), but the shadow variable "%s" cannot be found.
                            Maybe update sourceVariableName to an existing shadow variable in the entity %s."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadingUpdateShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    listener.sourceVariableName(),
                                    entityDescriptor.getEntityClass()));
        }
        if (sourceShadowVariableDescriptorSet.add(sourceDescriptor)) {
            sourceDescriptor.registerSinkVariableDescriptor(this);
        }
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return List.copyOf(sourceShadowVariableDescriptorSet);
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        return Collections.singleton(CollectionCascadingUpdateShadowVariableListener.class);
    }

    @Override
    public Demand<?> getProvidedDemand() {
        throw new UnsupportedOperationException("Cascade update element shadow variable cannot be demanded.");
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        // There are use cases where the shadow variable is applied to different fields
        // and relies on the same method to update their values.
        // Therefore, only one listener will be generated when multiple descriptors use the same method,
        //and the notifiable flag won't be enabled in such cases.
        if (notifiable) {
            AbstractCascadingUpdateShadowVariableListener<Solution_> listener;
            if (nextElementShadowVariableDescriptor != null) {
                if (targetVariableDescriptorList.size() == 1) {
                    listener = new SingleCascadingUpdateShadowVariableListener<>(targetVariableDescriptorList,
                            nextElementShadowVariableDescriptor, targetMethod);
                } else {
                    listener = new CollectionCascadingUpdateShadowVariableListener<>(targetVariableDescriptorList,
                            nextElementShadowVariableDescriptor, targetMethod);
                }
            } else {
                if (targetVariableDescriptorList.size() == 1) {
                    listener = new SingleCascadingUpdateShadowVariableWithSupplyListener<>(targetVariableDescriptorList,
                            supplyManager.demand(new NextElementVariableDemand<>(sourceListVariable)), targetMethod);
                } else {
                    listener = new CollectionCascadingUpdateShadowVariableWithSupplyListener<>(targetVariableDescriptorList,
                            supplyManager.demand(new NextElementVariableDemand<>(sourceListVariable)), targetMethod);
                }
            }
            return Collections.singleton(new VariableListenerWithSources<>(listener, getSourceVariableDescriptorList()));
        } else {
            return Collections.emptyList();
        }
    }

    private record TargetVariable<Solution_>(EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {

    }
}
