package ai.timefold.solver.core.config.util;

import static ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory.MemberAccessorType.FIELD_OR_READ_METHOD;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.impl.domain.common.AlphabeticMemberComparator;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessorFactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ConfigUtils {

    private static final AlphabeticMemberComparator alphabeticMemberComparator = new AlphabeticMemberComparator();

    /**
     * Create a new instance of clazz from a config's property.
     * <p>
     * If the instantiation fails, the simple class name of {@code configBean} will be used as the owner of
     * {@code propertyName}.
     * <p>
     * Intended usage:
     *
     * <pre>
     * selectionFilter = ConfigUtils.newInstance(config, "filterClass", config.getFilterClass());
     * </pre>
     *
     * @param configBean the bean holding the {@code clazz} to be instantiated
     * @param propertyName {@code configBean}'s property holding {@code clazz}
     * @param clazz {@code Class} representation of the type {@code T}
     * @param <T> the new instance type
     * @return new instance of clazz
     */
    public static <T> @NonNull T newInstance(@Nullable Object configBean, @NonNull String propertyName,
            @NonNull Class<T> clazz) {
        return newInstance(() -> (configBean == null ? "?" : configBean.getClass().getSimpleName()), propertyName, clazz);
    }

    /**
     * Create a new instance of clazz from a general source.
     * <p>
     * If the instantiation fails, the result of {@code ownerDescriptor} will be used to describe the owner of
     * {@code propertyName}.
     *
     * @param ownerDescriptor describes the owner of {@code propertyName}
     * @param propertyName property holding the {@code clazz}
     * @param clazz {@code Class} representation of the type {@code T}
     * @param <T> the new instance type
     * @return new instance of clazz
     */
    public static <T> @NonNull T newInstance(@NonNull Supplier<String> ownerDescriptor, @NonNull String propertyName,
            @NonNull Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Inner classes include local, anonymous and non-static member classes
            throw new IllegalArgumentException("The %s's %s (%s) does not have a public no-arg constructor%s"
                    .formatted(ownerDescriptor.get(), propertyName, clazz.getName(),
                            ((clazz.isLocalClass() || clazz.isAnonymousClass() || clazz.isMemberClass())
                                    && !Modifier.isStatic(clazz.getModifiers()) ? " because it is an inner class." : ".")),
                    e);
        }
    }

    public static void applyCustomProperties(@NonNull Object bean, @NonNull String beanClassPropertyName,
            @Nullable Map<@NonNull String, @NonNull String> customProperties, @NonNull String customPropertiesPropertyName) {
        if (customProperties == null) {
            return;
        }
        var beanClass = bean.getClass();
        customProperties.forEach((propertyName, valueString) -> {
            var setterMethod = ReflectionHelper.getSetterMethod(beanClass, propertyName);
            if (setterMethod == null) {
                throw new IllegalStateException(
                        """
                                The custom property %s (%s) in the %s cannot be set on the %s (%s) because that class has no public setter for that property.
                                Maybe add a public setter for that custom property (%s) on that class (%s).
                                Maybe don't configure that custom property %s (%s) in the %s."""
                                .formatted(propertyName, valueString, customPropertiesPropertyName, beanClassPropertyName,
                                        beanClass, propertyName, beanClass.getSimpleName(), propertyName, valueString,
                                        customPropertiesPropertyName));
            }
            var propertyType = setterMethod.getParameterTypes()[0];
            Object typedValue;
            try {
                if (propertyType.equals(String.class)) {
                    typedValue = valueString;
                } else if (propertyType.equals(Boolean.TYPE) || propertyType.equals(Boolean.class)) {
                    typedValue = Boolean.parseBoolean(valueString);
                } else if (propertyType.equals(Integer.TYPE) || propertyType.equals(Integer.class)) {
                    typedValue = Integer.parseInt(valueString);
                } else if (propertyType.equals(Long.TYPE) || propertyType.equals(Long.class)) {
                    typedValue = Long.parseLong(valueString);
                } else if (propertyType.equals(Float.TYPE) || propertyType.equals(Float.class)) {
                    typedValue = Float.parseFloat(valueString);
                } else if (propertyType.equals(Double.TYPE) || propertyType.equals(Double.class)) {
                    typedValue = Double.parseDouble(valueString);
                } else if (propertyType.equals(BigDecimal.class)) {
                    typedValue = new BigDecimal(valueString);
                } else if (propertyType.isEnum()) {
                    typedValue = Enum.valueOf((Class<? extends Enum>) propertyType, valueString);
                } else {
                    throw new IllegalStateException(
                            "The custom property %s (%s) in the %s has an unsupported propertyType (%s) for value (%s)."
                                    .formatted(propertyName, valueString, customPropertiesPropertyName, propertyType,
                                            valueString));
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "The custom property %s (%s) in the %s cannot be parsed to the propertyType (%s) of the setterMethod (%s)."
                                .formatted(propertyName, valueString, customPropertiesPropertyName, propertyType,
                                        setterMethod));
            }
            try {
                setterMethod.invoke(bean, typedValue);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "The custom property %s (%s) in the %s has a setterMethod (%s) on the beanClass (%s) that cannot be called for the typedValue (%s)."
                                .formatted(propertyName, valueString, customPropertiesPropertyName, setterMethod, beanClass,
                                        typedValue),
                        e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(
                        "The custom property %s (%s) in the %s has a setterMethod (%s) on the beanClass (%s) that throws an exception for the typedValue (%s)."
                                .formatted(propertyName, valueString, customPropertiesPropertyName, setterMethod, beanClass,
                                        typedValue),
                        e.getCause());
            }
        });
    }

    public static <Config_ extends AbstractConfig<Config_>> @Nullable Config_ inheritConfig(@Nullable Config_ original,
            @Nullable Config_ inherited) {
        if (inherited != null) {
            if (original == null) {
                original = inherited.copyConfig();
            } else {
                original.inherit(inherited);
            }
        }
        return original;
    }

    public static <Config_ extends AbstractConfig<Config_>> @Nullable List<Config_> inheritMergeableListConfig(
            @Nullable List<Config_> originalList, @Nullable List<Config_> inheritedList) {
        if (inheritedList != null) {
            List<Config_> mergedList = new ArrayList<>(inheritedList.size()
                    + (originalList == null ? 0 : originalList.size()));
            // The inheritedList should be before the originalList
            for (var inherited : inheritedList) {
                var copy = inherited.copyConfig();
                mergedList.add(copy);
            }
            if (originalList != null) {
                mergedList.addAll(originalList);
            }
            originalList = mergedList;
        }
        return originalList;
    }

    public static <T> @Nullable T inheritOverwritableProperty(@Nullable T original, @Nullable T inherited) {
        if (original != null) {
            // Original overwrites inherited
            return original;
        } else {
            return inherited;
        }
    }

    public static <T> @Nullable List<T> inheritMergeableListProperty(@Nullable List<T> originalList,
            @Nullable List<T> inheritedList) {
        if (inheritedList == null) {
            return originalList;
        } else if (originalList == null) {
            // Shallow clone due to modifications after calling inherit
            return new ArrayList<>(inheritedList);
        } else {
            // The inheritedList should be before the originalList
            List<T> mergedList = new ArrayList<>(inheritedList);
            mergedList.addAll(originalList);
            return mergedList;
        }
    }

    public static <E extends Enum<E>> @Nullable Set<E> inheritMergeableEnumSetProperty(@Nullable Set<E> originalSet,
            @Nullable Set<E> inheritedSet) {
        if (inheritedSet == null) {
            return originalSet;
        } else if (originalSet == null) {
            return EnumSet.copyOf(inheritedSet);
        } else {
            var newSet = EnumSet.copyOf(originalSet);
            newSet.addAll(inheritedSet);
            return newSet;
        }
    }

    public static <T> @Nullable List<T> inheritUniqueMergeableListProperty(@Nullable List<T> originalList,
            @Nullable List<T> inheritedList) {
        if (inheritedList == null) {
            return originalList;
        } else if (originalList == null) {
            // Shallow clone due to modifications after calling inherit
            return new ArrayList<>(inheritedList);
        } else {
            // The inheritedMap should be before the originalMap
            Set<T> mergedSet = new LinkedHashSet<>(inheritedList);
            mergedSet.addAll(originalList);
            return new ArrayList<>(mergedSet);
        }
    }

    public static <K, T> @Nullable Map<K, T> inheritMergeableMapProperty(@Nullable Map<K, T> originalMap,
            @Nullable Map<K, T> inheritedMap) {
        if (inheritedMap == null) {
            return originalMap;
        } else if (originalMap == null) {
            return inheritedMap;
        } else {
            Map<K, T> mergedMap = new LinkedHashMap<>(inheritedMap);
            mergedMap.putAll(originalMap);
            return mergedMap;
        }
    }

    public static <T> @Nullable T mergeProperty(@Nullable T a, @Nullable T b) {
        return Objects.equals(a, b) ? a : null;
    }

    /**
     * A relaxed version of {@link #mergeProperty(Object, Object)}. Used primarily for merging failed benchmarks,
     * where a property remains the same over benchmark runs (for example: dataset problem size), but the property in
     * the failed benchmark isn't initialized, therefore null. When merging, we can still use the correctly initialized
     * property of the benchmark that didn't fail.
     * <p>
     * Null-handling:
     * <ul>
     * <li>if <strong>both</strong> properties <strong>are null</strong>, returns null</li>
     * <li>if <strong>only one</strong> of the properties <strong>is not null</strong>, returns that property</li>
     * <li>if <strong>both</strong> properties <strong>are not null</strong>, returns
     * {@link #mergeProperty(Object, Object)}</li>
     * </ul>
     *
     * @see #mergeProperty(Object, Object)
     * @param a property {@code a}
     * @param b property {@code b}
     * @param <T> the type of property {@code a} and {@code b}
     * @return sometimes null
     */
    public static <T> @Nullable T meldProperty(@Nullable T a, @Nullable T b) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return mergeProperty(a, b);
    }

    public static boolean isEmptyCollection(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Divides and ceils the result without using floating point arithmetic. For floor division,
     * see {@link Math#floorDiv(long, long)}.
     *
     * @throws ArithmeticException if {@code divisor == 0}
     * @param dividend the dividend
     * @param divisor the divisor
     * @return dividend / divisor, ceiled
     */
    public static int ceilDivide(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero: %d/%d".formatted(dividend, divisor));
        }
        int correction;
        if (dividend % divisor == 0) {
            correction = 0;
        } else if (Integer.signum(dividend) * Integer.signum(divisor) < 0) {
            correction = 0;
        } else {
            correction = 1;
        }
        return (dividend / divisor) + correction;
    }

    public static int resolvePoolSize(@NonNull String propertyName, @NonNull String value,
            @NonNull String @NonNull... magicValues) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("The %s (%s) resolved to neither of (%s) nor a number.".formatted(propertyName,
                    value, Arrays.toString(magicValues)));
        }
    }

    // ************************************************************************
    // Member and annotation methods
    // ************************************************************************

    public static @NonNull List<@NonNull Class<?>> getAllParents(@Nullable Class<?> bottomClass) {
        if (bottomClass == null || bottomClass == Object.class) {
            return Collections.emptyList();
        }
        var superclass = bottomClass.getSuperclass();
        var lineageClassList = new ArrayList<>(getAllParents(superclass));
        for (var superInterface : bottomClass.getInterfaces()) {
            lineageClassList.addAll(getAllParents(superInterface));
        }
        lineageClassList.add(bottomClass);
        return lineageClassList;
    }

    public static @NonNull List<@NonNull Class<?>> getAllAnnotatedLineageClasses(@Nullable Class<?> bottomClass,
            @NonNull Class<? extends Annotation> annotation) {
        if (bottomClass == null || !bottomClass.isAnnotationPresent(annotation)) {
            return Collections.emptyList();
        }
        List<Class<?>> lineageClassList = new ArrayList<>();
        lineageClassList.add(bottomClass);
        var superclass = bottomClass.getSuperclass();
        lineageClassList.addAll(getAllAnnotatedLineageClasses(superclass, annotation));
        for (var superInterface : bottomClass.getInterfaces()) {
            lineageClassList.addAll(getAllAnnotatedLineageClasses(superInterface, annotation));
        }
        return lineageClassList;
    }

    /**
     * @return sorted by type (fields before methods), then by {@link AlphabeticMemberComparator}.
     */
    public static @NonNull List<Member> getDeclaredMembers(@NonNull Class<?> baseClass) {
        var fieldStream = Stream.of(baseClass.getDeclaredFields())
                // A synthetic field is a field generated by the compiler that
                // does not exist in the source code. It is used mainly in
                // nested classes so the inner class can access the fields
                // of the outer class.
                .filter(field -> !field.isSynthetic())
                .sorted(alphabeticMemberComparator);
        var methodStream = Stream.of(baseClass.getDeclaredMethods())
                // A synthetic method is a method generated by the compiler that does
                // not exist in the source code. These include bridge methods.
                // A bridge method is a generic variant that duplicates a concrete method
                // Example: "Score getScore()" that duplicates "HardSoftScore getScore()"
                .filter(method -> !method.isSynthetic())
                .sorted(alphabeticMemberComparator);
        return Stream.concat(fieldStream, methodStream)
                .collect(Collectors.toList());
    }

    /**
     * @return sorted by type (fields before methods), then by {@link AlphabeticMemberComparator}.
     */
    public static @NonNull List<Member> getAllMembers(@NonNull Class<?> baseClass,
            @NonNull Class<? extends Annotation> annotationClass) {
        var clazz = baseClass;
        Stream<Member> memberStream = Stream.empty();
        while (clazz != null) {
            var fieldStream = Stream.of(clazz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(annotationClass) && !field.isSynthetic());

            // Implementations of an interface method do not inherit the annotations of the declared method in
            // the interface, so we need to also add the interface's methods to the stream.
            var methodStream = Stream.concat(
                    Stream.of(clazz.getDeclaredMethods()),
                    Arrays.stream(clazz.getInterfaces())
                            .flatMap(implementedInterface -> Arrays.stream(implementedInterface.getMethods())))
                    .filter(method -> method.isAnnotationPresent(annotationClass) && !method.isSynthetic());
            memberStream = Stream.concat(memberStream, Stream.concat(fieldStream, methodStream));
            clazz = clazz.getSuperclass();
        }
        return memberStream.distinct().sorted(alphabeticMemberComparator).collect(Collectors.toList());
    }

    @SafeVarargs
    public static Class<? extends Annotation> extractAnnotationClass(@NonNull Member member,
            @NonNull Class<? extends Annotation>... annotationClasses) {
        var classList = extractAnnotationClasses(member, annotationClasses);
        if (classList.isEmpty()) {
            return null;
        } else if (classList.size() > 1) {
            throw new IllegalStateException(
                    "The class (%s) has a member (%s) that has both a @%s annotation and a @%s annotation.".formatted(
                            member.getDeclaringClass(), member, classList.get(0).getSimpleName(),
                            classList.get(1).getSimpleName()));
        } else {
            return classList.get(0);
        }
    }

    @SafeVarargs
    public static List<Class<? extends Annotation>> extractAnnotationClasses(@NonNull Member member,
            @NonNull Class<? extends Annotation>... annotationClasses) {
        var annotationClassList = new ArrayList<Class<? extends Annotation>>();
        for (var detectedAnnotationClass : annotationClasses) {
            if (((AnnotatedElement) member).isAnnotationPresent(detectedAnnotationClass)) {
                annotationClassList.add(detectedAnnotationClass);
            }
        }
        return annotationClassList;
    }

    public static Class<?> extractGenericTypeParameterOrFail(@NonNull String parentClassConcept, @NonNull Class<?> parentClass,
            @NonNull Class<?> type, @NonNull Type genericType, @Nullable Class<? extends Annotation> annotationClass,
            @NonNull String memberName) {
        return extractGenericTypeParameter(parentClassConcept, parentClass, type, genericType, annotationClass, memberName)
                .orElseThrow(() -> new IllegalArgumentException("""
                        The %s (%s) has a %s member (%s) with a member type (%s) which has no generic parameters.
                        Maybe the member (%s) should return a parameterized %s."""
                        .formatted(parentClassConcept, parentClass,
                                annotationClass == null ? "auto discovered"
                                        : "@" + annotationClass.getSimpleName() + " annotated",
                                memberName, type, memberName, type.getSimpleName())));
    }

    public static Optional<Class<?>> extractGenericTypeParameter(@NonNull String parentClassConcept,
            @NonNull Class<?> parentClass, @NonNull Class<?> type, @NonNull Type genericType,
            @Nullable Class<? extends Annotation> annotationClass, @NonNull String memberName) {
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return Optional.empty();
        }
        var typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length != 1) {
            throw new IllegalArgumentException("""
                    The %s (%s) has a %s member (%s) with a member type (%s) which is a parameterized collection \
                    with an unsupported number of generic parameters (%s)."""
                    .formatted(parentClassConcept, parentClass,
                            annotationClass == null ? "auto discovered" : "@" + annotationClass.getSimpleName() + " annotated",
                            memberName, type, typeArguments.length));
        }
        var typeArgument = typeArguments[0];
        if (typeArgument instanceof ParameterizedType parameterizedTypeArgument) {
            // Remove the type parameters, so it can be cast to a Class.
            typeArgument = parameterizedTypeArgument.getRawType();
        }
        if (typeArgument instanceof WildcardType wildcardType) {
            var upperBounds = wildcardType.getUpperBounds();
            typeArgument = switch (upperBounds.length) {
                case 0 -> Object.class;
                case 1 -> upperBounds[0];
                // Multiple upper bounds are impossible in traditional Java.
                // Other JVM languages or future java versions might enable triggering this.
                default -> throw new IllegalArgumentException("""
                        The %s (%s) has a %s  member (%s) with a member type (%s) which is a parameterized collection \
                        with a wildcard type argument (%s) that has multiple upper bounds (%s).
                        Maybe don't use wildcards with multiple upper bounds for the member (%s)."""
                        .formatted(parentClassConcept, parentClass,
                                annotationClass == null ? "auto discovered"
                                        : "@" + annotationClass.getSimpleName() + " annotated",
                                memberName, type, typeArgument, Arrays.toString(upperBounds), memberName));
            };
        }
        if (typeArgument instanceof Class<?> class1) {
            return Optional.of(class1);
        } else if (typeArgument instanceof ParameterizedType parameterizedTypeArgument) {
            // Turns SomeGenericType<T> into SomeGenericType.
            return Optional.of((Class<?>) parameterizedTypeArgument.getRawType());
        } else {
            throw new IllegalArgumentException("""
                    The %s (%s) has a %s member (%s) with a member type (%s) which is a parameterized collection \
                    with a type argument (%s) that is not a class or interface."""
                    .formatted(parentClassConcept, parentClass,
                            annotationClass == null ? "auto discovered" : "@" + annotationClass.getSimpleName() + " annotated",
                            memberName, type, typeArgument));
        }
    }

    /**
     * This method is heavy, and it is effectively a computed constant.
     * It is recommended that its results are cached at call sites.
     *
     * @return null if no accessor found
     * @param <C> the class type
     */
    public static <C> @Nullable MemberAccessor findPlanningIdMemberAccessor(@NonNull Class<C> clazz,
            @NonNull MemberAccessorFactory memberAccessorFactory, @NonNull DomainAccessType domainAccessType) {
        var member = getSingleMember(clazz, PlanningId.class);
        if (member == null) {
            return null;
        }
        var memberAccessor =
                memberAccessorFactory.buildAndCacheMemberAccessor(member, FIELD_OR_READ_METHOD, PlanningId.class,
                        domainAccessType);
        assertPlanningIdMemberIsComparable(clazz, member, memberAccessor);
        return memberAccessor;
    }

    private static void assertPlanningIdMemberIsComparable(Class<?> clazz, Member member, MemberAccessor memberAccessor) {
        if (!memberAccessor.getType().isPrimitive() && !Comparable.class.isAssignableFrom(memberAccessor.getType())) {
            throw new IllegalArgumentException(
                    """
                            The class (%s) has a member (%s) with a @%s annotation that returns a type (%s) that does not implement %s.
                            Maybe use a %s or %s type instead."""
                            .formatted(clazz, member, PlanningId.class.getSimpleName(), memberAccessor.getType(),
                                    Comparable.class.getSimpleName(), Long.class.getSimpleName(),
                                    String.class.getSimpleName()));
        }
    }

    private static <C> Member getSingleMember(Class<C> clazz, Class<? extends Annotation> annotationClass) {
        var memberList = getAllMembers(clazz, annotationClass);
        if (memberList.isEmpty()) {
            return null;
        }
        var size = memberList.size();
        if (clazz.isRecord()) {
            /*
             * A record has a field and a getter for each record component.
             * When the component is annotated with @PlanningId,
             * the annotation ends up both on the field and on the getter.
             */
            if (size == 2) { // The getter is used to retrieve the value of the record component.
                var methodMembers = getMembers(memberList, true);
                if (methodMembers.isEmpty()) {
                    throw new IllegalStateException("Impossible state: record (%s) doesn't have any method members (%s)."
                            .formatted(clazz.getCanonicalName(), memberList));
                }
                return methodMembers.get(0);
            } else { // There is more than one component annotated with @PlanningId; take the fields and fail.
                var componentList = getMembers(memberList, false)
                        .stream()
                        .map(Member::getName)
                        .toList();
                throw new IllegalArgumentException("The record (%s) has %s components (%s) with %s annotation."
                        .formatted(clazz, componentList.size(), componentList, annotationClass.getSimpleName()));
            }
        } else if (size > 1) {
            throw new IllegalArgumentException("The class (%s) has %s members (%s) with %s annotation."
                    .formatted(clazz, memberList.size(), memberList, annotationClass.getSimpleName()));
        }
        return memberList.get(0);
    }

    private static List<Member> getMembers(List<Member> memberList, boolean needMethod) {
        var filteredMemberList = new ArrayList<Member>(memberList.size());
        for (var member : memberList) {
            if (member instanceof Method && needMethod) {
                filteredMemberList.add(member);
            } else if (member instanceof Field && !needMethod) {
                filteredMemberList.add(member);
            }
        }
        return filteredMemberList;
    }

    public static @NonNull String abbreviate(@Nullable List<@Nullable String> list, int limit) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        var abbreviation = list.stream().limit(limit).collect(Collectors.joining(", "));
        if (list.size() > limit) {
            abbreviation += ", ...";
        }
        return abbreviation;
    }

    public static @NonNull String abbreviate(@Nullable List<@Nullable String> list) {
        return abbreviate(list, 3);
    }

    // ************************************************************************
    // Private constructor
    // ************************************************************************

    private ConfigUtils() {
    }

}
