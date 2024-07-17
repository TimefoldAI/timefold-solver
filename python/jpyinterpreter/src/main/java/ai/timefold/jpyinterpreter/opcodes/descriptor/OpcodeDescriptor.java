package ai.timefold.jpyinterpreter.opcodes.descriptor;

public sealed interface OpcodeDescriptor permits AsyncOpDescriptor,
        CollectionOpDescriptor,
        ControlOpDescriptor,
        DunderOpDescriptor,
        ExceptionOpDescriptor,
        FunctionOpDescriptor,
        GeneratorOpDescriptor,
        MetaOpDescriptor,
        ModuleOpDescriptor,
        ObjectOpDescriptor,
        StackOpDescriptor,
        StringOpDescriptor,
        VariableOpDescriptor {
    String name();

    VersionMapping getVersionMapping();
}
