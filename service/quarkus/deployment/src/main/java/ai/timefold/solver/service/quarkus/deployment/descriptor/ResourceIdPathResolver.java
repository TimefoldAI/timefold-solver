package ai.timefold.solver.service.quarkus.deployment.descriptor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.service.definition.api.usage.UniqueResourceId;
import ai.timefold.solver.service.definition.internal.descriptor.usage.UniqueResource;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Finds the fields annotated with {@link UniqueResourceId} in the object structure of the model input
 * and creates a JSON Path expression to each of them.
 * <p>
 * Multiple fields may share the same {@link UniqueResourceId#type()};
 * the IDs must then be unique across all the fields of that type.
 * <p>
 * The resolver walks the fields of the indexed classes.
 * It goes into the elements of arrays, collections ({@code [*]}) and the values of maps ({@code .*}).
 * It honors {@code @JsonProperty} names and skips static, transient and {@code @JsonIgnore} fields.
 */
public final class ResourceIdPathResolver {

    private static final DotName RESOURCE_ID = DotName.createSimple(UniqueResourceId.class);
    private static final String RESOURCE_ID_TYPE_PARAM = "type";
    private static final DotName JSON_PROPERTY = DotName.createSimple(JsonProperty.class);
    private static final DotName JSON_IGNORE = DotName.createSimple(JsonIgnore.class);
    private static final DotName OBJECT = DotName.createSimple(Object.class);
    private static final String ROOT = "$";

    private ResourceIdPathResolver() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param index never null
     * @param modelInput never null
     * @return never null, one {@link UniqueResource} for each field annotated with {@link UniqueResourceId}
     *         reachable from the model input, sorted by the resource type and the path;
     *         empty if no such field exists
     * @throws IllegalStateException if a {@link UniqueResourceId#type()} is blank
     */
    public static List<UniqueResource> resolve(IndexView index, ClassInfo modelInput) {
        var resourceList = new ArrayList<UniqueResource>();
        visitClass(index, modelInput, ROOT, new HashSet<>(), resourceList);
        resourceList.sort(Comparator.comparing(UniqueResource::resourceType));
        return List.copyOf(resourceList);
    }

    private static void visitClass(IndexView index, ClassInfo classInfo, String path, Set<DotName> visitingSet,
            List<UniqueResource> resourceList) {
        if (!visitingSet.add(classInfo.name())) {
            // Cycle in the object structure.
            return;
        }
        var currentClass = classInfo;
        while (currentClass != null) {
            for (var field : currentClass.fields()) {
                if (isIgnored(field)) {
                    continue;
                }
                var fieldPath = path + "." + jsonName(field);
                if (field.hasDeclaredAnnotation(RESOURCE_ID)) {
                    resourceList.add(new UniqueResource(resourceType(field), fieldPath));
                } else {
                    visitType(index, field.type(), fieldPath, visitingSet, resourceList);
                }
            }
            var superName = currentClass.superName();
            currentClass = superName == null || superName.equals(OBJECT) ? null : index.getClassByName(superName);
        }
        visitingSet.remove(classInfo.name());
    }

    private static void visitType(IndexView index, Type type, String path, Set<DotName> visitingSet,
            List<UniqueResource> resourceList) {
        switch (type.kind()) {
            case ARRAY -> visitType(index, type.asArrayType().elementType(), path + "[*]", visitingSet, resourceList);
            case PARAMETERIZED_TYPE -> {
                var argumentList = type.asParameterizedType().arguments();
                if (isAssignableTo(type.name(), Collection.class) && argumentList.size() == 1) {
                    visitType(index, argumentList.get(0), path + "[*]", visitingSet, resourceList);
                } else if (isAssignableTo(type.name(), Map.class) && argumentList.size() == 2) {
                    visitType(index, argumentList.get(1), path + ".*", visitingSet, resourceList);
                } else {
                    visitClassByName(index, type.name(), path, visitingSet, resourceList);
                }
            }
            case CLASS -> visitClassByName(index, type.name(), path, visitingSet, resourceList);
            default -> {
                // Primitives are already handled by the visitClass method; type variables and wildcards have no fields to visit.
            }
        }
    }

    private static void visitClassByName(IndexView index, DotName className, String path, Set<DotName> visitingSet,
            List<UniqueResource> resourceList) {
        var classInfo = index.getClassByName(className);
        if (classInfo != null) {
            visitClass(index, classInfo, path, visitingSet, resourceList);
        }
    }

    private static boolean isAssignableTo(DotName className, Class<?> superType) {
        try {
            var clazz = Class.forName(className.toString(), false, ResourceIdPathResolver.class.getClassLoader());
            return superType.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isIgnored(FieldInfo field) {
        if (Modifier.isStatic(field.flags()) || Modifier.isTransient(field.flags())) {
            return true;
        }
        var jsonIgnore = field.declaredAnnotation(JSON_IGNORE);
        if (jsonIgnore == null) {
            return false;
        }
        var value = jsonIgnore.value();
        return value == null || value.asBoolean();
    }

    private static String resourceType(FieldInfo field) {
        var typeValue = field.declaredAnnotation(RESOURCE_ID).value(RESOURCE_ID_TYPE_PARAM);
        if (typeValue == null || typeValue.asString().trim().isBlank()) {
            throw new IllegalStateException("The field (%s) of the class (%s) is annotated with @%s with a blank %s."
                    .formatted(field.name(), field.declaringClass().name(), UniqueResourceId.class.getSimpleName(),
                            RESOURCE_ID_TYPE_PARAM));
        }
        return typeValue.asString();
    }

    private static String jsonName(FieldInfo field) {
        var jsonProperty = field.declaredAnnotation(JSON_PROPERTY);
        if (jsonProperty != null) {
            var value = jsonProperty.value();
            if (value != null && !value.asString().isEmpty()) {
                return value.asString();
            }
        }
        return field.name();
    }
}
