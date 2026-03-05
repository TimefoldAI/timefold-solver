package ai.timefold.solver.core.api.domain.specification;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;

/**
 * Describes how to clone a planning solution using lambdas.
 * <p>
 * The specification contains a complete cloning recipe: for every cloneable class (solution, entities,
 * {@code @DeepPlanningClone} facts), it lists all fields with their getter/setter lambdas and
 * a pre-classified {@link DeepCloneDecision} that determines how each field value is handled during cloning.
 *
 * @param solutionFactory creates a new empty solution instance
 * @param solutionProperties all fields on the solution class (including inherited), with clone decisions
 * @param cloneableClasses cloning recipes for entities and {@code @DeepPlanningClone} facts, keyed by exact class
 * @param entityClasses all entity classes (for runtime entity detection in cloneMap lookups)
 * @param deepCloneClasses all {@code @DeepPlanningClone}-annotated classes
 * @param customCloner optional custom cloner (null if using lambda-based cloning)
 * @param <S> the solution type
 */
public record CloningSpecification<S>(
        Supplier<S> solutionFactory,
        List<PropertyCopyDescriptor> solutionProperties,
        Map<Class<?>, CloneableClassDescriptor> cloneableClasses,
        Set<Class<?>> entityClasses,
        Set<Class<?>> deepCloneClasses,
        SolutionCloner<S> customCloner) {

    /**
     * Describes how to copy a single field during cloning.
     *
     * @param name the field name (for debugging)
     * @param getter reads the field value from an object
     * @param setter writes the field value to an object
     * @param deepCloneDecision how this field's value should be handled during cloning
     * @param cloneTimeValidationMessage if non-null, an error message to throw at clone time
     *        (used for deferred validation, e.g. {@code @DeepPlanningClone} on a {@code @PlanningVariable}
     *        whose type is not deep-cloneable)
     */
    public record PropertyCopyDescriptor(
            String name,
            Function<Object, Object> getter,
            BiConsumer<Object, Object> setter,
            DeepCloneDecision deepCloneDecision,
            String cloneTimeValidationMessage) {

        public PropertyCopyDescriptor(
                String name,
                Function<Object, Object> getter,
                BiConsumer<Object, Object> setter,
                DeepCloneDecision deepCloneDecision) {
            this(name, getter, setter, deepCloneDecision, null);
        }
    }

    /**
     * Pre-classified decision for how a field value is handled during cloning.
     * Determined at specification-build time so no runtime type inspection is needed.
     */
    public enum DeepCloneDecision {
        /** Immutable type: direct copy (no cloning needed). */
        SHALLOW,
        /** May be an entity or deep-clone type: resolve from cloneMap. */
        RESOLVE_ENTITY_REFERENCE,
        /** {@code @DeepPlanningClone} type: always deep clone. */
        ALWAYS_DEEP,
        /** Collection needing element resolution/deep-cloning. */
        DEEP_COLLECTION,
        /** Map needing key/value resolution/deep-cloning. */
        DEEP_MAP,
        /** Array needing element resolution/deep-cloning. */
        DEEP_ARRAY,
        /**
         * Non-immutable type where the field's declared type is not known to be deep-cloneable,
         * but the runtime value's class might be (e.g. a subclass annotated with {@code @DeepPlanningClone}).
         * At clone time: check if the value's actual class is deep-cloneable, and deep-clone if so.
         * Otherwise, shallow copy.
         */
        SHALLOW_OR_DEEP_BY_RUNTIME_TYPE
    }

    /**
     * Cloning recipe for a single cloneable class (entity or {@code @DeepPlanningClone} fact).
     *
     * @param clazz the class
     * @param factory creates a new empty instance (no-arg constructor)
     * @param properties all fields (including inherited), with clone decisions
     */
    public record CloneableClassDescriptor(
            Class<?> clazz,
            Supplier<Object> factory,
            List<PropertyCopyDescriptor> properties) {
    }
}
