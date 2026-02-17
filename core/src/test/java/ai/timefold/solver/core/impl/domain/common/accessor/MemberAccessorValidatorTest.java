package ai.timefold.solver.core.impl.domain.common.accessor;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import ai.timefold.solver.core.api.domain.common.PlanningId;

import org.junit.jupiter.api.Test;

class MemberAccessorValidatorTest {
    @Test
    void includeAnnotationInfoIfPresent() {
        assertThatCode(() -> {
            MemberAccessorValidator.verifyIsValidMember(PlanningId.class,
                    PublicClass.class.getDeclaredField("privateFieldWithoutGetter"),
                    MemberAccessorType.FIELD_OR_GETTER_METHOD);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContainingAll(
                "@%s annotated field".formatted(PlanningId.class.getSimpleName()),
                "privateFieldWithoutGetter");

        assertThatCode(() -> {
            MemberAccessorValidator.verifyIsValidMember(PlanningId.class,
                    PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateGetter"),
                    MemberAccessorType.FIELD_OR_GETTER_METHOD);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContainingAll(
                "@%s annotated method".formatted(PlanningId.class.getSimpleName()),
                "getPrivateFieldWithPrivateGetter");
    }

    @Test
    void membersInPrivateClassFail() throws NoSuchFieldException, NoSuchMethodException {
        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD, PrivateClass.class.getDeclaredField("publicField"),
                "its declaring class (%s) is not public".formatted(PrivateClass.class.getCanonicalName()));
        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PrivateClass.class.getDeclaredField("privateFieldWithGetter"),
                "its declaring class (%s) is not public".formatted(PrivateClass.class.getCanonicalName()));
        assertMethodFails(MemberAccessorType.VOID_METHOD, PrivateClass.class.getDeclaredMethod("publicVoidMethod"),
                "its declaring class (%s) is not public".formatted(PrivateClass.class.getCanonicalName()));
    }

    @Test
    void fieldOrGetterMethod() throws NoSuchFieldException, NoSuchMethodException {
        assertFieldPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD, PublicClass.class.getDeclaredField("publicField"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithGetter"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithGetterAndSetter"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetterAndSetter"));

        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithoutGetter"),
                "is not public and does not have a public getter method");
        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateGetter"),
                "does not have a public getter method");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD, PublicClass.class.getDeclaredMethod("getVoidMethod"),
                "has a public getter method that returns void");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateGetter"),
                "is a getter method, but it is not public");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD,
                PublicClass.class.getDeclaredMethod("publicReadMethod"),
                "is suppose to be a public getter method, but its name (publicReadMethod) does not start with \"get\"");
    }

    @Test
    void fieldOrGetterMethodWithSetter() throws NoSuchFieldException, NoSuchMethodException {
        assertFieldPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredField("publicField"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredField("privateFieldWithGetterAndSetter"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredField("privateFieldWithGetterAndSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetterAndSetter"));

        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredField("privateFieldWithGetter"),
                "does not have a setter method and is not public");
        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredField("privateFieldWithoutGetter"),
                "is not public and does not have a public getter method");
        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateGetter"),
                "does not have a public getter method");
        assertFieldFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateSetter"),
                "does not have a setter method and is not public");

        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateSetter"),
                "requires both a public getter and a public setter but only have a public getter");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetter"),
                "requires both a public getter and a public setter but only have a public getter");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredMethod("getVoidMethod"),
                "has a public getter method that returns void");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredMethod("getVoidMethod"),
                "has a public getter method that returns void");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateGetter"),
                "is a getter method, but it is not public");
        assertMethodFails(MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER,
                PublicClass.class.getDeclaredMethod("publicReadMethod"),
                "is suppose to be a public getter method, but its name (publicReadMethod) does not start with \"get\"");
    }

    @Test
    void fieldOrReadMethod() throws NoSuchFieldException, NoSuchMethodException {
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD, PublicClass.class.getDeclaredField("publicField"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithGetter"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithGetterAndSetter"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetterAndSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredMethod("publicReadMethod"));

        assertFieldFails(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithoutGetter"),
                "is not public and does not have a public getter method");
        assertFieldFails(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateGetter"),
                "does not have a public getter method");
        assertMethodFails(MemberAccessorType.FIELD_OR_READ_METHOD, PublicClass.class.getDeclaredMethod("getVoidMethod"),
                "is a public read method, but it returns void");
        assertMethodFails(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateGetter"),
                "is a read method, but it is not public.");
        assertMethodFails(MemberAccessorType.FIELD_OR_READ_METHOD,
                PublicClass.class.getDeclaredMethod("publicReadMethodWithParameter", String.class),
                "is a public read method, but takes (1) parameters instead of none");
    }

    @Test
    void fieldOrReadMethodWithOptionalParameter() throws NoSuchFieldException, NoSuchMethodException {
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredField("publicField"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredField("privateFieldWithGetter"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredField("privateFieldWithGetterAndSetter"));
        assertFieldPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithGetterAndSetter"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("publicReadMethod"));
        assertMethodPasses(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("publicReadMethodWithParameter", String.class));

        assertFieldFails(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredField("privateFieldWithoutGetter"),
                "is not public and does not have a public getter method");
        assertFieldFails(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredField("privateFieldWithPrivateGetter"),
                "does not have a public getter method");
        assertMethodFails(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("getVoidMethod"),
                "is a public read method, but it returns void");
        assertMethodFails(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("getPrivateFieldWithPrivateGetter"),
                "is a read method, but it is not public.");
        assertMethodFails(MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
                PublicClass.class.getDeclaredMethod("publicReadMethodWithManyParameter", String.class, String.class),
                "is a public read method, but takes (2) parameters instead of zero or one");
    }

    @Test
    void voidMethod() throws NoSuchFieldException, NoSuchMethodException {
        assertMethodPasses(MemberAccessorType.VOID_METHOD, PublicClass.class.getDeclaredMethod("getVoidMethod"));

        assertFieldFails(MemberAccessorType.VOID_METHOD, PublicClass.class.getDeclaredField("publicField"),
                "must be a void method, but is a field instead");
        assertFieldFails(MemberAccessorType.VOID_METHOD,
                PublicClass.class.getDeclaredField("privateFieldWithGetter"),
                "must be a void method, but is a field instead");
        assertMethodFails(MemberAccessorType.VOID_METHOD, PublicClass.class.getDeclaredMethod("publicReadMethod"),
                "must be a void method, but it returns (java.lang.String)");
        assertMethodFails(MemberAccessorType.VOID_METHOD, PublicClass.class.getDeclaredMethod("privateVoidMethod"),
                "is a void method, but it is not public");
    }

    void assertFieldPasses(MemberAccessorType memberAccessorType, Field field) {
        assertThatCode(() -> {
            MemberAccessorValidator.verifyIsValidMember(null, field, memberAccessorType);
        }).doesNotThrowAnyException();
    }

    void assertMethodPasses(MemberAccessorType memberAccessorType, Method method) {
        assertThatCode(() -> {
            MemberAccessorValidator.verifyIsValidMember(null, method, memberAccessorType);
        }).doesNotThrowAnyException();
    }

    void assertFieldFails(MemberAccessorType memberAccessorType, Field field, String... expectedMessages) {
        var prefix = "The field (%s) in class (%s)".formatted(field.getName(), field.getDeclaringClass().getCanonicalName());
        var allMessages = new ArrayList<String>();
        allMessages.add(prefix);
        allMessages.addAll(Arrays.asList(expectedMessages));

        assertThatCode(() -> {
            MemberAccessorValidator.verifyIsValidMember(null, field, memberAccessorType);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(allMessages.toArray(new String[0]));
    }

    void assertMethodFails(MemberAccessorType memberAccessorType, Method method, String... expectedMessages) {
        var prefix = "The method (%s) in class (%s)".formatted(method.getName(), method.getDeclaringClass().getCanonicalName());
        var allMessages = new ArrayList<String>();
        allMessages.add(prefix);
        allMessages.addAll(Arrays.asList(expectedMessages));

        assertThatCode(() -> {
            MemberAccessorValidator.verifyIsValidMember(null, method, memberAccessorType);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(allMessages.toArray(new String[0]));
    }

    public static class PublicClass {
        public String publicField;
        private String privateFieldWithoutGetter;
        private String privateFieldWithGetter;
        private String privateFieldWithGetterAndSetter;
        private String privateFieldWithPrivateGetter;
        private String privateFieldWithPrivateSetter;

        public String getPrivateFieldWithGetter() {
            return privateFieldWithGetter;
        }

        public String getPrivateFieldWithGetterAndSetter() {
            return privateFieldWithGetterAndSetter;
        }

        public void setPrivateFieldWithGetterAndSetter(String privateFieldWithGetterAndSetter) {
            this.privateFieldWithGetterAndSetter = privateFieldWithGetterAndSetter;
        }

        private String getPrivateFieldWithPrivateGetter() {
            return privateFieldWithPrivateGetter;
        }

        public String getPrivateFieldWithPrivateSetter() {
            return privateFieldWithPrivateSetter;
        }

        private void setPrivateFieldWithPrivateSetter(String privateFieldWithPrivateSetter) {
            this.privateFieldWithPrivateSetter = privateFieldWithPrivateSetter;
        }

        public void getVoidMethod() {
            // intentionally empty
        }

        private void privateVoidMethod() {
            // intentionally empty
        }

        public String publicReadMethod() {
            return null;
        }

        private String privateReadMethod() {
            return null;
        }

        public String publicReadMethodWithParameter(String parameter) {
            return parameter;
        }

        public String publicReadMethodWithManyParameter(String parameter1, String parameter2) {
            return parameter1;
        }
    }

    private static class PrivateClass {
        public String publicField;
        private String privateFieldWithGetter;

        public void publicVoidMethod() {
            // Intentionally empty
        }

        public String getPrivateFieldWithGetter() {
            return privateFieldWithGetter;
        }
    }
}