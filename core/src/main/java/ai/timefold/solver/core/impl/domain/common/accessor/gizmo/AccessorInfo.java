package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType;

/**
 * Additional information for the GIZMO accessor generation.
 *
 * @param returnTypeRequired a flag that indicates if the return type is required or optional
 * @param readMethodWithParameter a flag that allows the read method to accept an argument
 */
public record AccessorInfo(MemberAccessorType memberAccessorType, boolean returnTypeRequired,
        boolean readMethodWithParameter) {

    public static AccessorInfo withReturnValueAndNoArguments() {
        return new AccessorInfo(MemberAccessorType.FIELD_OR_READ_METHOD, true, false);
    }

    public static AccessorInfo withReturnValueAndArguments() {
        return new AccessorInfo(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER, true,
                true);
    }

    public static AccessorInfo of(MemberAccessorType memberAccessorType) {
        return new AccessorInfo(memberAccessorType, memberAccessorType != MemberAccessorType.VOID_METHOD,
                memberAccessorType == MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER);
    }
}
