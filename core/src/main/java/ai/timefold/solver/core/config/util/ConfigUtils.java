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
    public static <T> T newInstance(Object configBean, String propertyName, Class<T> clazz) {
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
    public static <T> T newInstance(Supplier<String> ownerDescriptor, String propertyName, Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("The " + ownerDescriptor.get() + "'s " + propertyName + " ("
                    + clazz.getName() + ") does not have a public no-arg constructor"
                    // Inner classes include local, anonymous and non-static member classes
                    + ((clazz.isLocalClass() || clazz.isAnonymousClass() || clazz.isMemberClass())
                            && !Modifier.isStatic(clazz.getModifiers()) ? " because it is an inner class." : "."),
                    e);
        }
    }

    public static void applyCustomProperties(Object bean, String beanClassPropertyName,
            Map<String, String> customProperties, String customPropertiesPropertyName) {
        if (customProperties == null) {
            return;
        }
        var beanClass = bean.getClass();
        customProperties.forEach((propertyName, valueString) -> {
            var setterMethod = ReflectionHelper.getSetterMethod(beanClass, propertyName);
            if (setterMethod == null) {
                throw new IllegalStateException("The custom property " + propertyName + " (" + valueString
                        + ") in the " + customPropertiesPropertyName
                        + " cannot be set on the " + beanClassPropertyName + " (" + beanClass
                        + ") because that class has no public setter for that property.\n"
                        + "Maybe add a public setter for that custom property (" + propertyName
                        + ") on that class (" + beanClass.getSimpleName() + ").\n"
                        + "Maybe don't configure that custom property " + propertyName + " (" + valueString
                        + ") in the " + customPropertiesPropertyName + ".");
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
                    throw new IllegalStateException("The custom property " + propertyName + " (" + valueString
                            + ") in the " + customPropertiesPropertyName
                            + " has an unsupported propertyType (" + propertyType + ") for value (" + valueString + ").");
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException("The custom property " + propertyName + " (" + valueString
                        + ") in the " + customPropertiesPropertyName
                        + " cannot be parsed to the propertyType (" + propertyType
                        + ") of the setterMethod (" + setterMethod + ").");
            }
            try {
                setterMethod.invoke(bean, typedValue);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("The custom property " + propertyName + " (" + valueString
                        + ") in the " + customPropertiesPropertyName
                        + " has a setterMethod (" + setterMethod + ") on the beanClass (" + beanClass
                        + ") that cannot be called for the typedValue (" + typedValue + ").", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("The custom property " + propertyName + " (" + valueString
                        + ") in the " + customPropertiesPropertyName
                        + " has a setterMethod (" + setterMethod + ") on the beanClass (" + beanClass
                        + ") that throws an exception for the typedValue (" + typedValue + ").",
                        e.getCause());
            }
        });
    }

    public static <Config_ extends AbstractConfig<Config_>> Config_ inheritConfig(Config_ original, Config_ inherited) {
        if (inherited != null) {
            if (original == null) {
                original = inherited.copyConfig();
            } else {
                original.inherit(inherited);
            }
        }
        return original;
    }

    public static <Config_ extends AbstractConfig<Config_>> List<Config_> inheritMergeableListConfig(
            List<Config_> originalList, List<Config_> inheritedList) {
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

    public static <T> T inheritOverwritableProperty(T original, T inherited) {
        if (original != null) {
            // Original overwrites inherited
            return original;
        } else {
            return inherited;
        }
    }

    public static <T> List<T> inheritMergeableListProperty(List<T> originalList, List<T> inheritedList) {
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

    public static <T> List<T> inheritUniqueMergeableListProperty(List<T> originalList, List<T> inheritedList) {
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

    public static <K, T> Map<K, T> inheritMergeableMapProperty(Map<K, T> originalMap, Map<K, T> inheritedMap) {
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

    public static <T> T mergeProperty(T a, T b) {
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
    public static <T> T meldProperty(T a, T b) {
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

    public static boolean isEmptyCollection(Collection<?> collection) {
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
            throw new ArithmeticException("Cannot divide by zero: " + dividend + "/" + divisor);
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

    public static int resolvePoolSize(String propertyName, String value, String... magicValues) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("The " + propertyName + " (" + value + ") resolved to neither of ("
                    + Arrays.toString(magicValues) + ") nor a number.");
        }
    }

    // ************************************************************************
    // Member and annotation methods
    // ************************************************************************

    public static List<Class<?>> getAllParents(Class<?> bottomClass) {
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

    public static List<Class<?>> getAllAnnotatedLineageClasses(Class<?> bottomClass,
            Class<? extends Annotation> annotation) {
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
     * @param baseClass never null
     * @return never null, sorted by type (fields before methods), then by {@link AlphabeticMemberComparator}.
     */
    public static List<Member> getDeclaredMembers(Class<?> baseClass) {
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
     * @param baseClass never null
     * @param annotationClass never null
     * @return never null, sorted by type (fields before methods), then by {@link AlphabeticMemberComparator}.
     */
    public static List<Member> getAllMembers(Class<?> baseClass, Class<? extends Annotation> annotationClass) {
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
    public static Class<? extends Annotation> extractAnnotationClass(Member member,
            Class<? extends Annotation>... annotationClasses) {
        Class<? extends Annotation> annotationClass = null;
        for (var detectedAnnotationClass : annotationClasses) {
            if (((AnnotatedElement) member).isAnnotationPresent(detectedAnnotationClass)) {
                if (annotationClass != null) {
                    throw new IllegalStateException("The class (" + member.getDeclaringClass()
                            + ") has a member (" + member + ") that has both a @"
                            + annotationClass.getSimpleName() + " annotation and a @"
                            + detectedAnnotationClass.getSimpleName() + " annotation.");
                }
                annotationClass = detectedAnnotationClass;
                // Do not break early: check other annotationClasses too
            }
        }
        return annotationClass;
    }

    public static Class<?> extractGenericTypeParameterOrFail(String parentClassConcept, Class<?> parentClass, Class<?> type,
            Type genericType, Class<? extends Annotation> annotationClass, String memberName) {
        return extractGenericTypeParameter(parentClassConcept, parentClass, type, genericType, annotationClass, memberName)
                .orElseThrow(() -> new IllegalArgumentException("""
                        The %s (%s) has a %s member (%s) with a member type (%s) which has no generic parameters.
                        Maybe the member (%s) should return a parameterized %s."""
                        .formatted(parentClassConcept, parentClass,
                                annotationClass == null ? "auto discovered"
                                        : "@" + annotationClass.getSimpleName() + " annotated",
                                memberName, type, memberName, type.getSimpleName())));
    }

    public static Optional<Class<?>> extractGenericTypeParameter(String parentClassConcept, Class<?> parentClass, Class<?> type,
            Type genericType, Class<? extends Annotation> annotationClass, String memberName) {
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
     * @param clazz never null
     * @param memberAccessorFactory never null
     * @param domainAccessType never null
     * @return null if no accessor found
     * @param <C> the class type
     */
    public static <C> MemberAccessor findPlanningIdMemberAccessor(Class<C> clazz,
            MemberAccessorFactory memberAccessorFactory, DomainAccessType domainAccessType) {
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
            throw new IllegalArgumentException("The class (" + clazz
                    + ") has a member (" + member + ") with a @" + PlanningId.class.getSimpleName()
                    + " annotation that returns a type (" + memberAccessor.getType()
                    + ") that does not implement " + Comparable.class.getSimpleName() + ".\n"
                    + "Maybe use a " + Long.class.getSimpleName()
                    + " or " + String.class.getSimpleName() + " type instead.");
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

    public static String abbreviate(List<String> list, int limit) {
        var abbreviation = "";
        if (list != null) {
            abbreviation = list.stream().limit(limit).collect(Collectors.joining(", "));
            if (list.size() > limit) {
                abbreviation += ", ...";
            }
        }
        return abbreviation;
    }

    public static String abbreviate(List<String> list) {
        return abbreviate(list, 3);
    }

    // ************************************************************************
    // Private constructor
    // ************************************************************************

    private ConfigUtils() {
    }

}
