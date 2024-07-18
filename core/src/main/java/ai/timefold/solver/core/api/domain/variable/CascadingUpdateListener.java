package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies that field may be updated by the target method when one or more source variables change.
 * <p>
 * Automatically cascades change events to {@link NextElementShadowVariable} of a {@link PlanningListVariable}.
 * <p>
 * Important: it must only change the shadow variable(s) for which it's configured.
 * It can be applied to multiple fields to modify different shadow variables.
 * It should never change a genuine variable or a problem fact.
 * It can change its shadow variable(s) on multiple entity instances
 * (for example: an arrivalTime change affects all trailing entities too).
 */
@Target({ FIELD })
@Retention(RUNTIME)
@Repeatable(CascadingUpdateListener.List.class)
public @interface CascadingUpdateListener {

    /**
     * The target method element.
     * 
     * @return method name of the source host element which will update the shadow variable
     */
    String targetMethodName();

    /**
     * The source variable name.
     *
     * @return never null, a genuine or shadow variable name
     */
    String sourceVariableName();

    /**
     * Defines several {@link ShadowVariable} annotations on the same element.
     */
    @Target({ FIELD })
    @Retention(RUNTIME)
    @interface List {

        CascadingUpdateListener[] value();
    }
}
