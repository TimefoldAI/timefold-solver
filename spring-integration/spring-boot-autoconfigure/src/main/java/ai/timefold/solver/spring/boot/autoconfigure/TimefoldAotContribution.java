package ai.timefold.solver.spring.boot.autoconfigure;

import java.util.ArrayList;
import java.util.HashSet;
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
import ai.timefold.solver.spring.boot.autoconfigure.util.PojoInliner;

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
                    final String SOLVER_CONFIG_MAP_FIELD = "solverConfigMap";

                    // Handwrite the SolverConfig map in the initializer
                    PojoInliner.inlineFields(builder,
                            PojoInliner.field(Map.class, SOLVER_CONFIG_MAP_FIELD, solverConfigMap));

                    // getSolverConfig fetches the SolverConfig with the given name from the map
                    CodeBlock.Builder getSolverConfigMethod = CodeBlock.builder();
                    getSolverConfigMethod.add("return ($T) $L.get(name);", SolverConfig.class, SOLVER_CONFIG_MAP_FIELD);
                    builder.addMethod(MethodSpec.methodBuilder("getSolverConfig")
                            .addModifiers(Modifier.PUBLIC)
                            .addModifiers(Modifier.STATIC)
                            .addParameter(String.class, "name")
                            .returns(SolverConfig.class)
                            .addCode(getSolverConfigMethod.build())
                            .build());

                    // Returns the key set of the solver config map
                    CodeBlock.Builder getSolverConfigNamesMethod = CodeBlock.builder();
                    getSolverConfigNamesMethod.add("return new $T($L.keySet());", ArrayList.class, SOLVER_CONFIG_MAP_FIELD);
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
