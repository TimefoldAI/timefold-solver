package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;
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
        @Nullable Object[] groupEntities) {

    public VariableUpdaterInfo(VariableMetaModel<Solution_, ?, ?> id,
            int groupId,
            DeclarativeShadowVariableDescriptor<Solution_> variableDescriptor,
            @Nullable ShadowVariableLoopedVariableDescriptor<Solution_> shadowVariableLoopedDescriptor,
            MemberAccessor memberAccessor,
            Function<Object, Object> calculator) {
        this(id, groupId, variableDescriptor, shadowVariableLoopedDescriptor, memberAccessor, calculator, null);
    }

    public VariableUpdaterInfo<Solution_> withGroupId(int groupId) {
        return new VariableUpdaterInfo<>(id, groupId, variableDescriptor, shadowVariableLoopedDescriptor, memberAccessor,
                calculator, groupEntities);
    }

    public VariableUpdaterInfo<Solution_> withGroupEntities(Object[] groupEntities) {
        return new VariableUpdaterInfo<>(id, groupId, variableDescriptor, shadowVariableLoopedDescriptor, memberAccessor,
                calculator, groupEntities);
    }

    @Override
    public String toString() {
        return (groupEntities == null) ? "%s (%d)".formatted(id.name(), groupId)
                : "%s (%d) %s".formatted(id.name(), groupId, Arrays.toString(groupEntities));
    }
}
