package ai.timefold.solver.spring.boot.autoconfigure.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.javapoet.CodeBlock;

public final class PojoInliner {
    static final String COMPLEX_POJO_MAP_FIELD_NAME = "$pojoMap";
    private final Map<Object, String> complexPojoToIdentifier = new IdentityHashMap<>();
    private final Set<Object> possibleCircularRecordReferenceSet = Collections.newSetFromMap(new IdentityHashMap<>());

    // The code below uses CodeBlock.Builder to generate the Java file
    // that stores the SolverConfig.
    // CodeBlock.Builder.add supports different kinds of formatting args.
    // The ones we use are:
    // - $L: Format as is (i.e. literal replacement).
    // - $S: Format as a Java String, doing the necessary escapes
    //       and surrounding it by double quotes.
    // - $T: Format as a fully qualified type, which allows you to use
    //       classes without importing them.

    /**
     * Serializes a Pojo to code that uses its no-args constructor
     * and setters to create the object.
     *
     * @param pojo The object to be serialized.
     * @param initializerCode The code block builder of the initializer
     * @return A string that can be used in a {@link CodeBlock.Builder} to access the object
     */
    public String getInlinedPojo(Object pojo,
            CodeBlock.Builder initializerCode) {
        // First, check for primitives
        if (pojo == null) {
            return "null";
        }
        if (pojo instanceof Boolean value) {
            return value.toString();
        }
        if (pojo instanceof Byte value) {
            // Cast to byte
            return "((byte) " + value + ")";
        }
        if (pojo instanceof Character value) {
            return "'\\u" + Integer.toHexString(value | 0x10000).substring(1) + "'";
        }
        if (pojo instanceof Short value) {
            // Cast to short
            return "((short) " + value + ")";
        }
        if (pojo instanceof Integer value) {
            return value.toString();
        }
        if (pojo instanceof Long value) {
            // Add long suffix to number string
            return value + "L";
        }
        if (pojo instanceof Float value) {
            // Add float suffix to number string
            return value + "f";
        }
        if (pojo instanceof Double value) {
            // Add double suffix to number string
            return value + "d";
        }

        // Check for builtin classes
        if (pojo instanceof String value) {
            return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
        }
        if (pojo instanceof Class<?> value) {
            if (!Modifier.isPublic(value.getModifiers())) {
                throw new IllegalArgumentException("Cannot serialize (" + value + ") because it is not a public class.");
            }
            return value.getCanonicalName() + ".class";
        }
        if (pojo instanceof ClassLoader) {
            // We don't support serializing ClassLoaders, so replace it
            // with the context class loader
            return "Thread.currentThread().getContextClassLoader()";
        }
        if (pojo instanceof Duration value) {
            return Duration.class.getCanonicalName() + ".ofNanos(" + value.toNanos() + "L)";
        }
        if (pojo.getClass().isEnum()) {
            // Use field access to read the enum
            Class<?> enumClass = pojo.getClass();
            if (!Modifier.isPublic(enumClass.getModifiers())) {
                throw new IllegalArgumentException(
                        "Cannot serialize (" + pojo + ") because its type (" + enumClass + ") is not a public class.");
            }
            Enum<?> pojoEnum = (Enum<?>) pojo;
            return enumClass.getCanonicalName() + "." + pojoEnum.name();
        }
        return getInlinedComplexPojo(pojo, initializerCode);
    }

    /**
     * Return a string that can be used in a {@link CodeBlock.Builder} to access a complex object
     *
     * @param pojo The object to be accessed
     * @return A string that can be used in a {@link CodeBlock.Builder} to access the object.
     */
    private String getPojoFromMap(Object pojo) {
        return CodeBlock.builder().add("(($T) $L.get($S))",
                pojo.getClass(),
                COMPLEX_POJO_MAP_FIELD_NAME,
                complexPojoToIdentifier.get(pojo)).build().toString();
    }

