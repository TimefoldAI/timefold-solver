package ai.timefold.solver.core.impl.domain.solution.cloner;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.ConcurrentMemoization;

import org.jspecify.annotations.NonNull;

/**
 * This class is thread-safe; score directors from the same solution descriptor will share the same instance.
 */
public final class FieldAccessingSolutionCloner<Solution_> implements SolutionCloner<Solution_> {

    private static final int MINIMUM_EXPECTED_OBJECT_COUNT = 1_000;

    private final Map<Class<?>, ClassMetadata> classMetadataMemoization = new ConcurrentMemoization<>();
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    // Exists to avoid creating a new lambda instance on every call to map.computeIfAbsent.
    private final Function<Class<?>, ClassMetadata> classMetadataConstructor;
    // Updated at the end of cloning, with the bet that the next solution to clone will have a similar number of objects.
    private final AtomicInteger expectedObjectCountRef = new AtomicInteger(MINIMUM_EXPECTED_OBJECT_COUNT);

    public FieldAccessingSolutionCloner(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
        this.classMetadataConstructor = clz -> new ClassMetadata(solutionDescriptor, clz);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public @NonNull Solution_ cloneSolution(@NonNull Solution_ originalSolution) {
        var expectedObjectCount = expectedObjectCountRef.get();
        var originalToCloneMap = CollectionUtils.newIdentityHashMap(expectedObjectCount);
        var unprocessedQueue = new ArrayDeque<Unprocessed>(expectedObjectCount);
        var cloneSolution = clone(originalSolution, originalToCloneMap, unprocessedQueue,
                retrieveClassMetadata(originalSolution.getClass()));
        while (!unprocessedQueue.isEmpty()) {
            var unprocessed = unprocessedQueue.remove();
            var cloneValue = process(unprocessed, originalToCloneMap, unprocessedQueue);
            FieldCloningUtils.setObjectFieldValue(unprocessed.bean, unprocessed.cloner.getFieldHandles(), cloneValue);
        }
        expectedObjectCountRef.updateAndGet(old -> decideNextExpectedObjectCount(old, originalToCloneMap.size()));
        validateCloneSolution(originalSolution, cloneSolution);
        return cloneSolution;
    }

    private static int decideNextExpectedObjectCount(int currentExpectedObjectCount, int currentObjectCount) {
        // For cases where solutions of vastly different sizes are cloned in a row,
        // we want to make sure the memory requirements don't grow too large, or the capacity too small.
        var halfTheDifference = (int) Math.round(Math.abs(currentObjectCount - currentExpectedObjectCount) / 2.0);
        if (currentObjectCount > currentExpectedObjectCount) {
            // Guard against integer overflow.
            return Math.min(currentExpectedObjectCount + halfTheDifference, Integer.MAX_VALUE);
        } else if (currentObjectCount < currentExpectedObjectCount) {
            // Don't go exceedingly low; cloning so few objects is always fast, re-growing the map back would be.
            return Math.max(currentExpectedObjectCount - halfTheDifference, MINIMUM_EXPECTED_OBJECT_COUNT);
        } else {
            return currentExpectedObjectCount;
        }
    }

    /**
     * Used by GIZMO when it encounters an undeclared entity class, such as when an abstract planning entity is extended.
     */
    @SuppressWarnings("unused")
    public Object gizmoFallbackDeepClone(Object originalValue, Map<Object, Object> originalToCloneMap) {
        if (originalValue == null) {
            return null;
        }
        var unprocessedQueue = new ArrayDeque<Unprocessed>(expectedObjectCountRef.get());
        var fieldType = originalValue.getClass();
        return clone(originalValue, originalToCloneMap, unprocessedQueue, fieldType);
    }

    private Object clone(Object originalValue, Map<Object, Object> originalToCloneMap, Queue<Unprocessed> unprocessedQueue,
            Class<?> fieldType) {
        if (originalValue instanceof Collection<?> collection) {
            return cloneCollection(fieldType, collection, originalToCloneMap, unprocessedQueue);
        } else if (originalValue instanceof Map<?, ?> map) {
            return cloneMap(fieldType, map, originalToCloneMap, unprocessedQueue);
        }
        var originalClass = originalValue.getClass();
        if (originalClass.isArray()) {
            return cloneArray(fieldType, originalValue, originalToCloneMap, unprocessedQueue);
        } else {
            return clone(originalValue, originalToCloneMap, unprocessedQueue, retrieveClassMetadata(originalClass));
        }
    }

    private Object process(Unprocessed unprocessed, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue) {
        var originalValue = unprocessed.originalValue;
        var field = unprocessed.cloner.getFieldHandles().field();
        var fieldType = field.getType();
        return clone(originalValue, originalToCloneMap, unprocessedQueue, fieldType);
    }

    @SuppressWarnings("unchecked")
    private <C> C clone(C original, Map<Object, Object> originalToCloneMap, Queue<Unprocessed> unprocessedQueue,
            ClassMetadata declaringClassMetadata) {
        if (original == null) {
            return null;
        }
        var existingClone = (C) originalToCloneMap.get(original);
        if (existingClone != null) {
            return existingClone;
        }

        var declaringClass = (Class<C>) original.getClass();
        var clone = constructClone(original, declaringClassMetadata);
        originalToCloneMap.put(original, clone);
        copyFields(declaringClass, original, clone, unprocessedQueue, declaringClassMetadata);
        return clone;
    }

    @SuppressWarnings("unchecked")
    private static <C> C constructClone(C original, ClassMetadata classMetadata) {
        if (original instanceof PlanningCloneable<?> planningCloneable) {
            return (C) planningCloneable.createNewInstance();
        } else {
            return constructClone(classMetadata);
        }
    }

    @SuppressWarnings("unchecked")
    private static <C> C constructClone(ClassMetadata classMetadata) {
        var constructor = classMetadata.getConstructor();
        try {
            return (C) constructor.invoke();
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Can not create a new instance of class (%s) for a planning clone, using its no-arg constructor."
                            .formatted(classMetadata.declaringClass.getCanonicalName()),
                    e);
        }
    }

