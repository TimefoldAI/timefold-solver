/*
 * Copied from the Hibernate Validator project
 * Original authors: Hardy Ferentschik, Gunnar Morling and Kevin Pollet.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.timefold.solver.core.impl.domain.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReflectionHelper {

    private static final String PROPERTY_ACCESSOR_PREFIX_GET = "get";
    private static final String PROPERTY_ACCESSOR_PREFIX_IS = "is";
    private static final String[] PROPERTY_ACCESSOR_PREFIXES = {
            PROPERTY_ACCESSOR_PREFIX_GET,
            PROPERTY_ACCESSOR_PREFIX_IS
    };

    private static final String PROPERTY_MUTATOR_PREFIX = "set";

    /**
     * Returns the JavaBeans property name of the given member.
     *
     * @param member never null
     * @return null if the member is neither a field nor a getter method according to the JavaBeans standard
     */
    public static String getGetterPropertyName(Member member) {
        if (member instanceof Field) {
            return member.getName();
        } else if (member instanceof Method) {
            String methodName = member.getName();
            for (String prefix : PROPERTY_ACCESSOR_PREFIXES) {
                if (methodName.startsWith(prefix)) {
                    return decapitalizePropertyName(methodName.substring(prefix.length()));
                }
            }
        }
        return null;
    }

    private static String decapitalizePropertyName(String propertyName) {
        if (propertyName.isEmpty() || startsWithSeveralUpperCaseLetters(propertyName)) {
            return propertyName;
        } else {
            return propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
        }
    }

    private static boolean startsWithSeveralUpperCaseLetters(String propertyName) {
        return propertyName.length() > 1 &&
                Character.isUpperCase(propertyName.charAt(0)) &&
                Character.isUpperCase(propertyName.charAt(1));
    }

    /**
     * Checks whether the given method is a valid getter method according to the JavaBeans standard.
     *
     * @param method never null
     * @return true if the given method is a getter method
     */
    public static boolean isGetterMethod(Method method) {
        if (method.getParameterTypes().length != 0) {
            return false;
        }
        String methodName = method.getName();
        if (methodName.startsWith(PROPERTY_ACCESSOR_PREFIX_GET) && method.getReturnType() != void.class) {
            return true;
        } else if (methodName.startsWith(PROPERTY_ACCESSOR_PREFIX_IS) && method.getReturnType() == boolean.class) {
            return true;
        }
        return false;
    }

    /**
     * @param containingClass never null
     * @param propertyName never null
     * @return true if that getter exists
     */
    public static boolean hasGetterMethod(Class<?> containingClass, String propertyName) {
        return getGetterMethod(containingClass, propertyName) != null;
    }

    /**
     * @param containingClass never null
     * @param propertyName never null
     * @return sometimes null
     */
    public static Method getGetterMethod(Class<?> containingClass, String propertyName) {
        String capitalizedPropertyName = capitalizePropertyName(propertyName);
        String getterName = PROPERTY_ACCESSOR_PREFIX_GET + capitalizedPropertyName;
        try {
            return containingClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // intentionally empty
        }
        String isserName = PROPERTY_ACCESSOR_PREFIX_IS + capitalizedPropertyName;
        try {

            Method method = containingClass.getMethod(isserName);
            if (method.getReturnType() == boolean.class) {
                return method;
            }
        } catch (NoSuchMethodException e) {
            // intentionally empty
        }
        return null;
    }

    private static String capitalizePropertyName(String propertyName) {
        if (propertyName.isEmpty() || Character.isUpperCase(propertyName.charAt(0))) {
            return propertyName;
        } else {
            return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        }
    }

    /**
     * @param containingClass never null
     * @param fieldName never null
     * @return true if that field exists
     */
    public static boolean hasField(Class<?> containingClass, String fieldName) {
        return getField(containingClass, fieldName) != null;
    }

    /**
     * @param containingClass never null
     * @param fieldName never null
     * @return sometimes null
     */
    public static Field getField(Class<?> containingClass, String fieldName) {
        try {
            return containingClass.getField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * @param containingClass never null
     * @param propertyType never null
     * @param propertyName never null
     * @return null if it doesn't exist
     */
    public static Method getSetterMethod(Class<?> containingClass, Class<?> propertyType, String propertyName) {
        String setterName = PROPERTY_MUTATOR_PREFIX + capitalizePropertyName(propertyName);
        try {
            return containingClass.getMethod(setterName, propertyType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * @param containingClass never null
     * @param propertyName never null
     * @return null if it doesn't exist
     */
    public static Method getSetterMethod(Class<?> containingClass, String propertyName) {
        String setterName = PROPERTY_MUTATOR_PREFIX + capitalizePropertyName(propertyName);
        Method[] methods = Arrays.stream(containingClass.getMethods())
                .filter(method -> method.getName().equals(setterName))
                .toArray(Method[]::new);
        if (methods.length == 0) {
            return null;
        }
        if (methods.length > 1) {
            throw new IllegalStateException("The containingClass (" + containingClass
                    + ") has multiple setter methods (" + Arrays.toString(methods)
                    + ") with the propertyName (" + propertyName + ").");
        }
        return methods[0];
    }

    public static boolean isMethodOverwritten(Method parentMethod, Class<?> childClass) {
        Method leafMethod;
        try {
            leafMethod = childClass.getDeclaredMethod(parentMethod.getName(), parentMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return false;
        }
        return !leafMethod.getDeclaringClass().equals(parentMethod.getClass());
    }

    public static void assertGetterMethod(Method getterMethod) {
        if (getterMethod.getParameterTypes().length != 0) {
            throw new IllegalStateException("The getterMethod (%s) must not have any parameters (%s)."
                    .formatted(getterMethod, Arrays.toString(getterMethod.getParameterTypes())));
        }
        var methodName = getterMethod.getName();
        if (methodName.startsWith(PROPERTY_ACCESSOR_PREFIX_GET)) {
            if (getterMethod.getReturnType() == void.class) {
                throw new IllegalStateException(
                        "The getterMethod (%s) must have a non-void return type (%s)."
                                .formatted(getterMethod, getterMethod.getReturnType()));
            }
        } else if (methodName.startsWith(PROPERTY_ACCESSOR_PREFIX_IS)) {
            if (getterMethod.getReturnType() != boolean.class) {
                throw new IllegalStateException("""
                        The getterMethod (%s) must have a primitive boolean return type (%s) \
                        or use another prefix in its methodName (%s).
                        Maybe use '%s' instead of '%s'?"""
                        .formatted(getterMethod, getterMethod.getReturnType(), methodName, PROPERTY_ACCESSOR_PREFIX_GET,
                                PROPERTY_ACCESSOR_PREFIX_IS));
            }
        } else {
            throw new IllegalStateException(
                    "The getterMethod (%s) has a methodName (%s) that does not start with a valid prefix (%s)."
                            .formatted(getterMethod, methodName, Arrays.toString(PROPERTY_ACCESSOR_PREFIXES)));
        }
    }

    public static void assertGetterMethod(Method getterMethod, Class<? extends Annotation> annotationClass) {
        if (getterMethod.getParameterTypes().length != 0) {
            throw new IllegalStateException("The getterMethod (%s) with a %s annotation must not have any parameters (%s)."
                    .formatted(getterMethod, annotationClass.getSimpleName(),
                            Arrays.toString(getterMethod.getParameterTypes())));
        }
        var methodName = getterMethod.getName();
        if (methodName.startsWith(PROPERTY_ACCESSOR_PREFIX_GET)) {
            if (getterMethod.getReturnType() == void.class) {
                throw new IllegalStateException(
                        "The getterMethod (%s) with a %s annotation must have a non-void return type (%s)."
                                .formatted(getterMethod, annotationClass.getSimpleName(), getterMethod.getReturnType()));
            }
        } else if (methodName.startsWith(PROPERTY_ACCESSOR_PREFIX_IS)) {
            if (getterMethod.getReturnType() != boolean.class) {
                throw new IllegalStateException("""
                        The getterMethod (%s) with a %s annotation must have a primitive boolean return type (%s) \
                        or use another prefix in its methodName (%s).
                        Maybe use '%s' instead of '%s'?"""
                        .formatted(getterMethod, annotationClass.getSimpleName(), getterMethod.getReturnType(), methodName,
                                PROPERTY_ACCESSOR_PREFIX_GET, PROPERTY_ACCESSOR_PREFIX_IS));
            }
        } else {
            throw new IllegalStateException(
                    "The getterMethod (%s) with a %s annotation has a methodName (%s) that does not start with a valid prefix (%s)."
                            .formatted(getterMethod, annotationClass.getSimpleName(), methodName,
                                    Arrays.toString(PROPERTY_ACCESSOR_PREFIXES)));
        }
    }

    public static void assertReadMethod(Method readMethod) {
        if (readMethod.getParameterTypes().length != 0) {
            throw new IllegalStateException("The readMethod (%s) must not have any parameters (%s)."
                    .formatted(readMethod, Arrays.toString(readMethod.getParameterTypes())));
        }
        if (readMethod.getReturnType() == void.class) {
            throw new IllegalStateException("The readMethod (%s) must have a non-void return type (%s)."
                    .formatted(readMethod, readMethod.getReturnType()));
        }
    }

    public static void assertReadMethod(Method readMethod, Class<? extends Annotation> annotationClass) {
        if (readMethod.getParameterTypes().length != 0) {
            throw new IllegalStateException("The readMethod (%s) with a %s annotation must not have any parameters (%s)."
                    .formatted(readMethod, annotationClass.getSimpleName(), Arrays.toString(readMethod.getParameterTypes())));
        }
        if (readMethod.getReturnType() == void.class) {
            throw new IllegalStateException("The readMethod (%s) with a %s annotation must have a non-void return type (%s)."
                    .formatted(readMethod, annotationClass.getSimpleName(), readMethod.getReturnType()));
        }
    }

    @SuppressWarnings("unchecked")
    public static <Value_> List<Value_> transformArrayToList(Object arrayObject) {
        if (arrayObject == null) {
            return null;
        }
        var arrayLength = Array.getLength(arrayObject);
        var list = new ArrayList<Value_>(arrayLength);
        for (var i = 0; i < arrayLength; i++) {
            list.add((Value_) Array.get(arrayObject, i));
        }
        return list;
    }

    private ReflectionHelper() {
    }

}
