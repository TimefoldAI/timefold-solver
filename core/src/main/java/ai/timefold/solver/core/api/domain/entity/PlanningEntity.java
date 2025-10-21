package ai.timefold.solver.core.api.domain.entity;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
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
 * The class should have a public no-arg constructor, so it can be cloned
 * (unless the {@link PlanningSolution#solutionCloner()} is specified).
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface PlanningEntity {

    /**
     * A pinned planning entity is never changed during planning,
     * this is useful in repeated planning use cases (such as continuous planning and real-time planning).
     * This applies to all the planning variables of this planning entity.
     * <p>
     * The method {@link PinningFilter#accept(Object, Object)} returns false if the selection entity is pinned
     * and it returns true if the selection entity is movable
     *
     * @return {@link NullPinningFilter} when it is null (workaround for annotation limitation)
     * @deprecated Prefer using {@link PlanningPin}.
     */
    @Deprecated(forRemoval = true, since = "1.23.0")
    Class<? extends PinningFilter> pinningFilter() default NullPinningFilter.class;

    /**
     * Workaround for annotation limitation in {@link #pinningFilter()}.
     * 
     * @deprecated Prefer using {@link PlanningPin}.
     */
    @Deprecated(forRemoval = true, since = "1.23.0")
    interface NullPinningFilter extends PinningFilter {
    }

    /**
     * Allows a collection of planning entities to be sorted by difficulty.
     * A difficultyWeight estimates how hard is to plan a certain PlanningEntity.
     * Some algorithms benefit from planning on more difficult planning entities first/last or from focusing on them.
     * <p>
     * The {@link Comparator} should sort in ascending difficulty
     * (even though many optimization algorithms will reverse it).
     * For example: sorting 3 processes on difficultly based on their RAM usage requirement:
     * Process B (1GB RAM), Process A (2GB RAM), Process C (7GB RAM),
     * <p>
     * Do not use together with {@link #difficultyWeightFactoryClass()}.
     *
     * @deprecated Deprecated in favor of {@link #comparatorClass()}.
     *
     * @return {@link NullDifficultyComparator} when it is null (workaround for annotation limitation)
     * @see #difficultyWeightFactoryClass()
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    Class<? extends Comparator> difficultyComparatorClass() default NullDifficultyComparator.class;

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

    /**
     * Workaround for annotation limitation in {@link #difficultyComparatorClass()}.
     *
     * @deprecated Deprecated in favor of {@link NullComparator}.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    interface NullDifficultyComparator<T> extends NullComparator<T> {
    }

    interface NullComparator<T> extends Comparator<T> {
    }

    /**
     * The {@link ComparatorFactory} alternative for {@link #difficultyComparatorClass()}.
     * <p>
     * Do not use together with {@link #difficultyComparatorClass()}.
     *
     * @deprecated Deprecated in favor of {@link #comparatorFactoryClass()}.
     *
     * @return {@link NullDifficultyWeightFactory} when it is null (workaround for annotation limitation)
     * @see #difficultyComparatorClass()
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    Class<? extends ComparatorFactory> difficultyWeightFactoryClass() default NullDifficultyWeightFactory.class;

    /**
     * The {@link ComparatorFactory} alternative for {@link #comparatorClass()}.
     * <p>
     * Do not use together with {@link #comparatorClass()}.
     *
     * @return {@link NullComparatorFactory} when it is null (workaround for annotation limitation)
     * @see #comparatorClass()
     */
    Class<? extends ComparatorFactory> comparatorFactoryClass() default NullComparatorFactory.class;

    /**
     * Workaround for annotation limitation in {@link #difficultyWeightFactoryClass()}.
     *
     * @deprecated Deprecated in favor of {@link NullComparatorFactory}.
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    interface NullDifficultyWeightFactory<Solution_, T> extends NullComparatorFactory<Solution_, T> {
    }

    interface NullComparatorFactory<Solution_, T> extends ComparatorFactory<Solution_, T> {
    }

}
