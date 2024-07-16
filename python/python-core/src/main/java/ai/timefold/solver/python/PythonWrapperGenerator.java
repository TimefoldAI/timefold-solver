package ai.timefold.solver.python;

import static ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator.writeClassOutput;
import static ai.timefold.jpyinterpreter.types.BuiltinTypes.asmClassLoader;
import static ai.timefold.jpyinterpreter.types.BuiltinTypes.classNameToBytecode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class PythonWrapperGenerator {
    @SuppressWarnings("unused")
    public static ClassLoader getClassLoaderForAliasMap(Map<String, Class<?>> aliasMap) {
        return new ClassLoader() {
            // getName() is an abstract method in Java 11 but not in Java 8
            public String getName() {
                return "Timefold Alias Map ClassLoader";
            }

            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                if (aliasMap.containsKey(name)) {
                    // Gizmo generated class
                    return aliasMap.get(name);
                } else {
                    // Not a Gizmo generated class; load from parent class loader
                    return asmClassLoader.loadClass(name);
                }
            }
        };
    }

    private static ClassOutput getClassOutput(AtomicReference<byte[]> bytesReference) {
        return (path, byteCode) -> {
            bytesReference.set(byteCode);
        };
    }

    /**
     * Creates a class that looks like this:
     *
     * class JavaWrapper implements NaryFunction&lt;A0,A1,A2,...,AN&gt; {
     * public static NaryFunction&lt;A0,A1,A2,...,AN&gt; delegate;
     *
     * #64;Override
     * public AN apply(A0 arg0, A1 arg1, ..., A(N-1) finalArg) {
     * return delegate.apply(arg0,arg1,...,finalArg);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param baseInterface the base interface
     * @param delegate The Python function to delegate to
     * @return never null
     */
    @SuppressWarnings({ "unused", "unchecked" })
    public static <A> Class<? extends A> defineWrapperFunction(String className, Class<A> baseInterface,
            Object delegate) {
        Method[] interfaceMethods = baseInterface.getMethods();
        if (interfaceMethods.length != 1) {
            throw new IllegalArgumentException("Can only call this function for functional interfaces (only 1 method)");
        }
        if (classNameToBytecode.containsKey(className)) {
            try {
                return (Class<? extends A>) asmClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = getClassOutput(classBytecodeHolder);

        // holds the delegate (static; same one is reused; should be stateless)
        FieldDescriptor delegateField;
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(baseInterface)
                .classOutput(classOutput)
                .build()) {
            delegateField = classCreator.getFieldCreator("delegate", baseInterface)
                    .setModifiers(Modifier.STATIC | Modifier.PUBLIC)
                    .getFieldDescriptor();
            MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(interfaceMethods[0]));

            ResultHandle pythonProxy = methodCreator.readStaticField(delegateField);
            ResultHandle[] args = new ResultHandle[interfaceMethods[0].getParameterCount()];
            for (int i = 0; i < args.length; i++) {
                args[i] = methodCreator.getMethodParam(i);
            }
            ResultHandle constraints = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(interfaceMethods[0]),
                    pythonProxy, args);
            methodCreator.returnValue(constraints);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        writeClassOutput(classNameToBytecode, className, classBytecodeHolder.get());
        try {
            // Now that the class created, we need to set it static field to the delegate function
            Class<? extends A> out = (Class<? extends A>) asmClassLoader.loadClass(className);
            out.getField(delegateField.getName()).set(null, delegate);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    /**
     * Creates a class that looks like this:
     *
     * class JavaWrapper implements SomeInterface {
     * public static Supplier&lt;SomeInterface&gt; supplier;
     *
     * private SomeInterface delegate;
     *
     * public JavaWrapper() {
     * delegate = supplier.get(); classNameToBytecode.put(className, classBytecodeHolder.get());
     * }
     *
     * #64;Override
     * public Result interfaceMethod1(A0 arg0, A1 arg1, ..., A(N-1) finalArg) {
     * return delegate.interfaceMethod1(arg0,arg1,...,finalArg);
     * }
     *
     * #64;Override
     * public Result interfaceMethod2(A0 arg0, A1 arg1, ..., A(N-1) finalArg) {
     * return delegate.interfaceMethod2(arg0,arg1,...,finalArg);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param baseInterface the base interface
     * @param delegateSupplier The Python class to delegate to
     * @return never null
     */
    @SuppressWarnings({ "unused", "unchecked" })
    public static <A> Class<? extends A> defineWrapperClass(String className, Class<? extends A> baseInterface,
            Supplier<? extends A> delegateSupplier) {
        Method[] interfaceMethods = baseInterface.getMethods();
        if (classNameToBytecode.containsKey(className)) {
            try {
                return (Class<? extends A>) asmClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Impossible State: the class (" + className + ") should exists since it was created");
            }
        }
        AtomicReference<byte[]> classBytecodeHolder = new AtomicReference<>();
        ClassOutput classOutput = getClassOutput(classBytecodeHolder);

        // holds the supplier of the delegate (static)
        FieldDescriptor supplierField;

        // holds the delegate (instance; new one created for each instance)
        FieldDescriptor delegateField;
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(baseInterface)
                .classOutput(classOutput)
                .build()) {
            supplierField = classCreator.getFieldCreator("delegateSupplier", Supplier.class)
                    .setModifiers(Modifier.STATIC | Modifier.PUBLIC)
                    .getFieldDescriptor();
            delegateField = classCreator.getFieldCreator("delegate", baseInterface)
                    .setModifiers(Modifier.PUBLIC | Modifier.FINAL)
                    .getFieldDescriptor();

            MethodCreator constructorCreator =
                    classCreator.getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName()));
            constructorCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), constructorCreator.getThis());
            constructorCreator.writeInstanceField(delegateField, constructorCreator.getThis(),
                    constructorCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Supplier.class, "get", Object.class),
                            constructorCreator.readStaticField(supplierField)));
            constructorCreator.returnValue(constructorCreator.getThis());

            for (Method method : interfaceMethods) {
                MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.ofMethod(method));
                ResultHandle pythonProxy = methodCreator.readInstanceField(delegateField, methodCreator.getThis());
                ResultHandle[] args = new ResultHandle[method.getParameterCount()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = methodCreator.getMethodParam(i);
                }
                ResultHandle result = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(method),
                        pythonProxy, args);
                methodCreator.returnValue(result);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        writeClassOutput(classNameToBytecode, className, classBytecodeHolder.get());
        try {
            // Now that the class created, we need to set it static field to the supplier of the delegate
            Class<? extends A> out = (Class<? extends A>) asmClassLoader.loadClass(className);
            out.getField(supplierField.getName()).set(null, delegateSupplier);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossible State: the class (" + className + ") should exists since it was just created");
        }
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonConstraintProvider implements ConstraintProvider {
     * public static Function&lt;ConstraintFactory, Constraint[]&gt; defineConstraintsImpl;
     *
     * &#64;Override
     * public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
     * return defineConstraintsImpl.apply(constraintFactory);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param defineConstraintsImpl The Python function that return the list of constraints
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineConstraintProviderClass(String className,
            ConstraintProvider defineConstraintsImpl) {
        return defineWrapperFunction(className, ConstraintProvider.class, defineConstraintsImpl);
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonEasyScoreCalculator implements EasyScoreCalculator {
     * public static EasyScoreCalculator easyScoreCalculatorImpl;
     *
     * &#64;Override
     * public Score calculateScore(Solution solution) {
     * return easyScoreCalculatorImpl.calculateScore(solution);
     * }
     * }
     *
     * @param className The simple name of the generated class
     * @param easyScoreCalculatorImpl The Python function that return the score for the solution
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineEasyScoreCalculatorClass(String className,
            EasyScoreCalculator easyScoreCalculatorImpl) {
        return defineWrapperFunction(className, EasyScoreCalculator.class, easyScoreCalculatorImpl);
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonIncrementalScoreCalculator implements IncrementalScoreCalculator {
     * public static Supplier&lt;IncrementalScoreCalculator&gt; supplier;
     * public final IncrementalScoreCalculator delegate;
     *
     * public PythonIncrementalScoreCalculator() {
     * delegate = supplier.get();
     * }
     *
     * &#64;Override
     * public Score calculateScore(Solution solution) {
     * return delegate.calculateScore(solution);
     * }
     *
     * ...
     * }
     *
     * @param className The simple name of the generated class
     * @param incrementalScoreCalculatorSupplier A supplier that returns a new instance of the incremental score calculator on
     *        each call
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineIncrementalScoreCalculatorClass(String className,
            Supplier<? extends IncrementalScoreCalculator> incrementalScoreCalculatorSupplier,
            boolean constraintMatchAware) {
        if (constraintMatchAware) {
            return defineWrapperClass(className, ConstraintMatchAwareIncrementalScoreCalculator.class,
                    (Supplier<ConstraintMatchAwareIncrementalScoreCalculator>) incrementalScoreCalculatorSupplier);
        }
        return defineWrapperClass(className, IncrementalScoreCalculator.class, incrementalScoreCalculatorSupplier);
    }

    /**
     * Creates a class that looks like this:
     *
     * class PythonVariableListener implements VariableListener {
     * public static Supplier&lt;VariableListener&gt; supplier;
     * public final VariableListener delegate;
     *
     * public PythonVariableListener() {
     * delegate = supplier.get();
     * }
     *
     * public void afterVariableChange(scoreDirector, entity) {
     * delegate.afterVariableChange(scoreDirector, entity);
     * }
     * ...
     * }
     *
     * @param className The simple name of the generated class
     * @param variableListenerSupplier A supplier that returns a new instance of the variable listener on
     *        each call
     * @return never null
     */
    @SuppressWarnings("unused")
    public static Class<?> defineVariableListenerClass(String className,
            Supplier<? extends VariableListener> variableListenerSupplier) {
        return defineWrapperClass(className, VariableListener.class, variableListenerSupplier);
    }
}
