package ai.timefold.jpyinterpreter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.wrappers.CPythonType;
import ai.timefold.jpyinterpreter.types.wrappers.OpaquePythonReference;
import ai.timefold.jpyinterpreter.util.JavaIdentifierUtils;

public class PythonCompiledClass {
    /**
     * The module where the class was defined.
     */
    public String module;

    /**
     * The path to the file that defines the module.
     */
    public String moduleFilePath;

    /**
     * The qualified name of the class. Does not include module.
     */
    public String qualifiedName;

    public String className;

    /**
     * The annotations on the type
     */
    public List<AnnotationMetadata> annotations;

    /**
     * Type annotations for fields
     */
    public Map<String, TypeHint> typeAnnotations;

    /**
     * Java interfaces the class implement
     */
    public List<Class<?>> javaInterfaces;

    /**
     * Mapping from Python types to Java types
     */
    public List<PythonJavaTypeMapping<?, ?>> pythonJavaTypeMappings;

    /**
     * The binary type of this PythonCompiledClass;
     * typically {@link CPythonType}. Used when methods
     * cannot be generated.
     */
    public PythonLikeType binaryType;
    public List<PythonLikeType> superclassList;
    public Map<String, PythonCompiledFunction> instanceFunctionNameToPythonBytecode;
    public Map<String, PythonCompiledFunction> staticFunctionNameToPythonBytecode;
    public Map<String, PythonCompiledFunction> classFunctionNameToPythonBytecode;

    /**
     * Contains static attributes that are not instances of this class
     */
    public Map<String, PythonLikeObject> staticAttributeNameToObject;

    /**
     * Contains static attributes that are instances of this class
     */
    public Map<String, OpaquePythonReference> staticAttributeNameToClassInstance;

    /**
     * Contains static attributes that have get/set descriptors
     */
    public Set<String> staticAttributeDescriptorNames;

    public String getGeneratedClassBaseName() {
        return getGeneratedClassBaseName(module, qualifiedName);
    }

    public static String getGeneratedClassBaseName(String module, String qualifiedName) {
        if (module == null || module.isEmpty()) {
            return JavaIdentifierUtils.sanitizeClassName((qualifiedName != null) ? qualifiedName : "PythonClass");
        }
        return JavaIdentifierUtils
                .sanitizeClassName((qualifiedName != null) ? module + "." + qualifiedName : module + "." + "PythonClass");
    }
}
