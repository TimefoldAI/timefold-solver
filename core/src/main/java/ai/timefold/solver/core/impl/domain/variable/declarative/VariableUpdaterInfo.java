package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record VariableUpdaterInfo<Solution_>(
        VariableMetaModel<Solution_, ?, ?> id,
        int groupId,
        DeclarativeShadowVariableDescriptor<Solution_> variableDescriptor,
        @Nullable ShadowVariableLoopedVariableDescriptor<Solution_> shadowVariableLoopedDescriptor,
        MemberAccessor memberAccessor,
        Function<Object, Object> calculator,
        @Nullable Object[] groupEntities,
        boolean isGroupAligned) {

    public VariableUpdaterInfo(VariableMetaModel<Solution_, ?, ?> id,
            int groupId,
            DeclarativeShadowVariableDescriptor<Solution_> variableDescriptor,
            @Nullable ShadowVariableLoopedVariableDescriptor<Solution_> shadowVariableLoopedDescriptor,
            MemberAccessor memberAccessor,
            Function<Object, Object> calculator) {
        // isGroupAligned defaults to true, so we can just check it instead of checking
        // if groupEntities is null before determining what updateIfChanged to call
        this(id, groupId, variableDescriptor, shadowVariableLoopedDescriptor, memberAccessor, calculator, null, true);
    }

    public VariableUpdaterInfo<Solution_> withGroupId(int groupId) {
        return new VariableUpdaterInfo<>(id, groupId, variableDescriptor, shadowVariableLoopedDescriptor, memberAccessor,
                calculator, groupEntities, isGroupAligned);
    }

    public VariableUpdaterInfo<Solution_> withGroupEntities(Object[] groupEntities, boolean isGroupAligned) {
        return new VariableUpdaterInfo<>(id, groupId, variableDescriptor, shadowVariableLoopedDescriptor, memberAccessor,
                calculator, groupEntities, isGroupAligned);
    }

    public boolean updateIfChanged(Object entity, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        if (isGroupAligned) {
            return updateIfChanged(entity, calculator.apply(entity), changedVariableNotifier);
        } else {
            var anyChanged = false;
            for (var groupEntity : groupEntities) {
                var oldValue = variableDescriptor.getValue(groupEntity);
                var newValue = calculator.apply(groupEntity);

                if (!Objects.equals(oldValue, newValue)) {
                    changedVariableNotifier.beforeVariableChanged().accept(variableDescriptor, groupEntity);
                    variableDescriptor.setValue(groupEntity, newValue);
                    changedVariableNotifier.afterVariableChanged().accept(variableDescriptor, groupEntity);
                    anyChanged = true;
                }
            }
            return anyChanged;
        }
    }

    public boolean updateIfChanged(Object entity, @Nullable Object newValue,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var oldValue = variableDescriptor.getValue(entity);
        if (!Objects.equals(oldValue, newValue)) {
            if (groupEntities == null) {
                changedVariableNotifier.beforeVariableChanged().accept(variableDescriptor, entity);
                variableDescriptor.setValue(entity, newValue);
                changedVariableNotifier.afterVariableChanged().accept(variableDescriptor, entity);
            } else {
                for (var groupEntity : groupEntities) {
                    changedVariableNotifier.beforeVariableChanged().accept(variableDescriptor, groupEntity);
                    variableDescriptor.setValue(groupEntity, newValue);
                    changedVariableNotifier.afterVariableChanged().accept(variableDescriptor, groupEntity);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VariableUpdaterInfo<?> that))
            return false;
        return groupId == that.groupId && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, groupId);
    }

    @Override
    public String toString() {
        return (groupEntities == null) ? "%s (%d)".formatted(id.name(), groupId)
                : "%s (%d) %s".formatted(id.name(), groupId, Arrays.toString(groupEntities));
    }
}
