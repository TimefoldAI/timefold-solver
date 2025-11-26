package ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class GizmoSolutionClonerImplementor {
    private static final String FALLBACK_CLONER = "fallbackCloner";
    public static final boolean DEBUG = false;

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
                isChanged |= classToSubclassLevel.keySet().stream()
                        .filter(otherClazz -> otherClazz != clazz && otherClazz.isAssignableFrom(clazz))
                        .map(classToSubclassLevel::get)
                        .max(Integer::compare)
                        .map(subclassLevel -> {
                            var oldVal = (int) classToSubclassLevel.getOrDefault(clazz, -1);
                            var newVal = subclassLevel + 1;
                            if (newVal > oldVal) {
                                classToSubclassLevel.put(clazz, newVal);
                                return true;
                            }
                            return false;
                        }).orElse(false);
            }
        }

        return Comparator.<Class<?>, Integer> comparing(classToSubclassLevel::get)
                .thenComparing(Class::getName).reversed();
    }

    protected ClonerDescriptor withFallbackClonerField(ClonerDescriptor clonerDescriptor) {
        return clonerDescriptor.withFallbackClonerField(clonerDescriptor.classCreator.staticField(FALLBACK_CLONER, field -> {
            field.private_();
            field.setType(FieldAccessingSolutionCloner.class);
        }));
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
                classCreator, null);

        classCreator.defaultConstructor();

        clonerDescriptor = implementor.withFallbackClonerField(clonerDescriptor);
        implementor.createSetSolutionDescriptor(clonerDescriptor);

        createCloneSolutionRun(clonerDescriptor, solutionClassSet, instanceOfComparator);
        createCloneSolution(clonerDescriptor);

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

    public static ClassOutput createClassOutputWithDebuggingCapability(Map<String, byte[]> classBytecodeHolder) {
        return new GizmoSolutionClonerClassOutput(classBytecodeHolder);
    }

    static <T> SolutionCloner<T> createClonerFor(SolutionDescriptor<T> solutionDescriptor,
            GizmoClassLoader gizmoClassLoader) {
        var implementor = new GizmoSolutionClonerImplementor();
        var className = GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor);
        if (gizmoClassLoader.hasBytecodeFor(className)) {
            return implementor.createInstance(className, gizmoClassLoader, solutionDescriptor);
        }
        var classBytecodeHolder = new HashMap<String, byte[]>();

        var gizmo = Gizmo.create(createClassOutputWithDebuggingCapability(classBytecodeHolder));
        gizmo.class_(className, classCreator -> {
            classCreator.implements_(GizmoSolutionCloner.class);
            classCreator.extends_(Object.class);
            classCreator.final_();

            var deepClonedClassSet = GizmoCloningUtils.getDeepClonedClasses(solutionDescriptor, Collections.emptyList());

            defineClonerFor(() -> implementor, classCreator, solutionDescriptor,
                    Collections.singleton(solutionDescriptor.getSolutionClass()),
                    new HashMap<>(), deepClonedClassSet);
        });

        for (var bytecodeEntry : classBytecodeHolder.entrySet()) {
            gizmoClassLoader.storeBytecode(bytecodeEntry.getKey(), bytecodeEntry.getValue());
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

    protected void createSetSolutionDescriptor(ClonerDescriptor clonerDescriptor) {
        clonerDescriptor.classCreator.method("setSolutionDescriptor", methodCreator -> {
            methodCreator.public_();
            methodCreator.returning(void.class);
            var solutionDescriptor = methodCreator.parameter("solutionDescriptor", SolutionDescriptor.class);
            methodCreator.body(blockCreator -> {
                blockCreator.set(clonerDescriptor.fallbackClonerField,
                        blockCreator.new_(FieldAccessingSolutionCloner.class, solutionDescriptor));
                blockCreator.return_();
            });
        });
    }

    private static void createCloneSolution(ClonerDescriptor clonerDescriptor) {
        var solutionClass = clonerDescriptor.solutionDescriptor.getSolutionClass();
        clonerDescriptor.classCreator.method("cloneSolution", methodCreator -> {
            methodCreator.returning(Object.class);
            var original = methodCreator.parameter("original", Object.class);
            methodCreator.body(blockCreator -> {
                var clone = blockCreator.invokeStatic(
                        ClassMethodDesc.of(
                                ClassDesc.of(
                                        GizmoSolutionClonerFactory.getGeneratedClassName(clonerDescriptor.solutionDescriptor)),
                                "cloneSolutionRun", solutionClass, solutionClass, Map.class),
                        original,
                        blockCreator.new_(IdentityHashMap.class));
                blockCreator.return_(clone);
            });
        });
    }

    private static void createCloneSolutionRun(ClonerDescriptor clonerDescriptor,
            Set<Class<?>> solutionClassSet, Comparator<Class<?>> instanceOfComparator) {
        var solutionClass = clonerDescriptor.solutionDescriptor.getSolutionClass();

        clonerDescriptor.classCreator.staticMethod("cloneSolutionRun", methodCreator -> {
            methodCreator.public_();
            methodCreator.returning(solutionClass);
            var thisObj = methodCreator.parameter("original", solutionClass);
            var createdCloneMap = methodCreator.parameter("cloneMap", Map.class);
            methodCreator.body(blockCreator -> {
                blockCreator.ifNull(thisObj, BlockCreator::returnNull);
                var maybeClone = blockCreator.localVar("existingClone", solutionClass,
                        blockCreator.withMap(createdCloneMap).get(thisObj));
                blockCreator.ifNotNull(maybeClone, hasCloneBranch -> hasCloneBranch.return_(maybeClone));

                var sortedSolutionClassList = new ArrayList<>(solutionClassSet);
                sortedSolutionClassList.sort(instanceOfComparator);

                var thisObjClass =
                        blockCreator.localVar("clonedObjectClass",
                                blockCreator.withObject(thisObj).getClass_());
                for (Class<?> solutionSubclass : sortedSolutionClassList) {
                    var solutionSubclassConst = Const.of(solutionSubclass);
                    var isSubclass = blockCreator.objEquals(solutionSubclassConst, thisObjClass);
                    blockCreator.if_(isSubclass, isExactMatchBranch -> {
                        // Note: it appears Gizmo2 does not have a way to cast expressions, so we need to
                        //       use an ifInstanceOf to get a casted version
                        //       uncheckedCast does NOT do a checkcast, and will fail class verification
                        isExactMatchBranch.ifInstanceOf(thisObj, solutionSubclass,
                                (isExactMatchWithCastBranch, castedSolution) -> {
                                    var solutionSubclassDescriptor =
                                            clonerDescriptor.memoizedSolutionOrEntityDescriptorMap.computeIfAbsent(
                                                    solutionSubclass,
                                                    key -> new GizmoSolutionOrEntityDescriptor(
                                                            clonerDescriptor.solutionDescriptor,
                                                            solutionSubclass));

                                    var clone = isExactMatchWithCastBranch.localVar("newClone", solutionSubclass,
                                            Const.ofNull(solutionSubclass));
                                    if (PlanningCloneable.class.isAssignableFrom(solutionSubclass)) {
                                        var newInstance = isExactMatchWithCastBranch.invokeInterface(
                                                MethodDesc.of(PlanningCloneable.class, "createNewInstance", Object.class),
                                                castedSolution);
                                        isExactMatchWithCastBranch.set(clone, newInstance);
                                    } else {
                                        isExactMatchWithCastBranch.set(clone,
                                                isExactMatchWithCastBranch.new_(ConstructorDesc.of(solutionSubclass)));
                                    }
                                    isExactMatchWithCastBranch.withMap(createdCloneMap).put(castedSolution, clone);

                                    cloneShallowlyClonedFieldsOfObject(solutionSubclassDescriptor, clonerDescriptor,
                                            new ClonerMethodDescriptor(
                                                    solutionSubclassDescriptor,
                                                    isExactMatchWithCastBranch, createdCloneMap,
                                                    true,
                                                    isExactMatchWithCastBranch.localVar("cloneQueue",
                                                            isExactMatchWithCastBranch
                                                                    .new_(ConstructorDesc.of(ArrayDeque.class)))),
                                            castedSolution, clone);
                                    cloneDeepClonedFieldsOfSolution(clonerDescriptor, solutionSubclassDescriptor,
                                            isExactMatchWithCastBranch,
                                            castedSolution,
                                            createdCloneMap, clone);

                                    isExactMatchWithCastBranch.return_(clone);
                                });
                    });
                }
                var errorBuilder = blockCreator.localVar("errorMessageBuilder",
                        blockCreator.new_(ConstructorDesc.of(StringBuilder.class, String.class),
                                Const.of("Failed to create clone: encountered (")));

                var errorTemplate =
                        """
                                which is not a known subclass of the solution class (%s).
                                The known subclasses are: %s.
                                Maybe use DomainAccessType.REFLECTION?
                                """.formatted(
                                clonerDescriptor.solutionDescriptor.getSolutionClass(),
                                solutionClassSet.stream().map(Class::getName).collect(Collectors.joining(", ", "[", "]")));
                var APPEND =
                        MethodDesc.of(StringBuilder.class, "append", StringBuilder.class, Object.class);

                blockCreator.invokeVirtual(APPEND, errorBuilder, thisObjClass);
                blockCreator.invokeVirtual(APPEND, errorBuilder, Const.of(") " + errorTemplate));
                var errorMsg = blockCreator
                        .invokeVirtual(MethodDesc.of(Object.class, "toString", String.class), errorBuilder);
                var error = blockCreator
                        .new_(ConstructorDesc.of(IllegalArgumentException.class, String.class), errorMsg);
                blockCreator.throw_(error);
            });
        });
    }

    private static void cloneDeepClonedFieldsOfSolution(ClonerDescriptor clonerDescriptor,
            GizmoSolutionOrEntityDescriptor solutionSubclassDescriptor,
            BlockCreator isSubclassBranch, Var thisObj, Var createdCloneMap, Var clone) {
        for (Field deeplyClonedField : solutionSubclassDescriptor.getDeepClonedFields()) {
            var gizmoMemberDescriptor =
                    solutionSubclassDescriptor.getMemberDescriptorForField(deeplyClonedField);

            var fieldValue = isSubclassBranch.localVar(deeplyClonedField.getName() + "$Value",
                    gizmoMemberDescriptor.readMemberValue(isSubclassBranch, thisObj));
            var cloneValue = isSubclassBranch.localVar(deeplyClonedField.getName() + "$Clone", deeplyClonedField.getType(),
                    Const.ofNull(deeplyClonedField.getType()));
            writeDeepCloneInstructions(clonerDescriptor,
                    new ClonerMethodDescriptor(solutionSubclassDescriptor, isSubclassBranch, createdCloneMap,
                            true,
                            isSubclassBranch.localVar(deeplyClonedField.getName() + "$Queue",
                                    isSubclassBranch.new_(ArrayDeque.class))),
                    deeplyClonedField,
                    gizmoMemberDescriptor, fieldValue, cloneValue);

            if (!gizmoMemberDescriptor.writeMemberValue(isSubclassBranch, clone, cloneValue)) {
                throw new IllegalStateException("The member (%s) of class (%s) does not have a setter.".formatted(
                        gizmoMemberDescriptor.getName(), gizmoMemberDescriptor.getDeclaringClassName()));
            }
        }
    }

    private static void cloneShallowlyClonedFieldsOfObject(GizmoSolutionOrEntityDescriptor solutionSubclassDescriptor,
            ClonerDescriptor clonerDescriptor, ClonerMethodDescriptor solutionSubclassDescriptor1, Var thisObj,
            Var clone) {
        for (GizmoMemberDescriptor shallowlyClonedField : solutionSubclassDescriptor
                .getShallowClonedMemberDescriptors()) {
            writeShallowCloneInstructions(clonerDescriptor, solutionSubclassDescriptor1,
                    shallowlyClonedField, thisObj, clone);
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
    private static void writeShallowCloneInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            GizmoMemberDescriptor shallowlyClonedField,
            Var thisObj, Var clone) {
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

            var fieldValue = clonerMethodDescriptor.blockCreator.localVar(shallowlyClonedField.getName() + "$Value",
                    shallowlyClonedField.readMemberValue(clonerMethodDescriptor.blockCreator, thisObj));
            if (!entitySubclasses.isEmpty()) {
                var cloneResultHolder = clonerMethodDescriptor.blockCreator
                        .localVar(shallowlyClonedField.getName() + "$Clone", type, Const.ofNull(type));
                writeDeepCloneEntityOrFactInstructions(clonerDescriptor,
                        clonerMethodDescriptor,
                        type,
                        fieldValue, cloneResultHolder,
                        UnhandledCloneType.SHALLOW);
                fieldValue = cloneResultHolder;
            }
            if (!shallowlyClonedField.writeMemberValue(clonerMethodDescriptor.blockCreator, clone, fieldValue)) {
                throw new IllegalStateException("Field (%s) of class (%s) does not have a setter.".formatted(
                        shallowlyClonedField.getName(), shallowlyClonedField.getDeclaringClassName()));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Error creating Gizmo Solution Cloner", e);
        }
    }

    /**
     * @see #writeDeepCloneInstructions(ClonerDescriptor, ClonerMethodDescriptor, Class, Type, Var,
     *      Var)
     */
    private static void writeDeepCloneInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Field deeplyClonedField,
            GizmoMemberDescriptor gizmoMemberDescriptor, Var toClone, Var cloneResultHolder) {
        BlockCreator blockCreator = clonerMethodDescriptor.blockCreator;

        blockCreator.ifNull(toClone,
                isNullBranch -> isNullBranch.set(cloneResultHolder, Const.ofNull(cloneResultHolder.type())));

        blockCreator.ifNotNull(toClone, isNotNullBranch -> {
            var deeplyClonedFieldClass = deeplyClonedField.getType();
            var type = gizmoMemberDescriptor.getType();
            if (clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass()
                    .isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneSolutionInstructions(clonerMethodDescriptor.withBlockCreator(isNotNullBranch), toClone,
                        cloneResultHolder);
            } else if (Collection.class.isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneCollectionInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (Map.class.isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneMapInstructions(clonerDescriptor, clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (deeplyClonedFieldClass.isArray()) {
                writeDeepCloneArrayInstructions(clonerDescriptor, clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
                        deeplyClonedFieldClass, toClone, cloneResultHolder);
            } else {
                var unknownClassCloneType =
                        (DeepCloningUtils.isFieldDeepCloned(clonerMethodDescriptor.entityDescriptor.solutionDescriptor,
                                deeplyClonedField, deeplyClonedField.getDeclaringClass()))
                                        ? UnhandledCloneType.DEEP
                                        : UnhandledCloneType.SHALLOW;
                writeDeepCloneEntityOrFactInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
                        deeplyClonedFieldClass,
                        toClone, cloneResultHolder, unknownClassCloneType);
            }
        });
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
    private static void writeDeepCloneInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, Var toClone,
            Var cloneResultHolder) {
        BlockCreator blockCreator = clonerMethodDescriptor.blockCreator;

        blockCreator.ifNull(toClone,
                ifNullBranch -> ifNullBranch.set(cloneResultHolder, Const.ofNull(cloneResultHolder.type())));

        blockCreator.ifNotNull(toClone, isNotNullBranch -> {
            if (clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass()
                    .isAssignableFrom(deeplyClonedFieldClass)) {
                writeDeepCloneSolutionInstructions(clonerMethodDescriptor, toClone, cloneResultHolder);
            } else if (Collection.class.isAssignableFrom(deeplyClonedFieldClass)) {
                // Clone collection
                writeDeepCloneCollectionInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (Map.class.isAssignableFrom(deeplyClonedFieldClass)) {
                // Clone map
                writeDeepCloneMapInstructions(clonerDescriptor, clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
                        deeplyClonedFieldClass, type,
                        toClone, cloneResultHolder);
            } else if (deeplyClonedFieldClass.isArray()) {
                // Clone array
                writeDeepCloneArrayInstructions(clonerDescriptor, clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
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
                        clonerMethodDescriptor.withBlockCreator(isNotNullBranch),
                        deeplyClonedFieldClass,
                        toClone, cloneResultHolder, unknownClassCloneType);
            }
        });
    }

    private static void writeDeepCloneSolutionInstructions(
            ClonerMethodDescriptor clonerMethodDescriptor, Var toClone, Var cloneResultHolder) {
        clonerMethodDescriptor.blockCreator.ifNull(toClone,
                isNullBranch -> isNullBranch.set(cloneResultHolder, Const.ofNull(cloneResultHolder.type())));
        clonerMethodDescriptor.blockCreator.ifNotNull(toClone, isNotNullBranch -> {
            var clone = isNotNullBranch.invokeStatic(
                    ClassMethodDesc.of(
                            ClassDesc.of(GizmoSolutionClonerFactory
                                    .getGeneratedClassName(clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor())),
                            "cloneSolutionRun",
                            clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass(),
                            clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor().getSolutionClass(), Map.class),
                    toClone,
                    clonerMethodDescriptor.createdCloneMap);
            isNotNullBranch.set(cloneResultHolder, clone);
        });
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
    private static void writeDeepCloneCollectionInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, Var toClone,
            Var cloneResultHolder) {
        var blockCreator = clonerMethodDescriptor.blockCreator;

        var size = blockCreator.localVar(toClone.name() + "$Size", blockCreator.withCollection(toClone).size());

        if (PlanningCloneable.class.isAssignableFrom(deeplyClonedFieldClass)) {
            var emptyInstance = blockCreator
                    .invokeInterface(MethodDesc.of(PlanningCloneable.class, "createNewInstance",
                            Object.class), toClone);
            blockCreator.set(cloneResultHolder, emptyInstance);
        } else if (List.class.isAssignableFrom(deeplyClonedFieldClass)) {
            blockCreator.set(cloneResultHolder,
                    blockCreator.new_(ConstructorDesc.of(ArrayList.class, int.class), size));
        } else if (Set.class.isAssignableFrom(deeplyClonedFieldClass)) {
            blockCreator.ifInstanceOf(toClone, SortedSet.class, (isSortedSetBranch, castedToClone) -> {
                var setComparator = isSortedSetBranch
                        .invokeInterface(MethodDesc.of(SortedSet.class,
                                "comparator", Comparator.class), castedToClone);
                isSortedSetBranch.set(cloneResultHolder,
                        isSortedSetBranch.new_(ConstructorDesc.of(TreeSet.class, Comparator.class),
                                setComparator));
            });
            blockCreator.ifNotInstanceOf(toClone, SortedSet.class,
                    isNotSortedSetBranch -> isNotSortedSetBranch.set(cloneResultHolder,
                            isNotSortedSetBranch.new_(ConstructorDesc.of(LinkedHashSet.class, int.class), size)));
        } else {
            // field is probably of type collection; determine collection semantics at runtime
            blockCreator.ifInstanceOf(toClone, Set.class, (isSetBranch, castedToClone) -> {
                blockCreator.ifInstanceOf(toClone, SortedSet.class, (isSortedSetBranch, sortedCastedToClone) -> {
                    var setComparator = isSortedSetBranch
                            .invokeInterface(MethodDesc.of(SortedSet.class,
                                    "comparator", Comparator.class), sortedCastedToClone);
                    isSortedSetBranch.set(cloneResultHolder,
                            isSortedSetBranch.new_(ConstructorDesc.of(TreeSet.class, Comparator.class),
                                    setComparator));
                });
                blockCreator.ifNotInstanceOf(toClone, SortedSet.class,
                        isNotSortedSetBranch -> isNotSortedSetBranch.set(cloneResultHolder,
                                isNotSortedSetBranch.new_(ConstructorDesc.of(LinkedHashSet.class, int.class), size)));
            });
            // Default to ArrayList
            blockCreator.ifNotInstanceOf(toClone, Set.class, isNotSetBranch -> isNotSetBranch.set(cloneResultHolder,
                    isNotSetBranch.new_(ConstructorDesc.of(ArrayList.class, int.class), size)));
        }
        var iterator =
                blockCreator.localVar(toClone.name() + "$Iterator", blockCreator.withCollection(toClone).iterator());
        blockCreator.while_(condition -> condition.yield(condition.withIterator(iterator).hasNext()), whileLoopBlock -> {
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
                    throw new IllegalStateException("Unhandled type (%s).".formatted(elementClassType));
                }
            } else {
                throw new IllegalStateException("Cannot infer element type for Collection type (%s).".formatted(type));
            }

            // Odd case of member get and set being on different classes; will work as we only
            // use get on the original and set on the clone.
            var next = whileLoopBlock.localVar(toClone.name() + "$Item", whileLoopBlock.withIterator(iterator).next());
            var clonedElement =
                    whileLoopBlock.localVar(cloneResultHolder.name() + "$Item", elementClass, Const.ofNull(elementClass));
            writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor.withBlockCreator(whileLoopBlock),
                    elementClass, elementClassType, next, clonedElement);
            whileLoopBlock.withCollection(cloneResultHolder).add(clonedElement);
        });
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
    private static void writeDeepCloneMapInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, Var toClone,
            Var cloneResultHolder) {
        var blockCreator = clonerMethodDescriptor.blockCreator;

        if (PlanningCloneable.class.isAssignableFrom(deeplyClonedFieldClass)) {
            var emptyInstance = blockCreator
                    .invokeInterface(MethodDesc.of(PlanningCloneable.class, "createNewInstance",
                            Object.class), toClone);
            blockCreator.set(cloneResultHolder, emptyInstance);
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

            var size = blockCreator.withMap(toClone).size();
            try {
                holderClass.getConstructor(int.class);
                blockCreator.set(cloneResultHolder, blockCreator.new_(ConstructorDesc.of(holderClass, int.class), size));
            } catch (NoSuchMethodException e) {
                blockCreator.set(cloneResultHolder, blockCreator.new_(holderClass));
            }
        }

        var entrySet = blockCreator.withMap(toClone).entrySet();
        var iterator = blockCreator.localVar(toClone.name() + "$EntrySet$Iterator",
                blockCreator.withCollection(entrySet).iterator());

        blockCreator.while_(condition -> condition.yield(condition.withIterator(iterator).hasNext()), whileLoopBlock -> {
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
                    throw new IllegalStateException("Unhandled type (%s).".formatted(elementClassType));
                }

                if (keyType instanceof Class<?> class1) {
                    keyClass = class1;
                } else if (keyType instanceof ParameterizedType parameterizedElementClassType) {
                    keyClass = (Class<?>) parameterizedElementClassType.getRawType();
                } else {
                    throw new IllegalStateException("Unhandled type (%s).".formatted(keyType));
                }
            } else {
                throw new IllegalStateException("Cannot infer element type for Map type (%s).".formatted(type));
            }

            var entitySubclasses = clonerDescriptor.deepClonedClassesSortedSet.stream()
                    .filter(keyClass::isAssignableFrom).toList();
            var entry = whileLoopBlock.localVar(toClone.name() + "$Entry", whileLoopBlock.withIterator(iterator).next());
            var toCloneValue = whileLoopBlock.localVar(toClone.name() + "$Value",
                    whileLoopBlock.invokeInterface(MethodDesc.of(Map.Entry.class, "getValue", Object.class), entry));
            var clonedElement =
                    whileLoopBlock.localVar(cloneResultHolder.name() + "$Element", elementClass, Const.ofNull(elementClass));
            writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor.withBlockCreator(whileLoopBlock),
                    elementClass, elementClassType, toCloneValue, clonedElement);

            var key = whileLoopBlock.localVar(toClone.name() + "$Key",
                    whileLoopBlock.invokeInterface(MethodDesc.of(Map.Entry.class, "getKey", Object.class), entry));
            if (!entitySubclasses.isEmpty()) {
                var keyCloneResultHolder =
                        whileLoopBlock.localVar(cloneResultHolder.name() + "$Key", keyClass, Const.ofNull(keyClass));
                writeDeepCloneEntityOrFactInstructions(clonerDescriptor,
                        clonerMethodDescriptor.withBlockCreator(whileLoopBlock),
                        keyClass,
                        key, keyCloneResultHolder, UnhandledCloneType.DEEP);
                whileLoopBlock.withMap(cloneResultHolder).put(keyCloneResultHolder, clonedElement);
            } else {
                whileLoopBlock.withMap(cloneResultHolder).put(key, clonedElement);
            }
        });
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
    private static void writeDeepCloneArrayInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass, Var toClone, Var cloneResultHolder) {
        var blockCreator = clonerMethodDescriptor.blockCreator;

        // Clone array
        var arrayComponent = deeplyClonedFieldClass.getComponentType();
        var arrayLength = toClone.length();
        blockCreator.set(cloneResultHolder, blockCreator.newEmptyArray(arrayComponent, arrayLength));

        var iterations = blockCreator.localVar("i", Const.of(0));
        blockCreator.while_(condition -> condition.yield(condition.lt(iterations, arrayLength)),
                whileLoopBlock -> {
                    var toCloneElement = whileLoopBlock.localVar(toClone.name() + "$Element", toClone.elem(iterations));
                    var clonedElement = whileLoopBlock.localVar(cloneResultHolder.name() + "$Element", arrayComponent,
                            Const.ofNull(arrayComponent));

                    writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor.withBlockCreator(whileLoopBlock),
                            arrayComponent,
                            arrayComponent, toCloneElement, clonedElement);
                    whileLoopBlock.set(cloneResultHolder.elem(iterations), clonedElement);
                    whileLoopBlock.inc(iterations);
                });
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
    private static void writeDeepCloneEntityOrFactInstructions(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> deeplyClonedFieldClass,
            Var toClone,
            Var cloneResultHolder,
            UnhandledCloneType unhandledCloneType) {
        var deepClonedSubclasses = clonerDescriptor.deepClonedClassesSortedSet.stream()
                .filter(deeplyClonedFieldClass::isAssignableFrom)
                .filter(type -> DeepCloningUtils.isClassDeepCloned(
                        clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor(),
                        type))
                .toList();
        var currentBranch = clonerMethodDescriptor.blockCreator;
        var isHandled = currentBranch.localVar(cloneResultHolder.name() + "$IsHandled", Const.of(false));
        // If the field holds an instance of one of the field's declared type's subtypes, clone the subtype instead.
        for (Class<?> deepClonedSubclass : deepClonedSubclasses) {
            currentBranch.ifNot(isHandled, notHandledBranch -> notHandledBranch.ifInstanceOf(toClone, deepClonedSubclass,
                    (isInstanceBranch, castedToClone) -> {
                        var cloneObj = isInstanceBranch.invokeStatic(
                                ClassMethodDesc.of(
                                        ClassDesc.of(GizmoSolutionClonerFactory
                                                .getGeneratedClassName(
                                                        clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor())),
                                        getEntityHelperMethodName(deepClonedSubclass), deepClonedSubclass, deepClonedSubclass,
                                        Map.class,
                                        boolean.class, ArrayDeque.class),
                                castedToClone, clonerMethodDescriptor.createdCloneMap,
                                Const.of(clonerMethodDescriptor.isBottom),
                                clonerMethodDescriptor.cloneQueue);
                        isInstanceBranch.set(cloneResultHolder, cloneObj);
                        isInstanceBranch.set(isHandled, Const.of(true));
                    }));
        }
        currentBranch.ifNot(isHandled, notHandledBranch -> {
            // We are certain that the instance is of the same type as the declared field type.
            // (Or is an undeclared subclass of the planning entity)
            switch (unhandledCloneType) {
                case SHALLOW -> notHandledBranch.set(cloneResultHolder, toClone);
                case DEEP -> {
                    var cloneObj = notHandledBranch.invokeStatic(
                            ClassMethodDesc.of(
                                    ClassDesc.of(GizmoSolutionClonerFactory
                                            .getGeneratedClassName(
                                                    clonerMethodDescriptor.entityDescriptor.getSolutionDescriptor())),
                                    getEntityHelperMethodName(deeplyClonedFieldClass), deeplyClonedFieldClass,
                                    deeplyClonedFieldClass, Map.class, boolean.class, ArrayDeque.class),
                            toClone, clonerMethodDescriptor.createdCloneMap, Const.of(clonerMethodDescriptor.isBottom),
                            clonerMethodDescriptor.cloneQueue);
                    notHandledBranch.set(cloneResultHolder, cloneObj);
                }
            }
        });
    }

    protected static String getEntityHelperMethodName(Class<?> entityClass) {
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
     *     // code knownClassHandler produces
     * }
     * </pre>
     */
    protected void handleUnknownClass(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Class<?> entityClass,
            Var toClone,
            Consumer<BlockCreator> knownClassHandler) {
        var actualClass = clonerMethodDescriptor.blockCreator.withObject(toClone).getClass_();
        var isClassReferenceNotEqual = clonerMethodDescriptor.blockCreator.localVar("isUnknownClass",
                clonerMethodDescriptor.blockCreator.ne(actualClass, Const.of(entityClass)));

        clonerMethodDescriptor.blockCreator.if_(isClassReferenceNotEqual, currentBranch -> {
            var fallbackCloner = clonerDescriptor.fallbackClonerField;
            var cloneObj =
                    currentBranch.invokeVirtual(MethodDesc.of(FieldAccessingSolutionCloner.class,
                            "gizmoFallbackDeepClone", Object.class, Object.class, Map.class),
                            fallbackCloner, toClone, clonerMethodDescriptor.createdCloneMap);
            currentBranch.return_(cloneObj);
        });

        knownClassHandler.accept(clonerMethodDescriptor.blockCreator);
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
        clonerDescriptor.classCreator.staticMethod(getEntityHelperMethodName(entityClass), methodCreator -> {
            var toClone = methodCreator.parameter("toClone", entityClass);
            var cloneMap = methodCreator.parameter("cloneMap", Map.class);
            var isBottom = methodCreator.parameter("isBottom", boolean.class);
            var cloneQueue = methodCreator.parameter("cloneQueue", ArrayDeque.class);

            methodCreator.returning(entityClass);
            methodCreator.public_();
            methodCreator.body(blockCreator -> {
                var entityDescriptor =
                        clonerDescriptor.memoizedSolutionOrEntityDescriptorMap.computeIfAbsent(entityClass,
                                key -> new GizmoSolutionOrEntityDescriptor(clonerDescriptor.solutionDescriptor, entityClass));
                var maybeClone = blockCreator.localVar("existingClone", blockCreator.withMap(cloneMap).get(toClone));
                blockCreator.ifNotNull(maybeClone, hasCloneBranch -> hasCloneBranch.return_(maybeClone));

                handleUnknownClass(clonerDescriptor,
                        new ClonerMethodDescriptor(entityDescriptor, blockCreator, cloneMap, false, cloneQueue),
                        entityClass,
                        toClone,
                        newNoCloneBranch -> {
                            LocalVar cloneObj =
                                    newNoCloneBranch.localVar("clonedObject", entityClass, Const.ofNull(entityClass));
                            if (PlanningCloneable.class.isAssignableFrom(entityClass)) {
                                newNoCloneBranch.set(cloneObj, newNoCloneBranch.invokeInterface(
                                        MethodDesc.of(PlanningCloneable.class, "createNewInstance", Object.class),
                                        toClone));
                            } else {
                                newNoCloneBranch.set(cloneObj, newNoCloneBranch.new_(entityClass));
                            }
                            newNoCloneBranch.withMap(cloneMap).put(toClone, cloneObj);

                            // When deep cloning fields, they cannot be the first entity in the stack, since
                            // the current entity is below them in the stack.
                            var clonerMethodDescriptor =
                                    new ClonerMethodDescriptor(entityDescriptor, newNoCloneBranch, cloneMap, false, cloneQueue);
                            cloneShallowlyClonedFieldsOfObject(entityDescriptor, clonerDescriptor, clonerMethodDescriptor,
                                    toClone,
                                    cloneObj);

                            for (Field deeplyClonedField : entityDescriptor.getDeepClonedFields()) {
                                addDeepCloneFieldInitializerToQueue(clonerDescriptor, clonerMethodDescriptor, deeplyClonedField,
                                        toClone,
                                        cloneObj);
                            }

                            // To prevent stack overflow, only the bottom/first encountered entity can
                            // create new deep cloned objects. Deep-cloned fields add their initializers
                            // (which potentially create a new deep cloned object) to the queue, and we
                            // iterate through the queue until it is empty, at which point this object
                            // is fully initialized.
                            newNoCloneBranch.if_(isBottom, bottomObjectBranch -> bottomObjectBranch.while_(
                                    condition -> condition.yield(condition.logicalNot(condition.invokeVirtual(
                                            MethodDesc.of(ArrayDeque.class, "isEmpty", boolean.class), cloneQueue))),
                                    queueNotEmptyBlock -> {
                                        var next = queueNotEmptyBlock
                                                .invokeVirtual(MethodDesc.of(ArrayDeque.class, "pop", Object.class),
                                                        cloneQueue);
                                        queueNotEmptyBlock.invokeInterface(
                                                MethodDesc.of(BiConsumer.class, "accept", void.class, Object.class,
                                                        Object.class),
                                                next, cloneMap,
                                                cloneQueue);
                                    }));
                            newNoCloneBranch.return_(cloneObj);
                        });
            });
        });
    }

    private static void addDeepCloneFieldInitializerToQueue(ClonerDescriptor clonerDescriptor,
            ClonerMethodDescriptor clonerMethodDescriptor,
            Field deeplyClonedField, Var toClone, Var cloneObj) {
        var entityDescriptor = clonerMethodDescriptor.entityDescriptor;
        var blockCreator = clonerMethodDescriptor.blockCreator;
        var cloneQueue = clonerMethodDescriptor.cloneQueue;

        var gizmoMemberDescriptor =
                entityDescriptor.getMemberDescriptorForField(deeplyClonedField);

        // Initialize the field inside a Runnable (BiConsumer here since you
        // cannot share ResultHandles across different BytecodeCreators).
        var consumer = blockCreator.newAnonymousClass(BiConsumer.class,
                consumerClassCreator -> consumerClassCreator.method("accept", consumerMethodCreator -> {
                    consumerMethodCreator.returning(void.class);
                    var innerCloneMapObject = consumerMethodCreator.parameter("cloneMapObject", Object.class);
                    var innerCloneQueueObject = consumerMethodCreator.parameter("cloneQueueObject", Object.class);
                    consumerMethodCreator.body(consumerBlockCreator -> {
                        var innerCloneMap =
                                consumerBlockCreator.localVar("cloneMap", innerCloneMapObject);
                        var innerCloneQueue = consumerBlockCreator.localVar("cloneQueue", innerCloneQueueObject);
                        var subfieldValue = consumerBlockCreator.localVar("toClone",
                                gizmoMemberDescriptor.readMemberValue(consumerBlockCreator,
                                        consumerClassCreator.capture(toClone)));
                        var cloneValue = consumerBlockCreator.localVar("clonedValue", deeplyClonedField.getType(),
                                Const.ofNull(deeplyClonedField.getType()));
                        writeDeepCloneInstructions(clonerDescriptor, clonerMethodDescriptor
                                .withBlockCreator(consumerBlockCreator)
                                .withCreatedCloneMap(innerCloneMap)
                                .withCloneQueue(innerCloneQueue),
                                deeplyClonedField, gizmoMemberDescriptor, subfieldValue,
                                cloneValue);

                        if (!gizmoMemberDescriptor.writeMemberValue(consumerBlockCreator,
                                consumerClassCreator.capture(cloneObj),
                                cloneValue)) {
                            throw new IllegalStateException("The member (%s) of class (%s) does not have a setter.".formatted(
                                    gizmoMemberDescriptor.getName(), gizmoMemberDescriptor.getDeclaringClassName()));
                        }
                        consumerBlockCreator.return_();
                    });
                }));

        // Add the initializer to the queue
        blockCreator.invokeVirtual(
                MethodDesc.of(ArrayDeque.class, "push", void.class, Object.class),
                cloneQueue, consumer);
    }

    protected void createAbstractDeepCloneHelperMethod(ClonerDescriptor clonerDescriptor,
            Class<?> entityClass) {
        clonerDescriptor.classCreator.staticMethod(getEntityHelperMethodName(entityClass), methodCreator -> {
            var toClone = methodCreator.parameter("toClone", entityClass);
            var cloneMap = methodCreator.parameter("cloneMap", Map.class);
            var ignoredIsBottom = methodCreator.parameter("isBottom", boolean.class);
            var ignoredQueue = methodCreator.parameter("cloneQueue", ArrayDeque.class);

            methodCreator.public_();
            methodCreator.returning(entityClass);
            methodCreator.body(blockCreator -> {
                var maybeClone = blockCreator.localVar("existingClone", blockCreator.withMap(cloneMap).get(toClone));
                blockCreator.ifNotNull(maybeClone, hasCloneBranch -> hasCloneBranch.return_(maybeClone));

                var fallbackCloner = clonerDescriptor.fallbackClonerField;
                var cloneObj =
                        blockCreator.invokeVirtual(MethodDesc.of(FieldAccessingSolutionCloner.class,
                                "gizmoFallbackDeepClone", Object.class, Object.class, Map.class),
                                fallbackCloner, toClone, cloneMap);
                blockCreator.return_(cloneObj);
            });
        });
    }

    private enum UnhandledCloneType {
        SHALLOW,
        DEEP
    }

    protected record ClonerDescriptor(SolutionDescriptor<?> solutionDescriptor,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet,
            ClassCreator classCreator, StaticFieldVar fallbackClonerField) {
        public ClonerDescriptor withFallbackClonerField(StaticFieldVar fallbackClonerField) {
            return new ClonerDescriptor(solutionDescriptor,
                    memoizedSolutionOrEntityDescriptorMap, deepClonedClassesSortedSet,
                    classCreator, fallbackClonerField);
        }
    }

    protected record ClonerMethodDescriptor(GizmoSolutionOrEntityDescriptor entityDescriptor,
            BlockCreator blockCreator,
            Var createdCloneMap,
            boolean isBottom,
            Var cloneQueue) {
        public ClonerMethodDescriptor withBlockCreator(BlockCreator blockCreator) {
            return new ClonerMethodDescriptor(entityDescriptor, blockCreator, createdCloneMap, isBottom, cloneQueue);
        }

        public ClonerMethodDescriptor withCreatedCloneMap(Var createdCloneMap) {
            return new ClonerMethodDescriptor(entityDescriptor, blockCreator, createdCloneMap, isBottom, cloneQueue);
        }

        public ClonerMethodDescriptor withCloneQueue(Var cloneQueue) {
            return new ClonerMethodDescriptor(entityDescriptor, blockCreator, createdCloneMap, isBottom, cloneQueue);
        }
    }
}
