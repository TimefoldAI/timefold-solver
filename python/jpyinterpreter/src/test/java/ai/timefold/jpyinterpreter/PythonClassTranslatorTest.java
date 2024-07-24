package ai.timefold.jpyinterpreter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import ai.timefold.jpyinterpreter.opcodes.descriptor.ControlOpDescriptor;
import ai.timefold.jpyinterpreter.opcodes.descriptor.DunderOpDescriptor;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.PythonFunctionBuilder;

import org.junit.jupiter.api.Test;

public class PythonClassTranslatorTest {

    @Test
    public void testPythonClassTranslation() throws ClassNotFoundException, NoSuchMethodException {
        PythonCompiledClass compiledClass = new PythonCompiledClass();

        PythonCompiledFunction initFunction = PythonFunctionBuilder.newFunction("self", "age")
                .loadParameter("age")
                .loadParameter("self")
                .storeAttribute("age")
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction ageFunction = PythonFunctionBuilder.newFunction("self")
                .loadParameter("self")
                .getAttribute("age")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction helloWorldFunction = PythonFunctionBuilder.newFunction()
                .loadConstant("hello world")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        compiledClass.annotations = Collections.emptyList();
        compiledClass.javaInterfaces = Collections.emptyList();
        compiledClass.pythonJavaTypeMappings = List.of();
        compiledClass.className = "MyClass";
        compiledClass.superclassList = List.of(BuiltinTypes.BASE_TYPE);
        compiledClass.staticAttributeNameToObject = Map.of("type_variable", new PythonString("type_value"));
        compiledClass.staticAttributeNameToClassInstance = Map.of();
        compiledClass.staticAttributeDescriptorNames = Set.of();
        compiledClass.typeAnnotations = Map.of("age", TypeHint.withoutAnnotations(BuiltinTypes.INT_TYPE));
        compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                "get_age", ageFunction);
        compiledClass.staticFunctionNameToPythonBytecode = Map.of("hello_world", helloWorldFunction);
        compiledClass.classFunctionNameToPythonBytecode = Map.of();

        PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
        Class<?> generatedClass = BuiltinTypes.asmClassLoader.loadClass(
                classType.getJavaTypeInternalName().replace('/', '.'));

        assertThat(generatedClass).hasPublicFields(PythonClassTranslator.getJavaMethodHolderName("get_age"),
                PythonClassTranslator.getJavaFieldName("age"));
        assertThat(generatedClass).hasPublicMethods(
                PythonClassTranslator.getJavaMethodName("__init__"),
                PythonClassTranslator.getJavaMethodName("get_age"));
        assertThat(generatedClass.getMethod(PythonClassTranslator.getJavaMethodName("get_age")).getParameterTypes()).isEmpty();

