package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;

/**
 * Additional information for the GIZMO accessor generation.
 *
 * @param returnTypeRequired a flag that indicates if the return type is required or optional
 * @param readMethodWithParameter a flag that allows the read method to accept an argument
 */
public record AccessorInfo(MemberAccessorFactory.MemberAccessorType memberAccessorType, boolean returnTypeRequired,
        boolean readMethodWithParameter) {

    public static AccessorInfo withReturnValueAndNoArguments() {
        return new AccessorInfo(MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD, true, false);
    }

    public static AccessorInfo withReturnValueAndArguments() {
        return new AccessorInfo(MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER, true,
                true);
    }

    public static AccessorInfo of(MemberAccessorFactory.MemberAccessorType memberAccessorType) {
        return new AccessorInfo(memberAccessorType, memberAccessorType != MemberAccessorFactory.MemberAccessorType.VOID_METHOD,
                memberAccessorType == MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER);
    }
}
