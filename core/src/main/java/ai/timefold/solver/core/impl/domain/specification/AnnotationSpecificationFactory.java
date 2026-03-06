package ai.timefold.solver.core.impl.domain.specification;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType.FIELD_OR_GETTER_METHOD;
import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER;
import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType.FIELD_OR_READ_METHOD;
import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER;
import static ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor.extractInheritedClasses;
import static ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptorValidator.assertNotMixedInheritance;
import static ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptorValidator.assertSingleInheritance;
import static ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptorValidator.assertValidPlanningVariables;

import java.lang.annotation.Annotation;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.domain.solution.cloner.DeepPlanningClone;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.CloneableClassDescriptor;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.DeepCloneDecision;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.PropertyCopyDescriptor;
import ai.timefold.solver.core.api.domain.specification.ConstraintWeightSpecification;
import ai.timefold.solver.core.api.domain.specification.EntityCollectionSpecification;
import ai.timefold.solver.core.api.domain.specification.EntitySpecification;
import ai.timefold.solver.core.api.domain.specification.FactSpecification;
import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.domain.specification.ScoreSpecification;
import ai.timefold.solver.core.api.domain.specification.ShadowSpecification;
import ai.timefold.solver.core.api.domain.specification.ValueRangeSpecification;
import ai.timefold.solver.core.api.domain.specification.VariableSpecification;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorType;
import ai.timefold.solver.core.impl.domain.solution.cloner.DeepCloningUtils;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

/**
 * Scans annotated domain classes and produces a {@link PlanningSpecification}.
 * This unifies the annotation path through the same intermediate layer as the programmatic API.
 */
