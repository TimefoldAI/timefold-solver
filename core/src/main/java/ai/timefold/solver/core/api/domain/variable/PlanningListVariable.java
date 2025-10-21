package ai.timefold.solver.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.SorterWeightFactory;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;

/**
 * Specifies that a bean property (or a field) can be changed and should be optimized by the optimization algorithms.
 * It is specified on a getter of a java bean property (or directly on a field) of a {@link PlanningEntity} class.
 * The type of the {@link PlanningListVariable} annotated bean property (or a field) must be {@link List}.
 *
 * <h2>List variable</h2>
 * <p>
 * A planning entity's property annotated with {@code @PlanningListVariable} is referred to as a <strong>list variable</strong>.
 * The way solver optimizes a list variable is by adding, removing, or changing order of elements in the {@code List} object
 * held by the list variable.
 *
 * <h2>Disjoint lists</h2>
 * <p>
 * Furthermore, the current implementation works under the assumption that the list variables of all entity instances
 * are "disjoint lists":
 * <ul>
 * <li><strong>List</strong> means that the order of elements inside a list planning variable is significant.</li>
 * <li><strong>Disjoint</strong> means that any given pair of entities have no common elements in their list variables.
 * In other words, each element from the list variable's value range appears in exactly one entity's list variable.</li>
 * </ul>
 *
 * <p>
 * This makes sense for common use cases, for example the Vehicle Routing Problem or Task Assigning. In both cases
 * the <em>order</em> in which customers are visited and tasks are being worked on matters. Also, each customer
 * must be visited <em>once</em> and each task must be completed by <em>exactly one</em> employee.
 *
 * @see PlanningPin
 * @see PlanningPinToIndex
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface PlanningListVariable {

    /**
     * If set to false (default), all elements must be assigned to some list.
     * If set to true, elements may be left unassigned.
     * 
     * @see PlanningVariable#allowsUnassigned() Basic planning value equivalent.
     */
    boolean allowsUnassignedValues() default false;

    String[] valueRangeProviderRefs() default {};

    /**
     * Allows a collection of planning values for this variable to be sorted by strength.
     * A strengthWeight estimates how strong a planning value is.
     * Some algorithms benefit from planning on weaker planning values first or from focusing on them.
     * <p>
     * The {@link Comparator} should sort in ascending strength.
     * For example: sorting 3 computers on strength based on their RAM capacity:
     * Computer B (1GB RAM), Computer A (2GB RAM), Computer C (7GB RAM),
     * <p>
     * Do not use together with {@link #strengthWeightFactoryClass()}.
     *
     * @return {@link PlanningVariable.NullStrengthComparator} when it is null (workaround for annotation limitation)
     * @see #strengthWeightFactoryClass()
     */
    Class<? extends Comparator> strengthComparatorClass() default PlanningVariable.NullStrengthComparator.class;

    /** Workaround for annotation limitation in {@link #strengthComparatorClass()}. */
    interface NullStrengthComparator extends Comparator {
    }

    /**
     * The {@link SorterWeightFactory} alternative for {@link #strengthComparatorClass()}.
     * <p>
     * Do not use together with {@link #strengthComparatorClass()}.
     *
     * @return {@link PlanningVariable.NullStrengthWeightFactory} when it is null (workaround for annotation limitation)
     * @see #strengthComparatorClass()
     */
    Class<? extends SorterWeightFactory> strengthWeightFactoryClass() default PlanningVariable.NullStrengthWeightFactory.class;

    /** Workaround for annotation limitation in {@link #strengthWeightFactoryClass()}. */
    interface NullStrengthWeightFactory<Solution_, T> extends SorterWeightFactory<Solution_, T> {
    }
}
