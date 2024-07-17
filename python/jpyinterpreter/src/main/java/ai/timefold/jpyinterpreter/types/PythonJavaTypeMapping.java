package ai.timefold.jpyinterpreter.types;

public interface PythonJavaTypeMapping<PythonType_, JavaType_> {
    PythonLikeType getPythonType();

    Class<? extends JavaType_> getJavaType();

    PythonType_ toPythonObject(JavaType_ javaObject);

    JavaType_ toJavaObject(PythonType_ pythonObject);
}
