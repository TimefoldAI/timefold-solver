package ai.timefold.solver.core.preview.api.domain.variable.declarative;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.stream.Constraint;

/**
 * Specifies that a boolean property (or field) of a {@link PlanningEntity}
 * tracks if any of its {@link ShadowVariable#supplierName() supplier variables}
 * are looped.
 * <p>
 * A supplier variable is looped if:
 * <ul>
 * <li>
 * One of its source variables include it as a source (for example,
 * `a` depends on `b` and `b` depends on `a`).
 * </li>
 * <li>
 * One of its source variables is looped (for example,
 * `c` depends on `a`, which depends on `b`, and `b` depends on `a`).
 * </li>
 * </ul>
 * <p>
 * Should be used in a filter for a hard {@link Constraint} to penalize
 * looped entities, since {@link PlanningSolution} with looped entities are
 * typically not valid.
 * <p>
 * Important:
 * Do not use a {@link ShadowVariableLooped} property in a method annotated
 * with {@link ShadowSources}. {@link ShadowVariableLooped} properties can
 * be updated after the {@link ShadowSources} marked method is called, causing
 * score corruption. {@link ShadowSources} marked methods do not need to check
 * {@link ShadowVariableLooped} properties, since they are only called if all
 * their dependencies are not looped.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface ShadowVariableLooped {
}
