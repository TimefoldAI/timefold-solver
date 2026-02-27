package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

/**
 * Specifies that a bean property (or a field) is a custom shadow variable of 1 or more source variables.
 * The source variable may be a genuine {@link PlanningVariable}, {@link PlanningListVariable},
 * or another shadow variable.
 * <p>
 * It is specified on a getter of a java bean property (or a field) of a {@link PlanningEntity} class.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface ShadowVariable {

    /**
     * If set, this {@link ShadowVariable} is a supplier variable,
     * and it is a name of a method annotated with {@link ShadowSources}
     * that computes the value of this {@link ShadowVariable}.
     *
     * @return the method that computes the value of this {@link ShadowVariable}.
     */
    String supplierName() default "";

}