public final class AnnotationSpecificationFactory {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] VARIABLE_ANNOTATION_CLASSES =
            new Class[] { PlanningVariable.class, PlanningListVariable.class, InverseRelationShadowVariable.class,
                    IndexShadowVariable.class, PreviousElementShadowVariable.class, NextElementShadowVariable.class,
                    ShadowVariable.class, CascadingUpdateShadowVariable.class, ShadowVariablesInconsistent.class };

    private static final MethodHandles.Lookup FRAMEWORK_LOOKUP = MethodHandles.lookup();

    private AnnotationSpecificationFactory() {
    }

    // ************************************************************************
    // LambdaMetafactory helpers
    // ************************************************************************

    /**
     * Creates a direct lambda getter from a Method using LambdaMetafactory.
     * The MethodHandle is consumed during lambda class generation, not stored.
     */
    @SuppressWarnings("unchecked")
    static Function<Object, Object> createGetter(MethodHandles.Lookup lookup, Method method) throws Throwable {
        var handle = lookup.unreflect(method);
        var callSite = LambdaMetafactory.metafactory(
                lookup, "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                handle,
                MethodType.methodType(method.getReturnType(), method.getDeclaringClass()));
        return (Function<Object, Object>) callSite.getTarget().invokeExact();
    }

    /**
     * Creates a direct lambda setter from a Method using LambdaMetafactory.
     */
    @SuppressWarnings("unchecked")
    static BiConsumer<Object, Object> createSetter(MethodHandles.Lookup lookup, Method method) throws Throwable {
        var handle = lookup.unreflect(method);
        // The instantiated type must use boxed wrapper types for primitives because
        // LambdaMetafactory validates that each instantiated parameter is a subtype of Object
        // (the SAM parameter type), and primitives are not subtypes of Object.
        var paramType = method.getParameterTypes()[0];
        var instantiatedParamType = paramType.isPrimitive()
                ? MethodType.methodType(paramType).wrap().returnType()
                : paramType;
        var callSite = LambdaMetafactory.metafactory(
                lookup, "accept",
                MethodType.methodType(BiConsumer.class),
                MethodType.methodType(void.class, Object.class, Object.class),
                handle,
                MethodType.methodType(void.class, method.getDeclaringClass(), instantiatedParamType));
        return (BiConsumer<Object, Object>) callSite.getTarget().invokeExact();
    }

    /**
     * Creates a direct lambda getter from a Field using MethodHandle.
     * Cannot use LambdaMetafactory here because field MethodHandles (getField/putField)
     * are not supported by LambdaMetafactory — only method invocations are.
     * The MethodHandle is converted to a generic form so the JIT can still optimize it.
     */
    static Function<Object, Object> createFieldGetter(MethodHandles.Lookup lookup, Field field) throws Throwable {
        var handle = lookup.unreflectGetter(field)
                .asType(MethodType.methodType(Object.class, Object.class));
        return bean -> {
            try {
                return handle.invokeExact(bean);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to read field '%s' on %s."
                        .formatted(field.getName(), field.getDeclaringClass().getSimpleName()), e);
            }
        };
    }

    /**
     * Creates a direct lambda setter from a Field using MethodHandle.
     * Cannot use LambdaMetafactory here because field MethodHandles (getField/putField)
     * are not supported by LambdaMetafactory — only method invocations are.
     */
    static BiConsumer<Object, Object> createFieldSetter(MethodHandles.Lookup lookup, Field field) throws Throwable {
        var handle = lookup.unreflectSetter(field)
                .asType(MethodType.methodType(void.class, Object.class, Object.class));
        return (bean, value) -> {
            try {
                handle.invokeExact(bean, value);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to write field '%s' on %s."
                        .formatted(field.getName(), field.getDeclaringClass().getSimpleName()), e);
            }
        };
    }

    /**
     * Attempts to create a fast LambdaMetafactory-based getter for a member.
     * Returns null if the optimization is not possible (e.g., due to access restrictions).
     */
    private static Function<Object, Object> tryCreateFastGetter(MethodHandles.Lookup lookup, Member member) {
        try {
            if (member instanceof Method method) {
                method.setAccessible(true);
                return createGetter(lookup, method);
            } else if (member instanceof Field field) {
                var getterMethod = ReflectionHelper.getGetterMethod(field.getDeclaringClass(), field.getName());
                if (getterMethod != null) {
                    getterMethod.setAccessible(true);
                    return createGetter(lookup, getterMethod);
                }
                field.setAccessible(true);
                return createFieldGetter(lookup, field);
            }
        } catch (Throwable e) {
            // Fall back to accessor-based wrapping
            // Fall back silently
        }
        return null;
    }

    /**
     * Attempts to create a fast LambdaMetafactory-based setter for a member.
     * Returns null if the optimization is not possible.
     */
    private static BiConsumer<Object, Object> tryCreateFastSetter(MethodHandles.Lookup lookup, Member member,
            Class<?> propertyType, String propertyName) {
        try {
            if (member instanceof Method method) {
                var setterMethod = ReflectionHelper.getDeclaredSetterMethod(
                        method.getDeclaringClass(), propertyType, propertyName);
                if (setterMethod != null) {
                    setterMethod.setAccessible(true);
                    return createSetter(lookup, setterMethod);
                }
            } else if (member instanceof Field field) {
                var setterMethod = ReflectionHelper.getDeclaredSetterMethod(
                        field.getDeclaringClass(), propertyType, propertyName);
                if (setterMethod != null) {
                    setterMethod.setAccessible(true);
                    return createSetter(lookup, setterMethod);
                }
                field.setAccessible(true);
                return createFieldSetter(lookup, field);
            }
        } catch (Throwable e) {
            // Fall back to accessor-based wrapping
        }
        return null;
    }

    // ************************************************************************
    // Existing annotation path (with LambdaMetafactory optimization)
    // ************************************************************************

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <S> PlanningSpecification<S> fromAnnotations(
            Class<S> solutionClass,
            List<Class<?>> entityClassList,
            DomainAccessType domainAccessType,
            Map<String, MemberAccessor> gizmoMemberAccessorMap) {

        gizmoMemberAccessorMap = gizmoMemberAccessorMap != null ? gizmoMemberAccessorMap : Collections.emptyMap();
        // LambdaMetafactory fast path only works when the framework lookup can see application classes.
        // In Quarkus with FORCE_REFLECTION, the framework classloader can't see app classes at invocation time.
        var useFastPath = domainAccessType != DomainAccessType.FORCE_REFLECTION;

        var memberAccessorFactory = new MemberAccessorFactory(gizmoMemberAccessorMap);

        ScoreSpecification<S> scoreSpec = null;
        var facts = new ArrayList<FactSpecification<S>>();
        var entityCollections = new ArrayList<EntityCollectionSpecification<S>>();
        var valueRanges = new ArrayList<ValueRangeSpecification<S>>();
        ConstraintWeightSpecification<S> constraintWeightsSpec = null;
        CloningSpecification<S> cloningSpec = null;

        // Process @PlanningSolution annotation for custom cloner
        var solutionAnnotation = extractPlanningSolutionAnnotation(solutionClass);
        var solutionClonerClass = solutionAnnotation.solutionCloner();
        if (solutionClonerClass != PlanningSolution.NullSolutionCloner.class) {
            var customCloner = (SolutionCloner<S>) ConfigUtils.newInstance(
                    () -> solutionClass.toString(), "solutionClonerClass", solutionClonerClass);
            cloningSpec = new CloningSpecification<>(null, null, null, null, null, customCloner);
        }

        // Detect ConstraintWeightOverrides field (unannotated) - per-class iteration matching old behavior
        for (var lineageClass : ConfigUtils.getAllParents(solutionClass)) {
            var constraintWeightFieldList = new ArrayList<Field>();
            for (var member : ConfigUtils.getDeclaredMembers(lineageClass)) {
                if (member instanceof Field field
                        && ConstraintWeightOverrides.class.isAssignableFrom(field.getType())) {
                    constraintWeightFieldList.add(field);
                }
            }
            switch (constraintWeightFieldList.size()) {
                case 0 -> {
                    // Do nothing.
                }
                case 1 -> {
                    if (constraintWeightsSpec != null) {
                        // The bottom-most class wins, they are parsed first due to ConfigUtil.getAllParents().
                        throw new IllegalStateException(
                                "The solutionClass (%s) has a field of type (%s) which was already found on its parent class."
                                        .formatted(lineageClass, ConstraintWeightOverrides.class));
                    }
                    var cwField = constraintWeightFieldList.getFirst();
                    var accessor = buildAccessor(memberAccessorFactory, cwField,
                            FIELD_OR_GETTER_METHOD_WITH_SETTER, null, domainAccessType);
                    var cwGetter = fastOrSlowGetter(cwField, accessor, useFastPath);
                    constraintWeightsSpec = new ConstraintWeightSpecification<>(
                            solution -> (ConstraintWeightOverrides<?>) cwGetter.apply(solution));
                }
                default ->
                    throw new IllegalStateException("The solutionClass (%s) has more than one field (%s) of type %s."
                            .formatted(solutionClass, constraintWeightFieldList, ConstraintWeightOverrides.class));
            }
        }

        // Scan annotated members on the solution class
        var lineageClassList = ConfigUtils.getAllAnnotatedLineageClasses(solutionClass, PlanningSolution.class);
        if (lineageClassList.isEmpty() && solutionClass.getSuperclass() != null
                && solutionClass.getSuperclass().isAnnotationPresent(PlanningSolution.class)) {
            lineageClassList = ConfigUtils.getAllAnnotatedLineageClasses(
                    solutionClass.getSuperclass(), PlanningSolution.class);
        }
        // Track seen member names for duplicate field+getter detection
        var seenFactNames = new HashMap<String, Class<? extends Annotation>>();
        var seenEntityNames = new HashMap<String, Class<? extends Annotation>>();
        // Track member accessors for duplicate error messages
        var seenFactAccessors = new HashMap<String, MemberAccessor>();
        var seenEntityAccessors = new HashMap<String, MemberAccessor>();
        MemberAccessor firstScoreAccessor = null;

        var potentiallyOverwritingMethodList = new ArrayList<Method>();
        for (var lineageClass : lineageClassList) {
            var memberList = ConfigUtils.getDeclaredMembers(lineageClass);
            for (var member : memberList) {
                if (member instanceof Method method
                        && potentiallyOverwritingMethodList.stream().anyMatch(
                                m -> member.getName().equals(m.getName())
                                        && ReflectionHelper.isMethodOverwritten(method, m.getDeclaringClass()))) {
                    continue;
                }
                // @ValueRangeProvider on solution
                if (((AnnotatedElement) member).isAnnotationPresent(ValueRangeProvider.class)) {
                    var accessor = buildAccessor(memberAccessorFactory, member,
                            FIELD_OR_READ_METHOD, ValueRangeProvider.class, domainAccessType);
                    var vrAnnotation = accessor.getAnnotation(ValueRangeProvider.class);
                    String id = vrAnnotation.id();
                    if (id != null && id.isEmpty()) {
                        id = null;
                    }
                    valueRanges.add(new ValueRangeSpecification<>(id,
                            wrapGetter(accessor, member, useFastPath), solutionClass, false, accessor.getGenericType()));
                }
                // Fact/Entity/Score annotations
                var annotationClass = ConfigUtils.extractAnnotationClass(member,
                        ProblemFactProperty.class, ProblemFactCollectionProperty.class,
                        PlanningEntityProperty.class, PlanningEntityCollectionProperty.class, PlanningScore.class);
                if (annotationClass != null) {
                    if (annotationClass.equals(ProblemFactProperty.class)) {
                        var accessor = buildAccessor(memberAccessorFactory, member,
                                FIELD_OR_READ_METHOD, annotationClass, domainAccessType);
                        assertNoFieldAndGetterDuplicationOrConflict(solutionClass, accessor, annotationClass,
                                seenFactNames, seenFactAccessors, seenEntityNames, seenEntityAccessors);
                        seenFactNames.put(accessor.getName(), annotationClass);
                        seenFactAccessors.put(accessor.getName(), accessor);
                        // Validate entity-as-fact
                        var problemFactType = accessor.getType();
                        if (problemFactType.isAnnotationPresent(PlanningEntity.class)) {
                            throw new IllegalStateException("""
                                    The solutionClass (%s) has a @%s-annotated member (%s) that returns a @%s.
                                    Maybe use @%s instead?""".formatted(solutionClass, annotationClass.getSimpleName(),
                                    accessor.getName(), PlanningEntity.class.getSimpleName(),
                                    PlanningEntityProperty.class.getSimpleName()));
                        }
                        facts.add(new FactSpecification<>(accessor.getName(), wrapGetter(accessor, member, useFastPath),
                                wrapSetter(accessor, member, useFastPath), false, accessor.getGenericType()));
                    } else if (annotationClass.equals(ProblemFactCollectionProperty.class)) {
                        var accessor = buildAccessor(memberAccessorFactory, member,
                                FIELD_OR_READ_METHOD, annotationClass, domainAccessType);
                        assertNoFieldAndGetterDuplicationOrConflict(solutionClass, accessor, annotationClass,
                                seenFactNames, seenFactAccessors, seenEntityNames, seenEntityAccessors);
                        seenFactNames.put(accessor.getName(), annotationClass);
                        seenFactAccessors.put(accessor.getName(), accessor);
                        // Validate collection type
                        var type = accessor.getType();
                        if (!(Collection.class.isAssignableFrom(type) || type.isArray())) {
                            throw new IllegalStateException(
                                    "The solutionClass (%s) has a @%s-annotated member (%s) that does not return a %s or an array."
                                            .formatted(solutionClass,
                                                    ProblemFactCollectionProperty.class.getSimpleName(),
                                                    member, Collection.class.getSimpleName()));
                        }
                        // Validate entity-as-fact for collection
                        Class<?> problemFactType;
                        if (type.isArray()) {
                            problemFactType = type.getComponentType();
                        } else {
                            problemFactType = ConfigUtils.extractGenericTypeParameterOrFail(
                                    PlanningSolution.class.getSimpleName(),
                                    accessor.getDeclaringClass(), type, accessor.getGenericType(),
                                    annotationClass, accessor.getName());
                        }
                        if (problemFactType.isAnnotationPresent(PlanningEntity.class)) {
                            throw new IllegalStateException("""
                                    The solutionClass (%s) has a @%s-annotated member (%s) that returns a @%s.
                                    Maybe use @%s instead?""".formatted(solutionClass, annotationClass.getSimpleName(),
                                    accessor.getName(), PlanningEntity.class.getSimpleName(),
                                    PlanningEntityCollectionProperty.class.getSimpleName()));
                        }
                        facts.add(new FactSpecification<>(accessor.getName(), wrapGetter(accessor, member, useFastPath),
                                wrapSetter(accessor, member, useFastPath), true, accessor.getGenericType()));
                    } else if (annotationClass.equals(PlanningEntityProperty.class)) {
                        var accessor = buildAccessor(memberAccessorFactory, member,
                                FIELD_OR_GETTER_METHOD, annotationClass, domainAccessType);
                        assertNoFieldAndGetterDuplicationOrConflict(solutionClass, accessor, annotationClass,
                                seenFactNames, seenFactAccessors, seenEntityNames, seenEntityAccessors);
                        seenEntityNames.put(accessor.getName(), annotationClass);
                        seenEntityAccessors.put(accessor.getName(), accessor);
                        var entityGetter = fastOrSlowGetter(member, accessor, useFastPath);
                        var entitySetter = fastOrSlowSetter(member, accessor, useFastPath);
                        entityCollections.add(new EntityCollectionSpecification<>(
                                accessor.getName(),
                                solution -> {
                                    var entity = entityGetter.apply(solution);
                                    return entity != null ? List.of(entity) : Collections.emptyList();
                                },
                                (BiConsumer<S, Object>) (solution, value) -> {
                                    // Singular entity: unwrap from the collection
                                    if (value instanceof Collection<?> coll) {
                                        entitySetter.accept(solution, coll.isEmpty() ? null : coll.iterator().next());
                                    } else {
                                        entitySetter.accept(solution, value);
                                    }
                                },
                                true));
                    } else if (annotationClass.equals(PlanningEntityCollectionProperty.class)) {
                        var accessor = buildAccessor(memberAccessorFactory, member,
                                FIELD_OR_GETTER_METHOD, annotationClass, domainAccessType);
                        assertNoFieldAndGetterDuplicationOrConflict(solutionClass, accessor, annotationClass,
                                seenFactNames, seenFactAccessors, seenEntityNames, seenEntityAccessors);
                        seenEntityNames.put(accessor.getName(), annotationClass);
                        seenEntityAccessors.put(accessor.getName(), accessor);
                        // Validate collection type
                        var type = accessor.getType();
                        if (!(Collection.class.isAssignableFrom(type) || type.isArray())) {
                            throw new IllegalStateException(
                                    "The solutionClass (%s) has a @%s annotated member (%s) that does not return a %s or an array."
                                            .formatted(solutionClass,
                                                    PlanningEntityCollectionProperty.class.getSimpleName(),
                                                    member, Collection.class.getSimpleName()));
                        }
                        entityCollections.add(new EntityCollectionSpecification<>(
                                accessor.getName(), wrapCollectionGetter(accessor, member, useFastPath),
                                wrapSetter(accessor, member, useFastPath), false));
                    } else if (annotationClass.equals(PlanningScore.class)) {
                        var accessor = buildAccessor(memberAccessorFactory, member,
                                FIELD_OR_GETTER_METHOD_WITH_SETTER, PlanningScore.class, domainAccessType);
                        if (scoreSpec == null) {
                            // Bottom class wins. Bottom classes are parsed first due to ConfigUtil.getAllAnnotatedLineageClasses().
                            firstScoreAccessor = accessor;
                            var scoreAnnotation = accessor.getAnnotation(PlanningScore.class);
                            int bendableHard = scoreAnnotation != null
                                    ? scoreAnnotation.bendableHardLevelsSize()
                                    : -1;
                            int bendableSoft = scoreAnnotation != null
                                    ? scoreAnnotation.bendableSoftLevelsSize()
                                    : -1;
                            scoreSpec = new ScoreSpecification<>(
                                    (Class<? extends Score<?>>) accessor.getType(),
                                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                                    bendableHard, bendableSoft);
                        } else {
                            // Duplicate score detection
                            if (!firstScoreAccessor.getName().equals(accessor.getName())
                                    || !firstScoreAccessor.equals(accessor)
                                    || !firstScoreAccessor.getClass().equals(accessor.getClass())) {
                                throw new IllegalStateException(
                                        "The solutionClass (" + solutionClass
                                                + ") has a @" + PlanningScore.class.getSimpleName()
                                                + " annotated member (" + accessor
                                                + ") that is duplicated by another member (" + firstScoreAccessor
                                                + ").\n"
                                                + "Maybe the annotation is defined on both the field and its getter.");
                            }
                        }
                    }
                }
            }
            potentiallyOverwritingMethodList.ensureCapacity(
                    potentiallyOverwritingMethodList.size() + memberList.size());
            memberList.stream().filter(Method.class::isInstance)
                    .forEach(m -> potentiallyOverwritingMethodList.add((Method) m));
        }

        // Validate at least one entity collection
        if (entityCollections.isEmpty()) {
            throw new IllegalStateException(
                    "The solutionClass (%s) must have at least 1 member with a %s annotation or a %s annotation."
                            .formatted(solutionClass,
                                    PlanningEntityCollectionProperty.class.getSimpleName(),
                                    PlanningEntityProperty.class.getSimpleName()));
        }

        // Validate @PlanningScore exists
        if (scoreSpec == null) {
            throw new IllegalStateException("""
                    The solutionClass (%s) must have 1 member with a @%s annotation.
                    Maybe add a getScore() method with a @%s annotation.""".formatted(solutionClass,
                    PlanningScore.class.getSimpleName(), PlanningScore.class.getSimpleName()));
        }

        // Build entity specifications
        var sortedEntityClassList = buildSortedEntityClassList(entityClassList);

        var entities = new ArrayList<EntitySpecification<S>>();
        for (var entityClass : sortedEntityClassList) {
            validateEntityInheritance(entityClass);
            entities.add(buildEntitySpec(entityClass, solutionClass, memberAccessorFactory, domainAccessType, useFastPath));
        }

        // Build complete cloning spec (unless custom cloner is set)
        if (cloningSpec == null) {
            cloningSpec = buildCloningSpec(solutionClass, sortedEntityClassList, useFastPath, null);
        }

        return new PlanningSpecification<>(
                solutionClass, scoreSpec,
                List.copyOf(facts), List.copyOf(entityCollections),
                List.copyOf(valueRanges), List.copyOf(entities),
                cloningSpec, constraintWeightsSpec);
    }

    // ************************************************************************
    // Lookup-based annotation path
    // ************************************************************************

    /**
     * Scans annotated domain classes and produces a {@link PlanningSpecification}
     * using the provided {@link MethodHandles.Lookup} for member access.
     * <p>
     * This allows package-private classes and methods to be used without {@code setAccessible()}.
     * All getters and setters are generated via {@link LambdaMetafactory} for optimal performance.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <S> PlanningSpecification<S> fromAnnotations(
            Class<S> solutionClass,
            List<Class<?>> entityClassList,
            MethodHandles.Lookup lookup) {

        ScoreSpecification<S> scoreSpec = null;
        var facts = new ArrayList<FactSpecification<S>>();
        var entityCollections = new ArrayList<EntityCollectionSpecification<S>>();
        var valueRanges = new ArrayList<ValueRangeSpecification<S>>();
        ConstraintWeightSpecification<S> constraintWeightsSpec = null;
        CloningSpecification<S> cloningSpec = null;

        // Process @PlanningSolution annotation for custom cloner
        var solutionAnnotation = extractPlanningSolutionAnnotation(solutionClass);
        var solutionClonerClass = solutionAnnotation.solutionCloner();
        if (solutionClonerClass != PlanningSolution.NullSolutionCloner.class) {
            var customCloner = (SolutionCloner<S>) ConfigUtils.newInstance(
                    () -> solutionClass.toString(), "solutionClonerClass", solutionClonerClass);
            cloningSpec = new CloningSpecification<>(null, null, null, null, null, customCloner);
        }

        // Detect ConstraintWeightOverrides field (unannotated)
        for (var lineageClass : ConfigUtils.getAllParents(solutionClass)) {
            var constraintWeightFieldList = new ArrayList<Field>();
            for (var member : ConfigUtils.getDeclaredMembers(lineageClass)) {
                if (member instanceof Field field
                        && ConstraintWeightOverrides.class.isAssignableFrom(field.getType())) {
                    constraintWeightFieldList.add(field);
                }
            }
            switch (constraintWeightFieldList.size()) {
                case 0 -> {
                    // Do nothing.
                }
                case 1 -> {
                    if (constraintWeightsSpec != null) {
                        throw new IllegalStateException(
                                "The solutionClass (%s) has a field of type (%s) which was already found on its parent class."
                                        .formatted(lineageClass, ConstraintWeightOverrides.class));
                    }
                    var cwField = constraintWeightFieldList.getFirst();
                    var cwGetter = createGetterForMember(lookup, cwField);
                    constraintWeightsSpec = new ConstraintWeightSpecification<>(
                            solution -> (ConstraintWeightOverrides<?>) cwGetter.apply(solution));
                }
                default ->
                    throw new IllegalStateException("The solutionClass (%s) has more than one field (%s) of type %s."
                            .formatted(solutionClass, constraintWeightFieldList, ConstraintWeightOverrides.class));
            }
        }

        // Scan annotated members on the solution class
        var lineageClassList = ConfigUtils.getAllAnnotatedLineageClasses(solutionClass, PlanningSolution.class);
        if (lineageClassList.isEmpty() && solutionClass.getSuperclass() != null
                && solutionClass.getSuperclass().isAnnotationPresent(PlanningSolution.class)) {
            lineageClassList = ConfigUtils.getAllAnnotatedLineageClasses(
                    solutionClass.getSuperclass(), PlanningSolution.class);
        }
        var seenFactNames = new HashMap<String, String>();
        var seenEntityNames = new HashMap<String, String>();
        String firstScoreMemberName = null;

        var potentiallyOverwritingMethodList = new ArrayList<Method>();
        for (var lineageClass : lineageClassList) {
            var memberList = ConfigUtils.getDeclaredMembers(lineageClass);
            for (var member : memberList) {
                if (member instanceof Method method
                        && potentiallyOverwritingMethodList.stream().anyMatch(
                                m -> member.getName().equals(m.getName())
                                        && ReflectionHelper.isMethodOverwritten(method, m.getDeclaringClass()))) {
                    continue;
                }
                var propertyName = getPropertyName(member);
                var propertyType = getPropertyType(member);
                var genericPropertyType = getGenericPropertyType(member);

                // @ValueRangeProvider on solution
                if (((AnnotatedElement) member).isAnnotationPresent(ValueRangeProvider.class)) {
                    var vrAnnotation = ((AnnotatedElement) member).getAnnotation(ValueRangeProvider.class);
                    String id = vrAnnotation.id();
                    if (id != null && id.isEmpty()) {
                        id = null;
                    }
                    valueRanges.add(new ValueRangeSpecification<>(id,
                            wrapGetterForLookup(lookup, member), solutionClass, false, genericPropertyType));
                }
                // Fact/Entity/Score annotations
                var annotationClass = ConfigUtils.extractAnnotationClass(member,
                        ProblemFactProperty.class, ProblemFactCollectionProperty.class,
                        PlanningEntityProperty.class, PlanningEntityCollectionProperty.class, PlanningScore.class);
                if (annotationClass != null) {
                    if (annotationClass.equals(ProblemFactProperty.class)) {
                        assertNoLookupDuplication(solutionClass, propertyName, annotationClass.getSimpleName(),
                                seenFactNames, seenEntityNames);
                        seenFactNames.put(propertyName, annotationClass.getSimpleName());
                        if (propertyType.isAnnotationPresent(PlanningEntity.class)) {
                            throw new IllegalStateException("""
                                    The solutionClass (%s) has a @%s-annotated member (%s) that returns a @%s.
                                    Maybe use @%s instead?""".formatted(solutionClass, annotationClass.getSimpleName(),
                                    propertyName, PlanningEntity.class.getSimpleName(),
                                    PlanningEntityProperty.class.getSimpleName()));
                        }
                        facts.add(new FactSpecification<>(propertyName, wrapGetterForLookup(lookup, member),
                                wrapSetterForLookup(lookup, member, propertyType, propertyName), false,
                                genericPropertyType));
                    } else if (annotationClass.equals(ProblemFactCollectionProperty.class)) {
                        assertNoLookupDuplication(solutionClass, propertyName, annotationClass.getSimpleName(),
                                seenFactNames, seenEntityNames);
                        seenFactNames.put(propertyName, annotationClass.getSimpleName());
                        if (!(Collection.class.isAssignableFrom(propertyType) || propertyType.isArray())) {
                            throw new IllegalStateException(
                                    "The solutionClass (%s) has a @%s-annotated member (%s) that does not return a %s or an array."
                                            .formatted(solutionClass,
                                                    ProblemFactCollectionProperty.class.getSimpleName(),
                                                    member, Collection.class.getSimpleName()));
                        }
                        Class<?> problemFactType;
                        if (propertyType.isArray()) {
                            problemFactType = propertyType.getComponentType();
                        } else {
                            problemFactType = ConfigUtils.extractGenericTypeParameterOrFail(
                                    PlanningSolution.class.getSimpleName(),
                                    member.getDeclaringClass(), propertyType, genericPropertyType,
                                    annotationClass, propertyName);
                        }
                        if (problemFactType.isAnnotationPresent(PlanningEntity.class)) {
                            throw new IllegalStateException("""
                                    The solutionClass (%s) has a @%s-annotated member (%s) that returns a @%s.
                                    Maybe use @%s instead?""".formatted(solutionClass, annotationClass.getSimpleName(),
                                    propertyName, PlanningEntity.class.getSimpleName(),
                                    PlanningEntityCollectionProperty.class.getSimpleName()));
                        }
                        facts.add(new FactSpecification<>(propertyName, wrapGetterForLookup(lookup, member),
                                wrapSetterForLookup(lookup, member, propertyType, propertyName), true,
                                genericPropertyType));
                    } else if (annotationClass.equals(PlanningEntityProperty.class)) {
                        assertNoLookupDuplication(solutionClass, propertyName, annotationClass.getSimpleName(),
                                seenFactNames, seenEntityNames);
                        seenEntityNames.put(propertyName, annotationClass.getSimpleName());
                        var entityGetter = createGetterForMember(lookup, member);
                        var entitySetter = createSetterForMember(lookup, member, propertyType, propertyName);
                        entityCollections.add(new EntityCollectionSpecification<>(
                                propertyName,
                                solution -> {
                                    var entity = entityGetter.apply(solution);
                                    return entity != null ? List.of(entity) : Collections.emptyList();
                                },
                                (BiConsumer<S, Object>) (solution, value) -> {
                                    if (value instanceof Collection<?> coll) {
                                        entitySetter.accept(solution, coll.isEmpty() ? null : coll.iterator().next());
                                    } else {
                                        entitySetter.accept(solution, value);
                                    }
                                },
                                true));
                    } else if (annotationClass.equals(PlanningEntityCollectionProperty.class)) {
                        assertNoLookupDuplication(solutionClass, propertyName, annotationClass.getSimpleName(),
                                seenFactNames, seenEntityNames);
                        seenEntityNames.put(propertyName, annotationClass.getSimpleName());
                        if (!(Collection.class.isAssignableFrom(propertyType) || propertyType.isArray())) {
                            throw new IllegalStateException(
                                    "The solutionClass (%s) has a @%s annotated member (%s) that does not return a %s or an array."
                                            .formatted(solutionClass,
                                                    PlanningEntityCollectionProperty.class.getSimpleName(),
                                                    member, Collection.class.getSimpleName()));
                        }
                        entityCollections.add(new EntityCollectionSpecification<>(
                                propertyName,
                                (Function<S, ? extends Collection<?>>) (Function) createGetterForMember(lookup, member),
                                wrapSetterForLookup(lookup, member, propertyType, propertyName),
                                false));
                    } else if (annotationClass.equals(PlanningScore.class)) {
                        if (scoreSpec == null) {
                            firstScoreMemberName = propertyName;
                            var scoreAnnotation = ((AnnotatedElement) member).getAnnotation(PlanningScore.class);
                            int bendableHard = scoreAnnotation != null
                                    ? scoreAnnotation.bendableHardLevelsSize()
                                    : -1;
                            int bendableSoft = scoreAnnotation != null
                                    ? scoreAnnotation.bendableSoftLevelsSize()
                                    : -1;
                            scoreSpec = new ScoreSpecification<>(
                                    (Class<? extends Score<?>>) propertyType,
                                    wrapGetterForLookup(lookup, member),
                                    wrapSetterForLookup(lookup, member, propertyType, propertyName),
                                    bendableHard, bendableSoft);
                        } else {
                            if (!firstScoreMemberName.equals(propertyName)) {
                                throw new IllegalStateException(
                                        "The solutionClass (" + solutionClass
                                                + ") has a @" + PlanningScore.class.getSimpleName()
                                                + " annotated member (" + propertyName
                                                + ") that is duplicated by another member (" + firstScoreMemberName
                                                + ").\n"
                                                + "Maybe the annotation is defined on both the field and its getter.");
                            }
                        }
                    }
                }
            }
            potentiallyOverwritingMethodList.ensureCapacity(
                    potentiallyOverwritingMethodList.size() + memberList.size());
            memberList.stream().filter(Method.class::isInstance)
                    .forEach(m -> potentiallyOverwritingMethodList.add((Method) m));
        }

        if (entityCollections.isEmpty()) {
            throw new IllegalStateException(
                    "The solutionClass (%s) must have at least 1 member with a %s annotation or a %s annotation."
                            .formatted(solutionClass,
                                    PlanningEntityCollectionProperty.class.getSimpleName(),
                                    PlanningEntityProperty.class.getSimpleName()));
        }

        if (scoreSpec == null) {
            throw new IllegalStateException("""
                    The solutionClass (%s) must have 1 member with a @%s annotation.
                    Maybe add a getScore() method with a @%s annotation.""".formatted(solutionClass,
                    PlanningScore.class.getSimpleName(), PlanningScore.class.getSimpleName()));
        }

        var sortedEntityClassList = buildSortedEntityClassList(entityClassList);

        var entities = new ArrayList<EntitySpecification<S>>();
        for (var entityClass : sortedEntityClassList) {
            validateEntityInheritance(entityClass);
            entities.add(buildEntitySpecForLookup(entityClass, solutionClass, lookup));
        }

        // Build complete cloning spec (unless custom cloner is set)
        if (cloningSpec == null) {
            cloningSpec = buildCloningSpec(solutionClass, sortedEntityClassList, true, lookup);
        }

        return new PlanningSpecification<>(
                solutionClass, scoreSpec,
                List.copyOf(facts), List.copyOf(entityCollections),
                List.copyOf(valueRanges), List.copyOf(entities),
                cloningSpec, constraintWeightsSpec);
    }

    // ************************************************************************
    // Cloning specification builder
    // ************************************************************************

    /**
     * Builds a complete {@link CloningSpecification} by scanning all fields on the solution class,
     * entity classes, and transitively-discovered {@code @DeepPlanningClone} types.
     * <p>
     * For each field, a getter/setter lambda pair is created (via {@link LambdaMetafactory} when possible)
     * and a {@link DeepCloneDecision} is pre-classified so no runtime type inspection is needed during cloning.
     *
     * @param solutionClass the solution class
     * @param entityClassList all entity classes (including inherited)
     * @param useFastPath true to use LambdaMetafactory, false for reflection (Quarkus FORCE_REFLECTION)
     * @param userLookup optional user-provided Lookup for package-private access (null for framework Lookup)
     */
    @SuppressWarnings("unchecked")
    private static <S> CloningSpecification<S> buildCloningSpec(
            Class<S> solutionClass,
            List<Class<?>> entityClassList,
            boolean useFastPath,
            MethodHandles.Lookup userLookup) {

        var entityClasses = new LinkedHashSet<>(entityClassList);
        var deepCloneClasses = new LinkedHashSet<Class<?>>();
        var descriptorNeededClasses = new LinkedHashSet<Class<?>>();
        var cloneableClasses = new LinkedHashMap<Class<?>, CloneableClassDescriptor>();

        // The solution class itself must be deep-cloneable so that entity→solution backlinking
        // references are resolved to the cloned solution via cloneMap.
        deepCloneClasses.add(solutionClass);

        // Discover @DeepPlanningClone types transitively from solution and entity fields
        discoverDeepCloneTypes(solutionClass, entityClasses, deepCloneClasses, descriptorNeededClasses);
        for (var entityClass : entityClassList) {
            discoverDeepCloneTypes(entityClass, entityClasses, deepCloneClasses, descriptorNeededClasses);
        }
        // Transitively discover from deep-clone types themselves
        var toScan = new ArrayList<>(deepCloneClasses);
        toScan.addAll(descriptorNeededClasses);
        while (!toScan.isEmpty()) {
            var scanning = new ArrayList<>(toScan);
            toScan.clear();
            for (var dcClass : scanning) {
                var sizeBefore = deepCloneClasses.size() + descriptorNeededClasses.size();
                discoverDeepCloneTypes(dcClass, entityClasses, deepCloneClasses, descriptorNeededClasses);
                if (deepCloneClasses.size() + descriptorNeededClasses.size() > sizeBefore) {
                    // New types discovered — add them to the scan list
                    for (var dc : deepCloneClasses) {
                        if (!scanning.contains(dc) && !cloneableClasses.containsKey(dc)) {
                            toScan.add(dc);
                        }
                    }
                    for (var dc : descriptorNeededClasses) {
                        if (!scanning.contains(dc) && !cloneableClasses.containsKey(dc)) {
                            toScan.add(dc);
                        }
                    }
                }
            }
        }

        // Build solution factory
        Supplier<S> solutionFactory = createFactory(solutionClass, useFastPath, userLookup);

        // Build solution properties
        var solutionProperties = buildPropertiesForClass(solutionClass, entityClasses, deepCloneClasses,
                useFastPath, userLookup);

        // Build entity descriptors
        for (var entityClass : entityClassList) {
            if (!cloneableClasses.containsKey(entityClass)) {
                // For interfaces/abstract classes, factory will be null — LambdaBasedSolutionCloner
                // will fall back to runtime class instantiation.
                Supplier<Object> factory = isConcrete(entityClass)
                        ? (Supplier<Object>) (Supplier<?>) createFactory(entityClass, useFastPath, userLookup)
                        : null;
                var properties = buildPropertiesForClass(entityClass, entityClasses, deepCloneClasses,
                        useFastPath, userLookup);
                cloneableClasses.put(entityClass, new CloneableClassDescriptor(entityClass, factory, properties));
            }
        }

        // Build deep-clone fact descriptors (for inherently deep-cloneable types)
        for (var dcClass : deepCloneClasses) {
            if (!cloneableClasses.containsKey(dcClass) && isConcrete(dcClass)) {
                Supplier<Object> factory = (Supplier<Object>) (Supplier<?>) createFactory(dcClass, useFastPath, userLookup);
                var properties = buildPropertiesForClass(dcClass, entityClasses, deepCloneClasses,
                        useFastPath, userLookup);
                cloneableClasses.put(dcClass, new CloneableClassDescriptor(dcClass, factory, properties));
            }
        }

        // Build descriptors for types that need cloning support (field/getter @DeepPlanningClone)
        // but aren't inherently deep-cloneable (not in deepCloneClasses)
        for (var dnClass : descriptorNeededClasses) {
            if (!cloneableClasses.containsKey(dnClass) && isConcrete(dnClass)) {
                Supplier<Object> factory = (Supplier<Object>) (Supplier<?>) createFactory(dnClass, useFastPath, userLookup);
                var properties = buildPropertiesForClass(dnClass, entityClasses, deepCloneClasses,
                        useFastPath, userLookup);
                cloneableClasses.put(dnClass, new CloneableClassDescriptor(dnClass, factory, properties));
            }
        }

        return new CloningSpecification<>(
                solutionFactory,
                List.copyOf(solutionProperties),
                Map.copyOf(cloneableClasses),
                Set.copyOf(entityClasses),
                Set.copyOf(deepCloneClasses),
                null);
    }

    /**
     * Discovers {@code @DeepPlanningClone}-annotated types reachable from fields of the given class.
     *
     * @param clazz the class to scan
     * @param entityClasses known entity classes
     * @param deepCloneClasses types that are inherently deep-cloneable (type itself has @DeepPlanningClone)
     * @param descriptorNeededClasses types that need a cloning descriptor but aren't inherently deep-cloneable
     *        (e.g. because a field/getter pointing to them has @DeepPlanningClone)
     */
    private static void discoverDeepCloneTypes(Class<?> clazz, Set<Class<?>> entityClasses,
            Set<Class<?>> deepCloneClasses, Set<Class<?>> descriptorNeededClasses) {
        for (var current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (var field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                var fieldType = field.getType();
                if (DeepCloningUtils.isImmutable(fieldType)) {
                    continue;
                }
                // Check if the field type itself is @DeepPlanningClone → inherently deep-cloneable
                if (fieldType.isAnnotationPresent(DeepPlanningClone.class) && !entityClasses.contains(fieldType)) {
                    deepCloneClasses.add(fieldType);
                }
                // Check if @DeepPlanningClone is on the field or getter → type needs a descriptor
                // but is NOT inherently deep-cloneable (only specific fields force deep-cloning)
                boolean deepCloneOnFieldOrGetter = field.isAnnotationPresent(DeepPlanningClone.class);
                if (!deepCloneOnFieldOrGetter) {
                    var getter = ReflectionHelper.getGetterMethod(clazz, field.getName());
                    if (getter != null && getter.isAnnotationPresent(DeepPlanningClone.class)) {
                        deepCloneOnFieldOrGetter = true;
                    }
                }
                if (deepCloneOnFieldOrGetter) {
                    if (!Collection.class.isAssignableFrom(fieldType) && !Map.class.isAssignableFrom(fieldType)
                            && !fieldType.isArray() && !entityClasses.contains(fieldType)
                            && !deepCloneClasses.contains(fieldType)) {
                        descriptorNeededClasses.add(fieldType);
                    }
                }
                // Check generic type arguments for entity/deep-clone references
                checkGenericTypeForDeepClone(field.getGenericType(), entityClasses, deepCloneClasses);
            }
        }
    }

    private static void checkGenericTypeForDeepClone(Type genericType, Set<Class<?>> entityClasses,
            Set<Class<?>> deepCloneClasses) {
        if (genericType instanceof ParameterizedType paramType) {
            for (var typeArg : paramType.getActualTypeArguments()) {
                if (typeArg instanceof Class<?> argClass) {
                    if (argClass.isAnnotationPresent(DeepPlanningClone.class) && !entityClasses.contains(argClass)) {
                        deepCloneClasses.add(argClass);
                    }
                }
                checkGenericTypeForDeepClone(typeArg, entityClasses, deepCloneClasses);
            }
        }
    }

    private record FieldClassification(DeepCloneDecision decision, String validationMessage) {
        FieldClassification(DeepCloneDecision decision) {
            this(decision, null);
        }
    }

    /**
     * Builds {@link PropertyCopyDescriptor}s for all declared fields on the given class hierarchy.
     */
    private static List<PropertyCopyDescriptor> buildPropertiesForClass(
            Class<?> clazz, Set<Class<?>> entityClasses, Set<Class<?>> deepCloneClasses,
            boolean useFastPath, MethodHandles.Lookup userLookup) {
        var properties = new ArrayList<PropertyCopyDescriptor>();
        for (var current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (var field : current.getDeclaredFields()) {
                var modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                var accessors = createCloningAccessors(field, Modifier.isFinal(modifiers), useFastPath, userLookup);
                var getter = accessors.getter();
                var setter = accessors.setter();
                var classification = classifyField(field, clazz, entityClasses, deepCloneClasses);
                properties.add(new PropertyCopyDescriptor(field.getName(), getter, setter,
                        classification.decision(), classification.validationMessage()));
            }
        }
        return List.copyOf(properties);
    }

    /**
     * Classifies a field's deep clone decision at spec-build time.
     * Replicates the logic from {@link DeepCloningUtils#needsDeepClone} but pre-computes the decision.
     */
    private static FieldClassification classifyField(Field field, Class<?> owningClass,
            Set<Class<?>> entityClasses, Set<Class<?>> deepCloneClasses) {
        var fieldType = field.getType();

        // Immutable types → SHALLOW
        if (DeepCloningUtils.isImmutable(fieldType)) {
            return new FieldClassification(DeepCloneDecision.SHALLOW);
        }

        // @DeepPlanningClone on field or getter → force deep clone
        boolean fieldAnnotated = field.isAnnotationPresent(DeepPlanningClone.class);
        Method getterMethod = ReflectionHelper.getGetterMethod(owningClass, field.getName());
        if (!fieldAnnotated && getterMethod != null && getterMethod.isAnnotationPresent(DeepPlanningClone.class)) {
            fieldAnnotated = true;
        }

        // Deferred validation: @PlanningVariable + @DeepPlanningClone on non-deep-cloned type
        // We store the message and throw at clone time.
        String validationMessage = null;
        if (fieldAnnotated && isFieldAPlanningBasicVariable(field, owningClass, getterMethod)
                && !isDeepCloneableType(fieldType, entityClasses, deepCloneClasses)) {
            validationMessage = """
                    The field (%s) of class (%s) is configured to be deep-cloned, \
                    but its type (%s) is not deep-cloned. \
                    Maybe remove the @%s annotation from the field? \
                    Maybe annotate the type (%s) with @%s?"""
                    .formatted(field.getName(), owningClass.getCanonicalName(),
                            fieldType.getCanonicalName(),
                            DeepPlanningClone.class.getSimpleName(),
                            fieldType.getCanonicalName(),
                            DeepPlanningClone.class.getSimpleName());
        }

        if (fieldAnnotated) {
            DeepCloneDecision decision;
            if (Collection.class.isAssignableFrom(fieldType)) {
                decision = DeepCloneDecision.DEEP_COLLECTION;
            } else if (Map.class.isAssignableFrom(fieldType)) {
                decision = DeepCloneDecision.DEEP_MAP;
            } else if (fieldType.isArray()) {
                decision = DeepCloneDecision.DEEP_ARRAY;
            } else {
                decision = DeepCloneDecision.ALWAYS_DEEP;
            }
            return new FieldClassification(decision, validationMessage);
        }

        // @PlanningListVariable → DEEP_COLLECTION (list variable contents must be deep-cloned)
        if (isFieldAPlanningListVariable(field, owningClass, getterMethod)) {
            return new FieldClassification(DeepCloneDecision.DEEP_COLLECTION);
        }

        // @PlanningEntityCollectionProperty on field or getter → deep clone the container
        if (hasAnnotationOnFieldOrGetter(field, getterMethod, PlanningEntityCollectionProperty.class)) {
            if (fieldType.isArray()) {
                return new FieldClassification(DeepCloneDecision.DEEP_ARRAY);
            }
            return new FieldClassification(DeepCloneDecision.DEEP_COLLECTION);
        }

        // @PlanningEntityProperty on field or getter → RESOLVE_ENTITY_REFERENCE
        if (hasAnnotationOnFieldOrGetter(field, getterMethod, PlanningEntityProperty.class)) {
            return new FieldClassification(DeepCloneDecision.RESOLVE_ENTITY_REFERENCE);
        }

        // Field type is entity or deep-cloneable → RESOLVE_ENTITY_REFERENCE
        if (isDeepCloneableType(fieldType, entityClasses, deepCloneClasses)) {
            return new FieldClassification(DeepCloneDecision.RESOLVE_ENTITY_REFERENCE);
        }

        // Collection/Map/Array with entity or deep-clone type args
        if (Collection.class.isAssignableFrom(fieldType)) {
            if (hasDeepCloneTypeArg(field.getGenericType(), entityClasses, deepCloneClasses)) {
                return new FieldClassification(DeepCloneDecision.DEEP_COLLECTION);
            }
            return new FieldClassification(DeepCloneDecision.SHALLOW);
        } else if (Map.class.isAssignableFrom(fieldType)) {
            if (hasDeepCloneTypeArg(field.getGenericType(), entityClasses, deepCloneClasses)) {
                return new FieldClassification(DeepCloneDecision.DEEP_MAP);
            }
            return new FieldClassification(DeepCloneDecision.SHALLOW);
        } else if (fieldType.isArray()) {
            var componentType = fieldType.getComponentType();
            if (isDeepCloneableType(componentType, entityClasses, deepCloneClasses)) {
                return new FieldClassification(DeepCloneDecision.DEEP_ARRAY);
            }
            return new FieldClassification(DeepCloneDecision.SHALLOW);
        }

        // Non-immutable, non-container type: check runtime value type at clone time
        // (handles subclass types annotated with @DeepPlanningClone)
        return new FieldClassification(DeepCloneDecision.SHALLOW_OR_DEEP_BY_RUNTIME_TYPE);
    }

    private static boolean hasAnnotationOnFieldOrGetter(Field field, Method getterMethod,
            Class<? extends java.lang.annotation.Annotation> annotationClass) {
        if (field.isAnnotationPresent(annotationClass)) {
            return true;
        }
        return getterMethod != null && getterMethod.isAnnotationPresent(annotationClass);
    }

    private static boolean isDeepCloneableType(Class<?> type, Set<Class<?>> entityClasses,
            Set<Class<?>> deepCloneClasses) {
        return entityClasses.contains(type) || deepCloneClasses.contains(type)
                || type.isAnnotationPresent(DeepPlanningClone.class);
    }

    private static boolean isFieldAPlanningListVariable(Field field, Class<?> owningClass, Method getterMethod) {
        if (field.isAnnotationPresent(PlanningListVariable.class)) {
            return true;
        }
        return getterMethod != null && getterMethod.isAnnotationPresent(PlanningListVariable.class);
    }

    private static boolean isFieldAPlanningBasicVariable(Field field, Class<?> owningClass, Method getterMethod) {
        if (field.isAnnotationPresent(PlanningVariable.class)) {
            return true;
        }
        return getterMethod != null && getterMethod.isAnnotationPresent(PlanningVariable.class);
    }

    private static boolean hasDeepCloneTypeArg(Type genericType, Set<Class<?>> entityClasses,
            Set<Class<?>> deepCloneClasses) {
        if (genericType instanceof ParameterizedType paramType) {
            for (var typeArg : paramType.getActualTypeArguments()) {
                if (typeArg instanceof Class<?> argClass) {
                    if (entityClasses.contains(argClass) || deepCloneClasses.contains(argClass)
                            || argClass.isAnnotationPresent(DeepPlanningClone.class)) {
                        return true;
                    }
                }
                if (hasDeepCloneTypeArg(typeArg, entityClasses, deepCloneClasses)) {
                    return true;
                }
            }
        }
        return false;
    }

    private record CloningAccessors(Function<Object, Object> getter, BiConsumer<Object, Object> setter) {
    }

    /**
     * Creates getter and setter lambdas for a field for use in cloning.
     * <p>
     * Strategy (in order of preference):
     * <ol>
     * <li><b>Getter + setter methods via {@link LambdaMetafactory}</b> — JIT-inlineable lambdas,
     * best performance. Used when both getter and setter methods exist. We require BOTH because
     * wrapping getters (e.g., returning {@code Collections.unmodifiableList()}) virtually always
     * have no corresponding setter; requiring both ensures the getter returns the raw field value.</li>
     * <li><b>Direct field access via MethodHandle</b> — fallback when getter/setter methods don't exist.
     * Not JIT-inlineable but still avoids {@code setAccessible}. Zero reflection.</li>
     * <li><b>Final fields via {@code field.setAccessible(true)}</b> — narrow exception; the JVM does not
     * allow setting final fields via MethodHandles or VarHandle.</li>
     * <li><b>Reflection</b> — Quarkus {@code FORCE_REFLECTION} only (build-time {@code setAccessible}).</li>
     * </ol>
     */
    private static CloningAccessors createCloningAccessors(Field field, boolean isFinal,
            boolean useFastPath, MethodHandles.Lookup userLookup) {
        if (isFinal) {
            // Final fields cannot be set via MethodHandles or VarHandle.
            // Use reflection as a narrow exception — the JVM still allows Field.set() after setAccessible(true).
            // For the getter, we still use the fast path if available (reading final fields works fine).
            var getter = createCloningGetter(field, useFastPath, userLookup);
            field.setAccessible(true);
            BiConsumer<Object, Object> setter = (bean, value) -> {
                try {
                    field.set(bean, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to write final field '%s' on %s."
                            .formatted(field.getName(), field.getDeclaringClass().getSimpleName()), e);
                }
            };
            return new CloningAccessors(getter, setter);
        }
        if (useFastPath) {
            try {
                var lookup = getLookupForField(field, userLookup);
                // Try getter + setter methods via LambdaMetafactory (JIT-inlineable).
                // We require BOTH to exist — wrapping getters (e.g., Collections.unmodifiableList())
                // have no corresponding setter, so requiring both ensures the getter is safe.
                var getterMethod = ReflectionHelper.getGetterMethod(field.getDeclaringClass(), field.getName());
                if (getterMethod == null) {
                    getterMethod = findDeclaredGetterMethod(field.getDeclaringClass(), field.getName());
                }
                var setterMethod = ReflectionHelper.getDeclaredSetterMethod(
                        field.getDeclaringClass(), field.getType(), field.getName());
                if (getterMethod != null && setterMethod != null) {
                    return new CloningAccessors(
                            createGetter(lookup, getterMethod),
                            createSetter(lookup, setterMethod));
                }
                // Fallback: direct field access via MethodHandle (not JIT-inlineable, but no setAccessible)
                return new CloningAccessors(
                        createFieldGetter(lookup, field),
                        createFieldSetter(lookup, field));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot access field (%s) on class (%s) for planning cloning. To use private fields, either:\n"
                                .formatted(field.getName(), field.getDeclaringClass().getSimpleName())
                                + "  1. Make the field package-private or public, or\n"
                                + "  2. Provide a MethodHandles.Lookup via SolverConfig.withLookup(MethodHandles.lookup()), or\n"
                                + "  3. Use the programmatic PlanningSpecification API.",
                        e);
            } catch (Throwable e) {
                throw new IllegalStateException(
                        "Failed to create accessors for field (%s) on class (%s)."
                                .formatted(field.getName(), field.getDeclaringClass().getSimpleName()),
                        e);
            }
        }
        // Reflection fallback (Quarkus FORCE_REFLECTION only)
        field.setAccessible(true);
        Function<Object, Object> getter = bean -> {
            try {
                return field.get(bean);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to read field '%s' on %s."
                        .formatted(field.getName(), field.getDeclaringClass().getSimpleName()), e);
            }
        };
        BiConsumer<Object, Object> setter = (bean, value) -> {
            try {
                field.set(bean, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to write field '%s' on %s."
                        .formatted(field.getName(), field.getDeclaringClass().getSimpleName()), e);
            }
        };
        return new CloningAccessors(getter, setter);
    }

    /**
     * Creates just a getter for a field — used for final fields where the setter is handled separately.
     */
    private static Function<Object, Object> createCloningGetter(Field field,
            boolean useFastPath, MethodHandles.Lookup userLookup) {
        if (useFastPath) {
            try {
                var lookup = getLookupForField(field, userLookup);
                return createFieldGetter(lookup, field);
            } catch (Throwable e) {
                // Fall through to reflection
            }
        }
        field.setAccessible(true);
        return bean -> {
            try {
                return field.get(bean);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to read field '%s' on %s."
                        .formatted(field.getName(), field.getDeclaringClass().getSimpleName()), e);
            }
        };
    }

    /**
     * Gets the appropriate Lookup for a field.
     * Uses {@code privateLookupIn} to teleport into the target class's module,
     * which allows accessing private fields without {@code setAccessible}.
     * For the unnamed module (most user code), all packages are implicitly open so this always works.
     * For named modules, this works if the user's module {@code opens} the package.
     */
    private static MethodHandles.Lookup getLookupForField(Field field, MethodHandles.Lookup userLookup) throws Throwable {
        var baseLookup = userLookup != null ? userLookup : FRAMEWORK_LOOKUP;
        return MethodHandles.privateLookupIn(field.getDeclaringClass(), baseLookup);
    }

    private static boolean isConcrete(Class<?> clazz) {
        return !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Creates a no-arg constructor factory for a class.
     * Returns {@code null} if the class has no no-arg constructor (clone-time error will be raised if needed).
     */
    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> createFactory(Class<T> clazz, boolean useFastPath,
            MethodHandles.Lookup userLookup) {
        if (useFastPath) {
            try {
                var lookup = userLookup != null
                        ? MethodHandles.privateLookupIn(clazz, userLookup)
                        : FRAMEWORK_LOOKUP;
                var ctorHandle = lookup.findConstructor(clazz, MethodType.methodType(void.class));
                var callSite = LambdaMetafactory.metafactory(
                        lookup, "get",
                        MethodType.methodType(Supplier.class),
                        MethodType.methodType(Object.class),
                        ctorHandle,
                        MethodType.methodType(clazz));
                return (Supplier<T>) callSite.getTarget().invokeExact();
            } catch (Throwable e) {
                // Fall through to reflection
            }
        }
        // Reflection fallback
        try {
            var ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return () -> {
                try {
                    return ctor.newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Failed to create instance of %s.".formatted(clazz.getSimpleName()), e);
                }
            };
        } catch (NoSuchMethodException e) {
            // No no-arg constructor — return null; error will be raised at clone time if this class is actually cloned
            return null;
        }
    }

    // ************************************************************************
    // Entity spec builders
    // ************************************************************************

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <S> EntitySpecification<S> buildEntitySpec(
            Class<?> entityClass,
            Class<S> solutionClass,
            MemberAccessorFactory memberAccessorFactory,
            DomainAccessType domainAccessType,
            boolean useFastPath) {

        // Check mutability before processing members (records/enums can't be entities)
        SolutionDescriptor.assertMutable(entityClass, "entityClass");

        var entityAnnotation = findEntityAnnotation(entityClass);
        java.util.Comparator<?> difficultyComparator = extractDifficultyComparator(entityClass, entityAnnotation);
        Class<?> difficultyComparatorFactoryClass = extractDifficultyComparatorFactoryClass(entityAnnotation);

        var variables = new ArrayList<VariableSpecification<S>>();
        var shadows = new ArrayList<ShadowSpecification<S>>();
        var entityScopedValueRanges = new ArrayList<ValueRangeSpecification<S>>();
        Function<?, ?> planningIdGetter = null;
        Predicate<?> pinnedPredicate = null;
        ToIntFunction<?> pinToIndexFunction = null;

        for (var member : ConfigUtils.getDeclaredMembers(entityClass)) {
            // @ValueRangeProvider on entity
            if (((AnnotatedElement) member).isAnnotationPresent(ValueRangeProvider.class)) {
                var accessor = buildAccessor(memberAccessorFactory, member,
                        FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER, ValueRangeProvider.class, domainAccessType);
                var vrAnnotation = accessor.getAnnotation(ValueRangeProvider.class);
                String id = vrAnnotation.id();
                if (id != null && id.isEmpty()) {
                    id = null;
                }
                entityScopedValueRanges.add(new ValueRangeSpecification<>(id,
                        wrapGetter(accessor, member, useFastPath), entityClass, true, accessor.getGenericType()));
            }

            // Planning variables
            var varAnnotationClass = ConfigUtils.extractAnnotationClass(member, VARIABLE_ANNOTATION_CLASSES);
            if (varAnnotationClass != null) {
                var accessor = buildAccessor(memberAccessorFactory, member,
                        FIELD_OR_GETTER_METHOD_WITH_SETTER, varAnnotationClass, domainAccessType);
                processVariable(entityClass, solutionClass, accessor, member, varAnnotationClass, variables, shadows,
                        useFastPath);
            }

            // @PlanningPin
            if (((AnnotatedElement) member).isAnnotationPresent(PlanningPin.class)) {
                var accessor = buildAccessor(memberAccessorFactory, member,
                        FIELD_OR_READ_METHOD, PlanningPin.class, domainAccessType);
                var pinGetter = fastOrSlowGetter(member, accessor, useFastPath);
                pinnedPredicate = (Predicate<Object>) entity -> Boolean.TRUE.equals(pinGetter.apply(entity));
            }

            // @PlanningPinToIndex
            if (((AnnotatedElement) member).isAnnotationPresent(PlanningPinToIndex.class)) {
                var accessor = buildAccessor(memberAccessorFactory, member,
                        FIELD_OR_READ_METHOD, PlanningPinToIndex.class, domainAccessType);
                var pinIndexGetter = fastOrSlowGetter(member, accessor, useFastPath);
                pinToIndexFunction = (ToIntFunction<Object>) entity -> (int) pinIndexGetter.apply(entity);
            }

            // @PlanningId
            if (((AnnotatedElement) member).isAnnotationPresent(PlanningId.class)) {
                var accessor = buildAccessor(memberAccessorFactory, member,
                        FIELD_OR_READ_METHOD, PlanningId.class, domainAccessType);
                planningIdGetter = wrapGetter(accessor, member, useFastPath);
            }
        }

        return new EntitySpecification<>(
                entityClass, planningIdGetter, difficultyComparator, difficultyComparatorFactoryClass,
                pinnedPredicate, pinToIndexFunction,
                List.copyOf(variables), List.copyOf(shadows),
                List.copyOf(entityScopedValueRanges));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <S> EntitySpecification<S> buildEntitySpecForLookup(
            Class<?> entityClass,
            Class<S> solutionClass,
            MethodHandles.Lookup lookup) {

        SolutionDescriptor.assertMutable(entityClass, "entityClass");

        var entityAnnotation = findEntityAnnotation(entityClass);
        java.util.Comparator<?> difficultyComparator = extractDifficultyComparator(entityClass, entityAnnotation);
        Class<?> difficultyComparatorFactoryClass = extractDifficultyComparatorFactoryClass(entityAnnotation);

        var variables = new ArrayList<VariableSpecification<S>>();
        var shadows = new ArrayList<ShadowSpecification<S>>();
        var entityScopedValueRanges = new ArrayList<ValueRangeSpecification<S>>();
        Function<?, ?> planningIdGetter = null;
        Predicate<?> pinnedPredicate = null;
        ToIntFunction<?> pinToIndexFunction = null;

        for (var member : ConfigUtils.getDeclaredMembers(entityClass)) {
            var propertyName = getPropertyName(member);
            var propertyType = getPropertyType(member);

            // @ValueRangeProvider on entity
            if (((AnnotatedElement) member).isAnnotationPresent(ValueRangeProvider.class)) {
                var vrAnnotation = ((AnnotatedElement) member).getAnnotation(ValueRangeProvider.class);
                String id = vrAnnotation.id();
                if (id != null && id.isEmpty()) {
                    id = null;
                }
                entityScopedValueRanges.add(new ValueRangeSpecification<>(id,
                        wrapGetterForLookup(lookup, member), entityClass, true, getGenericPropertyType(member)));
            }

            // Planning variables
            var varAnnotationClass = ConfigUtils.extractAnnotationClass(member, VARIABLE_ANNOTATION_CLASSES);
            if (varAnnotationClass != null) {
                processVariableForLookup(entityClass, solutionClass, member, lookup, varAnnotationClass, variables, shadows);
            }

            // @PlanningPin
            if (((AnnotatedElement) member).isAnnotationPresent(PlanningPin.class)) {
                var pinGetter = createGetterForMember(lookup, member);
                pinnedPredicate = (Predicate<Object>) entity -> Boolean.TRUE.equals(pinGetter.apply(entity));
            }

            // @PlanningPinToIndex
            if (((AnnotatedElement) member).isAnnotationPresent(PlanningPinToIndex.class)) {
                var pinIndexGetter = createGetterForMember(lookup, member);
                pinToIndexFunction = (ToIntFunction<Object>) entity -> (int) pinIndexGetter.apply(entity);
            }

            // @PlanningId
            if (((AnnotatedElement) member).isAnnotationPresent(PlanningId.class)) {
                planningIdGetter = wrapGetterForLookup(lookup, member);
            }
        }

        return new EntitySpecification<>(
                entityClass, planningIdGetter, difficultyComparator, difficultyComparatorFactoryClass,
                pinnedPredicate, pinToIndexFunction,
                List.copyOf(variables), List.copyOf(shadows),
                List.copyOf(entityScopedValueRanges));
    }

    // ************************************************************************
    // Variable processing
    // ************************************************************************

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <S> void processVariable(
            Class<?> entityClass,
            Class<S> solutionClass,
            MemberAccessor accessor,
            Member member,
            Class<? extends Annotation> annotationClass,
            List<VariableSpecification<S>> variables,
            List<ShadowSpecification<S>> shadows,
            boolean useFastPath) {

        var name = accessor.getName();

        if (annotationClass.equals(PlanningVariable.class)) {
            var annotation = accessor.getAnnotation(PlanningVariable.class);
            var valueRangeRefs = annotation.valueRangeProviderRefs();
            var strengthComparator = extractStrengthComparator(annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class);
            var comparatorFactoryClass = extractComparatorFactoryClass(annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            assertNoMixedComparators(entityClass, name, annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class, annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            variables.add(new VariableSpecification<>(
                    name, accessor.getType(),
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                    false, annotation.allowsUnassigned(),
                    valueRangeRefs.length > 0 ? List.of(valueRangeRefs) : List.of(),
                    strengthComparator, comparatorFactoryClass));
        } else if (annotationClass.equals(PlanningListVariable.class)) {
            var annotation = accessor.getAnnotation(PlanningListVariable.class);
            var valueRangeRefs = annotation.valueRangeProviderRefs();
            var elementType = ConfigUtils.extractGenericTypeParameterOrFail(
                    "entityClass", entityClass, accessor.getType(), accessor.getGenericType(),
                    PlanningListVariable.class, name);
            var strengthComparator = extractStrengthComparator(annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class);
            var comparatorFactoryClass = extractComparatorFactoryClass(annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            assertNoMixedComparators(entityClass, name, annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class, annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            variables.add(new VariableSpecification<>(
                    name, elementType,
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                    true, annotation.allowsUnassignedValues(),
                    valueRangeRefs.length > 0 ? List.of(valueRangeRefs) : List.of(),
                    strengthComparator, comparatorFactoryClass));
        } else if (annotationClass.equals(InverseRelationShadowVariable.class)) {
            var annotation = accessor.getAnnotation(InverseRelationShadowVariable.class);
            shadows.add(new ShadowSpecification.InverseRelation<>(
                    name, accessor.getType(),
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(IndexShadowVariable.class)) {
            var annotation = accessor.getAnnotation(IndexShadowVariable.class);
            var indexGetter = fastOrSlowGetter(member, accessor, useFastPath);
            var indexSetter = fastOrSlowSetter(member, accessor, useFastPath);
            shadows.add(new ShadowSpecification.Index<>(
                    name, accessor.getType(),
                    (ToIntFunction<Object>) entity -> (int) indexGetter.apply(entity),
                    (java.util.function.ObjIntConsumer<Object>) (entity, value) -> indexSetter.accept(entity, value),
                    indexGetter, (BiConsumer<Object, Object>) indexSetter::accept,
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(PreviousElementShadowVariable.class)) {
            var annotation = accessor.getAnnotation(PreviousElementShadowVariable.class);
            shadows.add(new ShadowSpecification.PreviousElement<>(
                    name, accessor.getType(),
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(NextElementShadowVariable.class)) {
            var annotation = accessor.getAnnotation(NextElementShadowVariable.class);
            shadows.add(new ShadowSpecification.NextElement<>(
                    name, accessor.getType(),
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(ShadowVariable.class)) {
            var annotation = accessor.getAnnotation(ShadowVariable.class);
            var supplierName = annotation.supplierName();
            // Find the supplier method on the entity class (0 or 1 param)
            var supplierMethod = ReflectionHelper.getDeclaredMethod(entityClass, supplierName);
            if (supplierMethod == null) {
                supplierMethod = ReflectionHelper.getDeclaredMethod(entityClass, supplierName, solutionClass);
            }
            if (supplierMethod == null) {
                throw new IllegalArgumentException(
                        "@%s (%s) defines a supplierName (%s) that does not exist inside its declaring class (%s)."
                                .formatted(ShadowVariable.class.getSimpleName(), name, supplierName,
                                        entityClass.getCanonicalName()));
            }
            var sourcesAnnotation = supplierMethod.getAnnotation(
                    ai.timefold.solver.core.api.domain.variable.ShadowSources.class);
            if (sourcesAnnotation == null) {
                throw new IllegalArgumentException(
                        "Method \"%s\" referenced from @%s member %s is not annotated with @%s."
                                .formatted(supplierName, ShadowVariable.class.getSimpleName(), name,
                                        ai.timefold.solver.core.api.domain.variable.ShadowSources.class
                                                .getSimpleName()));
            }
            var sourcePaths = List.of(sourcesAnnotation.value());
            var alignmentKey = (sourcesAnnotation.alignmentKey() != null
                    && !sourcesAnnotation.alignmentKey().isEmpty())
                            ? sourcesAnnotation.alignmentKey()
                            : null;
            // For 0-param suppliers, create a fast getter; for 1-param, store the method for the compiler
            Function<Object, Object> supplierGetter = null;
            if (supplierMethod.getParameterCount() == 0) {
                if (useFastPath) {
                    supplierMethod.setAccessible(true);
                    supplierGetter = tryCreateFastGetter(FRAMEWORK_LOOKUP, supplierMethod);
                }
                if (supplierGetter == null) {
                    var m = supplierMethod;
                    m.setAccessible(true);
                    supplierGetter = entity -> {
                        try {
                            return m.invoke(entity);
                        } catch (Exception e) {
                            throw new IllegalStateException(
                                    "Failed to invoke supplier method '%s' on entity."
                                            .formatted(m.getName()),
                                    e);
                        }
                    };
                }
            }
            shadows.add(new ShadowSpecification.Declarative<>(
                    name, accessor.getType(),
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                    supplierGetter, sourcePaths, alignmentKey, supplierMethod));
        } else if (annotationClass.equals(CascadingUpdateShadowVariable.class)) {
            var annotation = accessor.getAnnotation(CascadingUpdateShadowVariable.class);
            var targetMethodName = annotation.targetMethodName();
            // Find the target method on the entity class
            var targetMethodList = ConfigUtils.getDeclaredMembers(entityClass).stream()
                    .filter(m -> m.getName().equals(targetMethodName)
                            && m instanceof Method method
                            && method.getParameterCount() == 0)
                    .toList();
            java.util.function.Consumer<Object> updateMethod = null;
            if (!targetMethodList.isEmpty()) {
                var targetMember = (Method) targetMethodList.getFirst();
                targetMember.setAccessible(true);
                var tm = targetMember;
                updateMethod = entity -> {
                    try {
                        tm.invoke(entity);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "Failed to invoke target method '%s' on entity."
                                        .formatted(tm.getName()),
                                e);
                    }
                };
            }
            shadows.add(new ShadowSpecification.CascadingUpdate<>(
                    name, accessor.getType(),
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath),
                    updateMethod, List.of(), targetMethodName));
        } else if (annotationClass.equals(ShadowVariablesInconsistent.class)) {
            shadows.add(new ShadowSpecification.Inconsistent<>(
                    name, accessor.getType(),
                    wrapGetter(accessor, member, useFastPath), wrapSetter(accessor, member, useFastPath)));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <S> void processVariableForLookup(
            Class<?> entityClass,
            Class<S> solutionClass,
            Member member,
            MethodHandles.Lookup lookup,
            Class<? extends Annotation> annotationClass,
            List<VariableSpecification<S>> variables,
            List<ShadowSpecification<S>> shadows) {

        var name = getPropertyName(member);
        var propertyType = getPropertyType(member);
        var genericPropertyType = getGenericPropertyType(member);

        if (annotationClass.equals(PlanningVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(PlanningVariable.class);
            var valueRangeRefs = annotation.valueRangeProviderRefs();
            var strengthComparator = extractStrengthComparator(annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class);
            var comparatorFactoryClass = extractComparatorFactoryClass(annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            assertNoMixedComparators(entityClass, name, annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class, annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            variables.add(new VariableSpecification<>(
                    name, propertyType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name),
                    false, annotation.allowsUnassigned(),
                    valueRangeRefs.length > 0 ? List.of(valueRangeRefs) : List.of(),
                    strengthComparator, comparatorFactoryClass));
        } else if (annotationClass.equals(PlanningListVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(PlanningListVariable.class);
            var valueRangeRefs = annotation.valueRangeProviderRefs();
            var elementType = ConfigUtils.extractGenericTypeParameterOrFail(
                    "entityClass", entityClass, propertyType, genericPropertyType,
                    PlanningListVariable.class, name);
            var strengthComparator = extractStrengthComparator(annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class);
            var comparatorFactoryClass = extractComparatorFactoryClass(annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            assertNoMixedComparators(entityClass, name, annotation.comparatorClass(),
                    PlanningVariable.NullComparator.class, annotation.comparatorFactoryClass(),
                    PlanningVariable.NullComparatorFactory.class);
            variables.add(new VariableSpecification<>(
                    name, elementType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name),
                    true, annotation.allowsUnassignedValues(),
                    valueRangeRefs.length > 0 ? List.of(valueRangeRefs) : List.of(),
                    strengthComparator, comparatorFactoryClass));
        } else if (annotationClass.equals(InverseRelationShadowVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(InverseRelationShadowVariable.class);
            shadows.add(new ShadowSpecification.InverseRelation<>(
                    name, propertyType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name),
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(IndexShadowVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(IndexShadowVariable.class);
            var indexGetter = createGetterForMember(lookup, member);
            var indexSetter = createSetterForMember(lookup, member, propertyType, name);
            shadows.add(new ShadowSpecification.Index<>(
                    name, propertyType,
                    (ToIntFunction<Object>) entity -> (int) indexGetter.apply(entity),
                    (java.util.function.ObjIntConsumer<Object>) (entity, value) -> indexSetter.accept(entity, value),
                    indexGetter, (BiConsumer<Object, Object>) indexSetter::accept,
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(PreviousElementShadowVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(PreviousElementShadowVariable.class);
            shadows.add(new ShadowSpecification.PreviousElement<>(
                    name, propertyType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name),
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(NextElementShadowVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(NextElementShadowVariable.class);
            shadows.add(new ShadowSpecification.NextElement<>(
                    name, propertyType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name),
                    annotation.sourceVariableName()));
        } else if (annotationClass.equals(ShadowVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(ShadowVariable.class);
            var supplierName = annotation.supplierName();
            var supplierMethod = ReflectionHelper.getDeclaredMethod(entityClass, supplierName);
            if (supplierMethod == null) {
                supplierMethod = ReflectionHelper.getDeclaredMethod(entityClass, supplierName, solutionClass);
            }
            if (supplierMethod == null) {
                throw new IllegalArgumentException(
                        "@%s (%s) defines a supplierName (%s) that does not exist inside its declaring class (%s)."
                                .formatted(ShadowVariable.class.getSimpleName(), name, supplierName,
                                        entityClass.getCanonicalName()));
            }
            var sourcesAnnotation = supplierMethod.getAnnotation(
                    ai.timefold.solver.core.api.domain.variable.ShadowSources.class);
            if (sourcesAnnotation == null) {
                throw new IllegalArgumentException(
                        "Method \"%s\" referenced from @%s member %s is not annotated with @%s."
                                .formatted(supplierName, ShadowVariable.class.getSimpleName(), name,
                                        ai.timefold.solver.core.api.domain.variable.ShadowSources.class
                                                .getSimpleName()));
            }
            var sourcePaths = List.of(sourcesAnnotation.value());
            var alignmentKey = (sourcesAnnotation.alignmentKey() != null
                    && !sourcesAnnotation.alignmentKey().isEmpty())
                            ? sourcesAnnotation.alignmentKey()
                            : null;
            Function<Object, Object> supplierGetter = null;
            if (supplierMethod.getParameterCount() == 0) {
                supplierGetter = createGetterForMember(lookup, supplierMethod);
            }
            shadows.add(new ShadowSpecification.Declarative<>(
                    name, propertyType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name),
                    supplierGetter, sourcePaths, alignmentKey, supplierMethod));
        } else if (annotationClass.equals(CascadingUpdateShadowVariable.class)) {
            var annotation = ((AnnotatedElement) member).getAnnotation(CascadingUpdateShadowVariable.class);
            var targetMethodName = annotation.targetMethodName();
            var targetMethodList = ConfigUtils.getDeclaredMembers(entityClass).stream()
                    .filter(m -> m.getName().equals(targetMethodName)
                            && m instanceof Method method
                            && method.getParameterCount() == 0)
                    .toList();
            java.util.function.Consumer<Object> updateMethod = null;
            if (!targetMethodList.isEmpty()) {
                var targetMember = (Method) targetMethodList.getFirst();
                try {
                    var handle = lookup.unreflect(targetMember);
                    updateMethod = entity -> {
                        try {
                            handle.invoke(entity);
                        } catch (Throwable e) {
                            throw new IllegalStateException(
                                    "Failed to invoke target method '%s' on entity."
                                            .formatted(targetMember.getName()),
                                    e);
                        }
                    };
                } catch (IllegalAccessException e) {
                    targetMember.setAccessible(true);
                    var tm = targetMember;
                    updateMethod = entity -> {
                        try {
                            tm.invoke(entity);
                        } catch (Exception ex) {
                            throw new IllegalStateException(
                                    "Failed to invoke target method '%s' on entity."
                                            .formatted(tm.getName()),
                                    ex);
                        }
                    };
                }
            }
            shadows.add(new ShadowSpecification.CascadingUpdate<>(
                    name, propertyType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name),
                    updateMethod, List.of(), targetMethodName));
        } else if (annotationClass.equals(ShadowVariablesInconsistent.class)) {
            shadows.add(new ShadowSpecification.Inconsistent<>(
                    name, propertyType,
                    wrapGetterForLookup(lookup, member), wrapSetterForLookup(lookup, member, propertyType, name)));
        }
    }

    // ************************************************************************
    // Shared helpers
    // ************************************************************************

    private static void validateEntityInheritance(Class<?> entityClass) {
        var inheritedEntityClasses = extractInheritedClasses(entityClass);
        assertNotMixedInheritance(entityClass, inheritedEntityClasses);
        assertSingleInheritance(entityClass, inheritedEntityClasses);
        assertValidPlanningVariables(entityClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void assertNoMixedComparators(
            Class<?> entityClass, String propertyName,
            Class<? extends java.util.Comparator> comparatorClass, Class<?> comparatorNullSentinel,
            Class<? extends ComparatorFactory> comparatorFactoryClass, Class<?> factoryNullSentinel) {
        boolean hasComparator = comparatorClass != null && !comparatorNullSentinel.isAssignableFrom(comparatorClass);
        boolean hasFactory = comparatorFactoryClass != null && !factoryNullSentinel.isAssignableFrom(comparatorFactoryClass);
        if (hasComparator && hasFactory) {
            throw new IllegalStateException(
                    "The entityClass (%s) property (%s) cannot have a comparatorClass (%s) and a comparatorFactoryClass (%s) at the same time."
                            .formatted(entityClass, propertyName, comparatorClass.getName(),
                                    comparatorFactoryClass.getName()));
        }
    }

    private static java.util.Comparator<?> extractStrengthComparator(
            Class<? extends java.util.Comparator> comparatorClass,
            Class<?> nullSentinelClass) {
        if (comparatorClass == null || nullSentinelClass.isAssignableFrom(comparatorClass)) {
            return null;
        }
        return ConfigUtils.newInstance(() -> "variable", "comparatorClass", comparatorClass);
    }

    @SuppressWarnings("rawtypes")
    private static Class<?> extractComparatorFactoryClass(
            Class<? extends ComparatorFactory> comparatorFactoryClass,
            Class<?> nullSentinelClass) {
        if (comparatorFactoryClass == null || nullSentinelClass.isAssignableFrom(comparatorFactoryClass)) {
            return null;
        }
        return comparatorFactoryClass;
    }

    private static PlanningSolution extractPlanningSolutionAnnotation(Class<?> solutionClass) {
        var annotation = solutionClass.getAnnotation(PlanningSolution.class);
        if (annotation != null) {
            return annotation;
        }
        var superclass = solutionClass.getSuperclass();
        if (superclass != null) {
            var parentAnnotation = superclass.getAnnotation(PlanningSolution.class);
            if (parentAnnotation != null) {
                return parentAnnotation;
            }
        }
        throw new IllegalStateException(
                "The solutionClass (%s) does not have a @%s annotation."
                        .formatted(solutionClass.getCanonicalName(), PlanningSolution.class.getSimpleName()));
    }

    private static PlanningEntity findEntityAnnotation(Class<?> entityClass) {
        var entityAnnotation = entityClass.getAnnotation(PlanningEntity.class);
        if (entityAnnotation == null) {
            for (var ic : extractInheritedClasses(entityClass)) {
                entityAnnotation = ic.getAnnotation(PlanningEntity.class);
                if (entityAnnotation != null) {
                    break;
                }
            }
        }
        return entityAnnotation;
    }

    private static java.util.Comparator<?> extractDifficultyComparator(
            Class<?> entityClass, PlanningEntity entityAnnotation) {
        if (entityAnnotation != null) {
            var comparatorClass = entityAnnotation.comparatorClass();
            if (comparatorClass != null
                    && !PlanningEntity.NullComparator.class.isAssignableFrom(comparatorClass)) {
                return ConfigUtils.newInstance(
                        () -> entityClass.toString(), "comparatorClass", comparatorClass);
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static Class<?> extractDifficultyComparatorFactoryClass(PlanningEntity entityAnnotation) {
        if (entityAnnotation != null) {
            var factoryClass = entityAnnotation.comparatorFactoryClass();
            if (factoryClass != null
                    && !PlanningEntity.NullComparatorFactory.class.isAssignableFrom(factoryClass)) {
                return factoryClass;
            }
        }
        return null;
    }

    private static List<Class<?>> buildSortedEntityClassList(List<Class<?>> entityClassList) {
        var updatedEntityClassList = new ArrayList<>(entityClassList);
        for (var entityClass : entityClassList) {
            for (var inherited : extractInheritedClasses(entityClass)) {
                if (!updatedEntityClassList.contains(inherited)) {
                    updatedEntityClassList.add(inherited);
                }
            }
        }
        return sortEntityClassList(updatedEntityClassList);
    }

    private static List<Class<?>> sortEntityClassList(List<Class<?>> entityClassList) {
        var sortedEntityClassList = new ArrayList<Class<?>>(entityClassList.size());
        for (var entityClass : entityClassList) {
            var added = false;
            for (var i = 0; i < sortedEntityClassList.size(); i++) {
                if (entityClass.isAssignableFrom(sortedEntityClassList.get(i))) {
                    sortedEntityClassList.add(i, entityClass);
                    added = true;
                    break;
                }
            }
            if (!added) {
                sortedEntityClassList.add(entityClass);
            }
        }
        return sortedEntityClassList;
    }

    // ************************************************************************
    // Member metadata helpers
    // ************************************************************************

    private static String getPropertyName(Member member) {
        if (member instanceof Field field) {
            return field.getName();
        } else if (member instanceof Method) {
            return ReflectionHelper.getGetterPropertyName(member);
        }
        return member.getName();
    }

    private static Class<?> getPropertyType(Member member) {
        if (member instanceof Field field) {
            return field.getType();
        } else if (member instanceof Method method) {
            return method.getReturnType();
        }
        throw new IllegalStateException("Unsupported member type: " + member.getClass());
    }

    private static Type getGenericPropertyType(Member member) {
        if (member instanceof Field field) {
            return field.getGenericType();
        } else if (member instanceof Method method) {
            return method.getGenericReturnType();
        }
        throw new IllegalStateException("Unsupported member type: " + member.getClass());
    }

    // ************************************************************************
    // Getter/setter wrapper methods (existing accessor path with fast optimization)
    // ************************************************************************

    @SuppressWarnings("unchecked")
    private static <S> Function<S, ?> wrapGetter(MemberAccessor accessor, Member member, boolean useFastPath) {
        if (useFastPath) {
            var fast = tryCreateFastGetter(FRAMEWORK_LOOKUP, member);
            if (fast != null) {
                return (Function<S, ?>) (Function) fast;
            }
        }
        return (Function<S, ?>) (Function<Object, Object>) accessor::executeGetter;
    }

    @SuppressWarnings("unchecked")
    private static <S> BiConsumer<S, Object> wrapSetter(MemberAccessor accessor, Member member, boolean useFastPath) {
        if (useFastPath) {
            var fast = tryCreateFastSetter(FRAMEWORK_LOOKUP, member, accessor.getType(), accessor.getName());
            if (fast != null) {
                return (BiConsumer<S, Object>) (BiConsumer) fast;
            }
        }
        return (BiConsumer<S, Object>) (BiConsumer<Object, Object>) accessor::executeSetter;
    }

    @SuppressWarnings("unchecked")
    private static <S> Function<S, ? extends Collection<?>> wrapCollectionGetter(MemberAccessor accessor, Member member,
            boolean useFastPath) {
        if (useFastPath) {
            var fast = tryCreateFastGetter(FRAMEWORK_LOOKUP, member);
            if (fast != null) {
                return (Function<S, ? extends Collection<?>>) (Function) fast;
            }
        }
        return (Function<S, ? extends Collection<?>>) (Function) accessor::executeGetter;
    }

    private static Function<Object, Object> fastOrSlowGetter(Member member, MemberAccessor accessor,
            boolean useFastPath) {
        if (useFastPath) {
            var fast = tryCreateFastGetter(FRAMEWORK_LOOKUP, member);
            if (fast != null) {
                return fast;
            }
        }
        return accessor::executeGetter;
    }

    private static BiConsumer<Object, Object> fastOrSlowSetter(Member member, MemberAccessor accessor,
            boolean useFastPath) {
        if (useFastPath) {
            var fast = tryCreateFastSetter(FRAMEWORK_LOOKUP, member, accessor.getType(), accessor.getName());
            if (fast != null) {
                return fast;
            }
        }
        return accessor::executeSetter;
    }

    // ************************************************************************
    // Getter/setter wrapper methods (Lookup path — always uses LambdaMetafactory)
    // ************************************************************************

    /**
     * Creates a getter for a member using the user's Lookup.
     * Throws a clear error if access fails.
     */
    static Function<Object, Object> createGetterForMember(MethodHandles.Lookup lookup, Member member) {
        try {
            if (member instanceof Method method) {
                return createGetter(lookup, method);
            } else if (member instanceof Field field) {
                var getterMethod = findDeclaredGetterMethod(field.getDeclaringClass(), field.getName());
                if (getterMethod != null) {
                    return createGetter(lookup, getterMethod);
                }
                // Direct field access — need private access to the declaring class
                var privateLookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), lookup);
                return createFieldGetter(privateLookup, field);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot access member (%s) on class (%s). To use package-private or protected members, either:\n"
                            .formatted(member.getName(), member.getDeclaringClass().getSimpleName())
                            + "  1. Make the member public, or\n"
                            + "  2. Provide a MethodHandles.Lookup via SolverConfig.withLookup(MethodHandles.lookup()), or\n"
                            + "  3. Use the programmatic PlanningSpecification API.",
                    e);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to create getter for member (%s) on class (%s)."
                            .formatted(member.getName(), member.getDeclaringClass().getSimpleName()),
                    e);
        }
        throw new IllegalStateException("Unsupported member type: " + member.getClass());
    }

    /**
     * Creates a setter for a member using the user's Lookup.
     */
    static BiConsumer<Object, Object> createSetterForMember(MethodHandles.Lookup lookup, Member member,
            Class<?> propertyType, String propertyName) {
        try {
            Class<?> declaringClass;
            if (member instanceof Method method) {
                declaringClass = method.getDeclaringClass();
            } else if (member instanceof Field field) {
                declaringClass = field.getDeclaringClass();
            } else {
                throw new IllegalStateException("Unsupported member type: " + member.getClass());
            }
            var setterMethod = ReflectionHelper.getDeclaredSetterMethod(declaringClass, propertyType, propertyName);
            if (setterMethod != null) {
                return createSetter(lookup, setterMethod);
            }
            if (member instanceof Field field) {
                var privateLookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), lookup);
                return createFieldSetter(privateLookup, field);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot access setter for member (%s) on class (%s). To use package-private or protected members, either:\n"
                            .formatted(member.getName(), member.getDeclaringClass().getSimpleName())
                            + "  1. Make the member public, or\n"
                            + "  2. Provide a MethodHandles.Lookup via SolverConfig.withLookup(MethodHandles.lookup()), or\n"
                            + "  3. Use the programmatic PlanningSpecification API.",
                    e);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to create setter for member (%s) on class (%s)."
                            .formatted(member.getName(), member.getDeclaringClass().getSimpleName()),
                    e);
        }
        throw new IllegalStateException(
                "No setter found for member (%s) on class (%s)."
                        .formatted(propertyName, member.getDeclaringClass().getSimpleName()));
    }

    /**
     * Finds a declared getter method (including non-public) for a field name.
     * Walks up the class hierarchy.
     */
    private static Method findDeclaredGetterMethod(Class<?> clazz, String fieldName) {
        var capitalizedName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (var prefix : new String[] { "get", "is" }) {
            try {
                return clazz.getDeclaredMethod(prefix + capitalizedName);
            } catch (NoSuchMethodException e) {
                // try next
            }
        }
        if (clazz.getSuperclass() != null) {
            return findDeclaredGetterMethod(clazz.getSuperclass(), fieldName);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <S> Function<S, ?> wrapGetterForLookup(MethodHandles.Lookup lookup, Member member) {
        return (Function<S, ?>) (Function) createGetterForMember(lookup, member);
    }

    @SuppressWarnings("unchecked")
    private static <S> BiConsumer<S, Object> wrapSetterForLookup(MethodHandles.Lookup lookup, Member member,
            Class<?> propertyType, String propertyName) {
        return (BiConsumer<S, Object>) (BiConsumer) createSetterForMember(lookup, member, propertyType, propertyName);
    }

    // ************************************************************************
    // Validation helpers
    // ************************************************************************

    private static void assertNoFieldAndGetterDuplicationOrConflict(
            Class<?> solutionClass, MemberAccessor memberAccessor, Class<? extends Annotation> annotationClass,
            Map<String, Class<? extends Annotation>> seenFactNames, Map<String, MemberAccessor> seenFactAccessors,
            Map<String, Class<? extends Annotation>> seenEntityNames, Map<String, MemberAccessor> seenEntityAccessors) {
        var memberName = memberAccessor.getName();
        MemberAccessor duplicate = null;
        Class<? extends Annotation> otherAnnotationClass = null;
        if (seenFactNames.containsKey(memberName)) {
            duplicate = seenFactAccessors.get(memberName);
            otherAnnotationClass = seenFactNames.get(memberName);
        } else if (seenEntityNames.containsKey(memberName)) {
            duplicate = seenEntityAccessors.get(memberName);
            otherAnnotationClass = seenEntityNames.get(memberName);
        }
        if (duplicate != null) {
            throw new IllegalStateException("""
                    The solutionClass (%s) has a @%s annotated member (%s) that is duplicated by a @%s annotated member (%s).
                    %s""".formatted(solutionClass, annotationClass.getSimpleName(), memberAccessor,
                    otherAnnotationClass.getSimpleName(), duplicate,
                    annotationClass.equals(otherAnnotationClass)
                            ? "Maybe the annotation is defined on both the field and its getter."
                            : "Maybe 2 mutually exclusive annotations are configured."));
        }
    }

    private static void assertNoLookupDuplication(
            Class<?> solutionClass, String propertyName, String annotationName,
            Map<String, String> seenFactNames, Map<String, String> seenEntityNames) {
        String otherAnnotation = seenFactNames.get(propertyName);
        if (otherAnnotation == null) {
            otherAnnotation = seenEntityNames.get(propertyName);
        }
        if (otherAnnotation != null) {
            throw new IllegalStateException("""
                    The solutionClass (%s) has a @%s annotated member (%s) that is duplicated by a @%s annotated member.
                    %s""".formatted(solutionClass, annotationName, propertyName,
                    otherAnnotation,
                    annotationName.equals(otherAnnotation)
                            ? "Maybe the annotation is defined on both the field and its getter."
                            : "Maybe 2 mutually exclusive annotations are configured."));
        }
    }

    private static MemberAccessor buildAccessor(MemberAccessorFactory factory, Member member,
            MemberAccessorType memberAccessorType, Class<? extends Annotation> annotationClass,
            DomainAccessType domainAccessType) {
        return factory.buildAndCacheMemberAccessor(member, memberAccessorType, annotationClass, domainAccessType);
    }
}