    /**
     * Serializes collections and complex POJOs to code
     */
    private String getInlinedComplexPojo(Object pojo, CodeBlock.Builder initializerCode) {
        if (possibleCircularRecordReferenceSet.contains(pojo)) {
            // Records do not have a no-args constructor, so we cannot safely serialize self-references in records
            // as we cannot do a map lookup before the record is created.
            throw new IllegalArgumentException(
                    "Cannot serialize record (" + pojo + ") because it is a record containing contains a circular reference.");
        }

        // If we already serialized the object, we should just return
        // the code string
        if (complexPojoToIdentifier.containsKey(pojo)) {
            return getPojoFromMap(pojo);
        }
        if (pojo instanceof Record value) {
            // Records must set all fields at initialization time,
            // so we delay the declaration of its variable
            return getInlinedRecord(value, initializerCode);
        }
        // Object is not serialized yet
        // Create a new variable to store its value when setting its fields
        String newIdentifier = "$obj" + complexPojoToIdentifier.size();
        complexPojoToIdentifier.put(pojo, newIdentifier);

        // First, check if it is a collection type
        if (pojo.getClass().isArray()) {
            return getInlinedArray(newIdentifier, pojo, initializerCode);
        }
        if (pojo instanceof List<?> value) {
            return getInlinedList(newIdentifier, value, initializerCode);
        }
        if (pojo instanceof Set<?> value) {
            return getInlinedSet(newIdentifier, value, initializerCode);
        }
        if (pojo instanceof Map<?, ?> value) {
            return getInlinedMap(newIdentifier, value, initializerCode);
        }

        // Not a collection or record type, so serialize by creating a new instance and settings its fields
        if (!Modifier.isPublic(pojo.getClass().getModifiers())) {
            throw new IllegalArgumentException("Cannot serialize (" + pojo + ") because its type (" + pojo.getClass()
                    + ") is not public.");
        }
        initializerCode.add("\n$T $L;", pojo.getClass(), newIdentifier);
        try {
            Constructor<?> constructor = pojo.getClass().getConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                throw new IllegalArgumentException("Cannot serialize (" + pojo + ") because its type's (" + pojo.getClass()
                        + ") no-args constructor is not public.");
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot serialize (" + pojo + ") because its type (" + pojo.getClass()
                    + ") does not have a public no-args constructor.");
        }
        initializerCode.add("\n$L = new $T();", newIdentifier, pojo.getClass());
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        inlineFieldsOfPojo(pojo.getClass(), newIdentifier, pojo, initializerCode);
        return getPojoFromMap(pojo);
    }

    private String getInlinedArray(String newIdentifier, Object array, CodeBlock.Builder initializerCode) {
        Class<?> componentType = array.getClass().getComponentType();
        if (!Modifier.isPublic(componentType.getModifiers())) {
            throw new IllegalArgumentException(
                    "Cannot serialize array of type (" + componentType + ") because (" + componentType + ") is not public.");
        }
        initializerCode.add("\n$T $L;", array.getClass(), newIdentifier);

        // Get the length of the array
        int length = Array.getLength(array);

        // Create a new array from the component type with the given length
        initializerCode.add("\n$L = new $T[$L];", newIdentifier, componentType, Integer.toString(length));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (int i = 0; i < length; i++) {
            // Set the elements of the array
            initializerCode.add("\n$L[$L] = $L;",
                    newIdentifier,
                    Integer.toString(i),
                    getInlinedPojo(Array.get(array, i), initializerCode));
        }
        return getPojoFromMap(array);
    }

    private String getInlinedList(String newIdentifier, List<?> list, CodeBlock.Builder initializerCode) {
        initializerCode.add("\n$T $L;", List.class, newIdentifier);

        // Create an ArrayList
        initializerCode.add("\n$L = new $T($L);", newIdentifier, ArrayList.class, Integer.toString(list.size()));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (Object item : list) {
            // Add each item of the list to the ArrayList
            initializerCode.add("\n$L.add($L);",
                    newIdentifier,
                    getInlinedPojo(item, initializerCode));
        }
        return getPojoFromMap(list);
    }

    private String getInlinedSet(String newIdentifier, Set<?> set, CodeBlock.Builder initializerCode) {
        initializerCode.add("\n$T $L;", Set.class, newIdentifier);

        // Create a new HashSet
        initializerCode.add("\n$L = new $T($L);", newIdentifier, LinkedHashSet.class, Integer.toString(set.size()));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (Object item : set) {
            // Add each item of the set to the HashSet
            initializerCode.add("\n$L.add($L);",
                    newIdentifier,
                    getInlinedPojo(item, initializerCode));
        }
        return getPojoFromMap(set);
    }

