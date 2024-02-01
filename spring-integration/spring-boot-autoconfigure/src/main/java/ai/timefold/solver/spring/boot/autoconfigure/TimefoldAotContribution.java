package ai.timefold.solver.spring.boot.autoconfigure;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverManagerProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.aot.generate.DefaultMethodReference;
import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;

public class TimefoldAotContribution implements BeanFactoryInitializationAotContribution {
    private static final String DEFAULT_SOLVER_CONFIG_NAME = "getSolverConfig";

    /**
     * Name of the field that stores generated objects.
     * Complex pojo's should consult this map before creating
     * a new object to allow for cyclic references
     * (i.e., a = new ArrayList(); a.add(a);).
     */
    private static final String COMPLEX_POJO_MAP_FIELD_NAME = "$pojoMap";

    /**
     * Map of SolverConfigs that were recorded during the build.
     */
    private final Map<String, SolverConfig> solverConfigMap;

    public TimefoldAotContribution(Map<String, SolverConfig> solverConfigMap) {
        this.solverConfigMap = solverConfigMap;
    }

    /**
     * Register a type for reflection, allowing introspection
     * of its members at runtime in a native build.
     */
    private void registerType(ReflectionHints reflectionHints, Class<?> type) {
        reflectionHints.registerType(type,
                MemberCategory.INTROSPECT_PUBLIC_METHODS,
                MemberCategory.INTROSPECT_DECLARED_METHODS,
                MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
                MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS,
                MemberCategory.PUBLIC_FIELDS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    // The code below uses CodeBlock.Builder to generate the Java file
    // that stores the SolverConfig.
    // CodeBlock.Builder.add supports different kinds of formatting args.
    // The ones we use are:
    // - $L: Format as is (i.e. literal replacement).
    // - $S: Format as a Java String, doing the necessary escapes
    //       and surrounding it by double quotes.
    // - $T: Format as a fully qualified type, which allows you to use
    //       classes without importing them.

    /**
     * Serializes a Pojo to code that uses its no-args constructor
     * and setters to create the object.
     * 
     * @param pojo The object to be serialized.
     * @param initializerCode The code block builder of the initializer
     * @param complexPojoToIdentifier A map that stores objects already recorded
     * @return A string that can be used in a {@link CodeBlock.Builder} to access the object
     */
    public static String pojoToCode(Object pojo,
            CodeBlock.Builder initializerCode,
            Map<Object, String> complexPojoToIdentifier) {
        // First, check for primitives
        if (pojo == null) {
            return "null";
        }
        if (pojo instanceof Boolean value) {
            return value.toString();
        }
        if (pojo instanceof Byte value) {
            return value.toString();
        }
        if (pojo instanceof Character value) {
            return "\\u" + Integer.toHexString(value | 0x10000).substring(1);
        }
        if (pojo instanceof Short value) {
            return value.toString();
        }
        if (pojo instanceof Integer value) {
            return value.toString();
        }
        if (pojo instanceof Long value) {
            // Add long suffix to number string
            return value + "L";
        }
        if (pojo instanceof Float value) {
            // Add float suffix to number string
            return value + "f";
        }
        if (pojo instanceof Double value) {
            // Add double suffix to number string
            return value + "d";
        }

        // Check for builtin classes
        if (pojo instanceof String value) {
            return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
        }
        if (pojo instanceof Class<?> value) {
            return value.getName() + ".class";
        }
        if (pojo instanceof ClassLoader) {
            // We don't support serializing ClassLoaders, so replace it
            // with the context class loader
            return "Thread.currentThread().getContextClassLoader()";
        }
        if (pojo instanceof Duration value) {
            return Duration.class.getName() + ".ofNanos(" + value.toNanos() + "L)";
        }
        if (pojo.getClass().isEnum()) {
            // Use field access to read the enum
            Class<?> enumClass = pojo.getClass();
            Enum<?> pojoEnum = (Enum<?>) pojo;
            return enumClass.getName() + "." + pojoEnum.name();
        }
        return complexPojoToCode(pojo, initializerCode, complexPojoToIdentifier);
    }

    /**
     * Return a string that can be used in a {@link CodeBlock.Builder} to access a complex object
     * 
     * @param pojo The object to be accessed
     * @param complexPojoToIdentifier A Map from complex POJOs to their key in the map.
     * @return A string that can be used in a {@link CodeBlock.Builder} to access the object.
     */
    private static String getComplexPojo(Object pojo, Map<Object, String> complexPojoToIdentifier) {
        return "((" + pojo.getClass().getName() + ") " + COMPLEX_POJO_MAP_FIELD_NAME + ".get(\""
                + complexPojoToIdentifier.get(pojo) + "\"))";
    }

    /**
     * Serializes collections and complex POJOs to code
     */
    private static String complexPojoToCode(Object pojo, CodeBlock.Builder initializerCode,
            Map<Object, String> complexPojoToIdentifier) {
        // If we already serialized the object, we should just return
        // the code string
        if (complexPojoToIdentifier.containsKey(pojo)) {
            return getComplexPojo(pojo, complexPojoToIdentifier);
        }
        // Object is not serialized yet
        // Create a new variable to store its value when setting its fields
        String newIdentifier = "$obj" + complexPojoToIdentifier.size();
        complexPojoToIdentifier.put(pojo, newIdentifier);
        initializerCode.add("\n$T $L;", pojo.getClass(), newIdentifier);

        // First, check if it is a collection type
        if (pojo.getClass().isArray()) {
            return arrayToCode(newIdentifier, pojo, initializerCode, complexPojoToIdentifier);
        }
        if (pojo instanceof List<?> value) {
            return listToCode(newIdentifier, value, initializerCode, complexPojoToIdentifier);
        }
        if (pojo instanceof Set<?> value) {
            return setToCode(newIdentifier, value, initializerCode, complexPojoToIdentifier);
        }
        if (pojo instanceof Map<?, ?> value) {
            return mapToCode(newIdentifier, value, initializerCode, complexPojoToIdentifier);
        }

        // Not a collection type, so serialize by creating a new instance and settings its fields
        initializerCode.add("\n$L = new $T();", newIdentifier, pojo.getClass());
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        setComplexPojoFields(pojo.getClass(), newIdentifier, pojo, initializerCode, complexPojoToIdentifier);
        return getComplexPojo(pojo, complexPojoToIdentifier);
    }

    private static String arrayToCode(String newIdentifier, Object array, CodeBlock.Builder initializerCode,
            Map<Object, String> complexPojoToIdentifier) {
        // Get the length of the array
        int length = Array.getLength(array);

        // Create a new array from the component type with the given length
        initializerCode.add("\n$L = new $T[$L];", newIdentifier, array.getClass().getComponentType(), Integer.toString(length));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (int i = 0; i < length; i++) {
            // Set the elements of the array
            initializerCode.add("\n$L[$L] = $L;",
                    newIdentifier,
                    Integer.toString(i),
                    pojoToCode(Array.get(array, i), initializerCode, complexPojoToIdentifier));
        }
        return getComplexPojo(array, complexPojoToIdentifier);
    }

    private static String listToCode(String newIdentifier, List<?> list, CodeBlock.Builder initializerCode,
            Map<Object, String> complexPojoToIdentifier) {
        // Create an ArrayList
        initializerCode.add("\n$L = new $T($L);", newIdentifier, ArrayList.class, Integer.toString(list.size()));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (Object item : list) {
            // Add each item of the list to the ArrayList
            initializerCode.add("\n$L.add($L);",
                    newIdentifier,
                    pojoToCode(item, initializerCode, complexPojoToIdentifier));
        }
        return getComplexPojo(list, complexPojoToIdentifier);
    }

    private static String setToCode(String newIdentifier, Set<?> set, CodeBlock.Builder initializerCode,
            Map<Object, String> complexPojoToIdentifier) {
        // Create a new HashSet
        initializerCode.add("\n$L = new $T($L);", newIdentifier, HashSet.class, Integer.toString(set.size()));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (Object item : set) {
            // Add each item of the set to the HashSet
            initializerCode.add("\n$L.add($L);",
                    newIdentifier,
                    pojoToCode(item, initializerCode, complexPojoToIdentifier));
        }
        return getComplexPojo(set, complexPojoToIdentifier);
    }

    private static String mapToCode(String newIdentifier, Map<?, ?> map, CodeBlock.Builder initializerCode,
            Map<Object, String> complexPojoToIdentifier) {
        // Create a HashMap
        initializerCode.add("\n$L = new $T($L);", newIdentifier, HashMap.class, Integer.toString(map.size()));
        initializerCode.add("\n$L.put($S, $L);", COMPLEX_POJO_MAP_FIELD_NAME, newIdentifier, newIdentifier);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            // Put each entry of the map into the HashMap
            initializerCode.add("\n$L.put($L,$L);",
                    newIdentifier,
                    pojoToCode(entry.getKey(), initializerCode, complexPojoToIdentifier),
                    pojoToCode(entry.getValue(), initializerCode, complexPojoToIdentifier));
        }
        return getComplexPojo(map, complexPojoToIdentifier);
    }

    /**
     * Sets the fields of pojo declared in pojoClass and all its superclasses.
     * 
     * @param pojoClass A class assignable to pojo containing some of its fields.
     * @param identifier The name of the variable storing the serialized pojo.
     * @param pojo The object being serialized.
     * @param initializerCode The {@link CodeBlock.Builder} to use to generate code in the initializer.
     * @param complexPojoToIdentifier A map from complex POJOs to their variable name.
     */
    private static void setComplexPojoFields(Class<?> pojoClass, String identifier, Object pojo,
            CodeBlock.Builder initializerCode, Map<Object, String> complexPojoToIdentifier) {
        if (pojoClass == Object.class) {
            // We are the top-level, no more fields to set
            return;
        }
        for (Field field : pojo.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                // We do not want to write static fields
                continue;
            }
            // Set the field accessible so we can read its value
            field.setAccessible(true);
            try {
                // Convert the field value to code, and call the setter
                // corresponding to the field with the serialized field value.
                initializerCode.add("\n$L.set$L$L($L);", identifier,
                        Character.toUpperCase(field.getName().charAt(0)),
                        field.getName().substring(1),
                        pojoToCode(field.get(pojo), initializerCode, complexPojoToIdentifier));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        setComplexPojoFields(pojoClass.getSuperclass(), identifier, pojo, initializerCode, complexPojoToIdentifier);
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
        var reflectionHints = generationContext.getRuntimeHints().reflection();

        // Register all classes reachable from the SolverConfig for reflection
        // (so we can read their metadata)
        Set<Class<?>> classSet = new HashSet<>();
        for (SolverConfig solverConfig : solverConfigMap.values()) {
            solverConfig.visitReferencedClasses(clazz -> {
                if (clazz != null) {
                    classSet.add(clazz);
                }
            });
        }

        for (Class<?> clazz : classSet) {
            registerType(reflectionHints, clazz);
        }

        // Create a generated class to hold all the solver configs
        GeneratedClass generatedClass = generationContext.getGeneratedClasses().addForFeature("timefold-aot",
                builder -> {
                    builder.addField(Map.class, "solverConfigMap", Modifier.STATIC);
                    builder.addField(Map.class, COMPLEX_POJO_MAP_FIELD_NAME, Modifier.STATIC);

                    // Handwrite the SolverConfig map in the initializer
                    CodeBlock.Builder staticInitializer = CodeBlock.builder();
                    Map<Object, String> complexPojoToIdentifier = new IdentityHashMap<>();
                    staticInitializer.add("$L = new $T();", COMPLEX_POJO_MAP_FIELD_NAME, HashMap.class);
                    staticInitializer.add("\nsolverConfigMap = $L;",
                            complexPojoToCode(solverConfigMap, staticInitializer, complexPojoToIdentifier));
                    builder.addStaticBlock(staticInitializer.build());

                    // getSolverConfig fetches the SolverConfig with the given name from the map
                    CodeBlock.Builder getSolverConfigMethod = CodeBlock.builder();
                    getSolverConfigMethod.add("return ($T) solverConfigMap.get(name);", SolverConfig.class);
                    builder.addMethod(MethodSpec.methodBuilder("getSolverConfig")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.STATIC)
                            .addParameter(String.class, "name")
                            .returns(SolverConfig.class)
                            .addCode(getSolverConfigMethod.build())
                            .build());

                    // Returns the key set of the solver config map
                    CodeBlock.Builder getSolverConfigNamesMethod = CodeBlock.builder();
                    getSolverConfigNamesMethod.add("return new $T(solverConfigMap.keySet());", ArrayList.class);
                    builder.addMethod(MethodSpec.methodBuilder("getSolverConfigNames")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.STATIC)
                            .returns(List.class)
                            .addCode(getSolverConfigNamesMethod.build())
                            .build());

                    // Registers the SolverConfig(s) as beans that can be injected
                    CodeBlock.Builder registerSolverConfigsMethod = CodeBlock.builder();
                    // Get the timefold properties from the environment
                    registerSolverConfigsMethod.add("$T<TimefoldProperties> timefoldPropertiesResult = " +
                            "$T.get(environment).bind(\"timefold\", $T.class);", BindResult.class, Binder.class,
                            TimefoldProperties.class);
                    registerSolverConfigsMethod.add("\n$T timefoldProperties = timefoldPropertiesResult.orElseGet($T::new);",
                            TimefoldProperties.class, TimefoldProperties.class);

                    // Get the names of the solverConfigs
                    registerSolverConfigsMethod.add("\n$T solverConfigNames = getSolverConfigNames();\n", List.class);

                    // If there are no solverConfigs...
                    registerSolverConfigsMethod.beginControlFlow("if (solverConfigNames.isEmpty())");
                    // Create an empty one that can be used for injection
                    registerSolverConfigsMethod.add(
                            "\nbeanFactory.registerSingleton($S, new $T(beanFactory.getBeanClassLoader()));",
                            DEFAULT_SOLVER_CONFIG_NAME, SolverConfig.class);
                    registerSolverConfigsMethod.add("return;\n");
                    registerSolverConfigsMethod.endControlFlow();

                    // If there is only a single solver
                    registerSolverConfigsMethod.beginControlFlow(
                            "if (timefoldProperties.getSolver() == null || timefoldProperties.getSolver().size() == 1)");
                    // Use the default solver config name
                    registerSolverConfigsMethod.add(
                            "\nbeanFactory.registerSingleton($S, getSolverConfig((String) solverConfigNames.get(0)));",
                            DEFAULT_SOLVER_CONFIG_NAME);
                    registerSolverConfigsMethod.add("return;\n");
                    registerSolverConfigsMethod.endControlFlow();

                    // Otherwise, for each solver...
                    registerSolverConfigsMethod.beginControlFlow("for (Object solverNameObj : solverConfigNames)");
                    // Get the solver config with the given name
                    registerSolverConfigsMethod.add("\nString solverName = (String) solverNameObj;");
                    registerSolverConfigsMethod.add(
                            "\n$T solverConfig = getSolverConfig(solverName);",
                            SolverConfig.class);

                    // Create a solver manager from that solver config
                    registerSolverConfigsMethod.add("\n$T solverFactory = $T.create(solverConfig);", SolverFactory.class,
                            SolverFactory.class);
                    registerSolverConfigsMethod.add("\n$T solverManagerConfig = new $T();", SolverManagerConfig.class,
                            SolverManagerConfig.class);
                    registerSolverConfigsMethod.add("\n$T solverManagerProperties = timefoldProperties.getSolverManager();\n",
                            SolverManagerProperties.class);
                    registerSolverConfigsMethod.beginControlFlow(
                            "if (solverManagerProperties != null && solverManagerProperties.getParallelSolverCount() != null)");
                    registerSolverConfigsMethod.add(
                            "\nsolverManagerConfig.setParallelSolverCount(solverManagerProperties.getParallelSolverCount());\n");
                    registerSolverConfigsMethod.endControlFlow();

                    // Register that solver manager
                    registerSolverConfigsMethod.add(
                            "\nbeanFactory.registerSingleton(solverName, $T.create(solverFactory, solverManagerConfig));\n",
                            SolverManager.class);
                    registerSolverConfigsMethod.endControlFlow();

                    builder.addMethod(MethodSpec.methodBuilder("registerSolverConfigs")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.STATIC)
                            .addParameter(Environment.class, "environment")
                            .addParameter(ConfigurableListableBeanFactory.class, "beanFactory")
                            .addCode(registerSolverConfigsMethod.build())
                            .build());

                    builder.build();
                });

        // Make spring call our generated class when the native image starts
        beanFactoryInitializationCode.addInitializer(new DefaultMethodReference(
                MethodSpec.methodBuilder("registerSolverConfigs")
                        .addModifiers(Modifier.PUBLIC)
                        .addModifiers(Modifier.STATIC)
                        .addParameter(Environment.class, "environment")
                        .addParameter(ConfigurableListableBeanFactory.class, "beanFactory")
                        .build(),
                generatedClass.getName()));
    }
}
