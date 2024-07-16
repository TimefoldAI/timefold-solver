package ai.timefold.jpyinterpreter.implementors;

import java.util.Optional;

import ai.timefold.jpyinterpreter.FieldDescriptor;
import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.LocalVariableHelper;
import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.PythonSuperObject;
import ai.timefold.jpyinterpreter.types.errors.AttributeError;
import ai.timefold.jpyinterpreter.types.wrappers.JavaObjectWrapper;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implementations of opcodes related to objects
 */
public class ObjectImplementor {

    /**
     * Replaces TOS with getattr(TOS, co_names[instruction.arg])
     */
    public static void getAttribute(FunctionMetadata functionMetadata, StackMetadata stackMetadata, int nameIndex) {
        var methodVisitor = functionMetadata.methodVisitor;
        var className = functionMetadata.className;
        PythonLikeType tosType = stackMetadata.getTOSType();
        String name = functionMetadata.pythonCompiledFunction.co_names.get(nameIndex);
        Optional<FieldDescriptor> maybeFieldDescriptor = tosType.getInstanceFieldDescriptor(name);

        if (maybeFieldDescriptor.isPresent()) {
            FieldDescriptor fieldDescriptor = maybeFieldDescriptor.get();
            if (fieldDescriptor.isTrueFieldDescriptor()) {
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.declaringClassInternalName());
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, fieldDescriptor.declaringClassInternalName(),
                        fieldDescriptor.javaFieldName(),
                        fieldDescriptor.javaFieldTypeDescriptor());

                // Check if field is null. If it is null, then it was deleted, so we should raise an AttributeError
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);

