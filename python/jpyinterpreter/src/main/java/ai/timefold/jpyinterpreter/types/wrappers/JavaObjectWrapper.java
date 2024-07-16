package ai.timefold.jpyinterpreter.types.wrappers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.AttributeError;
import ai.timefold.jpyinterpreter.types.errors.RuntimeError;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class JavaObjectWrapper implements PythonLikeObject,
        Iterable<JavaObjectWrapper>,
        Comparable<JavaObjectWrapper> {

    final static Map<Class<?>, PythonLikeType> classToPythonTypeMap = new HashMap<>();
    final static Map<Class<?>, Map<String, Field>> classToAttributeNameToMemberListMap = new HashMap<>();

    private final PythonLikeType type;

    private final Object wrappedObject;
    private final Class<?> objectClass;
    private final Map<String, Field> attributeNameToMemberListMap;
    private final Map<Object, PythonLikeObject> convertedObjectMap;

    private static Map<String, Field> getAllFields(Class<?> baseClass) {
        return getAllDeclaredMembers(baseClass)
                .stream()
                .filter(member -> member instanceof Field && !Modifier.isStatic(member.getModifiers()))
                .collect(Collectors.toMap(Member::getName, member -> (Field) member,
                        (oldMember, newMember) -> {
                            if (oldMember.getDeclaringClass().isAssignableFrom(newMember.getDeclaringClass())) {
                                return newMember;
                            } else {
                                return oldMember;
                            }
                        }));
    }

    private static List<Member> getAllDeclaredMembers(Class<?> baseClass) {
        Class<?> clazz = baseClass;

        List<Member> members = new ArrayList<>();
        members.addAll(Arrays.asList(clazz.getFields()));
        members.addAll(Arrays.asList(clazz.getMethods()));
        return members;
    }

    private Method getGetterMethod(Field field) {
        String getterName;
        if (objectClass.isRecord()) {
            getterName = field.getName();
        } else {
            String propertyName = field.getName();
            String capitalizedName =
                    propertyName.isEmpty() ? "" : propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            getterName = (field.getType().equals(boolean.class) ? "is" : "get")
                    + capitalizedName;
        }
        PythonLikeObject object = type.$getAttributeOrNull(getterName);
        if (object instanceof JavaMethodReference methodReference) {
            return methodReference.getMethod();
        }
        if (object instanceof MultiDispatchJavaMethodReference multiDispatchJavaMethodReference) {
            return multiDispatchJavaMethodReference.getNoArgsMethod();
        }
        throw new AttributeError("Cannot get attribute (%s) on class (%s)."
                .formatted(field.getName(), type));
    }

    private Method getSetterMethod(Field field) {
        String propertyName = field.getName();
        String capitalizedName =
                propertyName.isEmpty() ? "" : propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String setterName = "set" + capitalizedName;

        PythonLikeObject object = type.$getAttributeOrNull(setterName);
        if (object instanceof JavaMethodReference methodReference) {
            return methodReference.getMethod();
        }
        throw new AttributeError("Cannot set attribute (%s) on class (%s)."
                .formatted(field.getName(), type));
    }

    public JavaObjectWrapper(Object wrappedObject) {
        this(wrappedObject, new IdentityHashMap<>());
    }

    public JavaObjectWrapper(Object wrappedObject, Map<Object, PythonLikeObject> convertedObjectMap) {
        convertedObjectMap.put(wrappedObject, this);
        this.wrappedObject = wrappedObject;
        this.objectClass = wrappedObject.getClass();
        this.convertedObjectMap = convertedObjectMap;
        this.attributeNameToMemberListMap =
                classToAttributeNameToMemberListMap.computeIfAbsent(objectClass, JavaObjectWrapper::getAllFields);
        this.type = getPythonTypeForClass(objectClass, convertedObjectMap);
    }

    public static PythonLikeType getPythonTypeForClass(Class<?> objectClass) {
        return getPythonTypeForClass(objectClass, new IdentityHashMap<>());
    }

    public static PythonLikeType getPythonTypeForClass(Class<?> objectClass, Map<Object, PythonLikeObject> convertedObjectMap) {
        if (classToPythonTypeMap.containsKey(objectClass)) {
            return classToPythonTypeMap.get(objectClass);
        }
        PythonLikeType out = generatePythonTypeForClass(objectClass, convertedObjectMap);
        classToPythonTypeMap.put(objectClass, out);
        return out;
    }

    private static boolean isInaccessible(Member member) {
        return isInaccessible(member.getDeclaringClass());
    }

    private static boolean isInaccessible(Class<?> clazz) {
        for (Class<?> declaringClass = clazz; declaringClass != null; declaringClass = declaringClass.getDeclaringClass()) {
            if (!Modifier.isPublic(declaringClass.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    private static Method findMethodInInterfaces(Class<?> declaringClass, Method method) {
        Queue<Class<?>> toVisit = new ArrayDeque<>();
        while (declaringClass != null) {
            toVisit.addAll(List.of(declaringClass.getInterfaces()));
            declaringClass = declaringClass.getSuperclass();
        }
        Set<Class<?>> visited = new HashSet<>();
        while (!toVisit.isEmpty()) {
            Class<?> interfaceClass = toVisit.poll();
            if (visited.contains(interfaceClass)) {
                continue;
            }
            visited.add(interfaceClass);
            toVisit.addAll(Arrays.asList(interfaceClass.getInterfaces()));
            if (isInaccessible(interfaceClass)) {
                continue;
            }
            try {
                return interfaceClass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                // Intentionally empty, need to search other interfaces
            }
        }
        return null;
    }

    private static void addMemberToPythonType(PythonLikeType type, Member member,
            Map<Object, PythonLikeObject> convertedObjectMap) {
        if (member instanceof Method method) {
            if (isInaccessible(member)) {
                method = findMethodInInterfaces(method.getDeclaringClass(), method);
                if (method == null) {
                    return;
                }
            }
            // For certain Collection/List/Map methods, also add their corresponding dunder methods,
            // so a[x], x in a, len(a) will work.
            switch (method.getName()) {
                case "size" -> {
                    if (Collection.class.isAssignableFrom(method.getDeclaringClass())) {
                        type.__dir__.put(PythonUnaryOperator.LENGTH.getDunderMethod(),
                                new JavaMethodReference(method, Map.of()));
                    }
                }
                case "contains" -> {
                    if (Collection.class.isAssignableFrom(method.getDeclaringClass())) {
                        type.__dir__.put(PythonBinaryOperator.CONTAINS.getDunderMethod(),
                                new JavaMethodReference(method, Map.of()));
                    }
                }
                case "get" -> {
                    type.__dir__.put(PythonBinaryOperator.GET_ITEM.getDunderMethod(),
                            new JavaMethodReference(method, Map.of()));
                }
            }
            ((MultiDispatchJavaMethodReference) type.__dir__.computeIfAbsent(method.getName(),
                    (name) -> new MultiDispatchJavaMethodReference())).addMethod(method);
        } else {
            if (isInaccessible(member)) {
                return;
            }
            Field field = (Field) member;
            if (Modifier.isPublic(field.getModifiers())) {
                try {
                    type.__dir__.put(field.getName(),
                            JavaPythonTypeConversionImplementor.wrapJavaObject(field.get(null),
                                    convertedObjectMap));
                } catch (IllegalAccessException e) {
                    throw (RuntimeError) new RuntimeError("Cannot get attribute (%s) on type (%s)."
                            .formatted(field.getName(), type.getTypeName())).initCause(e);
                }
            }
        }
    }

    private static PythonLikeType generatePythonTypeForClass(Class<?> objectClass,
            Map<Object, PythonLikeObject> convertedObjectMap) {
        var out = new PythonLikeType(objectClass.getName(), JavaObjectWrapper.class, objectClass);
        getAllDeclaredMembers(objectClass)
                .stream()
                .filter(member -> Modifier.isStatic(member.getModifiers()) || member instanceof Method)
                .forEach(member -> addMemberToPythonType(out, member, convertedObjectMap));
        return out;
    }

    public Object getWrappedObject() {
        return wrappedObject;
    }

    @Override
    public PythonLikeObject $getAttributeOrNull(String attributeName) {
        Field field = attributeNameToMemberListMap.get(attributeName);
        if (field == null) {
            return null;
        }
        Object result;
        try {
            if (Modifier.isPublic(field.getModifiers())) {
                result = field.get(wrappedObject);
            } else {
                Method getterMethod = getGetterMethod(field);
                result = getterMethod.invoke(wrappedObject);
            }
            return JavaPythonTypeConversionImplementor.wrapJavaObject(result, convertedObjectMap);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw (RuntimeError) new RuntimeError("Cannot get attribute (%s) on object (%s)."
                    .formatted(attributeName, this)).initCause(e);
        }
    }

    @Override
    public void $setAttribute(String attributeName, PythonLikeObject value) {
        Field field = attributeNameToMemberListMap.get(attributeName);
        if (field == null) {
            throw new AttributeError("(%s) object does not have attribute (%s)."
                    .formatted(type, attributeName));
        }
        try {
            Object javaObject = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(field.getType(), value);
            if (Modifier.isPublic(field.getModifiers())) {
                field.set(wrappedObject, javaObject);
            } else {
                Method setterMethod = getSetterMethod(field);
                setterMethod.invoke(wrappedObject, javaObject);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void $deleteAttribute(String attributeName) {
        throw new IllegalArgumentException("Cannot delete attributes on type '" + objectClass + "'");
    }

    @Override
    public PythonLikeType $getType() {
        return type;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compareTo(JavaObjectWrapper javaObjectWrapper) {
        if (!(wrappedObject instanceof Comparable comparable)) {
            throw new IllegalStateException("Class (%s) does not implement (%s).".formatted(objectClass, Comparable.class));
        }
        return comparable.compareTo(javaObjectWrapper.wrappedObject);
    }

    @Override
    public Iterator<JavaObjectWrapper> iterator() {
        if (!(wrappedObject instanceof Iterable<?> iterable)) {
            throw new IllegalStateException("Class (%s) does not implement (%s).".formatted(objectClass, Iterable.class));
        }
        return new WrappingJavaObjectIterator(iterable.iterator());
    }

    @Override
    public String toString() {
        return wrappedObject.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof JavaObjectWrapper) {
            return wrappedObject.equals(((JavaObjectWrapper) other).wrappedObject);
        }
        return wrappedObject.equals(other);
    }

    @Override
    public int hashCode() {
        return wrappedObject.hashCode();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
