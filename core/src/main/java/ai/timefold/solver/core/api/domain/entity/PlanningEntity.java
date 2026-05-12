package ai.timefold.solver.core.api.domain.entity;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Specifies that the class is a planning entity.
 * There are two types of entities:
 *
 * <dl>
 * <dt>Genuine entity</dt>
 * <dd>Must have at least 1 genuine {@link PlanningVariable planning variable},
 * and 0 or more shadow variables.</dd>
 * <dt>Shadow entity</dt>
 * <dd>Must have at least 1 shadow variable, and no genuine variables.</dd>
 * </dl>
 *
 * If a planning entity has neither a genuine nor a shadow variable,
 * it is not a planning entity and the solver will fail fast.
 *
 * <p>
 * Planning entities have special requirements on {@link Object#equals(Object)} and {@link Object#hashCode()}:
 *
 * <ul>
 * <li>If two different entities are equal as defined by {@link Object#equals(Object)},
 * they must represent the same entity.
 * The solver will treat them as one entity.</li>
 * <li>Entity {@link Object#equals(Object)} and {@link Object#hashCode()} must not depend on any planning variable.
 * The return value of these methods must not change when any planning variable changes.
 * It is recommended for entities to implement a unique {@link PlanningId},
 * and use that for equality comparisons.</li>
 * </ul>
 *
 * Failing to follow these requirements will cause undefined behavior in the solver,
 * ranging from explicit fail fasts to silently producing incorrect results.
 *
 * <p>
 * The class should have a public no-arg constructor, so it can be cloned
 * (unless the {@link PlanningSolution#solutionCloner()} is specified).
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface PlanningEntity {

    /**
     * Allows sorting a collection of planning entities for this variable.
     * Some algorithms perform better when the entities are sorted based on specific metrics.
     * <p>
     * The {@link Comparator} should sort the data in ascending order.
     * For example, prioritize three vehicles by sorting them based on their capacity:
     * Vehicle C (4 people), Vehicle A (6 people), Vehicle B (32 people)
     * <p>
     * Do not use together with {@link #comparatorFactoryClass()}.
     *
     * @return {@link PlanningVariable.NullComparator} when it is null (workaround for annotation limitation)
     * @see #comparatorFactoryClass()
     */
    Class<? extends Comparator> comparatorClass() default NullComparator.class;

    interface NullComparator<T> extends Comparator<T> {
    }

    /**
     * The {@link ComparatorFactory} alternative for {@link #comparatorClass()}.
     * <p>
     * Differs from {@link #comparatorClass()}
     * because it allows accessing the current solution when creating the comparator.
     * <p>
     * Do not use together with {@link #comparatorClass()}.
     *
     * @return {@link NullComparatorFactory} when it is null (workaround for annotation limitation)
     * @see #comparatorClass()
     */
    Class<? extends ComparatorFactory> comparatorFactoryClass() default NullComparatorFactory.class;

    interface NullComparatorFactory<Solution_, T> extends ComparatorFactory<Solution_, T> {
    }

}
