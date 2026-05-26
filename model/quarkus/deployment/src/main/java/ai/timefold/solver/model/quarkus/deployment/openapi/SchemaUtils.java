package ai.timefold.solver.model.quarkus.deployment.openapi;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.JandexReflection;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;

public final class SchemaUtils {

    private static final Set<String> WRAPPER_CLASSES = Set.of(
            Boolean.class.getName(),
            Byte.class.getName(),
            Character.class.getName(),
            Double.class.getName(),
            Float.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Short.class.getName());

    private SchemaUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Resolves the {@link Schema.SchemaType} from a {@link Type}.
     *
     * @param type the type of e.g. a field
     * @return the schema type
     */
    public static Schema.SchemaType resolveSchemaType(Type type) {
        switch (type.kind()) {
            case PARAMETERIZED_TYPE:
                if (isCollection(type)) {
                    return Schema.SchemaType.ARRAY;
                } else {
                    return Schema.SchemaType.OBJECT;
                }
            case CLASS:
                if (isString(type) || isDuration(type)) {
                    return Schema.SchemaType.STRING;
                } else if (isCollection(type)) {
                    return Schema.SchemaType.ARRAY;
                } else if (isWrapperClass(type)) {
                    return resolvePrimitiveSchemaType(PrimitiveType.unbox(type.asClassType()).primitive());
                } else {
                    return Schema.SchemaType.OBJECT;
                }
            case ARRAY:
                return Schema.SchemaType.ARRAY;
            case PRIMITIVE:
                return resolvePrimitiveSchemaType(type.asPrimitiveType().primitive());
            default:
                return Schema.SchemaType.STRING; // best effort
        }
    }

    /**
     * Resolves the {@link Schema.SchemaType} of a field that represents an array
     * or a {@link Collection}.
     *
     * @param field the field
     * @return the schema type
     */
    public static Schema.SchemaType resolveArrayItemSchemaType(FieldInfo field) {
        if (field.type().kind() == Type.Kind.ARRAY) {
            return resolveSchemaType(field.type().asArrayType().componentType());
        } else if (field.type().kind() == Type.Kind.PARAMETERIZED_TYPE && isCollection(field.type())) {
            return resolveSchemaType(field.type().asParameterizedType().arguments().getFirst());
        } else {
            throw new IllegalStateException(
                    "Field (%s) in class (%s) is not an array or collection.".formatted(field.name(),
                            field.declaringClass().name().toString()));
        }
    }

    private static Schema.SchemaType resolvePrimitiveSchemaType(PrimitiveType.Primitive primitive) {
        switch (primitive) {
            case BOOLEAN:
                return Schema.SchemaType.BOOLEAN;
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
                return Schema.SchemaType.INTEGER;
            case FLOAT:
            case DOUBLE:
                return Schema.SchemaType.NUMBER;
            case CHAR:
            default:
                return Schema.SchemaType.STRING;
        }
    }

    /**
     * Resolves the schema type reference for a field. In the generated OpenAPI schema, the schema type name is the simple class
     * name of the given type.
     * <p>
     * The method resolves the schema type reference as follows:
     * <ul>
     * <li>If the field type is a {@link Collection} or an array, the schema type reference is the simple class name
     * of the type argument or the array item, resp.</li>
     * <li>If the field type is a class, the schema type reference is the simple class name.</li>
     * <li>If the field type is a primitive type, a wrapper type, or a {@link String}, the schema type reference is
     * {@code null}. The same logic applies to an array item or {@link Collection} type argument.</li>
     * </ul>
     *
     * @param field the field
     * @return the schema type reference
     */
    public static String resolveSchemaTypeRef(FieldInfo field) {
        switch (field.type().kind()) {
            case PARAMETERIZED_TYPE:
                if (isCollection(field.type())) {
                    if (field.type().asParameterizedType().arguments().size() > 1) {
                        throw new IllegalStateException(
                                "Field (%s) in class (%s) has more than one type argument.".formatted(field.name(),
                                        field.declaringClass().name().toString()));
                    }
                    Type parameter = field.type().asParameterizedType().arguments().getFirst();
                    if (isStringOrWrapperClass(parameter)) {
                        return null;
                    }
                    // Type argument can only be a class, so the schema type ref is the simple class name.
                    return parameter.asClassType().name().withoutPackagePrefix();
                } else {
                    // Refer to the type itself and disregard the type parameter.
                    return field.type().asClassType().name().withoutPackagePrefix();
                }
            case CLASS:
                return isStringOrWrapperClass(field.type()) ? null
                        : field.type().asClassType().name().withoutPackagePrefix();
            case ARRAY:
                Type componentType = field.type().asArrayType().componentType();
                switch (componentType.kind()) {
                    case CLASS:
                        return isStringOrWrapperClass(componentType) ? null
                                : componentType.asClassType().name().withoutPackagePrefix();
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static boolean isStringOrWrapperClass(Type type) {
        return isString(type) || isWrapperClass(type);
    }

    private static boolean isString(Type type) {
        return type.name().toString().equals(String.class.getName());
    }

    private static boolean isCollection(Type type) {
        return Collection.class.isAssignableFrom(JandexReflection.loadRawType(type));
    }

    public static boolean isDuration(Type type) {
        return type.name().toString().equals(Duration.class.getName());
    }

    private static boolean isWrapperClass(Type type) {
        return WRAPPER_CLASSES.contains(type.name().toString());
    }
}