    private <C> void copyFields(Class<C> clazz, C original, C clone, Queue<Unprocessed> unprocessedQueue,
            ClassMetadata declaringClassMetadata) {
        for (var fieldCloner : declaringClassMetadata.getCopiedFieldArray()) {
            fieldCloner.clone(original, clone);
        }
        for (var fieldCloner : declaringClassMetadata.getClonedFieldArray()) {
            var unprocessedValue = fieldCloner.clone(solutionDescriptor, original, clone);
            if (unprocessedValue != null) {
                unprocessedQueue.add(new Unprocessed(clone, fieldCloner, unprocessedValue));
            }
        }
        var superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            copyFields(superclass, original, clone, unprocessedQueue, retrieveClassMetadata(superclass));
        }
    }

    private Object cloneArray(Class<?> expectedType, Object originalArray, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue) {
        var arrayLength = Array.getLength(originalArray);
        if (arrayLength == 0) {
            return originalArray; // No need to clone an empty array.
        }
        var cloneArray = Array.newInstance(originalArray.getClass().getComponentType(), arrayLength);
        if (!expectedType.isInstance(cloneArray)) {
            throw new IllegalStateException("""
                    The cloneArrayClass (%s) created for originalArrayClass (%s) is not assignable to the field's type (%s).
                    Maybe consider replacing the default %s."""
                    .formatted(cloneArray.getClass(), originalArray.getClass(), expectedType,
                            SolutionCloner.class.getSimpleName()));
        }
        var reuseHelper = new ClassMetadataReuseHelper(this::retrieveClassMetadata);
        for (var i = 0; i < arrayLength; i++) {
            var cloneElement = cloneCollectionsElementIfNeeded(Array.get(originalArray, i), originalToCloneMap,
                    unprocessedQueue, reuseHelper);
            Array.set(cloneArray, i, cloneElement);
        }
        return cloneArray;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <E> Collection<E> cloneCollection(Class<?> expectedType, Collection<E> originalCollection,
            Map<Object, Object> originalToCloneMap, Queue<Unprocessed> unprocessedQueue) {
        if (originalCollection instanceof EnumSet enumSet) {
            return EnumSet.copyOf(enumSet);
        }
        var cloneCollection = constructCloneCollection(originalCollection);
        if (!expectedType.isInstance(cloneCollection)) {
            throw new IllegalStateException(
                    """
                            The cloneCollectionClass (%s) created for originalCollectionClass (%s) is not assignable to the field's type (%s).
                            Maybe consider replacing the default %s."""
                            .formatted(cloneCollection.getClass(), originalCollection.getClass(), expectedType,
                                    SolutionCloner.class.getSimpleName()));
        }
        if (originalCollection.isEmpty()) {
            return cloneCollection; // No need to clone any elements.
        }
        var reuseHelper = new ClassMetadataReuseHelper(this::retrieveClassMetadata);
        for (var originalElement : originalCollection) {
            var cloneElement =
                    cloneCollectionsElementIfNeeded(originalElement, originalToCloneMap, unprocessedQueue, reuseHelper);
            cloneCollection.add(cloneElement);
        }
        return cloneCollection;
    }

    @SuppressWarnings("unchecked")
    private static <E> Collection<E> constructCloneCollection(Collection<E> originalCollection) {
        // TODO Don't hardcode all standard collections
        if (originalCollection instanceof PlanningCloneable<?> planningCloneable) {
            return (Collection<E>) planningCloneable.createNewInstance();
        }
        if (originalCollection instanceof LinkedList) {
            return new LinkedList<>();
        }
        var size = originalCollection.size();
        if (originalCollection instanceof Set) {
            if (originalCollection instanceof SortedSet<E> set) {
                var setComparator = set.comparator();
                return new TreeSet<>(setComparator);
            } else if (!(originalCollection instanceof LinkedHashSet)) {
                // Set is explicitly not ordered, so we can use a HashSet.
                // Can be replaced by checking for SequencedSet, but that is Java 21+.
                return CollectionUtils.newHashSet(size);
            } else { // Default to a LinkedHashSet to respect order.
                return CollectionUtils.newLinkedHashSet(size);
            }
        } else if (originalCollection instanceof Deque) {
            return new ArrayDeque<>(size);
        }
        // Default collection
        return new ArrayList<>(size);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <K, V> Map<K, V> cloneMap(Class<?> expectedType, Map<K, V> originalMap, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue) {
        if (originalMap instanceof EnumMap<?, ?> enumMap) {
            var cloneMap = new EnumMap(enumMap);
            if (cloneMap.isEmpty()) {
                return (Map<K, V>) cloneMap;
            }
            var reuseHelper = new ClassMetadataReuseHelper(this::retrieveClassMetadata);
            for (var originalEntry : enumMap.entrySet()) {
                var originalValue = originalEntry.getValue();
                var cloneValue = cloneCollectionsElementIfNeeded(originalValue, originalToCloneMap, unprocessedQueue,
                        reuseHelper);
                if (originalValue != cloneValue) { // Already exists in the map.
                    cloneMap.put(originalEntry.getKey(), cloneValue);
                }
            }
            return cloneMap;
        }
        var cloneMap = constructCloneMap(originalMap);
        if (!expectedType.isInstance(cloneMap)) {
            throw new IllegalStateException("""
                    The cloneMapClass (%s) created for originalMapClass (%s) is not assignable to the field's type (%s).
                    Maybe consider replacing the default %s."""
                    .formatted(cloneMap.getClass(), originalMap.getClass(), expectedType,
                            SolutionCloner.class.getSimpleName()));
        }
        if (originalMap.isEmpty()) {
            return cloneMap; // No need to clone any entries.
        }
        var keyReuseHelper = new ClassMetadataReuseHelper(this::retrieveClassMetadata);
        var valueReuseHelper = new ClassMetadataReuseHelper(this::retrieveClassMetadata);
        for (var originalEntry : originalMap.entrySet()) {
            var cloneKey = cloneCollectionsElementIfNeeded(originalEntry.getKey(), originalToCloneMap, unprocessedQueue,
                    keyReuseHelper);
            var cloneValue = cloneCollectionsElementIfNeeded(originalEntry.getValue(), originalToCloneMap, unprocessedQueue,
                    valueReuseHelper);
            cloneMap.put(cloneKey, cloneValue);
        }
        return cloneMap;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> constructCloneMap(Map<K, V> originalMap) {
        if (originalMap instanceof PlanningCloneable<?> planningCloneable) {
            return (Map<K, V>) planningCloneable.createNewInstance();
        }
        // Normally, a Map will never be selected for cloning, but extending implementations might anyway.
        if (originalMap instanceof SortedMap<K, V> map) {
            var setComparator = map.comparator();
            return new TreeMap<>(setComparator);
        }
        var originalMapSize = originalMap.size();
        if (!(originalMap instanceof LinkedHashMap<?, ?>)) {
            // Map is explicitly not ordered, so we can use a HashMap.
            // Can be replaced by checking for SequencedMap, but that is Java 21+.
            return CollectionUtils.newHashMap(originalMapSize);
        } else { // Default to a LinkedHashMap to respect order.
            return CollectionUtils.newLinkedHashMap(originalMapSize);
        }
    }

    private ClassMetadata retrieveClassMetadata(Class<?> declaringClass) {
        return classMetadataMemoization.computeIfAbsent(declaringClass, classMetadataConstructor);
    }

    @SuppressWarnings("unchecked")
    private <C> C cloneCollectionsElementIfNeeded(C original, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue, ClassMetadataReuseHelper classMetadataReuseHelper) {
        if (original == null) {
            return null;
        }
        /*
         * Because an element which is itself a Collection or Map might hold an entity,
         * we clone it too.
         * The List<Long> in Map<String, List<Long>> needs to be cloned if the List<Long> is a shadow,
         * despite that Long never needs to be cloned (because it's immutable).
         */
        if (original instanceof Collection<?> collection) {
            return (C) cloneCollection(Collection.class, collection, originalToCloneMap, unprocessedQueue);
        } else if (original instanceof Map<?, ?> map) {
            return (C) cloneMap(Map.class, map, originalToCloneMap, unprocessedQueue);
        } else if (original.getClass().isArray()) {
            return (C) cloneArray(original.getClass(), original, originalToCloneMap, unprocessedQueue);
        }
        var classMetadata = classMetadataReuseHelper.getClassMetadata(original);
        if (classMetadata.isDeepCloned) {
            return clone(original, originalToCloneMap, unprocessedQueue, classMetadata);
        } else {
            return original;
        }
    }

    /**
     * Fails fast if {@link DeepCloningUtils#isFieldAnEntityPropertyOnSolution} assumptions were wrong.
     *
     * @param originalSolution never null
     * @param cloneSolution never null
     */
    private void validateCloneSolution(Solution_ originalSolution, Solution_ cloneSolution) {
        for (var memberAccessor : solutionDescriptor.getEntityMemberAccessorMap().values()) {
            validateCloneProperty(originalSolution, cloneSolution, memberAccessor);
        }
        for (var memberAccessor : solutionDescriptor.getEntityCollectionMemberAccessorMap().values()) {
            validateCloneProperty(originalSolution, cloneSolution, memberAccessor);
        }
    }

    private static <Solution_> void validateCloneProperty(Solution_ originalSolution, Solution_ cloneSolution,
            MemberAccessor memberAccessor) {
        var originalProperty = memberAccessor.executeGetter(originalSolution);
        if (originalProperty != null) {
            var cloneProperty = memberAccessor.executeGetter(cloneSolution);
            if (originalProperty == cloneProperty) {
                throw new IllegalStateException("""
                        The solutionProperty (%s) was not cloned as expected.
                        The %s failed to recognize that property's field, probably because its field name is different."""
                        .formatted(memberAccessor.getName(), FieldAccessingSolutionCloner.class.getSimpleName()));
            }
        }
    }

    private static final class ClassMetadata {

        private final SolutionDescriptor<?> solutionDescriptor;
        private final Class<?> declaringClass;
        private final boolean isDeepCloned;

        /**
         * Contains the MethodHandle for the no-arg constructor of the class.
         * Lazily initialized to avoid unnecessary reflection overhead when the constructor is not called.
         */
        private volatile MethodHandle constructor = null;
        /**
         * Contains one cloner for every field that needs to be shallow cloned (= copied).
         * Lazy initialized; some types (such as String) will never get here.
         */
        private volatile ShallowCloningFieldCloner[] copiedFieldArray;
        /**
         * Contains one cloner for every field that needs to be deep-cloned.
         * Lazy initialized; some types (such as String) will never get here.
         */
        private volatile DeepCloningFieldCloner[] clonedFieldArray;

        public ClassMetadata(SolutionDescriptor<?> solutionDescriptor, Class<?> declaringClass) {
            this.solutionDescriptor = solutionDescriptor;
            this.declaringClass = declaringClass;
            this.isDeepCloned = DeepCloningUtils.isClassDeepCloned(solutionDescriptor, declaringClass);
        }

        public MethodHandle getConstructor() {
            if (constructor == null) {
                synchronized (this) {
                    if (constructor == null) { // Double-checked locking
                        try {
                            var ctor = declaringClass.getDeclaredConstructor();
                            ctor.setAccessible(true);
                            constructor = MethodHandles.lookup()
                                    .unreflectConstructor(ctor);
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalStateException(
                                    "To create a planning clone, the class (%s) must have a no-arg constructor."
                                            .formatted(declaringClass.getCanonicalName()),
                                    e);
                        }
                    }
                }
            }
            return constructor;
        }

        public ShallowCloningFieldCloner[] getCopiedFieldArray() {
            if (copiedFieldArray == null) {
                synchronized (this) {
                    if (copiedFieldArray == null) { // Double-checked locking
                        copiedFieldArray = Arrays.stream(declaringClass.getDeclaredFields())
                                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                                .filter(field -> DeepCloningUtils.isImmutable(field.getType()))
                                .peek(f -> {
                                    if (DeepCloningUtils.needsDeepClone(solutionDescriptor, f, declaringClass)) {
                                        throw new IllegalStateException("""
                                                The field (%s) of class (%s) needs to be deep-cloned,
                                                but its type (%s) is immutable and can not be deep-cloned.
                                                Maybe remove the @%s annotation from the field?
                                                Maybe do not reference planning entities inside Java records?"""
                                                .formatted(f.getName(), declaringClass.getCanonicalName(),
                                                        f.getType().getCanonicalName(),
                                                        DeepPlanningClone.class.getSimpleName()));
                                    }
                                })
                                .map(ShallowCloningFieldCloner::of)
                                .toArray(ShallowCloningFieldCloner[]::new);
                    }
                }
            }
            return copiedFieldArray;
        }

        public DeepCloningFieldCloner[] getClonedFieldArray() {
            if (clonedFieldArray == null) {
                synchronized (this) {
                    if (clonedFieldArray == null) { // Double-checked locking
                        clonedFieldArray = Arrays.stream(declaringClass.getDeclaredFields())
                                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                                .filter(field -> !DeepCloningUtils.isImmutable(field.getType()))
                                .map(DeepCloningFieldCloner::new)
                                .toArray(DeepCloningFieldCloner[]::new);
                    }
                }
            }
            return clonedFieldArray;
        }

    }

    private record Unprocessed(Object bean, DeepCloningFieldCloner cloner, Object originalValue) {
    }

    /**
     * Helper class to reuse ClassMetadata for the same class.
     * Use when cloning multiple objects of the same class,
     * such as when cloning a collection of objects,
     * where it's likely that they will be of the same class.
     * This is useful for performance,
     * as it avoids repeated calls to the function that retrieves {@link ClassMetadata}.
     */
    private static final class ClassMetadataReuseHelper {

        private final Function<Class<?>, ClassMetadata> classMetadataFunction;
        private Object previousClass;
        private ClassMetadata previousClassMetadata;

        public ClassMetadataReuseHelper(Function<Class<?>, ClassMetadata> classMetadataFunction) {
            this.classMetadataFunction = classMetadataFunction;
        }

        public @NonNull ClassMetadata getClassMetadata(@NonNull Object object) {
            var clazz = object.getClass();
            if (clazz != previousClass) {
                // Class of the element has changed, so we need to retrieve the metadata again.
                previousClass = clazz;
                previousClassMetadata = classMetadataFunction.apply(clazz);
            }
            return previousClassMetadata;
        }

    }

}
