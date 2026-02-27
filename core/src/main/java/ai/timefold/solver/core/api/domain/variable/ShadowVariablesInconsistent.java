package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.stream.Constraint;

/**
 * Specifies that a boolean property (or field) of a {@link PlanningEntity}
 * tracks if any of its {@link ShadowVariable#supplierName() supplier variables}
 * are inconsistent.
 * <p>
 * A supplier variable is inconsistent if:
 * <ul>
 * <li>
 * One of its source variables include it as a source (for example,
 * `a` depends on `b` and `b` depends on `a`).
 * </li>
 * <li>
 * One of its source variables is inconsistent (for example,
 * `c` depends on `a`, which depends on `b`, and `b` depends on `a`).
 * </li>
 * </ul>
 * <p>
 * Should be used in a filter for a hard {@link Constraint} to penalize
 * inconsistent entities, since {@link PlanningSolution} with inconsistent entities are
 * typically not valid.
 * <p>
 * There are three ways an inconsistency may be introduced:
 *
 * <ul>
 * <li>
 * Source-induced, when two declarative shadow variables' sources refer to each other:
 * 
 * <pre>
 * &#64;PlanningEntity
 * public class Entity {
 *     &#64;ShadowVariable(supplierName = "variable1Supplier")
 *     String variable1;
 *
 *     &#64;ShadowVariable(supplierName = "variable2Supplier")
 *     String variable2;
 *
 *     // ...
 *
 *     &#64;ShadowSources("variable2")
 *     String variable1Supplier() {
 *         // ...
 *     }
 *
 *     &#64;ShadowSources("variable1")
 *     String variable2Supplier() {
 *         // ...
 *     }
 * }
 * </pre>
 * 
 * </li>
 * 
 * <li>
 * Fact-induced, when a shadow variable has itself as a direct or transitive dependency via a fact:
 * 
 * <pre>
 * &#64;PlanningEntity
 * public class Entity {
 *     Entity dependency;
 *
 *     &#64;ShadowVariable(supplierName = "variableSupplier")
 *     String variable;
 *
 *     &#64;ShadowSources("dependency.variable")
 *     String variableSupplier() {
 *         // ...
 *     }
 *     // ...
 * }
 *
 * Entity a = new Entity();
 * Entity b = new Entity();
 * a.setDependency(b);
 * b.setDependency(a);
 * // a depends on b, and b depends on a, which is invalid.
 * </pre>
 * 
 * </li>
 * 
 * <li>
 * Variable-induced, when a shadow variable has itself as a direct or transitive dependency via a variable:
 * 
 * <pre>
 * &#64;PlanningEntity
 * public class Entity {
 *     Entity dependency;
 *
 *     &#64;PreviousElementShadowVariable()
 *     Entity previous;
 *
 *     &#64;ShadowVariable(supplierName = "variableSupplier")
 *     String variable;
 *
 *     &#64;ShadowSources({ "previous.variable", "dependency.variable" })
 *     String variableSupplier() {
 *         // ...
 *     }
 *     // ...
 * }
 *
 * Entity a = new Entity();
 * Entity b = new Entity();
 * b.setDependency(a);
 * a.setPrevious(b);
 * // b depends on a via a fact, and a depends on b via a variable
 * // The solver can break this loop by moving a after b.
 * </pre>
 * 
 * </li>
 * </ul>
 * Source-induced and fact-induced loops cannot be broken by the solver,
 * and represents an issue in either the input problem or the domain model.
 * The solver will fail-fast if it detects a source-induced or fact-induced loop.
 * <p>
 * Important:
 * Do not use a {@link ShadowVariablesInconsistent} property in a method annotated with {@link ShadowSources}.
 * {@link ShadowSources} marked methods do not need to check {@link ShadowVariablesInconsistent} properties,
 * since they are only called if all their dependencies are consistent.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface ShadowVariablesInconsistent {
}
