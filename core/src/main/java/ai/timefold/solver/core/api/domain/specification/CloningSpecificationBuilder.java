package ai.timefold.solver.core.api.domain.specification;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder for configuring lambda-based solution cloning.
 * <p>
 * This builder allows users to define a complete cloning recipe: for the solution class,
 * every entity class, and any {@code @DeepPlanningClone} fact classes, all fields are declared
 * with their getter/setter lambdas and a {@link CloningSpecification.DeepCloneDecision}.
 *
 * @param <S> the solution type
 */
public interface CloningSpecificationBuilder<S> {

    /**
     * Sets the factory that creates new empty solution instances.
     */
    CloningSpecificationBuilder<S> solutionFactory(Supplier<S> factory);

    /**
     * Declares a shallow-copy property on the solution class.
     */
    <V> CloningSpecificationBuilder<S> solutionProperty(String name, Function<S, V> getter, BiConsumer<S, V> setter);

    /**
     * Declares a property on the solution class with an explicit deep clone decision.
     */
    <V> CloningSpecificationBuilder<S> solutionProperty(String name, Function<S, V> getter, BiConsumer<S, V> setter,
            CloningSpecification.DeepCloneDecision decision);

    /**
     * Registers an entity class with its no-arg constructor and property definitions.
     */
    <E> CloningSpecificationBuilder<S> entityClass(Class<E> entityClass, Supplier<E> factory,
            Consumer<CloneableClassBuilder<E>> config);

    /**
     * Registers an entity class with its no-arg constructor, without additional property definitions.
     */
    default <E> CloningSpecificationBuilder<S> entityFactory(Class<E> entityClass, Supplier<E> factory) {
        return entityClass(entityClass, factory, e -> {
        });
    }

    /**
     * Registers a {@code @DeepPlanningClone} fact class with its no-arg constructor and property definitions.
     */
    <E> CloningSpecificationBuilder<S> deepCloneFact(Class<E> factClass, Supplier<E> factory,
            Consumer<CloneableClassBuilder<E>> config);

    /**
     * Builder for defining properties on a cloneable class (entity or deep-clone fact).
     *
     * @param <E> the class type
     */
    interface CloneableClassBuilder<E> {

        /**
         * Declares a shallow-copy property.
         */
        <V> CloneableClassBuilder<E> shallowProperty(String name, Function<E, V> getter, BiConsumer<E, V> setter);

        /**
         * Declares a property that may reference an entity (resolved from cloneMap).
         */
        <V> CloneableClassBuilder<E> entityRefProperty(String name, Function<E, V> getter, BiConsumer<E, V> setter);

        /**
         * Declares a property that should always be deep-cloned.
         */
        <V> CloneableClassBuilder<E> deepProperty(String name, Function<E, V> getter, BiConsumer<E, V> setter);

        /**
         * Declares a collection property that needs element resolution/deep-cloning.
         */
        <V> CloneableClassBuilder<E> deepCollectionProperty(String name, Function<E, V> getter,
                BiConsumer<E, V> setter);

        /**
         * Declares a map property that needs key/value resolution/deep-cloning.
         */
        <V> CloneableClassBuilder<E> deepMapProperty(String name, Function<E, V> getter, BiConsumer<E, V> setter);

        /**
         * Declares an array property that needs element resolution/deep-cloning.
         */
        <V> CloneableClassBuilder<E> deepArrayProperty(String name, Function<E, V> getter, BiConsumer<E, V> setter);

        /**
         * Declares a property with an explicit deep clone decision.
         */
        <V> CloneableClassBuilder<E> property(String name, Function<E, V> getter, BiConsumer<E, V> setter,
                CloningSpecification.DeepCloneDecision decision);
    }
}