                Label ifNotNull = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifNotNull);

                // Throw attribute error
                methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(AttributeError.class));
                methodVisitor.visitInsn(Opcodes.DUP);

                if (fieldDescriptor.fieldPythonLikeType().isInstance(PythonNone.INSTANCE)) {
                    methodVisitor.visitLdcInsn("'" + tosType.getTypeName() + "' object has no attribute '" + name + "'.");
                } else {
                    // None cannot be assigned to the field, meaning it will delete the attribute instead
                    methodVisitor.visitLdcInsn("'" + tosType.getTypeName() + "' object has no attribute '" + name + "'. " +
                            "It might of been deleted because None cannot be assigned to it; either use " +
                            "hasattr(obj, '" + name + "') or change the typing to allow None (ex: typing.Optional[" +
                            tosType.getTypeName() + "]).");
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(AttributeError.class),
                        "<init>",
                        Type.getMethodDescriptor(Type.getType(void.class), Type.getType(String.class)),
                        false);
                methodVisitor.visitInsn(Opcodes.ATHROW);

                // The attribute was not null
                if (fieldDescriptor.isJavaType()) {
                    // Need to wrap the object with JavaObjectWrapper
                    methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(JavaObjectWrapper.class));
                    methodVisitor.visitInsn(Opcodes.DUP_X1);
                    methodVisitor.visitInsn(Opcodes.DUP_X1);
                    methodVisitor.visitInsn(Opcodes.POP);
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(JavaObjectWrapper.class),
                            "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class)), false);
                }
                methodVisitor.visitLabel(ifNotNull);
            } else {
                // It a false field descriptor, which means TOS is a type and this is a field for a method
                // We can call $method$__getattribute__ directly (since type do not override it),
                // which is more efficient then going through the full logic of __getattribute__ dunder method impl.
                PythonConstantsImplementor.loadName(methodVisitor, className, nameIndex);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonString.class));
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(PythonLikeObject.class),
                        "$method$__getattribute__", Type.getMethodDescriptor(
                                Type.getType(PythonLikeObject.class),
                                Type.getType(PythonString.class)),
                        true);
            }
        } else {
            PythonConstantsImplementor.loadName(methodVisitor, className, nameIndex);
            DunderOperatorImplementor.binaryOperator(methodVisitor,
                    stackMetadata.pushTemp(BuiltinTypes.STRING_TYPE),
                    PythonBinaryOperator.GET_ATTRIBUTE);
        }
    }

    /**
     * Deletes co_names[instruction.arg] of TOS
     */
    public static void deleteAttribute(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction) {
        PythonLikeType tosType = stackMetadata.getTOSType();
        String name = functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg());
        Optional<FieldDescriptor> maybeFieldDescriptor = tosType.getInstanceFieldDescriptor(name);
        if (maybeFieldDescriptor.isPresent()) {
            FieldDescriptor fieldDescriptor = maybeFieldDescriptor.get();
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.declaringClassInternalName());
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, fieldDescriptor.declaringClassInternalName(),
                    fieldDescriptor.javaFieldName(),
                    fieldDescriptor.javaFieldTypeDescriptor());
        } else {
            PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg());
            DunderOperatorImplementor.binaryOperator(methodVisitor,
                    stackMetadata.pushTemp(BuiltinTypes.STRING_TYPE),
                    PythonBinaryOperator.DELETE_ATTRIBUTE);
        }
    }

    /**
     * Implement TOS.name = TOS1, where name is co_names[instruction.arg]. TOS and TOS1 are popped.
     */
    public static void setAttribute(FunctionMetadata functionMetadata, MethodVisitor methodVisitor, String className,
            StackMetadata stackMetadata,
            PythonBytecodeInstruction instruction, LocalVariableHelper localVariableHelper) {
        PythonLikeType tosType = stackMetadata.getTOSType();
        String name = functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg());
        Optional<FieldDescriptor> maybeFieldDescriptor = tosType.getInstanceFieldDescriptor(name);
        if (maybeFieldDescriptor.isPresent()) {
            FieldDescriptor fieldDescriptor = maybeFieldDescriptor.get();
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.declaringClassInternalName());
            StackManipulationImplementor.swap(methodVisitor);
            methodVisitor.visitLdcInsn(Type.getType(fieldDescriptor.fieldPythonLikeType().getJavaTypeDescriptor()));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                    "coerceToType", Type.getMethodDescriptor(Type.getType(Object.class),
                            Type.getType(PythonLikeObject.class),
                            Type.getType(Class.class)),
                    false);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, fieldDescriptor.fieldPythonLikeType().getJavaTypeInternalName());
            if (fieldDescriptor.isJavaType()) {
                // Need to unwrap the object
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(JavaObjectWrapper.class));
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(JavaObjectWrapper.class),
                        "getWrappedObject", Type.getMethodDescriptor(Type.getType(Object.class)), false);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                        Type.getType(fieldDescriptor.javaFieldTypeDescriptor()).getInternalName());
            }
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, fieldDescriptor.declaringClassInternalName(),
                    fieldDescriptor.javaFieldName(),
                    fieldDescriptor.javaFieldTypeDescriptor());
        } else {
            StackManipulationImplementor.swap(methodVisitor);
            PythonConstantsImplementor.loadName(methodVisitor, className, instruction.arg());
            StackManipulationImplementor.swap(methodVisitor);
            DunderOperatorImplementor.ternaryOperator(functionMetadata, stackMetadata.pop(2)
                    .push(stackMetadata.getValueSourceForStackIndex(0))
                    .pushTemp(BuiltinTypes.STRING_TYPE)
                    .push(stackMetadata.getValueSourceForStackIndex(1)),
                    PythonTernaryOperator.SET_ATTRIBUTE);
        }
    }

    /**
     * Implement (super = TOS2)(TOS1, TOS).attr
     */
    public static void getSuperAttribute(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata,
            int nameIndex,
            boolean isLoadMethod) {
        var methodVisitor = functionMetadata.methodVisitor;
        // Stack: super, type, instance
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(PythonSuperObject.class));
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        methodVisitor.visitInsn(Opcodes.POP);
        // Stack: super, <uninit superobject>, <uninit superobject>, type, instance
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonLikeType.class));
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(PythonSuperObject.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(PythonLikeType.class),
                        Type.getType(PythonLikeObject.class)));
        // Stack: super, superobject
        ObjectImplementor.getAttribute(functionMetadata, stackMetadata.pop(2).pushTemp(BuiltinTypes.SUPER_TYPE), nameIndex);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitInsn(Opcodes.POP);
        if (isLoadMethod) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitInsn(Opcodes.SWAP);
        }
    }
}
