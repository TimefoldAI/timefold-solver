package ai.timefold.solver.core.api.domain.constraintweight;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;

/**
 * Specifies that a property (or a field) on a {@link PlanningSolution} class is a {@link ConstraintConfiguration}.
 * This property is automatically a {@link ProblemFactProperty} too, so no need to declare that explicitly.
 * <p>
 * The type of this property (or field) must have a {@link ConstraintConfiguration} annotation.
 *
 * @deprecated Use {@link ConstraintWeightOverrides} instead.
 */
@Deprecated(forRemoval = true, since = "1.13.0")
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface ConstraintConfigurationProvider {

}
