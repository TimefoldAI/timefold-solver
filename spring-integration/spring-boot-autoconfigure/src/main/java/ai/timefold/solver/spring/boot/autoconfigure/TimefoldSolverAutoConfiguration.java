package ai.timefold.solver.spring.boot.autoconfigure;

import static ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptorValidator.assertValidPlanningVariables;
import static java.util.stream.Collectors.joining;

import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.CustomShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.spring.boot.autoconfigure.config.DiminishedReturnsProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TerminationProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NativeDetector;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SolverConfig.class, SolverFactory.class, SolutionManager.class, SolverManager.class })
@ConditionalOnMissingBean({ SolverConfig.class, SolverFactory.class, SolutionManager.class,
        SolverManager.class })
@EnableConfigurationProperties({ TimefoldProperties.class })
public class TimefoldSolverAutoConfiguration
        implements BeanClassLoaderAware, ApplicationContextAware, EnvironmentAware, BeanFactoryInitializationAotProcessor,
        BeanDefinitionRegistryPostProcessor {

    private static final Log LOG = LogFactory.getLog(TimefoldSolverAutoConfiguration.class);
    private static final String DEFAULT_SOLVER_CONFIG_NAME = "getSolverConfig";
    private static final Class<? extends Annotation>[] PLANNING_ENTITY_FIELD_ANNOTATIONS = new Class[] {
            PlanningPin.class,
            PlanningVariable.class,
            PlanningListVariable.class,
            CustomShadowVariable.class,
            IndexShadowVariable.class,
            InverseRelationShadowVariable.class,
            NextElementShadowVariable.class,
            PiggybackShadowVariable.class,
            PreviousElementShadowVariable.class,
            ShadowVariable.class,
            CascadingUpdateShadowVariable.class
    };
    // We filter out abstract classes and anything we use internally.
    static final Predicate<Class<?>> UNIQUENESS_PREDICATE = clazz -> {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        var pkg = clazz.getPackageName();
        // Only user classes should count, and classes from our own testdomain, which may legally be employed by tests.
        return !pkg.startsWith("ai.timefold.solver.core") || pkg.contains(".test");
    };

    private ApplicationContext context;
    private ClassLoader beanClassLoader;
    private TimefoldProperties timefoldProperties;

    protected TimefoldSolverAutoConfiguration() {
    }

    @Override
    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public void setEnvironment(Environment environment) {
        // postProcessBeanFactory runs before creating any bean, but we need TimefoldProperties.
        // Therefore, we use the Environment to load the properties
        BindResult<TimefoldProperties> result = Binder.get(environment).bind("timefold", TimefoldProperties.class);
        this.timefoldProperties = result.orElseGet(TimefoldProperties::new);
    }

    private Map<String, SolverConfig> getSolverConfigMap() {
        var entityScanner = new IncludeAbstractClassesEntityScanner(this.context);
        if (!entityScanner.hasSolutionOrEntityClasses()) {
            LOG.warn(
                    """
                            Skipping Timefold loading because there are no @%s  or @%s annotated classes.
                            Maybe your annotated classes are not in a subpackage of your @%s annotated class's package.
                            Maybe move your planning solution and entity classes to your application class's (sub)package (or use @%s)."""
                            .formatted(PlanningSolution.class.getSimpleName(), PlanningEntity.class.getSimpleName(),
                                    SpringBootApplication.class.getSimpleName(), EntityScan.class.getSimpleName()));
            return Map.of();
        }
        var solverConfigMap = new HashMap<String, SolverConfig>();
        // Step 1 - create all SolverConfig
        // If the config map is empty, we build the config using the default solver name
        if (timefoldProperties.getSolver() == null || timefoldProperties.getSolver().isEmpty()) {
            solverConfigMap.put(TimefoldProperties.DEFAULT_SOLVER_NAME,
                    createSolverConfig(timefoldProperties, TimefoldProperties.DEFAULT_SOLVER_NAME));
        } else {
            timefoldProperties.getSolver().keySet()
                    .forEach(solverName -> solverConfigMap.put(solverName, createSolverConfig(timefoldProperties, solverName)));
        }
        // Step 2 - first pass of validations
        assertValidaPlanningVariables(entityScanner);
        assertSolverConfigSolutionClasses(entityScanner, solverConfigMap);
        assertSolverConfigConstraintClasses(entityScanner, solverConfigMap);
        // Step 3 - load all additional information per SolverConfig
        solverConfigMap.forEach(
                (solverName, solverConfig) -> loadSolverConfig(entityScanner, timefoldProperties, solverName, solverConfig));
        // Step 4 - second pass of validations
        assertEmptySolutionClasses(solverConfigMap);
        assertMutableSolutionClasses(solverConfigMap);
        assertSolverConfigEntityClasses(solverConfigMap);
        return solverConfigMap;
    }

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        var solverConfigMap = getSolverConfigMap();
        return new TimefoldSolverAotContribution(solverConfigMap);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        var solverConfigMap = getSolverConfigMap();
        var solverConfigIO = new SolverConfigIO();
        registry.registerBeanDefinition(TimefoldSolverAotFactory.class.getName(),
                new RootBeanDefinition(TimefoldSolverAotFactory.class));
        if (solverConfigMap.isEmpty()) {
            var rootBeanDefinition = new RootBeanDefinition(SolverConfig.class);
            rootBeanDefinition.setFactoryBeanName(TimefoldSolverAotFactory.class.getName());
            rootBeanDefinition.setFactoryMethodName("solverConfigSupplier");
            var solverXmlOutput = new StringWriter();
            solverConfigIO.write(new SolverConfig(), solverXmlOutput);
            rootBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
                    solverXmlOutput.toString());
            registry.registerBeanDefinition(DEFAULT_SOLVER_CONFIG_NAME, rootBeanDefinition);
            return;
        }

        if (timefoldProperties.getSolver() == null || timefoldProperties.getSolver().size() == 1) {
            var rootBeanDefinition = new RootBeanDefinition(SolverConfig.class);
            rootBeanDefinition.setFactoryBeanName(TimefoldSolverAotFactory.class.getName());
            rootBeanDefinition.setFactoryMethodName("solverConfigSupplier");
            var solverXmlOutput = new StringWriter();
            solverConfigIO.write(solverConfigMap.values().iterator().next(), solverXmlOutput);
            rootBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
                    solverXmlOutput.toString());
            registry.registerBeanDefinition(DEFAULT_SOLVER_CONFIG_NAME, rootBeanDefinition);
        } else {
            // Only SolverManager can be injected for multiple solver configurations
            solverConfigMap.forEach((solverName, solverConfig) -> {
                var rootBeanDefinition = new RootBeanDefinition(SolverManager.class);
                rootBeanDefinition.setFactoryBeanName(TimefoldSolverAotFactory.class.getName());
                rootBeanDefinition.setFactoryMethodName("solverManagerSupplier");
                var solverXmlOutput = new StringWriter();
                solverConfigIO.write(solverConfig, solverXmlOutput);
                rootBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
                        solverXmlOutput.toString());
                registry.registerBeanDefinition(solverName, rootBeanDefinition);
            });
        }
    }

    private SolverConfig createSolverConfig(TimefoldProperties timefoldProperties, String solverName) {
        // 1 - The solver configuration takes precedence over root and default settings
        var solverConfigXml = timefoldProperties.getSolverConfig(solverName)
                .map(SolverProperties::getSolverConfigXml);

        // 2 - Root settings
        if (solverConfigXml.isEmpty()) {
            solverConfigXml = Optional.ofNullable(timefoldProperties.getSolverConfigXml());
        }

        SolverConfig solverConfig;
        if (solverConfigXml.isPresent()) {
            String solverUrl = solverConfigXml.get();
            if (beanClassLoader.getResource(solverUrl) == null) {
                String message =
                        "Invalid timefold.solverConfigXml property (%s): that classpath resource does not exist."
                                .formatted(solverUrl);
                if (!solverName.equals(TimefoldProperties.DEFAULT_SOLVER_NAME)) {
                    message =
                            "Invalid timefold.solver.\"%s\".solverConfigXML property (%s): that classpath resource does not exist."
                                    .formatted(solverName, solverUrl);
                }
                throw new IllegalStateException(message);
            }
            solverConfig = SolverConfig.createFromXmlResource(solverUrl);
        } else if (beanClassLoader.getResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL) != null) {
            // 3 - Default file URL
            solverConfig = SolverConfig.createFromXmlResource(
                    TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL);
        } else {
            solverConfig = new SolverConfig(beanClassLoader);
        }

        return solverConfig;
    }

    private void loadSolverConfig(IncludeAbstractClassesEntityScanner entityScanner, TimefoldProperties timefoldProperties,
            String solverName, SolverConfig solverConfig) {
        if (solverConfig.getSolutionClass() == null) {
            solverConfig.setSolutionClass(entityScanner.findFirstSolutionClass());
        }
        var solverEntityClassList = solverConfig.getEntityClassList();
        if (solverEntityClassList == null) {
            solverConfig.setEntityClassList(entityScanner.findEntityClassList());
        }
        timefoldProperties.getSolverConfig(solverName)
                .ifPresentOrElse(
                        solverProperties -> applyScoreDirectorFactoryProperties(entityScanner, solverConfig, solverProperties),
                        () -> applyScoreDirectorFactoryProperties(entityScanner, solverConfig));
    }

    private void applyScoreDirectorFactoryProperties(IncludeAbstractClassesEntityScanner entityScanner,
            SolverConfig solverConfig, SolverProperties solverProperties) {
        applyScoreDirectorFactoryProperties(entityScanner, solverConfig);
        if (solverProperties.getConstraintStreamAutomaticNodeSharing() != null
                && solverProperties.getConstraintStreamAutomaticNodeSharing()) {
            if (NativeDetector.inNativeImage()) {
                throw new UnsupportedOperationException(
                        "Constraint stream automatic node sharing is unsupported in a Spring native image.");
            }
            Objects.requireNonNull(solverConfig.getScoreDirectorFactoryConfig())
                    .setConstraintStreamAutomaticNodeSharing(true);
        }
        if (solverProperties.getConstraintStreamProfilingEnabled() != null) {
            Objects.requireNonNull(solverConfig.getScoreDirectorFactoryConfig())
                    .setConstraintStreamProfilingEnabled(solverProperties.getConstraintStreamProfilingEnabled());
        }
        if (solverProperties.getEnvironmentMode() != null) {
            solverConfig.setEnvironmentMode(solverProperties.getEnvironmentMode());
        }
        if (solverProperties.getDomainAccessType() != null) {
            solverConfig.setDomainAccessType(solverProperties.getDomainAccessType());
        }
        if (solverProperties.getEnabledPreviewFeatures() != null) {
            solverConfig.setEnablePreviewFeatureSet(new HashSet<>(solverProperties.getEnabledPreviewFeatures()));
        }
        if (solverProperties.getNearbyDistanceMeterClass() != null) {
            solverConfig.setNearbyDistanceMeterClass(solverProperties.getNearbyDistanceMeterClass());
        }
        if (solverProperties.getDaemon() != null) {
            solverConfig.setDaemon(solverProperties.getDaemon());
        }
        if (solverProperties.getMoveThreadCount() != null) {
            solverConfig.setMoveThreadCount(solverProperties.getMoveThreadCount());
        }
        if (solverProperties.getRandomSeed() != null) {
            solverConfig.setRandomSeed(solverProperties.getRandomSeed());
        }
        applyTerminationProperties(solverConfig, solverProperties.getTermination());
    }

    private void applyScoreDirectorFactoryProperties(IncludeAbstractClassesEntityScanner entityScanner,
            SolverConfig solverConfig) {
        if (solverConfig.getScoreDirectorFactoryConfig() == null) {
            solverConfig.setScoreDirectorFactoryConfig(defaultScoreDirectoryFactoryConfig(entityScanner));
        }
    }

    private static ScoreDirectorFactoryConfig
            defaultScoreDirectoryFactoryConfig(IncludeAbstractClassesEntityScanner entityScanner) {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig
                .setEasyScoreCalculatorClass(entityScanner.findFirstImplementingClass(EasyScoreCalculator.class));
        scoreDirectorFactoryConfig
                .setConstraintProviderClass(entityScanner.findFirstImplementingClass(ConstraintProvider.class));
        scoreDirectorFactoryConfig
                .setIncrementalScoreCalculatorClass(entityScanner.findFirstImplementingClass(IncrementalScoreCalculator.class));

        return scoreDirectorFactoryConfig;
    }

    static void applyTerminationProperties(SolverConfig solverConfig, TerminationProperties terminationProperties) {
        var terminationConfig = solverConfig.getTerminationConfig();
        if (terminationConfig == null) {
            terminationConfig = new TerminationConfig();
            solverConfig.setTerminationConfig(terminationConfig);
        }
        if (terminationProperties != null) {
            if (terminationProperties.getSpentLimit() != null) {
                terminationConfig.overwriteSpentLimit(terminationProperties.getSpentLimit());
            }
            if (terminationProperties.getUnimprovedSpentLimit() != null) {
                terminationConfig.overwriteUnimprovedSpentLimit(terminationProperties.getUnimprovedSpentLimit());
            }
            if (terminationProperties.getBestScoreLimit() != null) {
                terminationConfig.setBestScoreLimit(terminationProperties.getBestScoreLimit());
            }
            if (terminationProperties.getDiminishedReturns() != null) {
                applyDiminishedReturnsProperties(solverConfig, terminationProperties.getDiminishedReturns());
            }
        }
    }

    static void applyDiminishedReturnsProperties(SolverConfig solverConfig,
            DiminishedReturnsProperties diminishedReturnsProperties) {
        if (Objects.equals(false, diminishedReturnsProperties.getEnabled())) {
            // do nothing if explicitly disabled
            return;
        }

        var terminationConfig = solverConfig.getTerminationConfig();
        if (terminationConfig == null) {
            terminationConfig = new TerminationConfig();
            solverConfig.setTerminationConfig(terminationConfig);
        }
        terminationConfig.setDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig()
                .withSlidingWindowDuration(diminishedReturnsProperties.getSlidingWindowDuration())
                .withMinimumImprovementRatio(diminishedReturnsProperties.getMinimumImprovementRatio()));
    }

    private static void assertValidaPlanningVariables(IncludeAbstractClassesEntityScanner entityScanner) {
        var timefoldFieldAnnotationList =
                entityScanner.findClassesWithAnnotation(PLANNING_ENTITY_FIELD_ANNOTATIONS);
        for (var clazz : timefoldFieldAnnotationList) {
            assertValidPlanningVariables(clazz);
        }
    }

    private static void assertSolverConfigSolutionClasses(IncludeAbstractClassesEntityScanner entityScanner,
            Map<String, SolverConfig> solverConfigMap) {
        // Multiple classes and single solver
        try {
            var classes = entityScanner.scan(PlanningSolution.class).stream()
                    .filter(UNIQUENESS_PREDICATE)
                    .toList();
            var firstConfig = solverConfigMap.values().stream().findFirst().orElse(null);
            if (classes.size() > 1 && solverConfigMap.size() == 1 && firstConfig != null
                    && firstConfig.getSolutionClass() == null) {
                throw new IllegalStateException(
                        "Multiple classes ([%s]) found in the classpath with a @%s annotation.".formatted(
                                classes.stream().map(Class::getSimpleName).collect(joining(", ")),
                                PlanningSolution.class.getSimpleName()));
            }
            // Multiple classes and at least one solver config do not specify the solution class
            // We do not fail if all configurations define the solution,
            // even though there are additional "unused" solution classes in the classpath.
            var unconfiguredSolverConfigList = solverConfigMap.entrySet().stream()
                    .filter(e -> e.getValue().getSolutionClass() == null)
                    .map(Map.Entry::getKey)
                    .toList();
            if (classes.size() > 1 && !unconfiguredSolverConfigList.isEmpty()) {
                throw new IllegalStateException(
                        """
                                Some solver configs (%s) don't specify a %s class, yet there are multiple available (%s) on the classpath.
                                Maybe set the XML config file to the related solver configs, or add the missing solution classes to the XML files,
                                or remove the unnecessary solution classes from the classpath."""
                                .formatted(String.join(", ", unconfiguredSolverConfigList),
                                        PlanningSolution.class.getSimpleName(),
                                        classes.stream().map(Class::getSimpleName).collect(joining(", "))));
            }
            // Unused solution classes
            // When inheritance is used, we ignore the parent classes
            var unusedSolutionClassList = classes.stream()
                    .map(Class::getName)
                    .filter(planningClassName -> solverConfigMap.values().stream().filter(c -> c.getSolutionClass() != null)
                            .noneMatch(c -> c.getSolutionClass().getName().equals(planningClassName)
                                    || c.getSolutionClass().getSuperclass().getName().equals(planningClassName)))
                    .toList();
            if (classes.size() > 1 && !unusedSolutionClassList.isEmpty()) {
                throw new IllegalStateException(
                        "Unused classes ([%s]) found with a @%s annotation.".formatted(
                                String.join(", ", unusedSolutionClassList),
                                PlanningSolution.class.getSimpleName()));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Scanning for @%s annotations failed.".formatted(PlanningSolution.class.getSimpleName()), e);
        }
    }

    private static void assertEmptySolutionClasses(Map<String, SolverConfig> solverConfigMap) {
        // No solution class
        for (var config : solverConfigMap.values()) {
            if (config.getSolutionClass() == null) {
                throw new IllegalStateException(
                        "No classes were found with a @%s annotation.".formatted(PlanningSolution.class.getSimpleName()));
            }
        }
    }

    private static void assertMutableSolutionClasses(Map<String, SolverConfig> solverConfigMap) {
        // Assert it is mutable
        for (var config : solverConfigMap.values()) {
            SolutionDescriptor.assertMutable(config.getSolutionClass(), "solutionClass");
        }
    }

    private static void assertSolverConfigEntityClasses(Map<String, SolverConfig> solverConfigMap) {
        // No Entity class
        for (var config : solverConfigMap.values()) {
            // Assert if the list is empty
            var entityList = config.getEntityClassList();
            if (entityList == null || entityList.isEmpty()) {
                throw new IllegalStateException(
                        "No classes were found with a @%s annotation.".formatted(PlanningEntity.class.getSimpleName()));
            }
            // Assert the entity target and planning variables
            for (var clazz : entityList) {
                SolutionDescriptor.assertMutable(clazz, "entityClass");
                assertValidPlanningVariables(clazz);
            }
        }
    }

    private static void assertSolverConfigConstraintClasses(
            IncludeAbstractClassesEntityScanner entityScanner, Map<String, SolverConfig> solverConfigMap) {
        var simpleScoreClassList = entityScanner.findImplementingClassList(EasyScoreCalculator.class)
                .stream()
                .filter(UNIQUENESS_PREDICATE)
                .toList();
        var constraintScoreClassList = entityScanner.findImplementingClassList(ConstraintProvider.class)
                .stream()
                .filter(UNIQUENESS_PREDICATE)
                .toList();
        var incrementalScoreClassList = entityScanner.findImplementingClassList(IncrementalScoreCalculator.class)
                .stream()
                .filter(UNIQUENESS_PREDICATE)
                .toList();
        // No score calculators
        if (simpleScoreClassList.isEmpty() && constraintScoreClassList.isEmpty()
                && incrementalScoreClassList.isEmpty()) {
            throw new IllegalStateException(
                    """
                            No classes found that implement %s, %s, or %s.
                            Maybe your %s class is not in a subpackage of your @%s annotated class's package."
                            Maybe move your %s class to your application class's (sub)package (or use @%s)."""
                            .formatted(EasyScoreCalculator.class.getSimpleName(),
                                    ConstraintProvider.class.getSimpleName(), IncrementalScoreCalculator.class.getSimpleName(),
                                    ConstraintProvider.class.getSimpleName(), SpringBootApplication.class.getSimpleName(),
                                    ConstraintProvider.class.getSimpleName(), EntityScan.class.getSimpleName()));
        }
        assertSolverConfigsSpecifyScoreCalculatorWhenAmbigious(solverConfigMap, simpleScoreClassList, constraintScoreClassList,
                incrementalScoreClassList);
        assertNoUnusedScoreClasses(solverConfigMap, simpleScoreClassList, constraintScoreClassList, incrementalScoreClassList);
    }

    private static void assertSolverConfigsSpecifyScoreCalculatorWhenAmbigious(Map<String, SolverConfig> solverConfigMap,
            List<Class<? extends EasyScoreCalculator>> simpleScoreClassList,
            List<Class<? extends ConstraintProvider>> constraintScoreClassList,
            List<Class<? extends IncrementalScoreCalculator>> incrementalScoreClassList) {
        // Single solver, multiple score calculators
        var errorMessage = "Multiple score calculator classes (%s) that implements %s were found in the classpath.";
        if (simpleScoreClassList.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    simpleScoreClassList.stream().map(Class::getSimpleName).collect(joining(", ")),
                    EasyScoreCalculator.class.getSimpleName()));
        }
        if (constraintScoreClassList.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    constraintScoreClassList.stream().map(Class::getSimpleName).collect(joining(", ")),
                    ConstraintProvider.class.getSimpleName()));
        }
        if (incrementalScoreClassList.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    incrementalScoreClassList.stream().map(Class::getSimpleName).collect(joining(", ")),
                    IncrementalScoreCalculator.class.getSimpleName()));
        }
        // Multiple solvers, multiple score calculators
        errorMessage =
                """
                        Some solver configs (%s) don't specify a %s score calculator class, yet there are multiple available (%s) on the classpath.
                        Maybe set the XML config file to the related solver configs, or add the missing score calculator to the XML files,
                        or remove the unnecessary score calculator from the classpath.""";
        var solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (simpleScoreClassList.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    EasyScoreCalculator.class.getSimpleName(),
                    simpleScoreClassList.stream().map(Class::getSimpleName).collect(joining(", "))));
        }
        solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getConstraintProviderClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (constraintScoreClassList.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    ConstraintProvider.class.getSimpleName(),
                    constraintScoreClassList.stream().map(Class::getSimpleName).collect(joining(", "))));
        }
        solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (incrementalScoreClassList.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    IncrementalScoreCalculator.class.getSimpleName(),
                    incrementalScoreClassList.stream().map(Class::getSimpleName).collect(joining(", "))));
        }
    }

    private static void assertNoUnusedScoreClasses(Map<String, SolverConfig> solverConfigMap,
            List<Class<? extends EasyScoreCalculator>> simpleScoreClassList,
            List<Class<? extends ConstraintProvider>> constraintScoreClassList,
            List<Class<? extends IncrementalScoreCalculator>> incrementalScoreClassList) {
        String errorMessage;
        var solverConfigWithUnusedSolutionClassList = simpleScoreClassList.stream()
                .map(Class::getName)
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass().getName()
                                .equals(className)))
                .toList();
        errorMessage = "Unused classes ([%s]) that implements %s were found.";
        if (simpleScoreClassList.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    EasyScoreCalculator.class.getSimpleName()));
        }
        solverConfigWithUnusedSolutionClassList = constraintScoreClassList.stream()
                .map(Class::getName)
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getConstraintProviderClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getConstraintProviderClass().getName()
                                .equals(className)))
                .toList();
        if (constraintScoreClassList.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    ConstraintProvider.class.getSimpleName()));
        }
        solverConfigWithUnusedSolutionClassList = incrementalScoreClassList.stream()
                .map(Class::getName)
                .filter(className -> solverConfigMap.values().stream()
                        .filter(c -> c.getScoreDirectorFactoryConfig() != null
                                && c.getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass() != null)
                        .noneMatch(c -> c.getScoreDirectorFactoryConfig().getIncrementalScoreCalculatorClass().getName()
                                .equals(className)))
                .toList();
        if (incrementalScoreClassList.size() > 1 && !solverConfigWithUnusedSolutionClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(String.join(", ", solverConfigWithUnusedSolutionClassList),
                    IncrementalScoreCalculator.class.getSimpleName()));
        }
    }
}
