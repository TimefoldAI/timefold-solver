package ai.timefold.solver.core.api.domain.entity;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

/**
 * Specifies that an {@code int} property (or field) of a {@link PlanningEntity} determines
 * how far a {@link PlanningListVariable} is pinned.
 * <p>
 * This annotation can only be specified on a field of the same entity,
 * which also specifies a {@link PlanningListVariable}.
 * The annotated int field has the following semantics:
 *
 * <ul>
 * <li>0: Pinning is disabled.
 * All the values in the list can be removed,
 * new values may be added anywhere in the list,
 * values in the list may be reordered.</li>
 * <li>Positive int: Values before this index in the list are pinned.
 * No value can be added at those indexes,
 * removed from them, or shuffled between them.
 * Values on or after this index are not pinned
 * and can be added, removed or shuffled freely.</li>
 * <li>Positive int that exceeds the lists size: fail fast.</li>
 * <li>Negative int: fail fast.</li>
 * </ul>
 *
 * To pin the entire list and disallow any changes, use {@link PlanningPin} instead.
 *
 * <p>
 * Example: Assuming a list of values {@code [A, B, C]}:
 *
 * <ul>
 * <li>0 or null allows the entire list to be modified.</li>
 * <li>1 pins {@code A}; rest of the list may be modified or added to.</li>
 * <li>2 pins {@code A, B}; rest of the list may be modified or added to.</li>
 * <li>3 pins {@code A, B, C}; the list can only be added to.</li>
 * <li>4 fails fast as there is no such index in the list.</li>
 * </ul>
 *
 * If the same entity also specifies a {@link PlanningPin} and the pin is enabled,
 * any value of {@link PlanningPinToIndex} is ignored.
 * In other words, enabling {@link PlanningPin} pins the entire list without exception.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface PlanningPinToIndex {

}
