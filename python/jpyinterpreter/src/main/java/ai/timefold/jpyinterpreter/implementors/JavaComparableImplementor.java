package ai.timefold.jpyinterpreter.implementors;

import java.lang.reflect.Modifier;

import ai.timefold.jpyinterpreter.CompareOp;
import ai.timefold.jpyinterpreter.PythonClassTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledClass;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.NotImplementedError;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class JavaComparableImplementor extends JavaInterfaceImplementor {
    final String internalClassName;
    final CompareOp compareOp;

    public JavaComparableImplementor(String internalClassName, String method) {
        this.internalClassName = internalClassName;
        this.compareOp = CompareOp.getOpForDunderMethod(method);
        switch (compareOp) {
            case LESS_THAN:
            case LESS_THAN_OR_EQUALS:
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUALS:
                break;
            default:
                throw new IllegalStateException("Cannot use " + method + " for comparisons");
        }
    }

    @Override
    public Class<?> getInterfaceClass() {
        return Comparable.class;
    }

    private void typeCheck(MethodVisitor methodVisitor) {
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, internalClassName);

        Label isInstanceOfClass = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IFNE, isInstanceOfClass);

        // Throw an exception since the argument is not a Python Like Object
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(NotImplementedError.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitLdcInsn("compareTo arg 0 is not an instance of " + internalClassName);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(NotImplementedError.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                false);
        methodVisitor.visitInsn(Opcodes.ATHROW);

        methodVisitor.visitLabel(isInstanceOfClass);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, internalClassName);
    }

    @Override
    public void implement(ClassWriter classWriter, PythonCompiledClass compiledClass) {
        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "compareTo",
                Type.getMethodDescriptor(Type.INT_TYPE,
                        Type.getType(Object.class)),
                null,
                null);

        methodVisitor.visitParameter("other", 0);
        methodVisitor.visitCode();

        switch (compareOp) {
            case LESS_THAN:
                implementCompareToWithLessThan(methodVisitor, compiledClass);
                break;
            case LESS_THAN_OR_EQUALS:
                implementCompareToWithLessThanOrEqual(methodVisitor, compiledClass);
                break;
            case GREATER_THAN:
                implementCompareToWithGreaterThan(methodVisitor, compiledClass);
                break;
            case GREATER_THAN_OR_EQUALS:
                implementCompareToWithGreaterThanOrEqual(methodVisitor, compiledClass);
                break;
            default:
                throw new IllegalStateException("Impossible state: " + compareOp + " is not a comparison operator");
        }

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    // The following code exploit these fact:
    // a < b == a < b
    // a > b == b < a
    // !(a >= b) == a < b
    // !(a <= b) == a > b == b < a
    private void implementCompareToWithLessThan(MethodVisitor methodVisitor, PythonCompiledClass pythonCompiledClass) {
        PythonCompiledFunction comparisonFunction = pythonCompiledClass.instanceFunctionNameToPythonBytecode.get("__lt__");
        String comparisonMethodName = PythonClassTranslator.getJavaMethodName("__lt__");

        implementCompareTo(methodVisitor, comparisonFunction, comparisonMethodName, false, true);
    }

    private void implementCompareToWithGreaterThan(MethodVisitor methodVisitor, PythonCompiledClass pythonCompiledClass) {
        PythonCompiledFunction comparisonFunction = pythonCompiledClass.instanceFunctionNameToPythonBytecode.get("__gt__");
        String comparisonMethodName = PythonClassTranslator.getJavaMethodName("__gt__");

        implementCompareTo(methodVisitor, comparisonFunction, comparisonMethodName, false, false);
    }

    private void implementCompareToWithLessThanOrEqual(MethodVisitor methodVisitor, PythonCompiledClass pythonCompiledClass) {
        PythonCompiledFunction comparisonFunction = pythonCompiledClass.instanceFunctionNameToPythonBytecode.get("__le__");
        String comparisonMethodName = PythonClassTranslator.getJavaMethodName("__le__");

        implementCompareTo(methodVisitor, comparisonFunction, comparisonMethodName, true, false);
    }

    private void implementCompareToWithGreaterThanOrEqual(MethodVisitor methodVisitor,
            PythonCompiledClass pythonCompiledClass) {
        PythonCompiledFunction comparisonFunction = pythonCompiledClass.instanceFunctionNameToPythonBytecode.get("__ge__");
        String comparisonMethodName = PythonClassTranslator.getJavaMethodName("__ge__");

        implementCompareTo(methodVisitor, comparisonFunction, comparisonMethodName, true, true);
    }

    /**
     * Create this code:
     *
     * <pre>
     * if (self < other) == not negateComparisionResult:
     *     return isLessThan? -1 : 1
     * elif (other < self) == True:
     *     return isLessThan? 1 : -1
     * else:
     *     return 0
     * </pre>
     *
     * The negateComparisonResult turns __ge__ and __le__ into __lt__ and __gt__ respectively.
     * isLessThan reverses the comparison if false.
     *
     */
    private void implementCompareTo(MethodVisitor methodVisitor, PythonCompiledFunction comparisonFunction,
            String comparisonMethodName, boolean negateComparisionResult,
            boolean isLessThan) {
        PythonLikeType parameterType = comparisonFunction.getParameterTypes().get(1);
        Type returnType = PythonClassTranslator.getVirtualFunctionReturnType(comparisonFunction);

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        typeCheck(methodVisitor);

        methodVisitor.visitInsn(Opcodes.DUP2);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, comparisonMethodName,
                Type.getMethodDescriptor(returnType, Type.getType(parameterType.getJavaTypeDescriptor())),
                false);

        Label ifSelfNotLessThanOther = new Label();

        if (negateComparisionResult) {
            PythonConstantsImplementor.loadFalse(methodVisitor);
        } else {
            PythonConstantsImplementor.loadTrue(methodVisitor);
        }

        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifSelfNotLessThanOther);

        methodVisitor.visitInsn(isLessThan ? Opcodes.ICONST_M1 : Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.IRETURN);

        methodVisitor.visitLabel(ifSelfNotLessThanOther);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, comparisonMethodName,
                Type.getMethodDescriptor(returnType, Type.getType(parameterType.getJavaTypeDescriptor())),
                false);

        Label ifOtherNotLessThanSelf = new Label();
        if (negateComparisionResult) {
            PythonConstantsImplementor.loadFalse(methodVisitor);
        } else {
            PythonConstantsImplementor.loadTrue(methodVisitor);
        }
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifOtherNotLessThanSelf);

        methodVisitor.visitInsn(isLessThan ? Opcodes.ICONST_1 : Opcodes.ICONST_M1);
        methodVisitor.visitInsn(Opcodes.IRETURN);

        methodVisitor.visitLabel(ifOtherNotLessThanSelf);
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitInsn(Opcodes.IRETURN);
    }
}
