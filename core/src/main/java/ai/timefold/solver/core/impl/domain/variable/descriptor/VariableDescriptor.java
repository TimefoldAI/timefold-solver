package ai.timefold.solver.core.impl.domain.variable.descriptor;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class VariableDescriptor<Solution_> {

    protected final int ordinal;
    protected final EntityDescriptor<Solution_> entityDescriptor;
    protected final MemberAccessor variableMemberAccessor;
    protected final String variableName;
    protected final String simpleEntityAndVariableName;

    protected List<ShadowVariableDescriptor<Solution_>> sinkVariableDescriptorList = new ArrayList<>(4);

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    protected VariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        if (variableMemberAccessor.getType().isPrimitive()) {
            throw new IllegalStateException("""
                    The entityClass (%s) has a @%s annotated member (%s) that returns a primitive type (%s).
                    This means it cannot represent an uninitialized variable as null \
                    and the Construction Heuristics think it's already initialized.
                    Maybe let the member (%s) return its primitive wrapper type instead."""
                    .formatted(entityDescriptor.getEntityClass(),
                            PlanningVariable.class.getSimpleName(),
                            variableMemberAccessor,
                            variableMemberAccessor.getType(),
                            getSimpleEntityAndVariableName()));
        }
        this.ordinal = ordinal;
        this.entityDescriptor = entityDescriptor;
        this.variableMemberAccessor = variableMemberAccessor;
        this.variableName = variableMemberAccessor.getName();
        this.simpleEntityAndVariableName = entityDescriptor.getEntityClass().getSimpleName() + "." + variableName;
    }

    /**
     * A number unique within an {@link EntityDescriptor}, increasing sequentially from zero.
     * Used for indexing in arrays to avoid object hash lookups in maps.
     *
     * @return zero or higher
     */
    public int getOrdinal() {
        return ordinal;
    }

    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return entityDescriptor;
    }

    public String getVariableName() {
        return variableName;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public String getSimpleEntityAndVariableName() {
        return simpleEntityAndVariableName;
    }

    public Class<?> getVariablePropertyType() {
        return variableMemberAccessor.getType();
    }

    public abstract void linkVariableDescriptors(DescriptorPolicy descriptorPolicy);

    public final boolean isListVariable() {
        return this instanceof ListVariableDescriptor;
    }

    public boolean canBeUsedAsSource() {
        return true;
    }

    // ************************************************************************
    // Shadows
    // ************************************************************************

    public void registerSinkVariableDescriptor(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        sinkVariableDescriptorList.add(shadowVariableDescriptor);
    }

    /**
     * Inverse of {@link ShadowVariableDescriptor#getSourceVariableDescriptorList()}.
     *
     * @return never null, only direct shadow variables that are affected by this variable
     */
    public List<ShadowVariableDescriptor<Solution_>> getSinkVariableDescriptorList() {
        return sinkVariableDescriptorList;
    }

    /**
     * @param value never null
     * @return true if it might be an anchor, false if it is definitely not an anchor
     */
    public boolean isValuePotentialAnchor(Object value) {
        return !entityDescriptor.getEntityClass().isAssignableFrom(value.getClass());
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    public Object getValue(Object entity) {
        return variableMemberAccessor.executeGetter(entity);
    }

    public void setValue(Object entity, Object value) {
        variableMemberAccessor.executeSetter(entity, value);
    }

    public String getMemberAccessorSpeedNote() {
        return variableMemberAccessor.getSpeedNote();
    }

    public final boolean isGenuineAndUninitialized(Object entity) {
        return this instanceof GenuineVariableDescriptor<Solution_> genuineVariableDescriptor
                && !genuineVariableDescriptor.isInitialized(entity);
    }
}
