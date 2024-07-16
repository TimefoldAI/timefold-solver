package ai.timefold.jpyinterpreter.types;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.builtins.FunctionBuiltinOperations;
import ai.timefold.jpyinterpreter.builtins.GlobalBuiltins;
import ai.timefold.jpyinterpreter.types.collections.PythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeFrozenSet;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeSet;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.collections.view.DictItemView;
import ai.timefold.jpyinterpreter.types.collections.view.DictKeyView;
import ai.timefold.jpyinterpreter.types.collections.view.DictValueView;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonComplex;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.numeric.PythonNumber;
import ai.timefold.jpyinterpreter.types.wrappers.JavaMethodReference;

public class BuiltinTypes {
    public static final PythonLikeType NULL_TYPE =
            new PythonLikeType("NULL", PythonLikeObject.class, Collections.emptyList());
    public static final PythonLikeType BASE_TYPE =
            new PythonLikeType("base-object", PythonLikeObject.class, Collections.emptyList());
    public static final PythonLikeType TYPE_TYPE = new PythonLikeType("type", PythonLikeType.class, List.of(BASE_TYPE));
    public static final PythonLikeType SUPER_TYPE =
            new PythonLikeType("super", PythonSuperObject.class, List.of(BASE_TYPE));
    public static final PythonLikeType MODULE_TYPE = new PythonLikeType("module", PythonModule.class, List.of(BASE_TYPE));
    public static final PythonLikeType FUNCTION_TYPE =
            new PythonLikeType("function", PythonLikeFunction.class, List.of(BASE_TYPE));
    public static final PythonLikeType CLASS_FUNCTION_TYPE =
            new PythonLikeType("classmethod", PythonLikeFunction.class, List.of(BASE_TYPE));
    public static final PythonLikeType STATIC_FUNCTION_TYPE =
            new PythonLikeType("staticmethod", PythonLikeFunction.class, List.of(BASE_TYPE));
    public static final PythonLikeType METHOD_TYPE =
            new PythonLikeType("method", PythonLikeFunction.class, List.of(BASE_TYPE));
    public static final PythonLikeType GENERATOR_TYPE =
            new PythonLikeType("generator", PythonGenerator.class, List.of(BASE_TYPE));
    public static final PythonLikeType CODE_TYPE = new PythonLikeType("code", PythonCode.class, List.of(BASE_TYPE));
    public static final PythonLikeType CELL_TYPE = new PythonLikeType("cell", PythonCell.class, List.of(BASE_TYPE));

    public static final PythonLikeType NONE_TYPE = new PythonLikeType("NoneType", PythonNone.class, List.of(BASE_TYPE));
    public static final PythonLikeType NOT_IMPLEMENTED_TYPE =
            new PythonLikeType("NotImplementedType", NotImplemented.class, List.of(BASE_TYPE));
    public static final PythonLikeType ELLIPSIS_TYPE = new PythonLikeType("EllipsisType", Ellipsis.class, List.of(BASE_TYPE));

    public static final PythonLikeType NUMBER_TYPE = new PythonLikeType("number", PythonNumber.class, List.of(BASE_TYPE));
    public static final PythonLikeType INT_TYPE = new PythonLikeType("int", PythonInteger.class, List.of(NUMBER_TYPE));
    public static final PythonLikeType BOOLEAN_TYPE = new PythonLikeType("bool", PythonBoolean.class, List.of(INT_TYPE));
    public static final PythonLikeType FLOAT_TYPE = new PythonLikeType("float", PythonFloat.class, List.of(NUMBER_TYPE));
    public final static PythonLikeType COMPLEX_TYPE = new PythonLikeType("complex", PythonComplex.class, List.of(NUMBER_TYPE));
    public final static PythonLikeType DECIMAL_TYPE = new PythonLikeType("Decimal", PythonDecimal.class, List.of(NUMBER_TYPE));