    private String getInlinedMap(String newIdentifier, Map<?, ?> map, CodeBlock.Builder initializerCode) {
        initializerCode.add("\n$T $L;", Map.class, newIdentifier);

        // Create a HashMap
        initializerCode.add("\n$L = new $T($L);", newIdentifier, LinkedHashMap.class, Integer.toString(map.size()));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            // Put each entry of the map into the HashMap
            initializerCode.add("\n$L.put($L, $L);",
                    newIdentifier,
                    getInlinedPojo(entry.getKey(), initializerCode),
                    getInlinedPojo(entry.getValue(), initializerCode));
        }
        return getPojoFromMap(map);
    }

    private String getInlinedRecord(Record record, CodeBlock.Builder initializerCode) {
        possibleCircularRecordReferenceSet.add(record);
        Class<? extends Record> recordClass = record.getClass();
        if (!Modifier.isPublic(recordClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "Cannot serialize record (" + record + ") because its type (" + recordClass + ") is not public.");
        }

        RecordComponent[] recordComponents = recordClass.getRecordComponents();
        String[] componentAccessors = new String[recordComponents.length];
        for (int i = 0; i < recordComponents.length; i++) {
            Object value;
            Class<?> serializedType = getSerializedType(recordComponents[i].getType());
            if (!recordComponents[i].getType().equals(serializedType)) {
                throw new IllegalArgumentException(
                        "Cannot serialize type (" + recordClass + ") as its component (" + recordComponents[i].getName()
                                + ") uses an implementation of a collection ("
                                + recordComponents[i].getType() + ") instead of the interface type (" + serializedType + ").");
            }
            try {
                value = recordComponents[i].getAccessor().invoke(record);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            try {
                componentAccessors[i] = getInlinedPojo(value, initializerCode);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot serialize record (" + record + ") because the value ("
                        + value + ") for its component (" + recordComponents[i].getName() + ") is not serializable.", e);
            }
        }
        // All components serialized, so no circular references
        possibleCircularRecordReferenceSet.remove(record);
        StringBuilder constructorArgs = new StringBuilder();
        for (String componentAccessor : componentAccessors) {
            constructorArgs.append(componentAccessor).append(", ");
        }
        if (componentAccessors.length != 0) {
            constructorArgs.delete(constructorArgs.length() - 2, constructorArgs.length());
        }
        String newIdentifier = "$obj" + complexPojoToIdentifier.size();
        complexPojoToIdentifier.put(record, newIdentifier);
        initializerCode.add("\n$T $L = new $T($L);", recordClass, newIdentifier, recordClass, constructorArgs.toString());
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        return getPojoFromMap(record);
    }

    private Class<?> getSerializedType(Class<?> query) {
        if (List.class.isAssignableFrom(query)) {
            return List.class;
        }
        if (Set.class.isAssignableFrom(query)) {
            return Set.class;
        }
        if (Map.class.isAssignableFrom(query)) {
            return Map.class;
        }
        return query;
    }

    /**
     * Sets the fields of pojo declared in pojoClass and all its superclasses.
     *
     * @param pojoClass A class assignable to pojo containing some of its fields.
     * @param identifier The name of the variable storing the serialized pojo.
     * @param pojo The object being serialized.
     * @param initializerCode The {@link CodeBlock.Builder} to use to generate code in the initializer.
     */
    private void inlineFieldsOfPojo(Class<?> pojoClass, String identifier, Object pojo,
            CodeBlock.Builder initializerCode) {
        if (pojoClass == Object.class) {
            // We are the top-level, no more fields to set
            return;
        }
        Field[] fields = pojoClass.getDeclaredFields();
        // Sort by name to guarantee a consistent ordering
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                // We do not want to write static fields
                continue;
            }
            // Set the field accessible so we can read its value
            field.setAccessible(true);
            Class<?> serializedType = getSerializedType(field.getType());
            Method setterMethod = ReflectionHelper.getSetterMethod(pojoClass, serializedType, field.getName());
            // setterMethod guaranteed to be public
            if (setterMethod == null) {
                if (!field.getType().equals(serializedType)) {
                    throw new IllegalArgumentException(
                            "Cannot serialize type (" + pojoClass + ") as its field (" + field.getName()
                                    + ") uses an implementation of a collection ("
                                    + field.getType() + ") instead of the interface type (" + serializedType + ").");
                }
                throw new IllegalArgumentException(
                        "Cannot serialize type (" + pojoClass + ") as it is missing a public setter method for field ("
                                + field.getName() + ") of type (" + field.getType() + ").");
            }
            Object value;
            try {
                value = field.get(pojo);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            try {
                // Convert the field value to code, and call the setter
                // corresponding to the field with the serialized field value.
                initializerCode.add("\n$L.$L($L);", identifier,
                        setterMethod.getName(),
                        getInlinedPojo(value, initializerCode));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot serialize object (" + pojo + ") because the value (" + value
                        + ") for its field (" + field.getName() + ") is not serializable.", e);
            }
        }
        try {
            inlineFieldsOfPojo(pojoClass.getSuperclass(), identifier, pojo, initializerCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot serialize type (" + pojoClass + ") because its superclass ("
                    + pojoClass.getSuperclass() + ") is not serializable.", e);
        }

    }
}
