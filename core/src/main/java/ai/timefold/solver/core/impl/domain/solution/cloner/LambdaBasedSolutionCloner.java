package ai.timefold.solver.core.impl.domain.solution.cloner;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.CloneableClassDescriptor;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.DeepCloneDecision;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.PropertyCopyDescriptor;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SolutionCloner} that uses pre-built lambda accessors from a {@link CloningSpecification}
 * to clone solutions without runtime reflection or {@code setAccessible}.
 * <p>
 * Uses a queue-based algorithm to handle
 * circular references and deferred deep-cloning.
 *
 * @param <S> the solution type
 */
public final class LambdaBasedSolutionCloner<S> implements SolutionCloner<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaBasedSolutionCloner.class);
    private static final int MINIMUM_EXPECTED_OBJECT_COUNT = 1_000;

    private final CloningSpecification<S> cloningSpec;
    private final Map<Class<?>, CloneableClassDescriptor> runtimeDescriptorCache = new ConcurrentHashMap<>();
    private final AtomicInteger expectedObjectCountRef = new AtomicInteger(MINIMUM_EXPECTED_OBJECT_COUNT);

    public LambdaBasedSolutionCloner(CloningSpecification<S> cloningSpec) {
        this.cloningSpec = cloningSpec;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull S cloneSolution(@NonNull S original) {
        var expectedObjectCount = expectedObjectCountRef.get();
        var cloneMap = new IdentityHashMap<Object, Object>(expectedObjectCount);
        var queue = new ArrayDeque<Deferred>(expectedObjectCount);

        // 1. Clone solution via factory
        var factory = cloningSpec.solutionFactory();
        S solutionClone;
        List<PropertyCopyDescriptor> solutionProperties;
        if (factory != null) {
            solutionClone = factory.get();
            solutionProperties = cloningSpec.solutionProperties();
        } else {
            // Interface or abstract solution class: use the runtime class to create the clone
            // and build properties from the concrete class
            solutionClone = createInstanceFromRuntimeClass(original);
            solutionProperties = buildRuntimeProperties(original.getClass());
        }
        cloneMap.put(original, solutionClone);

        // 2. Process all solution properties
        for (var prop : solutionProperties) {
            processCopy(original, solutionClone, prop, cloneMap, queue);
        }

        // 3. Drain queue: process deferred work
        drainQueue(queue, cloneMap);

        expectedObjectCountRef.updateAndGet(old -> decideNextExpectedObjectCount(old, cloneMap.size()));
        return solutionClone;
    }

    private void drainQueue(Queue<Deferred> queue, IdentityHashMap<Object, Object> cloneMap) {
        while (!queue.isEmpty()) {
            var deferred = queue.poll();
            switch (deferred) {
                case DeferredValueClone d -> {
                    var cloneValue = deepCloneValue(d.originalValue, cloneMap, queue);
                    d.setter.accept(d.bean, cloneValue);
                }
                case DeferredSingleProperty d ->
                    processCopy(d.original, d.clonedObject, d.prop, cloneMap, queue);
            }
        }
    }

    private static int decideNextExpectedObjectCount(int currentExpected, int actual) {
        var halfDiff = (int) Math.round(Math.abs(actual - currentExpected) / 2.0);
        if (actual > currentExpected) {
            return Math.min(currentExpected + halfDiff, Integer.MAX_VALUE);
        } else if (actual < currentExpected) {
            return Math.max(currentExpected - halfDiff, MINIMUM_EXPECTED_OBJECT_COUNT);
        }
        return currentExpected;
    }

    private void processCopy(Object original, Object clone, PropertyCopyDescriptor prop,
            IdentityHashMap<Object, Object> cloneMap, Queue<Deferred> queue) {
        // Clone-time validation (deferred from spec-build time)
        if (prop.cloneTimeValidationMessage() != null) {
            throw new IllegalStateException(prop.cloneTimeValidationMessage());
        }
        var value = prop.getter().apply(original);
        if (value == null) {
            prop.setter().accept(clone, null);
            return;
        }
        switch (prop.deepCloneDecision()) {
            case SHALLOW -> prop.setter().accept(clone, value);
            case RESOLVE_ENTITY_REFERENCE -> {
                var resolved = cloneMap.get(value);
                if (resolved != null) {
                    prop.setter().accept(clone, resolved);
                } else if (isDeepCloneable(value.getClass())) {
                    // Entity/deep-clone type not yet cloned — defer
                    queue.add(new DeferredValueClone(clone, prop.setter(), value));
                } else {
                    // Not a cloneable type — shallow copy
                    prop.setter().accept(clone, value);
                }
            }
            case SHALLOW_OR_DEEP_BY_RUNTIME_TYPE -> {
                // Check the VALUE's actual class at runtime (not the field's declared type).
                // This handles subclasses annotated with @DeepPlanningClone.
                if (isDeepCloneable(value.getClass())) {
                    var resolved = cloneMap.get(value);
                    if (resolved != null) {
                        prop.setter().accept(clone, resolved);
                    } else {
                        queue.add(new DeferredValueClone(clone, prop.setter(), value));
                    }
                } else {
                    prop.setter().accept(clone, value);
                }
            }
            case ALWAYS_DEEP -> queue.add(new DeferredValueClone(clone, prop.setter(), value));
            case DEEP_COLLECTION -> {
                var clonedCollection = cloneCollection(value, cloneMap, queue);
                prop.setter().accept(clone, clonedCollection);
            }
            case DEEP_MAP -> {
                var clonedMap = cloneMap(value, cloneMap, queue);
                prop.setter().accept(clone, clonedMap);
            }
            case DEEP_ARRAY -> {
                var clonedArray = cloneArray(value, cloneMap, queue);
                prop.setter().accept(clone, clonedArray);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object deepCloneValue(Object originalValue, IdentityHashMap<Object, Object> cloneMap,
            Queue<Deferred> queue) {
        if (originalValue == null) {
            return null;
        }
        // Already cloned?
        var existing = cloneMap.get(originalValue);
        if (existing != null) {
            return existing;
        }
        // Collection/Map/Array
        if (originalValue instanceof Collection<?>) {
            return cloneCollection(originalValue, cloneMap, queue);
        } else if (originalValue instanceof Map<?, ?>) {
            return cloneMap(originalValue, cloneMap, queue);
        } else if (originalValue.getClass().isArray()) {
            return cloneArray(originalValue, cloneMap, queue);
        }
        // Object: look up descriptor
        return deepCloneObject(originalValue, cloneMap, queue);
    }

    private Object deepCloneObject(Object original, IdentityHashMap<Object, Object> cloneMap,
            Queue<Deferred> queue) {
        var existing = cloneMap.get(original);
        if (existing != null) {
            return existing;
        }
        var descriptor = findDescriptor(original.getClass());
        if (descriptor == null) {
            // Unknown type — return as-is (e.g., a problem fact not annotated with @DeepPlanningClone)
            return original;
        }
        var factory = descriptor.factory();
        var clone = factory != null ? factory.get() : createInstanceFromRuntimeClass(original);
        cloneMap.put(original, clone);

        for (var prop : descriptor.properties()) {
            processCopy(original, clone, prop, cloneMap, queue);
        }
        return clone;
    }

    private CloneableClassDescriptor findDescriptor(Class<?> clazz) {
        var descriptor = cloningSpec.cloneableClasses().get(clazz);
        if (descriptor != null) {
            return descriptor;
        }
        // Check runtime cache (for dynamically built descriptors)
        descriptor = runtimeDescriptorCache.get(clazz);
        if (descriptor != null) {
            return descriptor;
        }
        // Walk superclass chain for entity subclasses
        var current = clazz.getSuperclass();
        while (current != null && current != Object.class) {
            descriptor = cloningSpec.cloneableClasses().get(current);
            if (descriptor != null) {
                LOGGER.debug("Found cloneable class descriptor for {} via superclass {}.",
                        clazz.getSimpleName(), current.getSimpleName());
                return descriptor;
            }
            current = current.getSuperclass();
        }
        // Check implemented interfaces (for interface-based entity declarations)
        for (var iface : clazz.getInterfaces()) {
            descriptor = cloningSpec.cloneableClasses().get(iface);
            if (descriptor != null) {
                // Interface found — build a runtime descriptor for the concrete class
                // since the interface descriptor has no fields
                descriptor = buildRuntimeDescriptor(clazz);
                runtimeDescriptorCache.put(clazz, descriptor);
                return descriptor;
            }
        }
        return null;
    }

    private boolean isDeepCloneable(Class<?> clazz) {
        if (cloningSpec.entityClasses().contains(clazz) || cloningSpec.deepCloneClasses().contains(clazz)) {
            return true;
        }
        // Check @DeepPlanningClone annotation directly (for runtime subclass types not discovered at build time)
        if (clazz.isAnnotationPresent(DeepPlanningClone.class)) {
            return true;
        }
        // Check superclass chain
        var current = clazz.getSuperclass();
        while (current != null && current != Object.class) {
            if (cloningSpec.entityClasses().contains(current) || cloningSpec.deepCloneClasses().contains(current)) {
                return true;
            }
            current = current.getSuperclass();
        }
        // Check implemented interfaces
        for (var iface : clazz.getInterfaces()) {
            if (cloningSpec.entityClasses().contains(iface) || cloningSpec.deepCloneClasses().contains(iface)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createInstanceFromRuntimeClass(T original) {
        try {
            var ctor = original.getClass().getDeclaredConstructor();
            ctor.setAccessible(true);
            return (T) ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create a planning clone of %s. The class must have a no-arg constructor."
                            .formatted(original.getClass().getCanonicalName()),
                    e);
        }
    }

    /**
     * Builds a {@link CloneableClassDescriptor} for a concrete runtime class not known at spec-build time.
     * This handles interface/abstract entity classes whose concrete implementations are discovered at clone time.
     */
    private CloneableClassDescriptor buildRuntimeDescriptor(Class<?> clazz) {
        var properties = buildRuntimeProperties(clazz);
        return new CloneableClassDescriptor(clazz,
                () -> {
                    try {
                        var ctor = clazz.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        return ctor.newInstance();
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "Failed to create instance of %s.".formatted(clazz.getCanonicalName()), e);
                    }
                },
                properties);
    }

    /**
     * Builds property copy descriptors for a class using reflection.
     * Used at runtime for classes not known at spec-build time (interface implementations).
     */
    private List<PropertyCopyDescriptor> buildRuntimeProperties(Class<?> clazz) {
        var properties = new ArrayList<PropertyCopyDescriptor>();
        for (var current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (var field : current.getDeclaredFields()) {
                var modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                field.setAccessible(true);
                Function<Object, Object> getter = bean -> {
                    try {
                        return field.get(bean);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                };
                BiConsumer<Object, Object> setter;
                if (Modifier.isFinal(modifiers)) {
                    setter = (bean, value) -> {
                        throw new IllegalStateException(
                                "Cannot set final field %s on %s.".formatted(field.getName(), clazz.getSimpleName()));
                    };
                } else {
                    setter = (bean, value) -> {
                        try {
                            field.set(bean, value);
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    };
                }
                var decision = classifyFieldAtRuntime(field);
                properties.add(new PropertyCopyDescriptor(field.getName(), getter, setter, decision, null));
            }
        }
        return List.copyOf(properties);
    }

    /**
     * Classifies a field for deep clone decision at runtime.
     */
    private DeepCloneDecision classifyFieldAtRuntime(Field field) {
        var fieldType = field.getType();
        if (DeepCloningUtils.isImmutable(fieldType)) {
            return DeepCloneDecision.SHALLOW;
        }
        if (field.isAnnotationPresent(DeepPlanningClone.class)) {
            if (Collection.class.isAssignableFrom(fieldType)) {
                return DeepCloneDecision.DEEP_COLLECTION;
            } else if (Map.class.isAssignableFrom(fieldType)) {
                return DeepCloneDecision.DEEP_MAP;
            } else if (fieldType.isArray()) {
                return DeepCloneDecision.DEEP_ARRAY;
            }
            return DeepCloneDecision.ALWAYS_DEEP;
        }
        if (isDeepCloneable(fieldType)) {
            return DeepCloneDecision.RESOLVE_ENTITY_REFERENCE;
        }
        if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) {
            // Conservatively deep-clone collections/maps — they might contain entities
            return Collection.class.isAssignableFrom(fieldType)
                    ? DeepCloneDecision.DEEP_COLLECTION
                    : DeepCloneDecision.DEEP_MAP;
        }
        if (fieldType.isArray() && !fieldType.getComponentType().isPrimitive()) {
            return DeepCloneDecision.DEEP_ARRAY;
        }
        return DeepCloneDecision.SHALLOW;
    }

    // ************************************************************************
    // Collection/Map/Array cloning
    // ************************************************************************

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object cloneCollection(Object originalValue, IdentityHashMap<Object, Object> cloneMap,
            Queue<Deferred> queue) {
        var originalCollection = (Collection<Object>) originalValue;
        if (originalCollection instanceof EnumSet enumSet) {
            return EnumSet.copyOf(enumSet);
        }
        var cloneCollection = constructCloneCollection(originalCollection);
        if (originalCollection.isEmpty()) {
            return cloneCollection;
        }
        for (var element : originalCollection) {
            cloneCollection.add(resolveElement(element, cloneMap, queue));
        }
        return cloneCollection;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object cloneMap(Object originalValue, IdentityHashMap<Object, Object> cloneMap,
            Queue<Deferred> queue) {
        var originalMap = (Map<Object, Object>) originalValue;
        if (originalMap instanceof EnumMap enumMap) {
            var cloneEnumMap = new EnumMap(enumMap);
            if (!cloneEnumMap.isEmpty()) {
                for (var entry : ((Map<Object, Object>) cloneEnumMap).entrySet()) {
                    entry.setValue(resolveElement(entry.getValue(), cloneMap, queue));
                }
            }
            return cloneEnumMap;
        }
        var cloneMapInstance = constructCloneMap(originalMap);
        if (originalMap.isEmpty()) {
            return cloneMapInstance;
        }
        for (var entry : originalMap.entrySet()) {
            cloneMapInstance.put(
                    resolveElement(entry.getKey(), cloneMap, queue),
                    resolveElement(entry.getValue(), cloneMap, queue));
        }
        return cloneMapInstance;
    }

    private Object cloneArray(Object originalArray, IdentityHashMap<Object, Object> cloneMap,
            Queue<Deferred> queue) {
        var length = Array.getLength(originalArray);
        if (length == 0) {
            return originalArray;
        }
        var cloneArray = Array.newInstance(originalArray.getClass().getComponentType(), length);
        for (var i = 0; i < length; i++) {
            Array.set(cloneArray, i, resolveElement(Array.get(originalArray, i), cloneMap, queue));
        }
        return cloneArray;
    }

    /**
     * Resolves an element within a collection/map/array.
     * <p>
     * For deep-cloneable entities, creates the clone eagerly (for the collection reference)
     * but defers property processing to the queue (to avoid unbounded recursion).
     */
    @SuppressWarnings("unchecked")
    private Object resolveElement(Object element, IdentityHashMap<Object, Object> cloneMap,
            Queue<Deferred> queue) {
        if (element == null) {
            return null;
        }
        // Nested collection/map/array
        if (element instanceof Collection<?>) {
            return cloneCollection(element, cloneMap, queue);
        } else if (element instanceof Map<?, ?>) {
            return cloneMap(element, cloneMap, queue);
        } else if (element.getClass().isArray()) {
            return cloneArray(element, cloneMap, queue);
        }
        // Already cloned?
        var existing = cloneMap.get(element);
        if (existing != null) {
            return existing;
        }
        // Deep-cloneable? Create clone eagerly but defer property processing
        if (isDeepCloneable(element.getClass())) {
            return registerAndDeferProperties(element, cloneMap, queue);
        }
        return element;
    }

    /**
     * Creates a clone of an object and registers it in the cloneMap.
     * Shallow fields are copied immediately (so comparators in TreeSets work),
     * but deep-clone fields are deferred to the queue to avoid unbounded recursion.
     */
    private Object registerAndDeferProperties(Object original, IdentityHashMap<Object, Object> cloneMap,
            Queue<Deferred> queue) {
        var descriptor = findDescriptor(original.getClass());
        if (descriptor == null) {
            return original;
        }
        var factory = descriptor.factory();
        var clone = factory != null ? factory.get() : createInstanceFromRuntimeClass(original);
        cloneMap.put(original, clone);
        for (var prop : descriptor.properties()) {
            if (prop.deepCloneDecision() == CloningSpecification.DeepCloneDecision.SHALLOW) {
                // Copy shallow fields immediately (needed for TreeSet comparators, etc.)
                var value = prop.getter().apply(original);
                prop.setter().accept(clone, value);
            } else {
                // Defer deep-clone properties to the queue
                queue.add(new DeferredSingleProperty(original, clone, prop));
            }
        }
        return clone;
    }

    // ************************************************************************
    // Deferred work types
    // ************************************************************************

    private sealed interface Deferred permits DeferredValueClone, DeferredSingleProperty {
    }

    /**
     * Deep-clone a value and set it on a bean.
     */
    private record DeferredValueClone(
            Object bean,
            BiConsumer<Object, Object> setter,
            Object originalValue) implements Deferred {
    }

    /**
     * Process a single property copy from an original object to its clone.
     * Used when entity cloning is deferred from within collection resolution
     * (shallow fields are already copied; only deep-clone fields are deferred).
     */
    private record DeferredSingleProperty(
            Object original,
            Object clonedObject,
            PropertyCopyDescriptor prop) implements Deferred {
    }

    // ************************************************************************
    // Collection/Map construction helpers
    // ************************************************************************

    @SuppressWarnings("unchecked")
    static <E> Collection<E> constructCloneCollection(Collection<E> originalCollection) {
        if (originalCollection instanceof LinkedList) {
            return new LinkedList<>();
        }
        var size = originalCollection.size();
        if (originalCollection instanceof Set) {
            if (originalCollection instanceof SortedSet<E> set) {
                var setComparator = set.comparator();
                return new TreeSet<>(setComparator);
            } else if (!(originalCollection instanceof LinkedHashSet)) {
                return HashSet.newHashSet(size);
            } else {
                return LinkedHashSet.newLinkedHashSet(size);
            }
        } else if (originalCollection instanceof Deque) {
            return new ArrayDeque<>(size);
        }
        return new ArrayList<>(size);
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> constructCloneMap(Map<K, V> originalMap) {
        if (originalMap instanceof SortedMap<K, V> map) {
            var setComparator = map.comparator();
            return new TreeMap<>(setComparator);
        }
        var originalMapSize = originalMap.size();
        if (!(originalMap instanceof LinkedHashMap<?, ?>)) {
            return HashMap.newHashMap(originalMapSize);
        } else {
            return LinkedHashMap.newLinkedHashMap(originalMapSize);
        }
    }
}