    public static final PythonLikeType STRING_TYPE = new PythonLikeType("str", PythonString.class, List.of(BASE_TYPE));
    public static final PythonLikeType BYTES_TYPE = new PythonLikeType("bytes", PythonBytes.class, List.of(BASE_TYPE));
    public static final PythonLikeType BYTE_ARRAY_TYPE =
            new PythonLikeType("bytearray", PythonByteArray.class, List.of(BASE_TYPE));

    public static final PythonLikeType ITERATOR_TYPE = new PythonLikeType("iterator", PythonIterator.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_TYPE = new PythonLikeType("dict", PythonLikeDict.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_ITEM_VIEW_TYPE =
            new PythonLikeType("dict_items", DictItemView.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_KEY_VIEW_TYPE =
            new PythonLikeType("dict_keys", DictKeyView.class, List.of(BASE_TYPE));
    public static final PythonLikeType DICT_VALUE_VIEW_TYPE =
            new PythonLikeType("dict_values", DictValueView.class, List.of(BASE_TYPE));
    public static final PythonLikeType SET_TYPE = new PythonLikeType("set", PythonLikeSet.class, List.of(BASE_TYPE));
    public static final PythonLikeType FROZEN_SET_TYPE =
            new PythonLikeType("frozenset", PythonLikeFrozenSet.class, List.of(BASE_TYPE));
    public static final PythonLikeType TUPLE_TYPE = new PythonLikeType("tuple", PythonLikeTuple.class, List.of(BASE_TYPE));
    public static final PythonLikeType LIST_TYPE = new PythonLikeType("list", PythonLikeList.class, List.of(BASE_TYPE));
    public static final PythonLikeType RANGE_TYPE = new PythonLikeType("range", PythonRange.class, List.of(BASE_TYPE));
    public static final PythonLikeType SLICE_TYPE = new PythonLikeType("slice", PythonSlice.class, List.of(BASE_TYPE));
    /**
     * The ASM generated bytecode. Used by
     * asmClassLoader to create the Java versions of Python methods
     */
    public static final Map<String, byte[]> classNameToBytecode = new HashMap<>();
    /**
     * A custom classloader that looks for the class in
     * classNameToBytecode
     */
    public static ClassLoader asmClassLoader = new ClassLoader() {
        // getName() is an abstract method in Java 11 but not in Java 8
        public String getName() {
            return "JPyInterpreter Python Bytecode ClassLoader";
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            if (classNameToBytecode.containsKey(name)) {
                // Gizmo generated class
                byte[] byteCode = classNameToBytecode.get(name);
                return defineClass(name, byteCode, 0, byteCode.length);
            } else {
                // Not a Gizmo generated class; load from parent class loader
                return PythonBytecodeToJavaBytecodeTranslator.class.getClassLoader().loadClass(name);
            }
        }
    };

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeType::registerBaseType);
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeType::registerTypeType);

        try {
            FUNCTION_TYPE.__dir__.put(PythonTernaryOperator.GET.dunderMethod,
                    new JavaMethodReference(
                            FunctionBuiltinOperations.class.getMethod("bindFunctionToInstance", PythonLikeFunction.class,
                                    PythonLikeObject.class, PythonLikeType.class),
                            Map.of("self", 0, "obj", 1, "objtype", 2)));
            CLASS_FUNCTION_TYPE.__dir__.put(PythonTernaryOperator.GET.dunderMethod,
                    new JavaMethodReference(
                            FunctionBuiltinOperations.class.getMethod("bindFunctionToType", PythonLikeFunction.class,
                                    PythonLikeObject.class, PythonLikeType.class),
                            Map.of("self", 0, "obj", 1, "objtype", 2)));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        for (Field field : BuiltinTypes.class.getFields()) {
            try {
                if (!field.getType().equals(PythonLikeType.class)) {
                    continue;
                }

                PythonLikeType pythonLikeType = (PythonLikeType) field.get(null);
                Class<?> javaClass = pythonLikeType.getJavaClass();
                Class.forName(javaClass.getName(), true, javaClass.getClassLoader());
            } catch (IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        GlobalBuiltins.loadBuiltinConstants();
    }

    public static void load() {
    }

    // Class should not be initated; only holds static constants
    private BuiltinTypes() {
    }
}
