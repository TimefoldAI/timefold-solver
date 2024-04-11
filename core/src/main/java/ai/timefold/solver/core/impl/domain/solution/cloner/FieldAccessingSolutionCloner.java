package ai.timefold.solver.core.impl.domain.solution.cloner;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
import java.util.concurrent.ConcurrentMap;

import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.util.ConcurrentMemoization;

/**
 * This class is thread-safe.
 */
public final class FieldAccessingSolutionCloner<Solution_> implements SolutionCloner<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final ConcurrentMap<Class<?>, Constructor<?>> constructorMemoization = new ConcurrentMemoization<>();
    private final ConcurrentMap<Class<?>, ClassMetadata> classMetadataMemoization = new ConcurrentMemoization<>();

    public FieldAccessingSolutionCloner(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Solution_ cloneSolution(Solution_ originalSolution) {
        Map<Object, Object> originalToCloneMap = new IdentityHashMap<>();
        Queue<Unprocessed> unprocessedQueue = new ArrayDeque<>();
        Solution_ cloneSolution = clone(originalSolution, originalToCloneMap, unprocessedQueue,
                retrieveClassMetadata(originalSolution.getClass()));
        while (!unprocessedQueue.isEmpty()) {
            Unprocessed unprocessed = unprocessedQueue.remove();
            Object cloneValue = process(unprocessed, originalToCloneMap, unprocessedQueue);
            FieldCloningUtils.setObjectFieldValue(unprocessed.bean, unprocessed.field, cloneValue);
        }
        validateCloneSolution(originalSolution, cloneSolution);
        return cloneSolution;
    }

    /**
     * Used by GIZMO when it encounters an undeclared entity class, such as when an abstract planning entity is extended.
     */
    @SuppressWarnings("unused")
    public Object gizmoFallbackDeepClone(Object originalValue, Map<Object, Object> originalToCloneMap) {
        if (originalValue == null) {
            return null;
        }
        Queue<Unprocessed> unprocessedQueue = new ArrayDeque<>();
        Class<?> fieldType = originalValue.getClass();
        if (originalValue instanceof Collection<?> collection) {
            return cloneCollection(fieldType, collection, originalToCloneMap, unprocessedQueue);
        } else if (originalValue instanceof Map<?, ?> map) {
            return cloneMap(fieldType, map, originalToCloneMap, unprocessedQueue);
        } else if (originalValue.getClass().isArray()) {
            return cloneArray(fieldType, originalValue, originalToCloneMap, unprocessedQueue);
        } else {
            return clone(originalValue, originalToCloneMap, unprocessedQueue,
                    retrieveClassMetadata(originalValue.getClass()));
        }
    }

    private Object process(Unprocessed unprocessed, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue) {
        Object originalValue = unprocessed.originalValue;
        Field field = unprocessed.field;
        Class<?> fieldType = field.getType();
        if (originalValue instanceof Collection<?> collection) {
            return cloneCollection(fieldType, collection, originalToCloneMap, unprocessedQueue);
        } else if (originalValue instanceof Map<?, ?> map) {
            return cloneMap(fieldType, map, originalToCloneMap, unprocessedQueue);
        } else if (originalValue.getClass().isArray()) {
            return cloneArray(fieldType, originalValue, originalToCloneMap, unprocessedQueue);
        } else {
            return clone(originalValue, originalToCloneMap, unprocessedQueue,
                    retrieveClassMetadata(originalValue.getClass()));
        }
    }

    private <C> C clone(C original, Map<Object, Object> originalToCloneMap, Queue<Unprocessed> unprocessedQueue,
            ClassMetadata declaringClassMetadata) {
        if (original == null) {
            return null;
        }
        C existingClone = (C) originalToCloneMap.get(original);
        if (existingClone != null) {
            return existingClone;
        }

        Class<C> declaringClass = (Class<C>) original.getClass();
        C clone;
        if (original instanceof PlanningCloneable<?> planningCloneable) {
            clone = (C) planningCloneable.createNewInstance();
        } else {
            clone = constructClone(declaringClass);
        }
        originalToCloneMap.put(original, clone);
        copyFields(declaringClass, original, clone, unprocessedQueue, declaringClassMetadata);
        return clone;
    }

    private <C> C constructClone(Class<C> clazz) {
        var constructor = constructorMemoization.computeIfAbsent(clazz, key -> {
            try {
                var ctor = (Constructor<C>) key.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "To create a planning clone, the class (%s) must have a no-arg constructor."
                                .formatted(key.getCanonicalName()),
                        e);
            }
        });
        try {
            return (C) constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Can not create a new instance of class (%s) for a planning clone, using its no-arg constructor."
                            .formatted(clazz.getCanonicalName()),
                    e);
        }
    }

    private <C> void copyFields(Class<C> clazz, C original, C clone, Queue<Unprocessed> unprocessedQueue,
            ClassMetadata declaringClassMetadata) {
        for (ShallowCloningFieldCloner fieldCloner : declaringClassMetadata.getCopiedFieldArray()) {
            fieldCloner.clone(original, clone);
        }
        for (DeepCloningFieldCloner fieldCloner : declaringClassMetadata.getClonedFieldArray()) {
            Object unprocessedValue = fieldCloner.clone(solutionDescriptor, original, clone);
            if (unprocessedValue != null) {
                unprocessedQueue.add(new Unprocessed(clone, fieldCloner.getField(), unprocessedValue));
            }
        }
        Class<? super C> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            copyFields(superclass, original, clone, unprocessedQueue, retrieveClassMetadata(superclass));
        }
    }

    private Object cloneArray(Class<?> expectedType, Object originalArray, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue) {
        int arrayLength = Array.getLength(originalArray);
        Object cloneArray = Array.newInstance(originalArray.getClass().getComponentType(), arrayLength);
        if (!expectedType.isInstance(cloneArray)) {
            throw new IllegalStateException("The cloneArrayClass (" + cloneArray.getClass()
                    + ") created for originalArrayClass (" + originalArray.getClass()
                    + ") is not assignable to the field's type (" + expectedType + ").\n"
                    + "Maybe consider replacing the default " + SolutionCloner.class.getSimpleName() + ".");
        }
        for (int i = 0; i < arrayLength; i++) {
            Object cloneElement =
                    cloneCollectionsElementIfNeeded(Array.get(originalArray, i), originalToCloneMap, unprocessedQueue);
            Array.set(cloneArray, i, cloneElement);
        }
        return cloneArray;
    }

    private <E> Collection<E> cloneCollection(Class<?> expectedType, Collection<E> originalCollection,
            Map<Object, Object> originalToCloneMap, Queue<Unprocessed> unprocessedQueue) {
        Collection<E> cloneCollection = constructCloneCollection(originalCollection);
        if (!expectedType.isInstance(cloneCollection)) {
            throw new IllegalStateException("The cloneCollectionClass (" + cloneCollection.getClass()
                    + ") created for originalCollectionClass (" + originalCollection.getClass()
                    + ") is not assignable to the field's type (" + expectedType + ").\n"
                    + "Maybe consider replacing the default " + SolutionCloner.class.getSimpleName() + ".");
        }
        for (E originalElement : originalCollection) {
            E cloneElement = cloneCollectionsElementIfNeeded(originalElement, originalToCloneMap, unprocessedQueue);
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
                return new HashSet<>(size);
            } else { // Default to a LinkedHashSet to respect order.
                return new LinkedHashSet<>(size);
            }
        } else if (originalCollection instanceof Deque) {
            return new ArrayDeque<>(size);
        }
        // Default collection
        return new ArrayList<>(size);
    }

    private <K, V> Map<K, V> cloneMap(Class<?> expectedType, Map<K, V> originalMap, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue) {
        Map<K, V> cloneMap = constructCloneMap(originalMap);
        if (!expectedType.isInstance(cloneMap)) {
            throw new IllegalStateException("The cloneMapClass (" + cloneMap.getClass()
                    + ") created for originalMapClass (" + originalMap.getClass()
                    + ") is not assignable to the field's type (" + expectedType + ").\n"
                    + "Maybe consider replacing the default " + SolutionCloner.class.getSimpleName() + ".");
        }
        for (Map.Entry<K, V> originalEntry : originalMap.entrySet()) {
            K cloneKey = cloneCollectionsElementIfNeeded(originalEntry.getKey(), originalToCloneMap, unprocessedQueue);
            V cloneValue = cloneCollectionsElementIfNeeded(originalEntry.getValue(), originalToCloneMap, unprocessedQueue);
            cloneMap.put(cloneKey, cloneValue);
        }
        return cloneMap;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> constructCloneMap(Map<K, V> originalMap) {
        // Normally, a Map will never be selected for cloning, but extending implementations might anyway.
        if (originalMap instanceof PlanningCloneable<?> planningCloneable) {
            return (Map<K, V>) planningCloneable.createNewInstance();
        }
        if (originalMap instanceof SortedMap<K, V> map) {
            var setComparator = map.comparator();
            return new TreeMap<>(setComparator);
        }
        var originalMapSize = originalMap.size();
        if (!(originalMap instanceof LinkedHashMap)) {
            return new HashMap<>(originalMapSize);
        } else { // Default to a LinkedHashMap to respect order.
            return new LinkedHashMap<>(originalMapSize);
        }
    }

    private ClassMetadata retrieveClassMetadata(Class<?> declaringClass) {
        return classMetadataMemoization.computeIfAbsent(declaringClass, ClassMetadata::new);
    }

    private <C> C cloneCollectionsElementIfNeeded(C original, Map<Object, Object> originalToCloneMap,
            Queue<Unprocessed> unprocessedQueue) {
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
        ClassMetadata classMetadata = retrieveClassMetadata(original.getClass());
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
        for (MemberAccessor memberAccessor : solutionDescriptor.getEntityMemberAccessorMap().values()) {
            validateCloneProperty(originalSolution, cloneSolution, memberAccessor);
        }
        for (MemberAccessor memberAccessor : solutionDescriptor.getEntityCollectionMemberAccessorMap().values()) {
            validateCloneProperty(originalSolution, cloneSolution, memberAccessor);
        }
    }

    private static <Solution_> void validateCloneProperty(Solution_ originalSolution, Solution_ cloneSolution,
            MemberAccessor memberAccessor) {
        Object originalProperty = memberAccessor.executeGetter(originalSolution);
        if (originalProperty != null) {
            Object cloneProperty = memberAccessor.executeGetter(cloneSolution);
            if (originalProperty == cloneProperty) {
                throw new IllegalStateException(
                        "The solutionProperty (" + memberAccessor.getName() + ") was not cloned as expected."
                                + " The " + FieldAccessingSolutionCloner.class.getSimpleName() + " failed to recognize"
                                + " that property's field, probably because its field name is different.");
            }
        }
    }

    private final class ClassMetadata {

        private final Class<?> declaringClass;
        private final boolean isDeepCloned;

        /**
         * Contains one cloner for every field that needs to be shallow cloned (= copied).
         */
        private ShallowCloningFieldCloner[] copiedFieldArray;
        /**
         * Contains one cloner for every field that needs to be deep-cloned.
         */
        private DeepCloningFieldCloner[] clonedFieldArray;

        public ClassMetadata(Class<?> declaringClass) {
            this.declaringClass = declaringClass;
            this.isDeepCloned = DeepCloningUtils.isClassDeepCloned(solutionDescriptor, declaringClass);
        }

        public ShallowCloningFieldCloner[] getCopiedFieldArray() {
            if (copiedFieldArray == null) { // Lazy-loaded; some types (such as String) will never get here.
                copiedFieldArray = Arrays.stream(declaringClass.getDeclaredFields())
                        .filter(f -> !Modifier.isStatic(f.getModifiers()))
                        .filter(field -> DeepCloningUtils.isImmutable(field.getType()))
                        .peek(f -> {
                            if (DeepCloningUtils.needsDeepClone(solutionDescriptor, f, declaringClass)) {
                                throw new IllegalStateException("""
                                        The field (%s) of class (%s) needs to be deep-cloned,
                                        but its type (%s) is immutable and can not be deep-cloned.
                                        Maybe remove the @%s annotation from the field?
                                        Maybe do not reference planning entities inside Java records?
                                        """
                                        .strip()
                                        .formatted(f.getName(), declaringClass.getCanonicalName(),
                                                f.getType().getCanonicalName(), DeepPlanningClone.class.getSimpleName()));
                            } else {
                                f.setAccessible(true);
                            }
                        })
                        .map(ShallowCloningFieldCloner::of)
                        .toArray(ShallowCloningFieldCloner[]::new);
            }
            return copiedFieldArray;
        }

        public DeepCloningFieldCloner[] getClonedFieldArray() {
            if (clonedFieldArray == null) { // Lazy-loaded; some types (such as String) will never get here.
                clonedFieldArray = Arrays.stream(declaringClass.getDeclaredFields())
                        .filter(f -> !Modifier.isStatic(f.getModifiers()))
                        .filter(field -> !DeepCloningUtils.isImmutable(field.getType()))
                        .peek(f -> f.setAccessible(true))
                        .map(DeepCloningFieldCloner::new)
                        .toArray(DeepCloningFieldCloner[]::new);
            }
            return clonedFieldArray;
        }

    }

    private record Unprocessed(Object bean, Field field, Object originalValue) {
    }
}
