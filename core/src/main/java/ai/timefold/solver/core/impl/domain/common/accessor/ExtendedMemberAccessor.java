package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.reflect.Type;

/**
 * This class extends the {@link MemberAccessor} contract and adds a getter method that accepts a parameter.
 *
 * @see ReflectionMethodExtendedMemberAccessor
 */
public interface ExtendedMemberAccessor extends MemberAccessor {

    /**
     * @return returns the parameter type if the getter accepts a parameter.
     */
    Type getGetterMethodParameterType();

    /**
     * Differs from {@link #executeGetter(Object)} in that it accepts a single parameter.
     */
    Object executeGetter(Object bean, Object value);

}
