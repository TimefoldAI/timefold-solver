package ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoClassLoader;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberDescriptor;
import ai.timefold.solver.core.impl.domain.solution.cloner.DeepCloningUtils;
import ai.timefold.solver.core.impl.domain.solution.cloner.FieldAccessingSolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningCloneable;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.util.Pair;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class GizmoSolutionClonerImplementor {
    private static final MethodDescriptor EQUALS_METHOD = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class,
            Object.class);
    protected static final MethodDescriptor GET_METHOD = MethodDescriptor.ofMethod(Map.class, "get", Object.class,
            Object.class);
    private static final MethodDescriptor PUT_METHOD = MethodDescriptor.ofMethod(Map.class, "put", Object.class,
            Object.class, Object.class);
    private static final String FALLBACK_CLONER = "fallbackCloner";
    public static final boolean DEBUG = false;

    protected record ClonerDescriptor(SolutionDescriptor<?> solutionDescriptor,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet,
            ClassCreator classCreator) {
    }

    protected record ClonerMethodDescriptor(GizmoSolutionOrEntityDescriptor entityDescriptor,
            BytecodeCreator bytecodeCreator,
            ResultHandle createdCloneMap,
            boolean isBottom,
            ResultHandle cloneQueue) {
        public ClonerMethodDescriptor withBytecodeCreator(BytecodeCreator bytecodeCreator) {
            return new ClonerMethodDescriptor(entityDescriptor, bytecodeCreator, createdCloneMap, isBottom, cloneQueue);
        }

        public ClonerMethodDescriptor withCreatedCloneMap(ResultHandle createdCloneMap) {
            return new ClonerMethodDescriptor(entityDescriptor, bytecodeCreator, createdCloneMap, isBottom, cloneQueue);
        }

        public ClonerMethodDescriptor withCloneQueue(ResultHandle cloneQueue) {
            return new ClonerMethodDescriptor(entityDescriptor, bytecodeCreator, createdCloneMap, isBottom, cloneQueue);
        }
    }

    /**
     * Return a comparator that sorts classes into instanceof check order.
     * In particular, if x is a subclass of y, then x will appear earlier
     * than y in the list.
     *
     * @param deepClonedClassSet The set of classes to generate a comparator for
     * @return A comparator that sorts classes from deepClonedClassSet such that
     *         x &lt; y if x is assignable from y.
     */
    public static Comparator<Class<?>> getInstanceOfComparator(Set<Class<?>> deepClonedClassSet) {
        var classToSubclassLevel = new HashMap<Class<?>, Integer>();
        deepClonedClassSet
                .forEach(clazz -> {
                    if (deepClonedClassSet.stream()
                            .allMatch(
                                    otherClazz -> clazz.isAssignableFrom(otherClazz) || !otherClazz.isAssignableFrom(clazz))) {
                        classToSubclassLevel.put(clazz, 0);
                    }
                });
        var isChanged = true;
        while (isChanged) {
            // Need to iterate over all classes
            // since maxSubclassLevel can change
            // (for instance, Tiger extends Cat (1) implements Animal (0))
            isChanged = false;
            for (Class<?> clazz : deepClonedClassSet) {
                var maxParentSubclassLevel = classToSubclassLevel.keySet().stream()
                        .filter(otherClazz -> otherClazz != clazz && otherClazz.isAssignableFrom(clazz))
                        .map(classToSubclassLevel::get)
                        .max(Integer::compare);

                if (maxParentSubclassLevel.isPresent()) {
                    var oldVal = (int) classToSubclassLevel.getOrDefault(clazz, -1);
                    var newVal = maxParentSubclassLevel.get() + 1;
                    if (newVal > oldVal) {
                        isChanged = true;
                        classToSubclassLevel.put(clazz, newVal);
                    }
                }
            }
        }

        return Comparator.<Class<?>, Integer> comparing(classToSubclassLevel::get)
                .thenComparing(Class::getName).reversed();
    }

    protected void createFields(ClonerDescriptor clonerDescriptor) {
        clonerDescriptor.classCreator.getFieldCreator(FALLBACK_CLONER, FieldAccessingSolutionCloner.class)
                .setModifiers(Modifier.PRIVATE | Modifier.STATIC);
    }

    /**
     * Generates the constructor and implementations of SolutionCloner methods for the given SolutionDescriptor using the given
     * ClassCreator
     */
    public static void defineClonerFor(ClassCreator classCreator,
            SolutionDescriptor<?> solutionDescriptor,
            Set<Class<?>> solutionClassSet,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            Set<Class<?>> deepClonedClassSet) {
        defineClonerFor(GizmoSolutionClonerImplementor::new, classCreator, solutionDescriptor, solutionClassSet,
                memoizedSolutionOrEntityDescriptorMap, deepClonedClassSet);
    }

    public static boolean isCloneableClass(Class<?> clazz) {
        return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Generates the constructor and implementations of SolutionCloner
     * methods for the given SolutionDescriptor using the given ClassCreator
     */
    public static void defineClonerFor(Supplier<GizmoSolutionClonerImplementor> implementorSupplier,
            ClassCreator classCreator,
            SolutionDescriptor<?> solutionDescriptor,
            Set<Class<?>> solutionClassSet,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            Set<Class<?>> deepClonedClassSet) {
        var implementor = implementorSupplier.get();
        // Classes that are not instances of any other class in the collection
        // have a subclass level of 0.
        // Other classes subclass level is the maximum of the subclass level
        // of the classes it is a subclass of + 1
        var deepCloneClassesThatAreNotSolutionSet =
                deepClonedClassSet.stream()
                        .filter(clazz -> !solutionClassSet.contains(clazz) && !clazz.isArray())
                        .filter(GizmoSolutionClonerImplementor::isCloneableClass)
                        .collect(Collectors.toSet());

        var instanceOfComparator = getInstanceOfComparator(deepClonedClassSet);
        var deepCloneClassesThatAreNotSolutionSortedSet = new TreeSet<>(instanceOfComparator);
        deepCloneClassesThatAreNotSolutionSortedSet.addAll(deepCloneClassesThatAreNotSolutionSet);

        var clonerDescriptor = new ClonerDescriptor(solutionDescriptor, memoizedSolutionOrEntityDescriptorMap,
                deepCloneClassesThatAreNotSolutionSortedSet,
                classCreator);

        implementor.createFields(clonerDescriptor);
        implementor.createConstructor(clonerDescriptor);
        implementor.createSetSolutionDescriptor(clonerDescriptor);
        implementor.createCloneSolution(clonerDescriptor);
        implementor.createCloneSolutionRun(clonerDescriptor, solutionClassSet, instanceOfComparator);

        for (var deepClonedClass : deepCloneClassesThatAreNotSolutionSortedSet) {
            implementor.createDeepCloneHelperMethod(clonerDescriptor, deepClonedClass);
        }

        var abstractDeepCloneClassSet =
                deepClonedClassSet.stream()
                        .filter(clazz -> !solutionClassSet.contains(clazz) && !clazz.isArray())
                        .filter(Predicate.not(GizmoSolutionClonerImplementor::isCloneableClass))
                        .collect(Collectors.toSet());

        for (var abstractDeepClonedClass : abstractDeepCloneClassSet) {
            implementor.createAbstractDeepCloneHelperMethod(clonerDescriptor, abstractDeepClonedClass);
        }
    }

    public static ClassOutput createClassOutputWithDebuggingCapability(List<Pair<String, byte[]>> classBytecodeHolder) {
        return (path, byteCode) -> {
            classBytecodeHolder.add(new Pair<>(path.replace('/', '.'), byteCode));

            if (DEBUG) {
                Path debugRoot = Paths.get("target/timefold-solver-generated-classes");
                Path rest = Paths.get(path + ".class");
                Path destination = debugRoot.resolve(rest);

                try {
                    Files.createDirectories(destination.getParent());
                    Files.write(destination, byteCode);
                } catch (IOException e) {
                    throw new IllegalStateException("Fail to write debug class file " + destination + ".", e);
                }
            }
        };
    }

    static <T> SolutionCloner<T> createClonerFor(SolutionDescriptor<T> solutionDescriptor,
            GizmoClassLoader gizmoClassLoader) {
        var implementor = new GizmoSolutionClonerImplementor();
        var className = GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor);
        if (gizmoClassLoader.hasBytecodeFor(className)) {
            return implementor.createInstance(className, gizmoClassLoader, solutionDescriptor);
        }
        var classBytecodeHolder = new ArrayList<Pair<String, byte[]>>();
        var classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(GizmoSolutionCloner.class)
                .superClass(Object.class)
                .classOutput(createClassOutputWithDebuggingCapability(classBytecodeHolder))
                .setFinal(true)
                .build();

        var deepClonedClassSet = GizmoCloningUtils.getDeepClonedClasses(solutionDescriptor, Collections.emptyList());

        defineClonerFor(() -> implementor, classCreator, solutionDescriptor,
                Collections.singleton(solutionDescriptor.getSolutionClass()),
                new HashMap<>(), deepClonedClassSet);

        classCreator.close();
        for (var bytecodeEntry : classBytecodeHolder) {
            gizmoClassLoader.storeBytecode(bytecodeEntry.key(), bytecodeEntry.value());
        }

        return implementor.createInstance(className, gizmoClassLoader, solutionDescriptor);
    }

    private <T> SolutionCloner<T> createInstance(String className, ClassLoader gizmoClassLoader,
            SolutionDescriptor<T> solutionDescriptor) {
        try {
            @SuppressWarnings("unchecked")
            var outClass =
                    (Class<? extends GizmoSolutionCloner<T>>) gizmoClassLoader.loadClass(className);
            var out = outClass.getConstructor().newInstance();
            out.setSolutionDescriptor(solutionDescriptor);
            return out;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException
                | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createConstructor(ClonerDescriptor clonerDescriptor) {
        var methodCreator = clonerDescriptor.classCreator.getMethodCreator(
                MethodDescriptor.ofConstructor(clonerDescriptor.classCreator.getClassName()));
        var thisObj = methodCreator.getThis();

        // Invoke Object's constructor
        methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), thisObj);

        // Return this (it a constructor)
        methodCreator.returnValue(thisObj);
    }

    protected void createSetSolutionDescriptor(ClonerDescriptor clonerDescriptor) {
        var methodCreator = clonerDescriptor.classCreator.getMethodCreator(
                MethodDescriptor.ofMethod(GizmoSolutionCloner.class, "setSolutionDescriptor", void.class,
                        SolutionDescriptor.class));

        methodCreator.writeStaticField(FieldDescriptor.of(
                GizmoSolutionClonerFactory.getGeneratedClassName(clonerDescriptor.solutionDescriptor),
                FALLBACK_CLONER, FieldAccessingSolutionCloner.class),
                methodCreator.newInstance(
                        MethodDescriptor.ofConstructor(FieldAccessingSolutionCloner.class, SolutionDescriptor.class),
                        methodCreator.getMethodParam(0)));

        methodCreator.returnValue(null);
    }

    private void createCloneSolution(ClonerDescriptor clonerDescriptor) {
        var solutionClass = clonerDescriptor.solutionDescriptor.getSolutionClass();
        var methodCreator =
                clonerDescriptor.classCreator.getMethodCreator(MethodDescriptor.ofMethod(SolutionCloner.class,
                        "cloneSolution",
                        Object.class,
                        Object.class));

        var thisObj = methodCreator.getMethodParam(0);

        var clone = methodCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(
                        GizmoSolutionClonerFactory.getGeneratedClassName(clonerDescriptor.solutionDescriptor),
                        "cloneSolutionRun", solutionClass, solutionClass, Map.class),
                thisObj,
                methodCreator.newInstance(MethodDescriptor.ofConstructor(IdentityHashMap.class)));
        methodCreator.returnValue(clone);
    }

    private void createCloneSolutionRun(ClonerDescriptor clonerDescriptor,
            Set<Class<?>> solutionClassSet, Comparator<Class<?>> instanceOfComparator) {
        var solutionClass = clonerDescriptor.solutionDescriptor.getSolutionClass();
        var methodCreator =
                clonerDescriptor.classCreator.getMethodCreator("cloneSolutionRun", solutionClass, solutionClass, Map.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

        var thisObj = methodCreator.getMethodParam(0);
        var solutionNullBranchResult = methodCreator.ifNull(thisObj);
        try (var solutionIsNullBranch = solutionNullBranchResult.trueBranch()) {
            solutionIsNullBranch.returnValue(thisObj); // thisObj is null
        }

        try (var solutionIsNotNullBranch = solutionNullBranchResult.falseBranch()) {
            var createdCloneMap = methodCreator.getMethodParam(1);

            var maybeClone = solutionIsNotNullBranch.invokeInterfaceMethod(
                    GET_METHOD, createdCloneMap, thisObj);
            var hasCloneBranchResult = solutionIsNotNullBranch.ifNotNull(maybeClone);
            try (var hasCloneBranch = hasCloneBranchResult.trueBranch()) {
                hasCloneBranch.returnValue(maybeClone);
            }

            var noCloneBranch = hasCloneBranchResult.falseBranch();
            var sortedSolutionClassList = new ArrayList<>(solutionClassSet);
            sortedSolutionClassList.sort(instanceOfComparator);

            var currentBranch = noCloneBranch;
            var thisObjClass =
                    currentBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "getClass", Class.class),
                            thisObj);
            for (Class<?> solutionSubclass : sortedSolutionClassList) {
                var solutionSubclassResultHandle = currentBranch.loadClass(solutionSubclass);
                var isSubclass =
                        currentBranch.invokeVirtualMethod(EQUALS_METHOD, solutionSubclassResultHandle, thisObjClass);
                var isSubclassBranchResult = currentBranch.ifTrue(isSubclass);

                var isSubclassBranch = isSubclassBranchResult.trueBranch();

                var solutionSubclassDescriptor =
                        clonerDescriptor.memoizedSolutionOrEntityDescriptorMap.computeIfAbsent(solutionSubclass,
                                key -> new GizmoSolutionOrEntityDescriptor(clonerDescriptor.solutionDescriptor,
                                        solutionSubclass));

                ResultHandle clone;
                if (PlanningCloneable.class.isAssignableFrom(solutionSubclass)) {
                    clone = isSubclassBranch.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance", Object.class),
                            thisObj);
                    clone = isSubclassBranch.checkCast(clone, solutionSubclass);
                } else {
                    clone = isSubclassBranch.newInstance(MethodDescriptor.ofConstructor(solutionSubclass));
                }

                isSubclassBranch.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                        createdCloneMap, thisObj, clone);

                for (GizmoMemberDescriptor shallowlyClonedField : solutionSubclassDescriptor
                        .getShallowClonedMemberDescriptors()) {
                    writeShallowCloneInstructions(clonerDescriptor, new ClonerMethodDescriptor(
                            solutionSubclassDescriptor,
                            isSubclassBranch, createdCloneMap,
                            true, isSubclassBranch.newInstance(MethodDescriptor.ofConstructor(ArrayDeque.class))),
                            shallowlyClonedField, thisObj, clone);
                }

                for (Field deeplyClonedField : solutionSubclassDescriptor.getDeepClonedFields()) {
                    var gizmoMemberDescriptor =
                            solutionSubclassDescriptor.getMemberDescriptorForField(deeplyClonedField);

                    var fieldValue = gizmoMemberDescriptor.readMemberValue(isSubclassBranch, thisObj);
                    var cloneValue = isSubclassBranch.createVariable(deeplyClonedField.getType());
                    writeDeepCloneInstructions(clonerDescriptor,
                            new ClonerMethodDescriptor(solutionSubclassDescriptor, isSubclassBranch, createdCloneMap,
                                    true, isSubclassBranch.newInstance(MethodDescriptor.ofConstructor(ArrayDeque.class))),
                            deeplyClonedField,
                            gizmoMemberDescriptor, fieldValue, cloneValue);

                    if (!gizmoMemberDescriptor.writeMemberValue(isSubclassBranch, clone, cloneValue)) {
                        throw new IllegalStateException("The member (" + gizmoMemberDescriptor.getName() + ") of class (" +
                                gizmoMemberDescriptor.getDeclaringClassName() +
                                ") does not have a setter.");
                    }
                }
                isSubclassBranch.returnValue(clone);
                currentBranch.close();

                currentBranch = isSubclassBranchResult.falseBranch();
            }
            var errorBuilder = currentBranch.newInstance(MethodDescriptor.ofConstructor(StringBuilder.class, String.class),
                    currentBranch.load("Failed to create clone: encountered ("));
            var APPEND =
                    MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, Object.class);

            currentBranch.invokeVirtualMethod(APPEND, errorBuilder, thisObjClass);
            currentBranch.invokeVirtualMethod(APPEND, errorBuilder, currentBranch.load(") which is not a known subclass of " +
                    "the solution class (" + clonerDescriptor.solutionDescriptor.getSolutionClass()
                    + "). The known subclasses are "
                    +
                    solutionClassSet.stream().map(Class::getName).collect(Collectors.joining(", ", "[", "]")) + "." +
                    "\nMaybe use DomainAccessType.REFLECTION?"));
            var errorMsg = currentBranch
                    .invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "toString", String.class), errorBuilder);
            var error = currentBranch
                    .newInstance(MethodDescriptor.ofConstructor(IllegalArgumentException.class, String.class), errorMsg);
            currentBranch.throwException(error);
            currentBranch.close();
        }
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // If getter a field
     * clone.member = original.member
     * // If getter a method (i.e. Quarkus)
     * clone.setMember(original.getMember());
     * </pre>
     */
    private void writeShallowCloneInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            GizmoMemberDescriptor shallowlyClonedField,
            ResultHandle thisObj, ResultHandle clone) {
        try {
            var isArray = shallowlyClonedField.getTypeName().endsWith("[]");
            Class<?> type = null;
            if (shallowlyClonedField.getType() instanceof Class) {
                type = (Class<?>) shallowlyClonedField.getType();
            }

            List<Class<?>> entitySubclasses = Collections.emptyList();
            if (type == null && !isArray) {
                type = Class.forName(shallowlyClonedField.getTypeName().replace('/', '.'), false,
                        Thread.currentThread().getContextClassLoader());
            }

            if (type != null && !isArray) {
                entitySubclasses =
                        clonerDescriptor.deepClonedClassesSortedSet.stream().filter(type::isAssignableFrom).toList();
            }

            var fieldValue = shallowlyClonedField.readMemberValue(clonerMethodDescriptor.bytecodeCreator, thisObj);
            if (!entitySubclasses.isEmpty()) {
                var cloneResultHolder = clonerMethodDescriptor.bytecodeCreator.createVariable(type);
                writeDeepCloneEntityOrFactInstructions(clonerDescriptor,
                        clonerMethodDescriptor,
                        type,
                        fieldValue, cloneResultHolder,
                        UnhandledCloneType.SHALLOW);
                fieldValue = cloneResultHolder;
            }
            if (!shallowlyClonedField.writeMemberValue(clonerMethodDescriptor.bytecodeCreator, clone, fieldValue)) {
                throw new IllegalStateException("Field (" + shallowlyClonedField.getName() + ") of class (" +
                        shallowlyClonedField.getDeclaringClassName() +
                        ") does not have a setter.");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Error creating Gizmo Solution Cloner", e);
        }
    }

    /**
     * @see #writeDeepCloneInstructions(ClonerDescriptor, ClonerMethodDescriptor, Class, Type, ResultHandle,
     *      AssignableResultHandle)
     */
    private void writeDeepCloneInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Field deeplyClonedField,
            GizmoMemberDescriptor gizmoMemberDescriptor, ResultHandle toClone, AssignableResultHandle cloneResultHolder) {
        var isNull = clonerMethodDescriptor.bytecodeCreator.ifNull(toClone);

        try (var isNullBranch = isNull.trueBranch()) {
            isNullBranch.assign(cloneResultHolder, isNullBranch.loadNull());
        }

        try (var isNotNullBranch = isNull.falseBranch()) {
            var deeplyClonedFieldClass = deeplyClonedField.getType();
            var type = gizmoMemberDescriptor.getType();
            if (clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass()
                    .isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneSolutionInstructions(clonerMethodDescriptor, toClone, cloneResultHolder);
            } else if (Collection.class.isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneCollectionInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (Map.class.isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneMapInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (deeplyClonedFieldClass.isArray()) {
                writeDeepCloneArrayInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass, toClone, cloneResultHolder);
            } else {
                var unknownClassCloneType =
                        (DeepCloningUtils.isFieldDeepCloned(clonerMethodDescriptor.entityDescriptor.solutionDescriptor,
                                deeplyClonedField, deeplyClonedField.getDeclaringClass()))
                                        ? UnhandledCloneType.DEEP
                                        : UnhandledCloneType.SHALLOW;
                writeDeepCloneEntityOrFactInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass,
                        toClone, cloneResultHolder, unknownClassCloneType);
            }
        }
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For a Collection
     * Collection original = field;
     * Collection clone = new ActualCollectionType();
     * Iterator iterator = original.iterator();
     * while (iterator.hasNext()) {
     *     Object nextClone = (result from recursion on iterator.next());
     *     clone.add(nextClone);
     * }
     *
     * // For a Map
     * Map original = field;
     * Map clone = new ActualMapType();
     * Iterator iterator = original.entrySet().iterator();
     * while (iterator.hasNext()) {
     *      Entry next = iterator.next();
     *      nextClone = (result from recursion on next.getValue());
     *      clone.put(next.getKey(), nextClone);
     * }
     *
     * // For an array
     * Object[] original = field;
     * Object[] clone = new Object[original.length];
     *
     * for (int i = 0; i < original.length; i++) {
     *     clone[i] = (result from recursion on original[i]);
     * }
     *
     * // For an entity
     * if (original instanceof SubclassOfEntity1) {
     *     SubclassOfEntity1 original = field;
     *     SubclassOfEntity1 clone = new SubclassOfEntity1();
     *
     *     // shallowly clone fields using writeShallowCloneInstructions()
     *     // for any deeply cloned field, do recursion on it
     * } else if (original instanceof SubclassOfEntity2) {
     *     // ...
     * }
     * </pre>
     */
    private void writeDeepCloneInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, ResultHandle toClone,
            AssignableResultHandle cloneResultHolder) {
        var isNull = clonerMethodDescriptor.bytecodeCreator.ifNull(toClone);

        try (var isNullBranch = isNull.trueBranch()) {
            isNullBranch.assign(cloneResultHolder, isNullBranch.loadNull());
        }

        try (var isNotNullBranch = isNull.falseBranch()) {
            if (clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass()
                    .isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneSolutionInstructions(clonerMethodDescriptor, toClone, cloneResultHolder);
            } else if (Collection.class.isAssignableFrom(deeplyClonedFieldClass)) {
                // Clone collection
                writeDeepCloneCollectionInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (Map.class.isAssignableFrom(deeplyClonedFieldClass)) {
                // Clone map
                writeDeepCloneMapInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (deeplyClonedFieldClass.isArray()) {
                // Clone array
                writeDeepCloneArrayInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass,
                        toClone, cloneResultHolder);
            } else {
                // Clone entity
                UnhandledCloneType unknownClassCloneType =
                        (DeepCloningUtils.isClassDeepCloned(clonerMethodDescriptor.entityDescriptor.solutionDescriptor,
                                deeplyClonedFieldClass))
                                        ? UnhandledCloneType.DEEP
                                        : UnhandledCloneType.SHALLOW;
                writeDeepCloneEntityOrFactInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBytecodeCreator(isNotNullBranch),
                        deeplyClonedFieldClass,
                        toClone, cloneResultHolder, unknownClassCloneType);
            }
        }
    }

    private void writeDeepCloneSolutionInstructions(
            ClonerMethodDescriptor clonerMethodDescriptor, ResultHandle toClone, AssignableResultHandle cloneResultHolder) {
        var isNull = clonerMethodDescriptor.bytecodeCreator.ifNull(toClone);

        try (var isNullBranch = isNull.trueBranch()) {
            isNullBranch.assign(cloneResultHolder, isNullBranch.loadNull());
        }

        try (var isNotNullBranch = isNull.falseBranch()) {
            var clone = isNotNullBranch.invokeStaticMethod(
                    MethodDescriptor.ofMethod(
                            GizmoSolutionClonerFactory
                                    .getGeneratedClassName(clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor()),
                            "cloneSolutionRun",
                            clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass(),
                            clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass(), Map.class),
                    toClone,
                    clonerMethodDescriptor.createdCloneMap);
            isNotNullBranch.assign(cloneResultHolder, clone);
        }
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For a Collection
     * Collection clone = new ActualCollectionType();
     * Iterator iterator = toClone.iterator();
     * while (iterator.hasNext()) {
     *     Object toCloneElement = iterator.next();
     *     Object nextClone = (result from recursion on toCloneElement);
     *     clone.add(nextClone);
     * }
     * cloneResultHolder = clone;
     * </pre>
     **/
    private void writeDeepCloneCollectionInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, ResultHandle toClone,
            AssignableResultHandle cloneResultHolder) {
        var bytecodeCreator = clonerMethodDescriptor.bytecodeCreator;
        // Clone collection
        var cloneCollection = bytecodeCreator.createVariable(deeplyClonedFieldClass);

        var size = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Collection.class, "size", int.class), toClone);

        if (PlanningCloneable.class.isAssignableFrom(deeplyClonedFieldClass)) {
            var emptyInstance = bytecodeCreator
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance",
                            Object.class), bytecodeCreator.checkCast(toClone, PlanningCloneable.class));
            bytecodeCreator.assign(cloneCollection,
                    bytecodeCreator.checkCast(emptyInstance,
                            Collection.class));
        } else if (List.class.isAssignableFrom(deeplyClonedFieldClass)) {
            bytecodeCreator.assign(cloneCollection,
                    bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(ArrayList.class, int.class), size));
        } else if (Set.class.isAssignableFrom(deeplyClonedFieldClass)) {
            var isSortedSet = bytecodeCreator.instanceOf(toClone, SortedSet.class);
            var isSortedSetBranchResult = bytecodeCreator.ifTrue(isSortedSet);
            try (var isSortedSetBranch = isSortedSetBranchResult.trueBranch()) {
                var setComparator = isSortedSetBranch
                        .invokeInterfaceMethod(MethodDescriptor.ofMethod(SortedSet.class,
                                "comparator", Comparator.class), toClone);
                isSortedSetBranch.assign(cloneCollection,
                        isSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(TreeSet.class, Comparator.class),
                                setComparator));
            }
            try (var isNotSortedSetBranch = isSortedSetBranchResult.falseBranch()) {
                isNotSortedSetBranch.assign(cloneCollection,
                        isNotSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(LinkedHashSet.class, int.class), size));
            }
        } else {
            // field is probably of type collection
            var isSet = bytecodeCreator.instanceOf(toClone, Set.class);
            var isSetBranchResult = bytecodeCreator.ifTrue(isSet);
            try (var isSetBranch = isSetBranchResult.trueBranch()) {
                var isSortedSet = isSetBranch.instanceOf(toClone, SortedSet.class);
                var isSortedSetBranchResult = isSetBranch.ifTrue(isSortedSet);
                try (var isSortedSetBranch = isSortedSetBranchResult.trueBranch()) {
                    ResultHandle setComparator = isSortedSetBranch
                            .invokeInterfaceMethod(MethodDescriptor.ofMethod(SortedSet.class,
                                    "comparator", Comparator.class), toClone);
                    isSortedSetBranch.assign(cloneCollection,
                            isSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(TreeSet.class, Comparator.class),
                                    setComparator));
                }
                try (var isNotSortedSetBranch = isSortedSetBranchResult.falseBranch()) {
                    isNotSortedSetBranch.assign(cloneCollection,
                            isNotSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(LinkedHashSet.class, int.class),
                                    size));
                }
            }
            // Default to ArrayList
            try (var isNotSetBranch = isSetBranchResult.falseBranch()) {
                isNotSetBranch.assign(cloneCollection,
                        isNotSetBranch.newInstance(MethodDescriptor.ofConstructor(ArrayList.class, int.class), size));
            }
        }
        var iterator = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class), toClone);

        try (var whileLoopBlock = bytecodeCreator.whileLoop(conditionBytecode -> {
            ResultHandle hasNext = conditionBytecode
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class), iterator);
            return conditionBytecode.ifTrue(hasNext);
        }).block()) {
            Class<?> elementClass;
            java.lang.reflect.Type elementClassType;
            if (type instanceof ParameterizedType parameterizedType) {
                // Assume Collection follow Collection<T> convention of first type argument = element class
                elementClassType = parameterizedType.getActualTypeArguments()[0];
                if (elementClassType instanceof Class<?> class1) {
                    elementClass = class1;
                } else if (elementClassType instanceof ParameterizedType parameterizedElementClassType) {
                    elementClass = (Class<?>) parameterizedElementClassType.getRawType();
                } else if (elementClassType instanceof WildcardType wildcardType) {
                    elementClass = (Class<?>) wildcardType.getUpperBounds()[0];
                } else {
                    throw new IllegalStateException("Unhandled type " + elementClassType + ".");
                }
            } else {
                throw new IllegalStateException("Cannot infer element type for Collection type (" + type + ").");
            }

            // Odd case of member get and set being on different classes; will work as we only
            // use get on the original and set on the clone.
            var next =
                    whileLoopBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "next", Object.class),
                            iterator);
            var clonedElement = whileLoopBlock.createVariable(elementClass);
            writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(whileLoopBlock),
                    elementClass, elementClassType, next, clonedElement);
            whileLoopBlock.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Collection.class, "add", boolean.class, Object.class),
                    cloneCollection,
                    clonedElement);
        }
        bytecodeCreator.assign(cloneResultHolder, cloneCollection);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For a Map
     * Map clone = new ActualMapType();
     * Iterator iterator = toClone.entrySet().iterator();
     * while (iterator.hasNext()) {
     *      Entry next = iterator.next();
     *      Object toCloneValue = next.getValue();
     *      nextClone = (result from recursion on toCloneValue);
     *      clone.put(next.getKey(), nextClone);
     * }
     * cloneResultHolder = clone;
     * </pre>
     **/
    private void writeDeepCloneMapInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, ResultHandle toClone,
            AssignableResultHandle cloneResultHolder) {
        var bytecodeCreator = clonerMethodDescriptor.bytecodeCreator;
        ResultHandle cloneMap;

        if (PlanningCloneable.class.isAssignableFrom(deeplyClonedFieldClass)) {
            var emptyInstance = bytecodeCreator
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance",
                            Object.class), bytecodeCreator.checkCast(toClone, PlanningCloneable.class));
            cloneMap = bytecodeCreator.checkCast(emptyInstance, Map.class);
        } else {
            var holderClass = deeplyClonedFieldClass;
            try {
                holderClass.getConstructor();
            } catch (NoSuchMethodException e) {
                if (LinkedHashMap.class.isAssignableFrom(holderClass)) {
                    holderClass = LinkedHashMap.class;
                } else if (ConcurrentHashMap.class.isAssignableFrom(holderClass)) {
                    holderClass = ConcurrentHashMap.class;
                } else {
                    // Default to LinkedHashMap
                    holderClass = LinkedHashMap.class;
                }
            }

            var size =
                    bytecodeCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.class, "size", int.class), toClone);
            try {
                holderClass.getConstructor(int.class);
                cloneMap = bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(holderClass, int.class), size);
            } catch (NoSuchMethodException e) {
                cloneMap = bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(holderClass));
            }
        }

        var entrySet = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.class, "entrySet", Set.class), toClone);
        var iterator = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class), entrySet);

        var whileLoopBlock = bytecodeCreator.whileLoop(conditionBytecode -> {
            ResultHandle hasNext = conditionBytecode
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class), iterator);
            return conditionBytecode.ifTrue(hasNext);
        }).block();

        Class<?> keyClass;
        Class<?> elementClass;
        java.lang.reflect.Type keyType;
        java.lang.reflect.Type elementClassType;
        if (type instanceof ParameterizedType parameterizedType) {
            // Assume Map follow Map<K,V> convention of second type argument = value class
            keyType = parameterizedType.getActualTypeArguments()[0];
            elementClassType = parameterizedType.getActualTypeArguments()[1];
            if (elementClassType instanceof Class<?> class1) {
                elementClass = class1;
            } else if (elementClassType instanceof ParameterizedType parameterizedElementClassType) {
                elementClass = (Class<?>) parameterizedElementClassType.getRawType();
            } else {
                throw new IllegalStateException("Unhandled type " + elementClassType + ".");
            }

            if (keyType instanceof Class<?> class1) {
                keyClass = class1;
            } else if (keyType instanceof ParameterizedType parameterizedElementClassType) {
                keyClass = (Class<?>) parameterizedElementClassType.getRawType();
            } else {
                throw new IllegalStateException("Unhandled type " + keyType + ".");
            }
        } else {
            throw new IllegalStateException("Cannot infer element type for Map type (" + type + ").");
        }

        var entitySubclasses = clonerDescriptor.deepClonedClassesSortedSet.stream()
                .filter(keyClass::isAssignableFrom).toList();
        var entry = whileLoopBlock
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "next", Object.class), iterator);
        var toCloneValue = whileLoopBlock
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.Entry.class, "getValue", Object.class), entry);

        var clonedElement = whileLoopBlock.createVariable(elementClass);
        writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(whileLoopBlock),
                elementClass, elementClassType, toCloneValue, clonedElement);

        var key = whileLoopBlock
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.Entry.class, "getKey", Object.class), entry);
        if (!entitySubclasses.isEmpty()) {
            var keyCloneResultHolder = whileLoopBlock.createVariable(keyClass);
            writeDeepCloneEntityOrFactInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(whileLoopBlock),
                    keyClass,
                    key, keyCloneResultHolder, UnhandledCloneType.DEEP);
            whileLoopBlock.invokeInterfaceMethod(
                    PUT_METHOD,
                    cloneMap, keyCloneResultHolder, clonedElement);
        } else {
            whileLoopBlock.invokeInterfaceMethod(
                    PUT_METHOD,
                    cloneMap, key, clonedElement);
        }

        bytecodeCreator.assign(cloneResultHolder, cloneMap);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For an array
     * Object[] clone = new Object[toClone.length];
     *
     * for (int i = 0; i < original.length; i++) {
     *     clone[i] = (result from recursion on toClone[i]);
     * }
     * cloneResultHolder = clone;
     * </pre>
     **/
    private void writeDeepCloneArrayInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, ResultHandle toClone, AssignableResultHandle cloneResultHolder) {
        var bytecodeCreator = clonerMethodDescriptor.bytecodeCreator;
        // Clone array
        var arrayComponent = deeplyClonedFieldClass.getComponentType();
        var arrayLength = bytecodeCreator.arrayLength(toClone);
        var arrayClone = bytecodeCreator.newArray(arrayComponent, arrayLength);
        var iterations = bytecodeCreator.createVariable(int.class);
        bytecodeCreator.assign(iterations, bytecodeCreator.load(0));
        var whileLoopBlock = bytecodeCreator
                .whileLoop(conditionBytecode -> conditionBytecode.ifIntegerLessThan(iterations, arrayLength))
                .block();
        var toCloneElement = whileLoopBlock.readArrayValue(toClone, iterations);
        var clonedElement = whileLoopBlock.createVariable(arrayComponent);

        writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor.withBytecodeCreator(whileLoopBlock),
                arrayComponent,
                arrayComponent, toCloneElement, clonedElement);
        whileLoopBlock.writeArrayValue(arrayClone, iterations, clonedElement);
        whileLoopBlock.assign(iterations, whileLoopBlock.increment(iterations));

        bytecodeCreator.assign(cloneResultHolder, arrayClone);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For an entity
     * if (toClone instanceof SubclassOfEntity1) {
     *     SubclassOfEntity1 clone = new SubclassOfEntity1();
     *
     *     // shallowly clone fields using writeShallowCloneInstructions()
     *     // for any deeply cloned field, do recursion on it
     *     cloneResultHolder = clone;
     * } else if (toClone instanceof SubclassOfEntity2) {
     *     // ...
     * }
     * // ...
     * else if (toClone instanceof SubclassOfEntityN) {
     *     // ...
     * } else {
     *     // shallow or deep clone based on whether deep cloning is forced
     * }
     * </pre>
     **/
    private void writeDeepCloneEntityOrFactInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass,
            ResultHandle toClone,
            AssignableResultHandle cloneResultHolder,
            UnhandledCloneType unhandledCloneType) {
        var deepClonedSubclasses = clonerDescriptor.deepClonedClassesSortedSet.stream()
                .filter(deeplyClonedFieldClass::isAssignableFrom)
                .filter(type -> DeepCloningUtils.isClassDeepCloned(
                        clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor(),
                        type))
                .toList();
        var currentBranch = clonerMethodDescriptor.bytecodeCreator;
        // If the field holds an instance of one of the field's declared type's subtypes, clone the subtype instead.
        for (Class<?> deepClonedSubclass : deepClonedSubclasses) {
            var isInstance = currentBranch.instanceOf(toClone, deepClonedSubclass);
            var isInstanceBranchResult = currentBranch.ifTrue(isInstance);
            try (var isInstanceBranch = isInstanceBranchResult.trueBranch()) {
                var cloneObj = isInstanceBranch.invokeStaticMethod(
                        MethodDescriptor.ofMethod(
                                GizmoSolutionClonerFactory
                                        .getGeneratedClassName(clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor()),
                                getEntityHelperMethodName(deepClonedSubclass), deepClonedSubclass, deepClonedSubclass,
                                Map.class,
                                boolean.class, ArrayDeque.class),
                        toClone, clonerMethodDescriptor.createdCloneMap, currentBranch.load(clonerMethodDescriptor.isBottom),
                        clonerMethodDescriptor.cloneQueue);
                isInstanceBranch.assign(cloneResultHolder, cloneObj);
            }
            currentBranch.close();
            currentBranch = isInstanceBranchResult.falseBranch();
        }
        // We are certain that the instance is of the same type as the declared field type.
        // (Or is an undeclared subclass of the planning entity)
        switch (unhandledCloneType) {
            case SHALLOW -> currentBranch.assign(cloneResultHolder, toClone);
            case DEEP -> {
                var cloneObj = currentBranch.invokeStaticMethod(
                        MethodDescriptor.ofMethod(
                                GizmoSolutionClonerFactory
                                        .getGeneratedClassName(clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor()),
                                getEntityHelperMethodName(deeplyClonedFieldClass), deeplyClonedFieldClass,
                                deeplyClonedFieldClass, Map.class, boolean.class, ArrayDeque.class),
                        toClone, clonerMethodDescriptor.createdCloneMap, currentBranch.load(clonerMethodDescriptor.isBottom),
                        clonerMethodDescriptor.cloneQueue);
                currentBranch.assign(cloneResultHolder, cloneObj);
            }
        }
        currentBranch.close();
    }

    protected String getEntityHelperMethodName(Class<?> entityClass) {
        return "$clone" + entityClass.getName().replace('.', '_');
    }

    /**
     * Writes the following code:
     * <p>
     * In Quarkus: (nothing)
     * <p>
     * Outside Quarkus:
     *
     * <pre>
     * if (toClone.getClass() != entityClass) {
     *     Cloner.fallbackCloner.gizmoFallbackDeepClone(toClone, cloneMap);
     * } else {
     *     ...
     * }
     * </pre>
     *
     * @return The else branch {@link BytecodeCreator} outside of Quarkus, the original bytecodeCreator otherwise.
     */
    protected BytecodeCreator createUnknownClassHandler(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> entityClass,
            ResultHandle toClone) {
        var actualClass =
                clonerMethodDescriptor.bytecodeCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(Object.class, "getClass", Class.class),
                        toClone);
        var branchResult = clonerMethodDescriptor.bytecodeCreator.ifReferencesNotEqual(actualClass,
                clonerMethodDescriptor.bytecodeCreator.loadClass(entityClass));

        try (var currentBranch = branchResult.trueBranch()) {
            var fallbackCloner =
                    currentBranch.readStaticField(FieldDescriptor.of(
                            GizmoSolutionClonerFactory.getGeneratedClassName(clonerDescriptor.solutionDescriptor),
                            FALLBACK_CLONER, FieldAccessingSolutionCloner.class));
            var cloneObj =
                    currentBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(FieldAccessingSolutionCloner.class,
                            "gizmoFallbackDeepClone", Object.class, Object.class, Map.class),
                            fallbackCloner, toClone, clonerMethodDescriptor.createdCloneMap);
            currentBranch.returnValue(cloneObj);
        }

        return branchResult.falseBranch();
    }

    /**
     * Writes the following method:
     *
     * <pre>
     * public static Entity $cloneEntity(Entity entity, Map cloneMap, boolean isBottom, ArrayDeque cloneQueue) {
     *     var existingClonedEntity = (Entity) cloneMap.get(entity);
     *     if (existingClonedEntity != null) {
     *         return existingClonedEntity;
     *     }
     *     final var clonedEntity = new Entity();
     *     clonedEntity.shallowField1 = entity.shallowField1;
     *     // ...
     *     clonedEntity.shallowFieldN = entity.shallowFieldN;
     *     cloneQueue.push(() -> clonedEntity.deepClonedField1 = ...);
     *     // ...
     *     cloneQueue.push(() -> clonedEntity.deepClonedFieldN = ...);
     *     if (isBottom) {
     *         while (!cloneQueue.isEmpty()) {
     *             cloneQueue.pop().run();
     *         }
     *     }
     * }
     * </pre>
     *
     * The cloneQueue is to prevent stack overflow on chained models, or models
     * where many entities can be reached from a single entity.
     **/
    private void createDeepCloneHelperMethod(ClonerDescriptor clonerDescriptor,
            Class<?> entityClass) {
        var methodCreator =
                clonerDescriptor.classCreator.getMethodCreator(getEntityHelperMethodName(entityClass), entityClass, entityClass,
                        Map.class, boolean.class, ArrayDeque.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

        var entityDescriptor =
                clonerDescriptor.memoizedSolutionOrEntityDescriptorMap.computeIfAbsent(entityClass,
                        key -> new GizmoSolutionOrEntityDescriptor(clonerDescriptor.solutionDescriptor, entityClass));

        var toClone = methodCreator.getMethodParam(0);
        var cloneMap = methodCreator.getMethodParam(1);
        var isBottom = methodCreator.getMethodParam(2);
        var cloneQueue = methodCreator.getMethodParam(3);

        var maybeClone = methodCreator.invokeInterfaceMethod(
                GET_METHOD, cloneMap, toClone);
        var hasCloneBranchResult = methodCreator.ifNotNull(maybeClone);
        try (var hasCloneBranch = hasCloneBranchResult.trueBranch()) {
            hasCloneBranch.returnValue(maybeClone);
        }

        try (var originalNoCloneBranch = hasCloneBranchResult.falseBranch()) {
            try (var newNoCloneBranch = createUnknownClassHandler(clonerDescriptor,
                    new ClonerMethodDescriptor(entityDescriptor, originalNoCloneBranch, cloneMap, false, cloneQueue),
                    entityClass,
                    toClone)) {
                ResultHandle cloneObj;
                if (PlanningCloneable.class.isAssignableFrom(entityClass)) {
                    cloneObj = newNoCloneBranch.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance", Object.class),
                            toClone);
                    cloneObj = newNoCloneBranch.checkCast(cloneObj, entityClass);
                } else {
                    cloneObj = newNoCloneBranch.newInstance(MethodDescriptor.ofConstructor(entityClass));
                }
                newNoCloneBranch.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                        cloneMap, toClone, cloneObj);

                // When deep cloning fields, they cannot be the first entity in the stack, since
                // the current entity is below them in the stack.
                var clonerMethodDescriptor =
                        new ClonerMethodDescriptor(entityDescriptor, newNoCloneBranch, cloneMap, false, cloneQueue);
                for (GizmoMemberDescriptor shallowlyClonedField : entityDescriptor.getShallowClonedMemberDescriptors()) {
                    writeShallowCloneInstructions(clonerDescriptor, clonerMethodDescriptor, shallowlyClonedField, toClone,
                            cloneObj);
                }

                for (Field deeplyClonedField : entityDescriptor.getDeepClonedFields()) {
                    var gizmoMemberDescriptor =
                            entityDescriptor.getMemberDescriptorForField(deeplyClonedField);

                    // Initialize the field inside a Runnable (BiConsumer here since you
                    // cannot share ResultHandles across different BytecodeCreators).
                    var consumer = newNoCloneBranch.createFunction(BiConsumer.class);
                    var consumerCreator = consumer.getBytecode();
                    var subfieldValue = gizmoMemberDescriptor.readMemberValue(consumerCreator, toClone);

                    var cloneValue = consumerCreator.createVariable(deeplyClonedField.getType());
                    writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor
                            .withBytecodeCreator(consumerCreator)
                            .withCreatedCloneMap(consumerCreator.getMethodParam(0))
                            .withCloneQueue(consumerCreator.getMethodParam(1)),
                            deeplyClonedField, gizmoMemberDescriptor, subfieldValue,
                            cloneValue);

                    if (!gizmoMemberDescriptor.writeMemberValue(consumerCreator, cloneObj, cloneValue)) {
                        throw new IllegalStateException("The member (" + gizmoMemberDescriptor.getName() + ") of class (" +
                                gizmoMemberDescriptor.getDeclaringClassName() + ") does not have a setter.");
                    }
                    consumerCreator.returnVoid();

                    // Add the initializer to the queue
                    newNoCloneBranch.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(ArrayDeque.class, "push", void.class, Object.class),
                            cloneQueue, consumer.getInstance());
                }

                // To prevent stack overflow, only the bottom/first encountered entity can
                // create new deep cloned objects. Deep-cloned fields add their initializers
                // (which potentially create a new deep cloned object) to the queue, and we
                // iterate through the queue until it is empty, at which point this object
                // is fully initialized.
                try (var bottomObjectBranch = newNoCloneBranch.ifTrue(isBottom).trueBranch()) {
                    try (var queueNotEmptyBlock = bottomObjectBranch.whileLoop(condition -> condition.ifFalse(
                            condition.invokeVirtualMethod(MethodDescriptor.ofMethod(ArrayDeque.class, "isEmpty", boolean.class),
                                    cloneQueue)))
                            .block()) {
                        var next = queueNotEmptyBlock
                                .invokeVirtualMethod(MethodDescriptor.ofMethod(ArrayDeque.class, "pop", Object.class),
                                        cloneQueue);
                        queueNotEmptyBlock.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(BiConsumer.class, "accept", void.class, Object.class, Object.class),
                                queueNotEmptyBlock.checkCast(next, BiConsumer.class), cloneMap, cloneQueue);
                    }
                }

                newNoCloneBranch.returnValue(cloneObj);
            }
        }
    }

    protected void createAbstractDeepCloneHelperMethod(ClonerDescriptor clonerDescriptor,
            Class<?> entityClass) {
        var methodCreator =
                clonerDescriptor.classCreator.getMethodCreator(getEntityHelperMethodName(entityClass), entityClass, entityClass,
                        Map.class, boolean.class, ArrayDeque.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

        var toClone = methodCreator.getMethodParam(0);
        var cloneMap = methodCreator.getMethodParam(1);
        var maybeClone = methodCreator.invokeInterfaceMethod(
                GET_METHOD, cloneMap, toClone);
        var hasCloneBranchResult = methodCreator.ifNotNull(maybeClone);
        try (var hasCloneBranch = hasCloneBranchResult.trueBranch()) {
            hasCloneBranch.returnValue(maybeClone);
        }

        try (var noCloneBranch = hasCloneBranchResult.falseBranch()) {
            var fallbackCloner =
                    noCloneBranch.readStaticField(FieldDescriptor.of(
                            GizmoSolutionClonerFactory.getGeneratedClassName(clonerDescriptor.solutionDescriptor),
                            FALLBACK_CLONER, FieldAccessingSolutionCloner.class));
            var cloneObj =
                    noCloneBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(FieldAccessingSolutionCloner.class,
                            "gizmoFallbackDeepClone", Object.class, Object.class, Map.class),
                            fallbackCloner, toClone, cloneMap);
            noCloneBranch.returnValue(cloneObj);
        }
    }

    private enum UnhandledCloneType {
        SHALLOW,
        DEEP
    }
}
