package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

/**
 * Additional information for the GIZMO accessor generation.
 *
 * @param returnTypeRequired a flag that indicates if the return type is required or optional
 * @param readMethodWithParameter a flag that allows the read method to accept an argument
 */
public record AccessorInfo(boolean returnTypeRequired, boolean readMethodWithParameter) {

    public static AccessorInfo withReturnValueAndNoArguments() {
        return new AccessorInfo(true, false);
    }

    public static AccessorInfo withReturnValueAndArguments() {
        return new AccessorInfo(true, true);
    }

    public static AccessorInfo of(boolean returnTypeRequired, boolean readMethodWithParameter) {
        return new AccessorInfo(returnTypeRequired, readMethodWithParameter);
    }
}
