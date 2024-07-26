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
import ai.timefold.solver.core.impl.domain.variable.cascade.command.CascadingUpdateCommand;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.NextElementSupplyCommand;
import ai.timefold.solver.core.impl.domain.variable.cascade.command.ShadowVariableDescriptorCommand;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class CascadingUpdateShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private final List<ShadowVariableTarget<Solution_>> shadowVariableTargetList;
    private ListVariableDescriptor<Solution_> sourceListVariableDescriptor;
    private final List<VariableDescriptor<Solution_>> targetVariableDescriptorList = new ArrayList<>();
    private final Set<VariableDescriptor<Solution_>> sourceVariableDescriptorSet = new HashSet<>();
    private ShadowVariableDescriptor<Solution_> nextElementShadowVariableDescriptor;
    private MemberAccessor targetMethod;
    // This flag defines if the planning variable generates a listener, which will be notified later by the event system
    private boolean notifiable = true;

    public CascadingUpdateShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
        shadowVariableTargetList = new ArrayList<>();
        addTargetVariable(entityDescriptor, variableMemberAccessor);
    }

    public void addTargetVariable(EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        shadowVariableTargetList.add(new ShadowVariableTarget<>(entityDescriptor, variableMemberAccessor));
    }

    public void setNotifiable(boolean notifiable) {
        this.notifiable = notifiable;
    }

    private List<CascadingUpdateShadowVariable> getDeclaredShadowVariables(MemberAccessor variableMemberAccessor) {
        var declaredShadowVariableList = Arrays.asList(variableMemberAccessor
                .getDeclaredAnnotationsByType(CascadingUpdateShadowVariable.class));
        var targetMethodList = declaredShadowVariableList.stream()
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
        return declaredShadowVariableList;
    }

    public String getTargetMethodName() {
        return getDeclaredShadowVariables(variableMemberAccessor).get(0).targetMethodName();
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        for (ShadowVariableTarget<Solution_> shadowVariableTarget : shadowVariableTargetList) {
            var declaredShadowVariableList =
                    getDeclaredShadowVariables(shadowVariableTarget.variableMemberAccessor());
            for (var shadowVariable : declaredShadowVariableList) {
                linkVariableDescriptorToSource(shadowVariableTarget.variableMemberAccessor(), shadowVariable);
            }
            targetVariableDescriptorList.add(shadowVariableTarget.entityDescriptor()
                    .getShadowVariableDescriptor(shadowVariableTarget.variableMemberAccessor().getName()));
        }

        // Currently, only one list variable is supported per design.
        // So, we assume that only one list variable can be found in the available entities, or we fail fast otherwise.
        var listVariableDescriptorList = entityDescriptor.getSolutionDescriptor().getEntityDescriptors().stream()
                .flatMap(e -> e.getGenuineVariableDescriptorList().stream())
                .filter(VariableDescriptor::isListVariable)
                .toList();
        if (listVariableDescriptorList.size() > 1) {
            throw new IllegalArgumentException(
                    "The shadow variable @%s does not support models with multiple planning list variables [%s].".formatted(
                            CascadingUpdateShadowVariable.class.getSimpleName(),
                            listVariableDescriptorList.stream().map(
                                    v -> v.getEntityDescriptor().getEntityClass().getSimpleName() + "::" + v.getVariableName())
                                    .collect(joining(", "))));
        }
        sourceListVariableDescriptor = (ListVariableDescriptor<Solution_>) listVariableDescriptorList.get(0);

        nextElementShadowVariableDescriptor = entityDescriptor.getShadowVariableDescriptors().stream()
                .filter(variableDescriptor -> NextElementShadowVariableDescriptor.class
                        .isAssignableFrom(variableDescriptor.getClass()))
                .findFirst()
                .orElse(null);

        // Defining the source variable and the source entity class may result in references to different sources,
        // such as regular shadow or planning list variables.
        // In order to simplify implementation,
        // the cascading listener can be configured for multiple shadow variables or a single planning list variable.
        if (hasShadowVariable() && hasPlanningListVariable()) {
            throw new IllegalArgumentException(
                    """
                            The entity class (%s) has an @%s annotated properties, but the sources can be either a regular shadow variable or a planning list variable.
                            Maybe update sourceVariableName or sourceVariableNames and reference either regular shadow variables or a planning list variable.
                            Maybe configure a distinct targetMethodName for one of the source types, and a separate listener will be created for it."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadingUpdateShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName()));
        }

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

    public void linkVariableDescriptorToSource(MemberAccessor targetMemberAccessor,
            CascadingUpdateShadowVariable shadowVariable) {
        var nonEmptySources = Arrays.stream(shadowVariable.sourceVariableNames())
                .filter(s -> !s.isBlank())
                .toList();
        if (shadowVariable.sourceVariableName().isBlank() && nonEmptySources.isEmpty()) {
            throw new IllegalArgumentException(
                    """
                            The entity class (%s) has an @%s annotated property (%s), but neither the sourceVariableName nor the sourceVariableNames properties are set.
                            Maybe update the field "%s" and set one of the properties ([sourceVariableName, sourceVariableNames])."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadingUpdateShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    variableMemberAccessor.getName()));
        }
        if (!shadowVariable.sourceVariableName().isBlank() && !nonEmptySources.isEmpty()) {
            throw new IllegalArgumentException(
                    """
                            The entity class (%s) has an @%s annotated property (%s), but it is only possible to define either sourceVariableName or sourceVariableNames.
                            Maybe update the field "%s" to set only one of the properties ([sourceVariableName, sourceVariableNames])."""
                            .formatted(entityDescriptor.getEntityClass(),
                                    CascadingUpdateShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor.getName(),
                                    variableMemberAccessor.getName()));
        }
        if (nonEmptySources.isEmpty()) {
            registerSource(shadowVariable.sourceEntityClass(), shadowVariable.sourceVariableName(), targetMemberAccessor);
        } else {
            nonEmptySources.forEach(name -> registerSource(shadowVariable.sourceEntityClass(), name, targetMemberAccessor));
        }
    }

    private void registerSource(Class<?> sourceEntityClass, String sourceVariableName, MemberAccessor targetMemberAccessor) {
        var sourceEntityDescriptor = entityDescriptor;
        if (!sourceEntityClass.equals(CascadingUpdateShadowVariable.NullEntityClass.class)) {
            sourceEntityDescriptor = entityDescriptor.getSolutionDescriptor().findEntityDescriptor(sourceEntityClass);
            if (sourceEntityDescriptor == null) {
                throw new IllegalArgumentException(
                        """
                                The entityClass (%s) has a @%s annotated property (%s) with a sourceEntityClass (%s) which is not a valid planning entity.
                                Maybe check the annotations of the class (%s).
                                Maybe add the class (%s) among planning entities in the solver configuration."""
                                .formatted(entityDescriptor.getEntityClass(),
                                        CascadingUpdateShadowVariable.class.getSimpleName(), targetMemberAccessor.getName(),
                                        sourceEntityClass, sourceEntityClass, sourceEntityClass));
            }
        }

        var sourceVariableDescriptor = sourceEntityDescriptor.getVariableDescriptor(sourceVariableName);
        if (sourceVariableDescriptor == null) {
            if (sourceEntityDescriptor != entityDescriptor) {
                throw new IllegalArgumentException(
                        """
                                The entityClass (%s) has a @%s annotated property (%s) with a sourceEntityClass (%s), but the shadow variable "%s" cannot be found in the planning entity %s.
                                Maybe update sourceVariableName to an existing shadow variable in the entity %s."""
                                .formatted(entityDescriptor.getEntityClass(),
                                        CascadingUpdateShadowVariable.class.getSimpleName(),
                                        targetMemberAccessor.getName(),
                                        sourceEntityClass,
                                        sourceVariableName,
                                        sourceEntityDescriptor.getEntityClass(),
                                        sourceEntityDescriptor.getEntityClass()));
            } else {
                throw new IllegalArgumentException(
                        """
                                The entity class (%s) has an @%s annotated property (%s), but the shadow variable "%s" cannot be found.
                                Maybe update sourceVariableName to an existing shadow variable in the entity %s."""
                                .formatted(sourceEntityDescriptor.getEntityClass(),
                                        CascadingUpdateShadowVariable.class.getSimpleName(),
                                        targetMemberAccessor.getName(),
                                        sourceVariableName,
                                        sourceEntityDescriptor.getEntityClass()));
            }
        }
        if (sourceVariableDescriptorSet.add(sourceVariableDescriptor)) {
            sourceVariableDescriptor.registerSinkVariableDescriptor(this);
        }
    }

    private boolean hasShadowVariable() {
        return this.sourceVariableDescriptorSet.stream()
                .anyMatch(v -> ShadowVariableDescriptor.class.isAssignableFrom(v.getClass()));
    }

    private boolean hasPlanningListVariable() {
        return this.sourceVariableDescriptorSet.stream()
                .anyMatch(v -> ListVariableDescriptor.class.isAssignableFrom(v.getClass()));
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return List.copyOf(sourceVariableDescriptorSet);
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        if (targetVariableDescriptorList.size() == 1) {
            if (hasShadowVariable()) {
                return Collections.singleton(SingleCascadingUpdateShadowVariableListener.class);
            } else {
                return Collections.singleton(SingleCascadingUpdateListVariableListener.class);
            }
        } else {
            return Collections.singleton(CollectionCascadingUpdateShadowVariableListener.class);
        }
    }

    @Override
    public Demand<?> getProvidedDemand() {
        throw new UnsupportedOperationException("Cascade update element shadow variable cannot be demanded.");
    }

    @Override
    public boolean hasVariableListener() {
        // There are use cases where the shadow variable is applied to different fields
        // and relies on the same method to update their values.
        // Therefore, only one listener will be generated when multiple descriptors use the same method,
        //and the notifiable flag won't be enabled in such cases.
        return notifiable;
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        AbstractCascadingUpdateShadowVariableListener<Solution_> listener;
        CascadingUpdateCommand<Object> nextElementCommand;
        if (nextElementShadowVariableDescriptor != null) {
            nextElementCommand = new ShadowVariableDescriptorCommand<>(nextElementShadowVariableDescriptor);
        } else {
            nextElementCommand =
                    new NextElementSupplyCommand(
                            supplyManager.demand(new NextElementVariableDemand<>(sourceListVariableDescriptor)));
        }
        if (targetVariableDescriptorList.size() == 1) {
            if (hasShadowVariable()) {
                listener = new SingleCascadingUpdateShadowVariableListener<>(targetVariableDescriptorList, targetMethod,
                        nextElementCommand);
            } else {
                listener = new SingleCascadingUpdateListVariableListener<>(sourceListVariableDescriptor,
                        targetVariableDescriptorList, targetMethod,
                        nextElementCommand);
            }
        } else {
            listener = new CollectionCascadingUpdateShadowVariableListener<>(targetVariableDescriptorList, targetMethod,
                    nextElementCommand);
        }
        return Collections.singleton(new VariableListenerWithSources<>(listener, getSourceVariableDescriptorList()));
    }

    private record ShadowVariableTarget<Solution_>(EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {

    }
}
