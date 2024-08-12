package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies that a field may be updated by the target method when any of its variables change, genuine or shadow.
 * <p>
 * Automatically cascades change events to the subsequent elements of a {@link PlanningListVariable}.
 * <p>
 * A single listener is created
 * to execute user-defined logic from the {@code targetMethod} after all variable changes have been applied.
 * This means it will be the last step executed during the event lifecycle.
 * <p>
 * It can be applied in multiple fields to update various shadow variables.
 * The user's logic is responsible for defining the order in which each variable is updated.
 * <p>
 * Distinct {@code targetMethod} can be defined, but there is no guarantee about the order in which they are executed.
 * Therefore, caution is required when using multiple {@code targetMethod} per model.
 * <p>
 * Except for {@link PiggybackShadowVariable},
 * the use of {@link CascadingUpdateShadowVariable} as a source for other variables,
 * such as {@link ShadowVariable}, is not allowed.
 * <p>
 * Important: it must only change the shadow variable(s) for which it's configured.
 * It should never change a genuine variable or a problem fact.
 * It can change its shadow variable(s) on multiple entity instances
 * (for example: an arrivalTime change affects all trailing entities too).
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface CascadingUpdateShadowVariable {

    /**
     * The target method element.
     * <p>
     * Important: the method must be non-static and should not include any parameters.
     * There are no restrictions regarding the method visibility.
     * There is no restriction on the method's return type,
     * but if it returns a value, it will be ignored and will not impact the listener's execution.
     * 
     * @return method name of the source host element which will update the shadow variable
     */
    String targetMethodName();
}
