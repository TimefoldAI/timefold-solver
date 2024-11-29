package ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.impl.domain.solution.cloner.DeepCloningUtils;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningCloneable;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public final class GizmoCloningUtils {

    public static Set<Class<?>> getDeepClonedClasses(SolutionDescriptor<?> solutionDescriptor,
            Collection<Class<?>> entitySubclasses) {
        Set<Class<?>> deepClonedClassSet = new HashSet<>();
        Set<Class<?>> classesToProcess = new LinkedHashSet<>(solutionDescriptor.getEntityClassSet());
        classesToProcess.add(solutionDescriptor.getSolutionClass());
        classesToProcess.addAll(entitySubclasses);

        // deepClonedClassSet contains all processed classes so far,
        // so we can use it to determine if we need to add a class
        // to the classesToProcess set.
        //
        // This is important, since SolverConfig does not contain
        // info on @DeepPlanningCloned classes, so we need to discover
        // them from the domain classes, possibly recursively
        // (when a @DeepPlanningCloned class reference another @DeepPlanningCloned
        //  that is not referenced by any entity).
        while (!classesToProcess.isEmpty()) {
            var clazz = classesToProcess.iterator().next();
            classesToProcess.remove(clazz);
            deepClonedClassSet.add(clazz);
            for (Field field : getAllFields(clazz)) {
                var deepClonedTypeArguments = getDeepClonedTypeArguments(solutionDescriptor, field.getGenericType());
                for (var deepClonedTypeArgument : deepClonedTypeArguments) {
                    if (!deepClonedClassSet.contains(deepClonedTypeArgument)) {
                        classesToProcess.add(deepClonedTypeArgument);
                        deepClonedClassSet.add(deepClonedTypeArgument);
                    }
                }
                if (DeepCloningUtils.isFieldDeepCloned(solutionDescriptor, field, clazz)
                        && !PlanningCloneable.class.isAssignableFrom(field.getType())
                        && !deepClonedClassSet.contains(field.getType())) {
                    classesToProcess.add(field.getType());
                    deepClonedClassSet.add(field.getType());
                }
            }
        }
        return deepClonedClassSet;
    }

    /**
     * @return never null
     */
    private static Set<Class<?>> getDeepClonedTypeArguments(SolutionDescriptor<?> solutionDescriptor, Type genericType) {
        // Check the generic type arguments of the field.
        // It is possible for fields and methods, but not instances.
        if (!(genericType instanceof ParameterizedType)) {
            return Collections.emptySet();
        }

        Set<Class<?>> deepClonedTypeArguments = new HashSet<>();
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
            if (actualTypeArgument instanceof Class class1
                    && DeepCloningUtils.isClassDeepCloned(solutionDescriptor, class1)) {
                deepClonedTypeArguments.add(class1);
            }
            deepClonedTypeArguments.addAll(getDeepClonedTypeArguments(solutionDescriptor, actualTypeArgument));
        }
        return deepClonedTypeArguments;
    }

    private static List<Field> getAllFields(Class<?> baseClass) {
        Class<?> clazz = baseClass;
        Stream<Field> memberStream = Stream.empty();
        while (clazz != null) {
            Stream<Field> fieldStream = Stream.of(clazz.getDeclaredFields());
            memberStream = Stream.concat(memberStream, fieldStream);
            clazz = clazz.getSuperclass();
        }
        return memberStream.collect(Collectors.toList());
    }

    private GizmoCloningUtils() {
        // No external instances.
    }

}
