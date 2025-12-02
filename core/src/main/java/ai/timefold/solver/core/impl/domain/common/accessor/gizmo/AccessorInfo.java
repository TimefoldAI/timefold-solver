package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

/**
 * Additional information for the GIZMO accessor generation.
 *
 * @param returnTypeRequired a flag that indicates if the return type is required or optional
 * @param getterWithParameter a flag that allows the getter method to accept an argument
 */
public record AccessorInfo(boolean returnTypeRequired, boolean getterWithParameter) {

    public static AccessorInfo of(boolean returnTypeRequired, boolean getterWithParameter) {
        return new AccessorInfo(returnTypeRequired, getterWithParameter);
    }
}
