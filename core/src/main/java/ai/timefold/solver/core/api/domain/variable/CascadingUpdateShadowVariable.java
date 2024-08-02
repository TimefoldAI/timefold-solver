package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies that field may be updated by the target method when a dependency changes.
 * <p>
 * Automatically cascades change events to the next elements of a {@link PlanningListVariable}.
 * <p>
 * Important: it must only change the shadow variable(s) for which it's configured.
 * It can be applied to multiple fields to modify different shadow variables.
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
