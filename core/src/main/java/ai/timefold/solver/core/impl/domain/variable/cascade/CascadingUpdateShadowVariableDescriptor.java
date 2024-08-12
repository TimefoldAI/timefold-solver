package ai.timefold.solver.core.impl.domain.variable.cascade;

import static java.util.stream.Collectors.joining;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class CascadingUpdateShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private final List<ShadowVariableTarget<Solution_>> shadowVariableTargetList;
    private final List<VariableDescriptor<Solution_>> targetVariableDescriptorList = new ArrayList<>();
    private VariableDescriptor<Solution_> firstTargetVariableDescriptor;
    private MemberAccessor targetMethod;

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

    public String getTargetMethodName() {
        var variableMemberAccessor = shadowVariableTargetList.get(0).variableMemberAccessor();
        return Arrays
                .stream(variableMemberAccessor.getDeclaredAnnotationsByType(CascadingUpdateShadowVariable.class))
                .findFirst().map(CascadingUpdateShadowVariable::targetMethodName)
                .orElseThrow(() -> new IllegalStateException("The entity %s is not annotated with @%s."
                        .formatted(entityDescriptor.getEntityClass(), CascadingUpdateShadowVariable.class.getSimpleName())));
    }

    public boolean update(ScoreDirector<Solution_> scoreDirector, Object entity) {
        if (targetVariableDescriptorList.size() == 1) {
            return updateSingle(scoreDirector, entity);
        } else {
            return updateMultiple(scoreDirector, entity);
        }
    }

    private boolean updateSingle(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var oldValue = firstTargetVariableDescriptor.getValue(entity);
        scoreDirector.beforeVariableChanged(entity, firstTargetVariableDescriptor.getVariableName());
        targetMethod.executeGetter(entity);
        var newValue = firstTargetVariableDescriptor.getValue(entity);
        scoreDirector.afterVariableChanged(entity, firstTargetVariableDescriptor.getVariableName());
        return !Objects.equals(oldValue, newValue);
    }

    private boolean updateMultiple(ScoreDirector<Solution_> scoreDirector, Object entity) {
        var oldValueList = new ArrayList<>(targetVariableDescriptorList.size());
        for (var targetVariableDescriptor : targetVariableDescriptorList) {
            scoreDirector.beforeVariableChanged(entity, targetVariableDescriptor.getVariableName());
            oldValueList.add(targetVariableDescriptor.getValue(entity));
        }
        targetMethod.executeGetter(entity);
        var hasChange = false;
        for (var i = 0; i < targetVariableDescriptorList.size(); i++) {
            var targetVariableDescriptor = targetVariableDescriptorList.get(i);
            var newValue = targetVariableDescriptor.getValue(entity);
            scoreDirector.afterVariableChanged(entity, targetVariableDescriptor.getVariableName());
            if (!hasChange && !Objects.equals(oldValueList.get(i), newValue)) {
                hasChange = true;
            }
        }
        return hasChange;
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        for (var shadowVariableTarget : shadowVariableTargetList) {
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
        firstTargetVariableDescriptor = targetVariableDescriptorList.get(0);
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        return Collections.emptyList();
    }

    @Override
    public Demand<?> getProvidedDemand() {
        throw new UnsupportedOperationException("Cascade update element shadow variable cannot be demanded.");
    }

    @Override
    public boolean hasVariableListener() {
        return false;
    }

    @Override
    public boolean canBeUsedAsSource() {
        return false;
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        throw new UnsupportedOperationException("Cascade update element generates no listeners.");
    }

    private record ShadowVariableTarget<Solution_>(EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {

    }
}