        PythonLikeObject classObject = classType.$call(List.of(PythonInteger.valueOf(10)), Map.of(), null);
        PythonLikeFunction getAgeFunction =
                (PythonLikeFunction) classObject.$method$__getattribute__(PythonString.valueOf("get_age"));
        assertThat(getAgeFunction.$call(List.of(), Map.of(), null)).isEqualTo(PythonInteger.valueOf(10));
    }

    @Test
    public void testPythonClassComparable() throws ClassNotFoundException {
        PythonCompiledFunction initFunction = PythonFunctionBuilder.newFunction("self", "key")
                .loadParameter("key")
                .loadParameter("self")
                .storeAttribute("key")
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        Function<CompareOp, PythonCompiledFunction> getCompareFunction =
                compareOp -> PythonFunctionBuilder.newFunction("self", "other")
                        .loadParameter("self")
                        .getAttribute("key")
                        .loadParameter("other")
                        .getAttribute("key")
                        .compare(compareOp)
                        .op(ControlOpDescriptor.RETURN_VALUE)
                        .build();

        for (CompareOp compareOp : List.of(CompareOp.LESS_THAN, CompareOp.GREATER_THAN,
                CompareOp.LESS_THAN_OR_EQUALS, CompareOp.GREATER_THAN_OR_EQUALS)) {
            PythonCompiledFunction comparisonFunction = getCompareFunction.apply(compareOp);

            PythonCompiledClass compiledClass = new PythonCompiledClass();
            compiledClass.annotations = Collections.emptyList();
            compiledClass.javaInterfaces = Collections.emptyList();
            compiledClass.pythonJavaTypeMappings = List.of();
            compiledClass.className = "MyClass";
            compiledClass.superclassList = List.of(BuiltinTypes.BASE_TYPE);
            compiledClass.staticAttributeNameToObject = Map.of();
            compiledClass.staticAttributeNameToClassInstance = Map.of();
            compiledClass.staticAttributeDescriptorNames = Set.of();
            compiledClass.typeAnnotations = Map.of("key", TypeHint.withoutAnnotations(BuiltinTypes.INT_TYPE));
            compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                    compareOp.dunderMethod, comparisonFunction);
            compiledClass.staticFunctionNameToPythonBytecode = Map.of();
            compiledClass.classFunctionNameToPythonBytecode = Map.of();

            PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
            Class<?> generatedClass = BuiltinTypes.asmClassLoader.loadClass(
                    classType.getJavaTypeInternalName().replace('/', '.'));

            assertThat(Comparable.class.isAssignableFrom(generatedClass)).isTrue();
            assertThat(generatedClass).hasPublicFields(PythonClassTranslator.getJavaFieldName("key"));
            assertThat(generatedClass).hasPublicMethods(
                    PythonClassTranslator.getJavaMethodName("__init__"),
                    "compareTo");

            Comparable<Object> object1 =
                    (Comparable<Object>) classType.$call(List.of(PythonInteger.valueOf(1)), Map.of(), null);
            Comparable<Object> object2 =
                    (Comparable<Object>) classType.$call(List.of(PythonInteger.valueOf(2)), Map.of(), null);
            Comparable<Object> object1b =
                    (Comparable<Object>) classType.$call(List.of(PythonInteger.valueOf(1)), Map.of(), null);

            assertThat(object1.compareTo(object2))
                    .withFailMessage(compareOp.name() + " a < b incorrect")
                    .isLessThan(0);
            assertThat(object2.compareTo(object1))
                    .withFailMessage(compareOp.name() + " a > b incorrect")
                    .isGreaterThan(0);
            assertThat(object1.compareTo(object1))
                    .withFailMessage(compareOp.name() + " a == a incorrect")
                    .isEqualTo(0);
            assertThat(object1.compareTo(object1b))
                    .withFailMessage(compareOp.name() + " a == b incorrect")
                    .isEqualTo(0);
        }
    }

    @Test
    public void testPythonClassEqualsAndHashCode() throws ClassNotFoundException {
        PythonCompiledFunction initFunction = PythonFunctionBuilder.newFunction("self", "key")
                .loadParameter("key")
                .loadParameter("self")
                .storeAttribute("key")
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction equalsFunction = PythonFunctionBuilder.newFunction("self", "other")
                .loadParameter("self")
                .getAttribute("key")
                .loadParameter("other")
                .getAttribute("key")
                .compare(CompareOp.EQUALS)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction hashFunction = PythonFunctionBuilder.newFunction("self")
                .loadParameter("self")
                .getAttribute("key")
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledClass compiledClass = new PythonCompiledClass();
        compiledClass.annotations = Collections.emptyList();
        compiledClass.javaInterfaces = Collections.emptyList();
        compiledClass.pythonJavaTypeMappings = List.of();
        compiledClass.className = "MyClass";
        compiledClass.superclassList = List.of(BuiltinTypes.BASE_TYPE);
        compiledClass.staticAttributeNameToObject = Map.of();
        compiledClass.staticAttributeNameToClassInstance = Map.of();
        compiledClass.staticAttributeDescriptorNames = Set.of();
        compiledClass.typeAnnotations = Map.of("key", TypeHint.withoutAnnotations(BuiltinTypes.INT_TYPE));
        compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                "__eq__", equalsFunction,
                "__hash__", hashFunction);
        compiledClass.staticFunctionNameToPythonBytecode = Map.of();
        compiledClass.classFunctionNameToPythonBytecode = Map.of();

        PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
        Class<?> generatedClass = BuiltinTypes.asmClassLoader.loadClass(
                classType.getJavaTypeInternalName().replace('/', '.'));

        assertThat(generatedClass).hasPublicFields(PythonClassTranslator.getJavaFieldName("key"));
        assertThat(generatedClass).hasPublicMethods(
                PythonClassTranslator.getJavaMethodName("__init__"),
                "equals");

        Object object1a = classType.$call(List.of(PythonInteger.valueOf(1)), Map.of(), null);
        Object object1b = classType.$call(List.of(PythonInteger.valueOf(1)), Map.of(), null);
        Object object2 = classType.$call(List.of(PythonInteger.valueOf(2)), Map.of(), null);
        Object object3 = classType.$call(List.of(PythonInteger.valueOf(Long.MAX_VALUE)), Map.of(), null);

        assertThat(object1a.equals(object2))
                .isFalse();
        assertThat(object2.equals(object1a))
                .isFalse();
        assertThat(object1a.equals(object1b))
                .isTrue();
        assertThat(object1b.equals(object1a))
                .isTrue();
        assertThat(object1a.equals(object1a))
                .isTrue();

        assertThat(object1a.hashCode())
                .isEqualTo(PythonInteger.valueOf(1).hashCode());
        assertThat(object1b.hashCode())
                .isEqualTo(PythonInteger.valueOf(1).hashCode());
        assertThat(object2.hashCode())
                .isEqualTo(PythonInteger.valueOf(2).hashCode());
        assertThat(object3.hashCode())
                .isEqualTo(PythonInteger.valueOf(Long.MAX_VALUE).hashCode());
    }

    @Test
    public void testPythonClassSimpleInterface() throws ClassNotFoundException {
        PythonCompiledFunction initFunction = PythonFunctionBuilder.newFunction("self", "value")
                .loadParameter("value")
                .loadParameter("self")
                .storeAttribute("value")
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction applyAsInt = PythonFunctionBuilder.newFunction("self", "value")
                .loadParameter("self")
                .getAttribute("value")
                .loadParameter("value")
                .op(DunderOpDescriptor.BINARY_ADD)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledClass compiledClass = new PythonCompiledClass();
        compiledClass.annotations = Collections.emptyList();
        compiledClass.javaInterfaces = List.of(ToIntFunction.class);
        compiledClass.pythonJavaTypeMappings = List.of();
        compiledClass.className = "MyClass";
        compiledClass.superclassList = List.of(BuiltinTypes.BASE_TYPE);
        compiledClass.staticAttributeNameToObject = Map.of();
        compiledClass.staticAttributeNameToClassInstance = Map.of();
        compiledClass.staticAttributeDescriptorNames = Set.of();
        compiledClass.typeAnnotations = Map.of("key", TypeHint.withoutAnnotations(BuiltinTypes.INT_TYPE));
        compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                "applyAsInt", applyAsInt);
        compiledClass.staticFunctionNameToPythonBytecode = Map.of();
        compiledClass.classFunctionNameToPythonBytecode = Map.of();

        PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
        Class<?> generatedClass = BuiltinTypes.asmClassLoader.loadClass(
                classType.getJavaTypeInternalName().replace('/', '.'));

        assertThat(generatedClass).hasPublicFields(PythonClassTranslator.getJavaFieldName("value"));
        assertThat(generatedClass).hasPublicMethods(
                PythonClassTranslator.getJavaMethodName("__init__"),
                "applyAsInt");
        assertThat(generatedClass).isAssignableTo(ToIntFunction.class);

        var object1 = (ToIntFunction<PythonInteger>) classType.$call(List.of(PythonInteger.valueOf(1)), Map.of(), null);
        var object2 = (ToIntFunction<PythonInteger>) classType.$call(List.of(PythonInteger.valueOf(2)), Map.of(), null);
        var object3 = (ToIntFunction<PythonInteger>) classType.$call(List.of(PythonInteger.valueOf(3)), Map.of(), null);

        assertThat(object1.applyAsInt(PythonInteger.valueOf(1))).isEqualTo(2);
        assertThat(object2.applyAsInt(PythonInteger.valueOf(1))).isEqualTo(3);
        assertThat(object3.applyAsInt(PythonInteger.valueOf(1))).isEqualTo(4);
    }

    public interface ComplexInterface {
        int STATIC_FIELD = 10;

        static int staticMethod() {
            return STATIC_FIELD;
        }

        default void defaultMethod() {
        }

        int overloadedMethod();

        int overloadedMethod(int value);
    }

    @Test
    public void testPythonClassComplexInterface() throws ClassNotFoundException {
        PythonCompiledFunction initFunction = PythonFunctionBuilder.newFunction("self")
                .loadConstant(null)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        PythonCompiledFunction overloadedMethod = PythonFunctionBuilder.newFunction("self", "value")
                .loadParameter("value")
                .loadConstant(1)
                .op(DunderOpDescriptor.BINARY_ADD)
                .op(ControlOpDescriptor.RETURN_VALUE)
                .build();

        overloadedMethod.defaultPositionalArguments = PythonLikeTuple.fromItems(PythonInteger.ZERO);

        PythonCompiledClass compiledClass = new PythonCompiledClass();
        compiledClass.annotations = Collections.emptyList();
        compiledClass.javaInterfaces = List.of(ComplexInterface.class);
        compiledClass.pythonJavaTypeMappings = List.of();
        compiledClass.className = "MyClass";
        compiledClass.superclassList = List.of(BuiltinTypes.BASE_TYPE);
        compiledClass.staticAttributeNameToObject = Map.of();
        compiledClass.staticAttributeNameToClassInstance = Map.of();
        compiledClass.staticAttributeDescriptorNames = Set.of();
        compiledClass.typeAnnotations = Map.of("key", TypeHint.withoutAnnotations(BuiltinTypes.INT_TYPE));
        compiledClass.instanceFunctionNameToPythonBytecode = Map.of("__init__", initFunction,
                "overloadedMethod", overloadedMethod);
        compiledClass.staticFunctionNameToPythonBytecode = Map.of();
        compiledClass.classFunctionNameToPythonBytecode = Map.of();

        PythonLikeType classType = PythonClassTranslator.translatePythonClass(compiledClass);
        Class<?> generatedClass = BuiltinTypes.asmClassLoader.loadClass(
                classType.getJavaTypeInternalName().replace('/', '.'));

        assertThat(generatedClass).hasPublicMethods(
                PythonClassTranslator.getJavaMethodName("__init__"),
                "overloadedMethod");
        assertThat(generatedClass).isAssignableTo(ComplexInterface.class);

        var instance = (ComplexInterface) classType.$call(List.of(), Map.of(), null);

        assertThat(instance.overloadedMethod()).isEqualTo(1);
        assertThat(instance.overloadedMethod(1)).isEqualTo(2);
    }
}
