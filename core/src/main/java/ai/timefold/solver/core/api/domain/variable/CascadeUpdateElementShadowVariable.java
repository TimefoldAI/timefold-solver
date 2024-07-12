package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A listener automatically sourced on a {@link PreviousElementShadowVariable} and {@link InverseRelationShadowVariable}.
 * <p>
 * Change shadow variables when the previous or inverse relation elements changes.
 * <p>
 * Automatically cascades change events to {@link NextElementShadowVariable} of a {@link PlanningListVariable}.
 * <p>
 * Important: it must only change the shadow variable(s) for which it's configured!
 * It can be applied to multiple fields to modify different shadow variables.
 * It should never change a genuine variable or a problem fact.
 * It can change its shadow variable(s) on multiple entity instances
 * (for example: an arrivalTime change affects all trailing entities too).
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface CascadeUpdateElementShadowVariable {

    /**
     * The source method element.
     * 
     * @return method name of the source host element which will update the shadow variable
     */
    String sourceMethodName();
}
