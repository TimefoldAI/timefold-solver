package ai.timefold.jpyinterpreter.util.arguments;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.MethodDescriptor;
import ai.timefold.jpyinterpreter.PythonFunctionSignature;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class ArgumentSpec<Out_> {
    private static List<ArgumentSpec<?>> ARGUMENT_SPECS = new ArrayList<>();

    private final String functionReturnTypeName;
    private final String functionName;
    private final List<String> argumentNameList;
    private final List<String> argumentTypeNameList;
    private final List<ArgumentKind> argumentKindList;
    private final List<Object> argumentDefaultList;
    private final BitSet nullableArgumentSet;
    private final Optional<Integer> extraPositionalsArgumentIndex;
    private final Optional<Integer> extraKeywordsArgumentIndex;

    private final int numberOfPositionalArguments;
    private final int requiredPositionalArguments;

    private Class<?> functionReturnType = null;
    private List<Class> argumentTypeList = null;

    private ArgumentSpec(String functionName, String functionReturnTypeName) {
        this.functionReturnTypeName = functionReturnTypeName;
        this.functionName = functionName + "()";
        requiredPositionalArguments = 0;
        numberOfPositionalArguments = 0;
        argumentNameList = Collections.emptyList();
        argumentTypeNameList = Collections.emptyList();
        argumentKindList = Collections.emptyList();
        argumentDefaultList = Collections.emptyList();
        extraPositionalsArgumentIndex = Optional.empty();
        extraKeywordsArgumentIndex = Optional.empty();
        nullableArgumentSet = new BitSet();
    }

    private ArgumentSpec(String argumentName, String argumentTypeName, ArgumentKind argumentKind, Object defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex,
            boolean allowNull, ArgumentSpec<Out_> previousSpec) {
        functionName = previousSpec.functionName;
        functionReturnTypeName = previousSpec.functionReturnTypeName;

        if (previousSpec.numberOfPositionalArguments < previousSpec.getTotalArgumentCount()) {
            numberOfPositionalArguments = previousSpec.numberOfPositionalArguments;
        } else {
            if (argumentKind.allowPositional) {
                numberOfPositionalArguments = previousSpec.getTotalArgumentCount() + 1;
            } else {
                numberOfPositionalArguments = previousSpec.getTotalArgumentCount();
            }
        }

        if (argumentKind == ArgumentKind.POSITIONAL_ONLY) {
            if (previousSpec.requiredPositionalArguments != previousSpec.getTotalArgumentCount()) {
                throw new IllegalArgumentException("All required positional arguments must come before all other arguments");
            } else {
                requiredPositionalArguments = previousSpec.getTotalArgumentCount() + 1;
            }
        } else {
            requiredPositionalArguments = previousSpec.requiredPositionalArguments;
        }

        argumentNameList = new ArrayList<>(previousSpec.argumentNameList.size() + 1);
        argumentTypeNameList = new ArrayList<>(previousSpec.argumentTypeNameList.size() + 1);
        argumentKindList = new ArrayList<>(previousSpec.argumentKindList.size() + 1);
        argumentDefaultList = new ArrayList<>(previousSpec.argumentDefaultList.size() + 1);

        argumentNameList.addAll(previousSpec.argumentNameList);
        argumentNameList.add(argumentName);

        argumentTypeNameList.addAll(previousSpec.argumentTypeNameList);
        argumentTypeNameList.add(argumentTypeName);

        argumentKindList.addAll(previousSpec.argumentKindList);
        argumentKindList.add(argumentKind);

        argumentDefaultList.addAll(previousSpec.argumentDefaultList);
        argumentDefaultList.add(defaultValue);

        if (extraPositionalsArgumentIndex.isPresent() && previousSpec.extraPositionalsArgumentIndex.isPresent()) {
            throw new IllegalArgumentException("Multiple positional vararg arguments");
        }
        if (previousSpec.extraPositionalsArgumentIndex.isPresent()) {
            extraPositionalsArgumentIndex = previousSpec.extraPositionalsArgumentIndex;
        }

        if (extraKeywordsArgumentIndex.isPresent() && previousSpec.extraKeywordsArgumentIndex.isPresent()) {
            throw new IllegalArgumentException("Multiple keyword vararg arguments");
        }
        if (previousSpec.extraKeywordsArgumentIndex.isPresent()) {
            extraKeywordsArgumentIndex = previousSpec.extraKeywordsArgumentIndex;
        }

        this.extraPositionalsArgumentIndex = extraPositionalsArgumentIndex;
        this.extraKeywordsArgumentIndex = extraKeywordsArgumentIndex;
        this.nullableArgumentSet = (BitSet) previousSpec.nullableArgumentSet.clone();
        if (allowNull) {
            nullableArgumentSet.set(argumentNameList.size() - 1);
        }
    }

    public static <T extends PythonLikeObject> ArgumentSpec<T> forFunctionReturning(String functionName,
            String outClass) {
        return new ArgumentSpec<>(functionName, outClass);
    }

    public int getTotalArgumentCount() {
        return argumentNameList.size();
    }

    public int getAllowPositionalArgumentCount() {
        return numberOfPositionalArguments;
    }

    public boolean hasExtraPositionalArgumentsCapture() {
        return extraPositionalsArgumentIndex.isPresent();
    }

    public boolean hasExtraKeywordArgumentsCapture() {
        return extraKeywordsArgumentIndex.isPresent();
    }

    public String getArgumentTypeInternalName(int argumentIndex) {
        return argumentTypeNameList.get(argumentIndex).replace('.', '/');
    }

    public ArgumentKind getArgumentKind(int argumentIndex) {
        return argumentKindList.get(argumentIndex);
    }

    /**
     * Returns the index of an argument with the given name. Returns -1 if no argument has the given name.
     *
     * @param argumentName The name of the argument.
     * @return the index of an argument with the given name, or -1 if no argument has that name
     */
    public int getArgumentIndex(String argumentName) {
        return argumentNameList.indexOf(argumentName);
    }

    public boolean isArgumentNullable(int argumentIndex) {
        return nullableArgumentSet.get(argumentIndex);
    }

    public Optional<Integer> getExtraPositionalsArgumentIndex() {
        return extraPositionalsArgumentIndex;
    }

    public Optional<Integer> getExtraKeywordsArgumentIndex() {
        return extraKeywordsArgumentIndex;
    }

    public Collection<Integer> getUnspecifiedArgumentSet(int positionalArguments, List<String> keywordArgumentNameList) {
        int specArgumentCount = getTotalArgumentCount();
        if (hasExtraPositionalArgumentsCapture()) {
            specArgumentCount--;
        }
        if (hasExtraKeywordArgumentsCapture()) {
            specArgumentCount--;
        }

        return IntStream.range(positionalArguments, specArgumentCount)
                .filter(index -> !keywordArgumentNameList.contains(argumentNameList.get(index)))
                .boxed()
                .collect(Collectors.toList());
    }

    public static ArgumentSpec<?> getArgumentSpec(int argumentSpecIndex) {
        return ARGUMENT_SPECS.get(argumentSpecIndex);
    }

    public void loadArgumentSpec(MethodVisitor methodVisitor) {
        int index = ARGUMENT_SPECS.size();
        ARGUMENT_SPECS.add(this);
        methodVisitor.visitLdcInsn(index);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ArgumentSpec.class),
                "getArgumentSpec", Type.getMethodDescriptor(Type.getType(ArgumentSpec.class),
                        Type.INT_TYPE),
                false);
    }

    private void computeArgumentTypeList() {
        if (argumentTypeList == null) {
            try {
                functionReturnType = BuiltinTypes.asmClassLoader.loadClass(functionReturnTypeName.replace('/', '.'));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            argumentTypeList = argumentTypeNameList.stream()
                    .map(className -> {
                        try {
                            return (Class) BuiltinTypes.asmClassLoader.loadClass(className.replace('/', '.'));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }
    }

    public List<PythonLikeObject> extractArgumentList(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> keywordArguments) {
        computeArgumentTypeList();

        List<PythonLikeObject> out = new ArrayList<>(argumentNameList.size());

        if (positionalArguments.size() > numberOfPositionalArguments &&
                extraPositionalsArgumentIndex.isEmpty()) {
            throw new TypeError(functionName + " takes " + numberOfPositionalArguments + " positional arguments but "
                    + positionalArguments.size() + " were given");
        }

        if (positionalArguments.size() < requiredPositionalArguments) {
            int missing = (requiredPositionalArguments - positionalArguments.size());
            String argumentString = (missing == 1) ? "argument" : "arguments";
            List<String> missingArgumentNames = argumentNameList.subList(argumentNameList.size() - missing,
                    argumentNameList.size());
            throw new TypeError(functionName + " missing " + (requiredPositionalArguments - positionalArguments.size()) +
                    " required positional " + argumentString + ": '" + String.join("', ", missingArgumentNames) + "'");
        }

        int numberOfSetArguments = Math.min(numberOfPositionalArguments, positionalArguments.size());
        out.addAll(positionalArguments.subList(0, numberOfSetArguments));
        for (int i = numberOfSetArguments; i < argumentNameList.size(); i++) {
            out.add(null);
        }

        int remaining = argumentNameList.size() - numberOfSetArguments;

        PythonLikeDict extraKeywordArguments = null;
        if (extraPositionalsArgumentIndex.isPresent()) {
            remaining--;
            out.set(extraPositionalsArgumentIndex.get(),
                    PythonLikeTuple
                            .fromList(positionalArguments.subList(numberOfSetArguments, positionalArguments.size())));
        }

        if (extraKeywordsArgumentIndex.isPresent()) {
            remaining--;
            extraKeywordArguments = new PythonLikeDict();
            out.set(extraKeywordsArgumentIndex.get(),
                    extraKeywordArguments);
        }

        for (Map.Entry<PythonString, PythonLikeObject> keywordArgument : keywordArguments.entrySet()) {
            PythonString argumentName = keywordArgument.getKey();

            int position = argumentNameList.indexOf(argumentName.value);
            if (position == -1) {
                if (extraKeywordsArgumentIndex.isPresent()) {
                    extraKeywordArguments.put(argumentName, keywordArgument.getValue());
                    continue;
                } else {
                    throw new TypeError(functionName + " got an unexpected keyword argument " + argumentName.repr().value);
                }
            }

            if (out.get(position) != null) {
                throw new TypeError(functionName + " got multiple values for argument " + argumentName.repr().value);
            }

            if (!argumentKindList.get(position).allowKeyword) {
                throw new TypeError(functionName + " got some positional-only arguments passed as keyword arguments: "
                        + argumentName.repr().value);
            }

            remaining--;
            out.set(position, keywordArgument.getValue());
        }

        if (remaining > 0) {
            List<Integer> missing = new ArrayList<>(remaining);
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i) == null) {
                    if (argumentDefaultList.get(i) != null || nullableArgumentSet.get(i)) {
                        out.set(i, (PythonLikeObject) argumentDefaultList.get(i));
                        remaining--;
                    } else {
                        missing.add(i);
                    }
                }
            }

            if (remaining > 0) {
                if (missing.stream().anyMatch(index -> argumentKindList.get(index).allowPositional)) {
                    List<String> missingAllowsPositional = new ArrayList<>(remaining);
                    for (int index : missing) {
                        if (argumentKindList.get(index).allowPositional) {
                            missingAllowsPositional.add(argumentNameList.get(index));
                        }
                    }
                    String argumentString = (missingAllowsPositional.size() == 1) ? "argument" : "arguments";
                    throw new TypeError(functionName + " missing " + remaining + " required positional " + argumentString
                            + ": '" + String.join("', ", missingAllowsPositional) + "'");
                } else {
                    List<String> missingKeywordOnly = new ArrayList<>(remaining);
                    for (int index : missing) {
                        missingKeywordOnly.add(argumentNameList.get(index));
                    }
                    String argumentString = (missingKeywordOnly.size() == 1) ? "argument" : "arguments";
                    throw new TypeError(functionName + " missing " + remaining + " required keyword-only " + argumentString
                            + ": '" + String.join("', ", missingKeywordOnly) + "'");
                }
            }
        }

        for (int i = 0; i < argumentNameList.size(); i++) {
            if ((out.get(i) == null && !nullableArgumentSet.get(i))
                    || (out.get(i) != null && !argumentTypeList.get(i).isInstance(out.get(i)))) {
                throw new TypeError(functionName + "'s argument '" + argumentNameList.get(i) + "' has incorrect type: " +
                        "'" + argumentNameList.get(i) + "' must be a " +
                        JavaPythonTypeConversionImplementor.getPythonLikeType(argumentTypeList.get(i)) +
                        " (got "
                        + ((out.get(i) != null) ? JavaPythonTypeConversionImplementor.getPythonLikeType(out.get(i).getClass())
                                : "NULL")
                        + " instead)");
            }
        }
        return out;
    }

    public boolean verifyMatchesCallSignature(int positionalArgumentCount, List<String> keywordArgumentNameList,
            List<PythonLikeType> callStackTypeList) {
        computeArgumentTypeList();

        Set<Integer> missingValue = getRequiredArgumentIndexSet();
        for (int keywordIndex = 0; keywordIndex < keywordArgumentNameList.size(); keywordIndex++) {
            String keyword = keywordArgumentNameList.get(keywordIndex);
            PythonLikeType stackType = callStackTypeList.get(positionalArgumentCount + keywordIndex);
            int index = argumentNameList.indexOf(keyword);
            if (index == -1 && extraKeywordsArgumentIndex.isEmpty()) {
                return false;
            }
            if (index != -1 && index < positionalArgumentCount) {
                return false;
            } else {
                try {
                    if (!argumentTypeList.get(index).isAssignableFrom(stackType.getJavaClass())) {
                        return false;
                    }
                } catch (ClassNotFoundException e) {
                    // Assume if the type is not found, it assignable
                }
                missingValue.remove(index);
            }
        }

        if (positionalArgumentCount < requiredPositionalArguments || positionalArgumentCount > getTotalArgumentCount()) {
            return false;
        }

        for (int i = 0; i < positionalArgumentCount; i++) {
            missingValue.remove(i);
            try {
                if (!argumentTypeList.get(i).isAssignableFrom(callStackTypeList.get(i).getJavaClass())) {
                    return false;
                }
            } catch (ClassNotFoundException e) {
                // Assume if the type is not found, it assignable
            }
        }

        if (!missingValue.isEmpty()) {
            return false;
        }

        if (extraPositionalsArgumentIndex.isEmpty() && extraKeywordsArgumentIndex.isEmpty()) { // no *vargs or **kwargs
            return positionalArgumentCount <= numberOfPositionalArguments &&
                    positionalArgumentCount + keywordArgumentNameList.size() <= argumentNameList.size();
        } else if (extraPositionalsArgumentIndex.isPresent() && extraKeywordsArgumentIndex.isEmpty()) { // *vargs only
            return true;
        } else if (extraPositionalsArgumentIndex.isEmpty()) { // **kwargs only
            return positionalArgumentCount < numberOfPositionalArguments;
        } else { // *vargs and **kwargs
            return true;
        }
    }

    private Set<Integer> getRequiredArgumentIndexSet() {
        Set<Integer> out = new HashSet<>();
        for (int i = 0; i < argumentNameList.size(); i++) {
            if (argumentKindList.get(i) == ArgumentKind.VARARGS) {
                continue;
            }
            if (argumentDefaultList.get(i) != null || nullableArgumentSet.get(i)) {
                continue;
            }
            out.add(i);
        }
        return out;
    }

    private <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_> addArgument(String argumentName,
            String argumentTypeName, ArgumentKind argumentKind, ArgumentType_ defaultValue,
            Optional<Integer> extraPositionalsArgumentIndex, Optional<Integer> extraKeywordsArgumentIndex, boolean allowNull) {
        return new ArgumentSpec<>(argumentName, argumentTypeName, argumentKind, defaultValue,
                extraPositionalsArgumentIndex, extraKeywordsArgumentIndex, allowNull, this);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_> addArgument(String argumentName,
            String argumentTypeName) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.POSITIONAL_AND_KEYWORD, null,
                Optional.empty(), Optional.empty(), false);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_>
            addPositionalOnlyArgument(String argumentName, String argumentTypeName) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.POSITIONAL_ONLY, null,
                Optional.empty(), Optional.empty(), false);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_>
            addKeywordOnlyArgument(String argumentName, String argumentTypeName) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.KEYWORD_ONLY, null,
                Optional.empty(), Optional.empty(), false);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_> addArgument(String argumentName,
            String argumentTypeName, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.POSITIONAL_AND_KEYWORD, defaultValue,
                Optional.empty(), Optional.empty(), false);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_>
            addPositionalOnlyArgument(String argumentName, String argumentTypeName, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.POSITIONAL_ONLY, defaultValue,
                Optional.empty(), Optional.empty(), false);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_>
            addKeywordOnlyArgument(String argumentName, String argumentTypeName, ArgumentType_ defaultValue) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.KEYWORD_ONLY, defaultValue,
                Optional.empty(), Optional.empty(), false);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_> addNullableArgument(String argumentName,
            String argumentTypeName) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.POSITIONAL_AND_KEYWORD, null,
                Optional.empty(), Optional.empty(), true);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_> addNullablePositionalOnlyArgument(String argumentName,
            String argumentTypeName) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.POSITIONAL_ONLY, null,
                Optional.empty(), Optional.empty(), true);
    }

    public <ArgumentType_ extends PythonLikeObject> ArgumentSpec<Out_> addNullableKeywordOnlyArgument(String argumentName,
            String argumentTypeName) {
        return addArgument(argumentName, argumentTypeName, ArgumentKind.KEYWORD_ONLY, null,
                Optional.empty(), Optional.empty(), true);
    }

    public ArgumentSpec<Out_> addExtraPositionalVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeTuple.class.getName(), ArgumentKind.VARARGS, null,
                Optional.of(getTotalArgumentCount()), Optional.empty(), false);
    }

    public ArgumentSpec<Out_> addExtraKeywordVarArgument(String argumentName) {
        return addArgument(argumentName, PythonLikeDict.class.getName(), ArgumentKind.VARARGS, null,
                Optional.empty(), Optional.of(getTotalArgumentCount()), false);
    }

    public PythonFunctionSignature asPythonFunctionSignature(Method method) {
        verifyMethodMatchesSpec(method);
        return getPythonFunctionSignatureForMethodDescriptor(new MethodDescriptor(method),
                method.getReturnType());
    }

    public PythonFunctionSignature asStaticPythonFunctionSignature(Method method) {
        verifyMethodMatchesSpec(method);
        return getPythonFunctionSignatureForMethodDescriptor(new MethodDescriptor(method, MethodDescriptor.MethodType.STATIC),
                method.getReturnType());
    }

    public PythonFunctionSignature asClassPythonFunctionSignature(Method method) {
        verifyMethodMatchesSpec(method);
        return getPythonFunctionSignatureForMethodDescriptor(new MethodDescriptor(method, MethodDescriptor.MethodType.CLASS),
                method.getReturnType());
    }

    public PythonFunctionSignature asPythonFunctionSignature(String internalClassName, String methodName,
            String methodDescriptor) {
        MethodDescriptor method = new MethodDescriptor(internalClassName, MethodDescriptor.MethodType.VIRTUAL,
                methodName, methodDescriptor);
        try {
            return getPythonFunctionSignatureForMethodDescriptor(method,
                    BuiltinTypes.asmClassLoader.loadClass(
                            method.getReturnType().getClassName().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public PythonFunctionSignature asStaticPythonFunctionSignature(String internalClassName, String methodName,
            String methodDescriptor) {
        MethodDescriptor method = new MethodDescriptor(internalClassName, MethodDescriptor.MethodType.STATIC,
                methodName, methodDescriptor);
        try {
            return getPythonFunctionSignatureForMethodDescriptor(method,
                    BuiltinTypes.asmClassLoader.loadClass(
                            method.getReturnType().getClassName().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public PythonFunctionSignature asClassPythonFunctionSignature(String internalClassName, String methodName,
            String methodDescriptor) {
        MethodDescriptor method = new MethodDescriptor(internalClassName, MethodDescriptor.MethodType.CLASS,
                methodName, methodDescriptor);
        try {
            return getPythonFunctionSignatureForMethodDescriptor(method,
                    BuiltinTypes.asmClassLoader.loadClass(
                            method.getReturnType().getClassName().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyMethodMatchesSpec(Method method) {
        computeArgumentTypeList();

        if (!functionReturnType.isAssignableFrom(method.getReturnType())) {
            throw new IllegalArgumentException("Method (" + method + ") does not match the given spec (" + this +
                    "): its return type (" + method.getReturnType() + ") is not " +
                    "assignable to the spec return type (" + functionReturnTypeName + ").");
        }

        if (method.getParameterCount() != argumentNameList.size()) {
            throw new IllegalArgumentException("Method (" + method + ") does not match the given spec (" + this +
                    "): they have different parameter counts.");
        }

        for (int i = 0; i < method.getParameterCount(); i++) {
            if (!method.getParameterTypes()[i].isAssignableFrom(argumentTypeList.get(i))) {
                throw new IllegalArgumentException("Method (" + method + ") does not match the given spec (" + this +
                        "): its " + i + " parameter (" + method.getParameters()[i].toString() + ") cannot " +
                        " be assigned from the spec " + i + " parameter (" + argumentTypeList.get(i) + " "
                        + argumentNameList.get(i) + ").");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private PythonFunctionSignature getPythonFunctionSignatureForMethodDescriptor(MethodDescriptor methodDescriptor,
            Class<?> javaReturnType) {
        computeArgumentTypeList();

        int firstDefault = 0;

        while (firstDefault < argumentDefaultList.size() && argumentDefaultList.get(firstDefault) == null &&
                !nullableArgumentSet.get(firstDefault)) {
            firstDefault++;
        }
        List<PythonLikeObject> defaultParameterValueList;

        if (firstDefault != argumentDefaultList.size()) {
            defaultParameterValueList = (List<PythonLikeObject>) (List<?>) argumentDefaultList.subList(firstDefault,
                    argumentDefaultList.size());
        } else {
            defaultParameterValueList = Collections.emptyList();
        }

        List<PythonLikeType> parameterTypeList = argumentTypeList.stream()
                .map(JavaPythonTypeConversionImplementor::getPythonLikeType)
                .collect(Collectors.toList());

        PythonLikeType returnType = JavaPythonTypeConversionImplementor.getPythonLikeType(javaReturnType);
        Map<String, Integer> keywordArgumentToIndexMap = new HashMap<>();

        for (int i = 0; i < argumentNameList.size(); i++) {
            if (argumentKindList.get(i).allowKeyword) {
                keywordArgumentToIndexMap.put(argumentNameList.get(i), i);
            }
        }

        return new PythonFunctionSignature(methodDescriptor, defaultParameterValueList,
                keywordArgumentToIndexMap, returnType,
                parameterTypeList, extraPositionalsArgumentIndex, extraKeywordsArgumentIndex,
                this);
    }

    public Object getDefaultValue(int defaultIndex) {
        return argumentDefaultList.get(defaultIndex);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("ArgumentSpec(");
        out.append("name=").append(functionName)
                .append(", returnType=").append(functionReturnTypeName)
                .append(", arguments=[");

        for (int i = 0; i < argumentNameList.size(); i++) {
            out.append(argumentTypeNameList.get(i));
            out.append(" ");
            out.append(argumentNameList.get(i));

            if (nullableArgumentSet.get(i)) {
                out.append(" (nullable)");
            }

            if (argumentDefaultList.get(i) != null) {
                out.append(" (default: ");
                out.append(argumentDefaultList.get(i));
                out.append(")");
            }

            if (argumentKindList.get(i) != ArgumentKind.POSITIONAL_AND_KEYWORD) {
                if (extraPositionalsArgumentIndex.isPresent() && extraPositionalsArgumentIndex.get() == i) {
                    out.append(" (vargs)");
                } else if (extraKeywordsArgumentIndex.isPresent() && extraKeywordsArgumentIndex.get() == i) {
                    out.append(" (kwargs)");
                } else {
                    out.append(" (");
                    out.append(argumentKindList.get(i));
                    out.append(")");
                }
            }
            if (i != argumentNameList.size() - 1) {
                out.append(", ");
            }
        }
        out.append("])");

        return out.toString();
    }
}
