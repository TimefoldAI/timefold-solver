package ai.timefold.jpyinterpreter;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class FunctionMetadata {

    public FunctionMetadata() {

    }

    public PythonFunctionType functionType;
    public MethodDescriptor method;
    public String className;
    public MethodVisitor methodVisitor;
    public PythonCompiledFunction pythonCompiledFunction;
    public Map<Integer, Label> bytecodeCounterToLabelMap;
    public Map<Integer, List<Runnable>> bytecodeCounterToCodeArgumenterList;
}
