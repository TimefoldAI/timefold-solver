package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

/**
 * Specifies that field may be updated by the target method when one or more source variables change.
 * <p>
 * Automatically cascades change events to {@link NextElementShadowVariable} of a {@link PlanningListVariable}.
 * <p>
 * Important: it must only change the shadow variable(s) for which it's configured.
 * It is only possible to define either {@code sourceVariableName} or {@code sourceVariableNames}.
 * It can be applied to multiple fields to modify different shadow variables.
 * It should never change a genuine variable or a problem fact.
 * It can change its shadow variable(s) on multiple entity instances
 * (for example: an arrivalTime change affects all trailing entities too).
 */
@Target({ FIELD })
@Retention(RUNTIME)
@Repeatable(CascadingUpdateShadowVariable.List.class)
public @interface CascadingUpdateShadowVariable {

    /**
     * The source variable name.
     *
     * @return never null, a genuine or shadow variable name
     */
    String sourceVariableName() default "";

    /**
     * The source variable name.
     *
     * @return never null, a genuine or shadow variable name
     */
    String[] sourceVariableNames() default {};

    /**
     * The {@link PlanningEntity} class of the source variable.
     * <p>
     * Specified if the source variable is on a different {@link Class} than the class that uses this referencing annotation.
     *
     * @return {@link CascadingUpdateShadowVariable.NullEntityClass} when the attribute is omitted
     *         (workaround for annotation limitation).
     *         Defaults to the same {@link Class} as the one that uses this annotation.
     */
    Class<?> sourceEntityClass() default CascadingUpdateShadowVariable.NullEntityClass.class;

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

    /**
     * Defines several {@link ShadowVariable} annotations on the same element.
     */
    @Target({ FIELD })
    @Retention(RUNTIME)
    @interface List {

        CascadingUpdateShadowVariable[] value();
    }

    /** Workaround for annotation limitation in {@link #sourceEntityClass()}. */
    interface NullEntityClass {
    }
}
