package ai.timefold.solver.spring.boot.autoconfigure;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverManagerProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.spring.boot.autoconfigure.util.JacksonCustomPhaseConfigMixin;
import ai.timefold.solver.spring.boot.autoconfigure.util.JacksonSolverConfigMixin;
import ai.timefold.solver.spring.boot.autoconfigure.util.JacksonTerminationConfigMixin;

import org.springframework.aot.generate.GeneratedMethod;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TimefoldSolverAotContribution implements BeanFactoryInitializationAotContribution {
    private static final String DEFAULT_SOLVER_CONFIG_NAME = "getSolverConfig";

    /**
     * Map of SolverConfigs that were recorded during the build.
     */
    private final Map<String, SolverConfig> solverConfigMap;
    private final static Set<Class<?>> BANNED_CLASSES = Set.of(
            Class.class,
            ClassLoader.class);

    public TimefoldSolverAotContribution(Map<String, SolverConfig> solverConfigMap) {
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

    private void registerTypeRecursively(ReflectionHints reflectionHints, Class<?> type, Set<Class<?>> visited) {
        if (type == null || BANNED_CLASSES.contains(type) || visited.contains(type)) {
            return;
        }
        visited.add(type);
        registerType(reflectionHints, type);
        for (Field field : type.getDeclaredFields()) {
            registerTypeRecursively(reflectionHints, field.getType(), visited);
        }
        registerTypeRecursively(reflectionHints, type.getSuperclass(), visited);
    }

    public static void registerSolverConfigs(Environment environment,
            ConfigurableListableBeanFactory beanFactory,
            ClassLoader classLoader,
            Map<String, SolverConfig> solverConfigMap) {
        BindResult<TimefoldProperties> result = Binder.get(environment).bind("timefold", TimefoldProperties.class);
        TimefoldProperties timefoldProperties = result.orElseGet(TimefoldProperties::new);
        if (solverConfigMap.isEmpty()) {
            beanFactory.registerSingleton(DEFAULT_SOLVER_CONFIG_NAME, new SolverConfig(classLoader));
            return;
        }

        if (timefoldProperties.getSolver() == null || timefoldProperties.getSolver().size() == 1) {
            beanFactory.registerSingleton(DEFAULT_SOLVER_CONFIG_NAME, solverConfigMap.values().iterator().next());
        } else {
            // Only SolverManager can be injected for multiple solver configurations
            solverConfigMap.forEach((solverName, solverConfig) -> {
                SolverFactory<?> solverFactory = SolverFactory.create(solverConfig);

                SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
                SolverManagerProperties solverManagerProperties = timefoldProperties.getSolverManager();
                if (solverManagerProperties != null && solverManagerProperties.getParallelSolverCount() != null) {
                    solverManagerConfig.setParallelSolverCount(solverManagerProperties.getParallelSolverCount());
                }
                beanFactory.registerSingleton(solverName, SolverManager.create(solverFactory, solverManagerConfig));
            });
        }
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
    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
        var reflectionHints = generationContext.getRuntimeHints().reflection();

        // Register all classes reachable from the SolverConfig for reflection
        // (so we can read their metadata)
        Set<Class<?>> classSet = new HashSet<>();
        Map<String, String> solverXmlMap = new LinkedHashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(SolverConfig.class, JacksonSolverConfigMixin.class);
        objectMapper.addMixIn(TerminationConfig.class, JacksonTerminationConfigMixin.class);
        objectMapper.addMixIn(CustomPhaseConfig.class, JacksonCustomPhaseConfigMixin.class);
        for (Map.Entry<String, SolverConfig> solverConfigEntry : solverConfigMap.entrySet()) {
            solverConfigEntry.getValue().visitReferencedClasses(clazz -> {
                if (clazz != null) {
                    classSet.add(clazz);
                }
            });
            try {
                solverXmlMap.put(solverConfigEntry.getKey(), objectMapper.writeValueAsString(solverConfigEntry.getValue()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        registerType(reflectionHints, SolverConfig.class);
        registerTypeRecursively(reflectionHints, SolverConfig.class, new HashSet<>());

        for (Class<?> clazz : classSet) {
            registerType(reflectionHints, clazz);
        }

        // Create a generated class to hold all the solver configs
        GeneratedMethod generatedMethod = beanFactoryInitializationCode.getMethods().add("getSolverConfigs",
                builder -> {
                    builder.addParameter(Environment.class, "environment");
                    builder.addParameter(ConfigurableListableBeanFactory.class, "beanFactory");
                    var code = CodeBlock.builder();
                    code.beginControlFlow("try");
                    code.add("$T<$T, $T> solverConfigMap = new $T<>();\n", Map.class, String.class, SolverConfig.class,
                            LinkedHashMap.class);
                    code.add("$T objectMapper = new $T();\n", ObjectMapper.class, ObjectMapper.class);
                    code.add("objectMapper.addMixIn($T.class, $T.class);\n", SolverConfig.class,
                            JacksonSolverConfigMixin.class);
                    code.add("objectMapper.addMixIn($T.class, $T.class);\n", TerminationConfig.class,
                            JacksonTerminationConfigMixin.class);
                    code.add("objectMapper.addMixIn($T.class, $T.class);\n", CustomPhaseConfig.class,
                            JacksonCustomPhaseConfigMixin.class);
                    for (Map.Entry<String, String> solverConfigXmlEntry : solverXmlMap.entrySet()) {
                        code.add("solverConfigMap.put($S, objectMapper.readerFor($T.class).readValue($S));\n",
                                solverConfigXmlEntry.getKey(),
                                SolverConfig.class, solverConfigXmlEntry.getValue());
                    }
                    code.add(
                            "$T.registerSolverConfigs(environment, beanFactory, $T.currentThread().getContextClassLoader(), solverConfigMap);\n",
                            TimefoldSolverAotContribution.class, Thread.class);
                    code.endControlFlow();
                    code.beginControlFlow("catch ($T e)", JsonProcessingException.class);
                    code.add("throw new $T(e);\n", RuntimeException.class);
                    code.endControlFlow();
                    builder.addCode(code.build());
                });
        // Make spring call our generated method when the native image starts
        beanFactoryInitializationCode.addInitializer(generatedMethod.toMethodReference());
    }
}
