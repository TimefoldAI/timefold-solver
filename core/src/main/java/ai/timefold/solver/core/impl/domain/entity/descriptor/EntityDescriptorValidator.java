package ai.timefold.solver.core.impl.domain.entity.descriptor;

import static ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor.VARIABLE_ANNOTATION_CLASSES;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.config.util.ConfigUtils;

public class EntityDescriptorValidator {

    private static final Class[] ADDITIONAL_VARIABLE_ANNOTATION_CLASSES = {
            PlanningPin.class,
            PlanningPinToIndex.class
    };

    private EntityDescriptorValidator() {
    }

    /**
     * Mixed inheritance is not permitted. Therefore, inheritance must consist only of classes or only of interfaces.
     */
    public static void assertNotMixedInheritance(Class<?> entityClass, List<Class<?>> declaredInheritedEntityClassList) {
        if (declaredInheritedEntityClassList.isEmpty()) {
            return;
        }
        var hasClass = false;
        var hasInterface = false;
        for (var declaredEntityClass : declaredInheritedEntityClassList) {
            if (!hasClass && !declaredEntityClass.isInterface()) {
                hasClass = true;
            }
            if (!hasInterface && declaredEntityClass.isInterface()) {
                hasInterface = true;
            }
            if (hasClass && hasInterface) {
                break;
            }
        }
        if (hasClass && hasInterface) {
            var classes = declaredInheritedEntityClassList.stream()
                    .filter(clazz -> !clazz.isInterface())
                    .map(Class::getSimpleName)
                    .toList();
            var interfaces = declaredInheritedEntityClassList.stream()
                    .filter(Class::isInterface)
                    .map(Class::getSimpleName)
                    .toList();
            throw new IllegalStateException(
                    """
                            The class %s extends another class marked as an entity (%s) and also implements an interface that is annotated as an entity (%s). Mixed inheritance is not permitted.
                            Maybe remove either the entity class or one of the entity interfaces from the inheritance chain."""
                            .formatted(classes, interfaces, entityClass));
        }
    }

    public static void assertSingleInheritance(Class<?> entityClass, List<Class<?>> declaredInheritedEntityClassList) {
        if (declaredInheritedEntityClassList.size() > 1) {
            var classes = declaredInheritedEntityClassList.stream()
                    .filter(clazz -> !clazz.isInterface())
                    .map(Class::getSimpleName)
                    .toList();
            var interfaces = declaredInheritedEntityClassList.stream()
                    .filter(Class::isInterface)
                    .map(Class::getSimpleName)
                    .toList();
            throw new IllegalStateException(
                    """
                            The class %s inherits its @%s annotation both from entities (%s) and interfaces (%s).
                            Remove either the entity classes or entity interfaces from the inheritance chain to create a single-level inheritance structure."""
                            .formatted(entityClass.getName(), PlanningEntity.class.getSimpleName(), classes, interfaces));
        }
    }

    /**
     * If a class declares any variable (genuine or shadow), it must be annotated as an entity,
     * even if a supertype already has the annotation.
     */
    public static void assertValidPlanningVariables(Class<?> clazz) {
        // We first check the entity class
        if (clazz.getAnnotation(PlanningEntity.class) == null && hasAnyGenuineOrShadowVariables(clazz)) {
            var planningVariables = extractPlanningVariables(clazz).stream()
                    .map(Member::getName)
                    .toList();
            throw new IllegalStateException(
                    """
                            The class %s is not annotated with @PlanningEntity but defines genuine or shadow variables.
                            Maybe annotate %s with @PlanningEntity.
                            Maybe remove the planning variables (%s)."""
                            .formatted(clazz.getName(), clazz.getName(), planningVariables));
        }
        // We check the first level of the inheritance chain
        var classList = new ArrayList<Class<?>>();
        classList.add(clazz.getSuperclass());
        classList.addAll(Arrays.asList(clazz.getInterfaces()));
        for (var otherClazz : classList) {
            if (otherClazz != null && otherClazz.getAnnotation(PlanningEntity.class) == null
                    && hasAnyGenuineOrShadowVariables(otherClazz)) {
                var planningVariables = extractPlanningVariables(otherClazz).stream()
                        .map(Member::getName)
                        .toList();
                throw new IllegalStateException(
                        """
                                The class %s is not annotated with @PlanningEntity but defines genuine or shadow variables.
                                Maybe annotate %s with @PlanningEntity.
                                Maybe remove the planning variables (%s)."""
                                .formatted(otherClazz.getName(), otherClazz.getName(), planningVariables));
            }
        }
    }

    private static List<Member> extractPlanningVariables(Class<?> entityClass) {
        var membersList = ConfigUtils.getDeclaredMembers(entityClass);
        return membersList.stream()
                .filter(member -> ConfigUtils.extractAnnotationClass(member, VARIABLE_ANNOTATION_CLASSES) != null
                        || ConfigUtils.extractAnnotationClass(member, ADDITIONAL_VARIABLE_ANNOTATION_CLASSES) != null)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static boolean hasAnyGenuineOrShadowVariables(Class<?> entityClass) {
        return !extractPlanningVariables(entityClass).isEmpty();
    }

    public static boolean isEntityClass(Class<?> clazz) {
        return clazz.getAnnotation(PlanningEntity.class) != null
                || clazz.getSuperclass() != null && clazz.getSuperclass().getAnnotation(PlanningEntity.class) != null
                || Arrays.stream(clazz.getInterfaces()).anyMatch(i -> i.getAnnotation(PlanningEntity.class) != null);
    }
}
