package ai.timefold.solver.spring.boot.autoconfigure;

import static java.util.stream.Collectors.joining;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.jackson.api.TimefoldJacksonModule;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverManagerProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TerminationProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import ai.timefold.solver.test.api.score.stream.MultiConstraintVerification;
import ai.timefold.solver.test.api.score.stream.SingleConstraintVerification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.Module;

@Configuration
@ConditionalOnClass({ SolverConfig.class, SolverFactory.class, ScoreManager.class, SolutionManager.class, SolverManager.class })
@ConditionalOnMissingBean({ SolverConfig.class, SolverFactory.class, ScoreManager.class, SolutionManager.class,
        SolverManager.class })
@EnableConfigurationProperties({ TimefoldProperties.class })
public class TimefoldAutoConfiguration
        implements BeanClassLoaderAware, ApplicationContextAware, EnvironmentAware, BeanFactoryPostProcessor {

    private static final Log LOG = LogFactory.getLog(TimefoldAutoConfiguration.class);
    private static String DEFAULT_SOLVER_CONFIG_NAME = "getSolverConfig";
    private ApplicationContext context;
    private ClassLoader beanClassLoader;
    private TimefoldProperties timefoldProperties;

    protected TimefoldAutoConfiguration() {
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
        // postProcessBeanFactory runs before creating any bean, but we need TimefoldProperties. Therefore, we use the
        // Environment to load the properties
        BindResult<TimefoldProperties> result = Binder.get(environment).bind("timefold", TimefoldProperties.class);
        this.timefoldProperties = result.orElseGet(TimefoldProperties::new);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        IncludeAbstractClassesEntityScanner entityScanner = new IncludeAbstractClassesEntityScanner(this.context);
        if (!entityScanner.hasSolutionOrEntityClasses()) {
            LOG.warn(
                    """
                            Skipping Timefold loading because there are no @%s  or @%s annotated classes.
                            Maybe your annotated classes are not in a subpackage of your @%s annotated class's package.
                            Maybe move your planning solution and entity classes to your application class's (sub)package (or use @%s)."""
                            .formatted(PlanningSolution.class.getSimpleName(), PlanningEntity.class.getSimpleName(),
                                    SpringBootApplication.class.getSimpleName(), EntityScan.class.getSimpleName()));
            beanFactory.registerSingleton(DEFAULT_SOLVER_CONFIG_NAME, new SolverConfig(beanClassLoader));
            return;
        }
        Map<String, SolverConfig> solverConfigMap = new HashMap<>();
        // Step 1 - create all SolverConfig
        // If the config map is empty, we build the config using the default solver name
        if (timefoldProperties.getSolver() == null || timefoldProperties.getSolver().isEmpty()) {
            solverConfigMap.put(TimefoldProperties.DEFAULT_SOLVER_NAME,
                    createSolverConfig(timefoldProperties, TimefoldProperties.DEFAULT_SOLVER_NAME));
        } else {
            timefoldProperties.getSolver().keySet()
                    .forEach(solverName -> solverConfigMap.put(solverName, createSolverConfig(timefoldProperties, solverName)));
        }
        // Step 2 - validate all SolverConfig definitions
        // TODO - Should we assert planning members belongs to a PlanningEntity Quarkus#assertNoMemberAnnotationWithoutClassAnnotation?
        assertSolverConfigSolutionClasses(entityScanner, solverConfigMap);
        assertSolverConfigEntityClasses(entityScanner);
        assertSolverConfigConstraintClasses(entityScanner, solverConfigMap);

        // Step 3 - load all additional information per SolverConfig
        solverConfigMap.forEach(
                (solverName, solverConfig) -> loadSolverConfig(entityScanner, timefoldProperties, solverName, solverConfig));

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

    private SolverConfig createSolverConfig(TimefoldProperties timefoldProperties, String solverName) {
        // 1 - The solver configuration takes precedence over root and default settings
        Optional<String> solverConfigXml = timefoldProperties.getSolverConfig(solverName)
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
                        "\"Invalid timefold.solverConfigXml property (%s): that classpath resource does not exist."
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
        if (solverConfig.getEntityClassList() == null) {
            solverConfig.setEntityClassList(entityScanner.findEntityClassList());
        } else {
            long entityClassCount = solverConfig.getEntityClassList().stream()
                    .filter(Objects::nonNull)
                    .count();
            if (entityClassCount == 0L) {
                throw new IllegalStateException(
                        """
                                The solverConfig's entityClassList (%s) does not contain any non-null entries.
                                Maybe the classes listed there do not actually exist and therefore deserialization turned them to null?"""
                                .formatted(solverConfig.getEntityClassList().stream().map(Class::getSimpleName)
                                        .collect(joining(", "))));
            }
        }
        applyScoreDirectorFactoryProperties(entityScanner, solverConfig);
        Optional<SolverProperties> solverProperties = timefoldProperties.getSolverConfig(solverName);
        if (solverProperties.isPresent()) {
            if (solverProperties.get().getEnvironmentMode() != null) {
                solverConfig.setEnvironmentMode(solverProperties.get().getEnvironmentMode());
            }
            if (solverProperties.get().getDomainAccessType() != null) {
                solverConfig.setDomainAccessType(solverProperties.get().getDomainAccessType());
            }
            if (solverProperties.get().getDaemon() != null) {
                solverConfig.setDaemon(solverProperties.get().getDaemon());
            }
            if (solverProperties.get().getMoveThreadCount() != null) {
                solverConfig.setMoveThreadCount(solverProperties.get().getMoveThreadCount());
            }
            applyTerminationProperties(solverConfig, solverProperties.get().getTermination());
        }
    }

    protected void applyScoreDirectorFactoryProperties(IncludeAbstractClassesEntityScanner entityScanner,
            SolverConfig solverConfig) {
        if (solverConfig.getScoreDirectorFactoryConfig() == null) {
            solverConfig.setScoreDirectorFactoryConfig(defaultScoreDirectoryFactoryConfig(entityScanner));
        }
    }

    private ScoreDirectorFactoryConfig defaultScoreDirectoryFactoryConfig(IncludeAbstractClassesEntityScanner entityScanner) {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig
                .setEasyScoreCalculatorClass(entityScanner.findFirstImplementingClass(EasyScoreCalculator.class));
        scoreDirectorFactoryConfig
                .setConstraintProviderClass(entityScanner.findFirstImplementingClass(ConstraintProvider.class));
        scoreDirectorFactoryConfig
                .setIncrementalScoreCalculatorClass(entityScanner.findFirstImplementingClass(IncrementalScoreCalculator.class));

        return scoreDirectorFactoryConfig;
    }

    static void applyTerminationProperties(SolverConfig solverConfig, TerminationProperties terminationProperties) {
        TerminationConfig terminationConfig = solverConfig.getTerminationConfig();
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
        }
    }

    private void failInjectionWithMultipleSolvers(String resourceName) {
        if (timefoldProperties.getSolver() != null && timefoldProperties.getSolver().size() > 1) {
            throw new BeanCreationException(
                    "No qualifying bean of type '%s' available".formatted(resourceName));
        }
    }

    private void assertSolverConfigSolutionClasses(IncludeAbstractClassesEntityScanner entityScanner,
            Map<String, SolverConfig> solverConfigMap) {
        // Validate the solution class
        // No solution class
        String emptyListErrorMessage = """
                No classes were found with a @%s annotation.
                Maybe your @%s annotated class is not in a subpackage of your @%s annotated class's package.
                Maybe move your planning solution class to your application class's (sub)package (or use @%s).""".formatted(
                PlanningSolution.class.getSimpleName(), PlanningSolution.class.getSimpleName(),
                SpringBootApplication.class.getSimpleName(), EntityScan.class.getSimpleName());
        assertEmptyInstances(entityScanner, PlanningSolution.class, emptyListErrorMessage);
        // Multiple classes and single solver
        try {
            Set<Class<?>> annotationInstanceSet = entityScanner.scan(PlanningSolution.class);
            if (annotationInstanceSet.size() > 1 && solverConfigMap.size() == 1) {
                throw new IllegalStateException(
                        "Multiple classes ([%s]) found in the classpath with a @%s annotation.".formatted(
                                annotationInstanceSet.stream().map(Class::getSimpleName).collect(joining(", ")),
                                PlanningSolution.class.getSimpleName()));
            }
            // Multiple classes and at least one solver config does not specify the solution class
            List<String> solverConfigWithoutSolutionClassList = solverConfigMap.entrySet().stream()
                    .filter(e -> e.getValue().getSolutionClass() == null)
                    .map(Map.Entry::getKey)
                    .toList();
            if (annotationInstanceSet.size() > 1 && !solverConfigWithoutSolutionClassList.isEmpty()) {
                throw new IllegalStateException(
                        """
                                Some solver configs (%s) don't specify a %s class, yet there are multiple available (%s) on the classpath.
                                Maybe set the XML config file to the related solver configs, or add the missing solution classes to the XML files,
                                or remove the unnecessary solution classes from the classpath."""
                                .formatted(String.join(", ", solverConfigWithoutSolutionClassList),
                                        PlanningSolution.class.getSimpleName(),
                                        annotationInstanceSet.stream().map(Class::getSimpleName).collect(joining(", "))));
            }
            // Unused solution classes
            List<String> unusedSolutionClassList = annotationInstanceSet.stream()
                    .map(Class::getName)
                    .filter(planningClassName -> solverConfigMap.values().stream().filter(c -> c.getSolutionClass() != null)
                            .noneMatch(c -> c.getSolutionClass().getName().equals(planningClassName)))
                    .toList();
            if (annotationInstanceSet.size() > 1 && !unusedSolutionClassList.isEmpty()) {
                throw new IllegalStateException(
                        "Unused classes ([%s]) found with a @%s annotation.".formatted(
                                String.join(", ", unusedSolutionClassList),
                                PlanningSolution.class.getSimpleName()));
            }
            // TODO - Should we validate target types?
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Scanning for @%s annotations failed.".formatted(PlanningSolution.class.getSimpleName()), e);
        }
    }

    private void assertSolverConfigEntityClasses(IncludeAbstractClassesEntityScanner entityScanner) {
        // No Entity class
        String emptyListErrorMessage = """
                No classes were found with a @%s annotation.
                Maybe your @%s annotated class(es) are not in a subpackage of your @%s annotated class's package.
                Maybe move your planning entity classes to your application class's (sub)package(or use @%s)."""
                .formatted(
                        PlanningEntity.class.getSimpleName(), PlanningEntity.class.getSimpleName(),
                        SpringBootApplication.class.getSimpleName(), EntityScan.class.getSimpleName());
        assertEmptyInstances(entityScanner, PlanningEntity.class, emptyListErrorMessage);
        // TODO - Should we validate target types?
    }

    private void assertSolverConfigConstraintClasses(
            IncludeAbstractClassesEntityScanner entityScanner, Map<String, SolverConfig> solverConfigMap) {
        List<Class<? extends EasyScoreCalculator>> simpleScoreClassList =
                entityScanner.findImplementingClassList(EasyScoreCalculator.class);
        List<Class<? extends ConstraintProvider>> constraintScoreClassList =
                entityScanner.findImplementingClassList(ConstraintProvider.class);
        List<Class<? extends IncrementalScoreCalculator>> incrementalScoreClassList =
                entityScanner.findImplementingClassList(IncrementalScoreCalculator.class);
        // No score classes
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
        // Multiple classes and single solver
        String errorMessage = "Multiple score classes classes (%s) that implements %s were found in the classpath.";
        if (simpleScoreClassList.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    simpleScoreClassList.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")),
                    EasyScoreCalculator.class.getSimpleName()));
        }
        if (constraintScoreClassList.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    constraintScoreClassList.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")),
                    ConstraintProvider.class.getSimpleName()));
        }
        if (incrementalScoreClassList.size() > 1 && solverConfigMap.size() == 1) {
            throw new IllegalStateException(errorMessage.formatted(
                    incrementalScoreClassList.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")),
                    IncrementalScoreCalculator.class.getSimpleName()));
        }
        // Multiple classes and at least one solver config does not specify the score class
        errorMessage = """
                Some solver configs (%s) don't specify a %s score class, yet there are multiple available (%s) on the classpath.
                Maybe set the XML config file to the related solver configs, or add the missing score classes to the XML files,
                or remove the unnecessary score classes from the classpath.""";
        List<String> solverConfigWithoutConstraintClassList = solverConfigMap.entrySet().stream()
                .filter(e -> e.getValue().getScoreDirectorFactoryConfig() == null
                        || e.getValue().getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (simpleScoreClassList.size() > 1 && !solverConfigWithoutConstraintClassList.isEmpty()) {
            throw new IllegalStateException(errorMessage.formatted(
                    String.join(", ", solverConfigWithoutConstraintClassList),
                    EasyScoreCalculator.class.getSimpleName(),
                    simpleScoreClassList.stream().map(Class::getSimpleName).collect(Collectors.joining(", "))));
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
                    constraintScoreClassList.stream().map(Class::getSimpleName).collect(Collectors.joining(", "))));
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
                    incrementalScoreClassList.stream().map(Class::getSimpleName).collect(Collectors.joining(", "))));
        }
        // Unused score classes
        List<String> solverConfigWithUnusedSolutionClassList = simpleScoreClassList.stream()
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

    private void assertEmptyInstances(IncludeAbstractClassesEntityScanner entityScanner, Class<? extends Annotation> clazz,
            String errorMessage) {
        try {
            Collection<Class<?>> classInstanceCollection = entityScanner.scan(clazz);
            if (classInstanceCollection.isEmpty()) {
                throw new IllegalStateException(errorMessage);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @%s annotations failed.".formatted(clazz.getSimpleName()), e);
        }
    }

    @Bean
    @Lazy
    public TimefoldSolverBannerBean getBanner() {
        return new TimefoldSolverBannerBean();
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public <Solution_> SolverFactory<Solution_> getSolverFactory() {
        failInjectionWithMultipleSolvers(SolverFactory.class.getName());
        SolverConfig solverConfig = context.getBean(SolverConfig.class);
        if (solverConfig == null || solverConfig.getSolutionClass() == null) {
            return null;
        }
        return SolverFactory.create(solverConfig);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public <Solution_, ProblemId_> SolverManager<Solution_, ProblemId_> solverManager(SolverFactory solverFactory) {
        // TODO supply ThreadFactory
        if (solverFactory == null) {
            return null;
        }
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
        SolverManagerProperties solverManagerProperties = timefoldProperties.getSolverManager();
        if (solverManagerProperties != null && solverManagerProperties.getParallelSolverCount() != null) {
            solverManagerConfig.setParallelSolverCount(solverManagerProperties.getParallelSolverCount());
        }
        return SolverManager.create(solverFactory, solverManagerConfig);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    @Deprecated(forRemoval = true)
    public <Solution_, Score_ extends Score<Score_>> ScoreManager<Solution_, Score_> scoreManager() {
        failInjectionWithMultipleSolvers(ScoreManager.class.getName());
        SolverFactory solverFactory = context.getBean(SolverFactory.class);
        if (solverFactory == null) {
            return null;
        }
        return ScoreManager.create(solverFactory);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_> solutionManager() {
        failInjectionWithMultipleSolvers(SolutionManager.class.getName());
        SolverFactory solverFactory = context.getBean(SolverFactory.class);
        if (solverFactory == null) {
            return null;
        }
        return SolutionManager.create(solverFactory);
    }

    // @Bean wrapped by static class to avoid classloading issues if dependencies are absent
    @ConditionalOnClass({ ConstraintVerifier.class })
    @ConditionalOnMissingBean({ ConstraintVerifier.class })
    @AutoConfigureAfter(TimefoldAutoConfiguration.class)
    class TimefoldConstraintVerifierConfiguration {

        private final ApplicationContext context;

        protected TimefoldConstraintVerifierConfiguration(ApplicationContext context) {
            this.context = context;
        }

        @Bean
        @Lazy
        @SuppressWarnings("unchecked")
        <ConstraintProvider_ extends ConstraintProvider, SolutionClass_>
                ConstraintVerifier<ConstraintProvider_, SolutionClass_> constraintVerifier() {
            // Using SolverConfig as an injected parameter here leads to an injection failure on an empty app,
            // so we need to get the SolverConfig from context
            failInjectionWithMultipleSolvers(ConstraintProvider.class.getName());
            SolverConfig solverConfig;
            try {
                solverConfig = context.getBean(SolverConfig.class);
            } catch (BeansException exception) {
                solverConfig = null;
            }

            ScoreDirectorFactoryConfig scoreDirectorFactoryConfig =
                    (solverConfig != null) ? solverConfig.getScoreDirectorFactoryConfig() : null;
            if (scoreDirectorFactoryConfig == null || scoreDirectorFactoryConfig.getConstraintProviderClass() == null) {
                // Return a mock ConstraintVerifier so not having ConstraintProvider doesn't crash tests
                // (Cannot create custom condition that checks SolverConfig, since that
                //  requires TimefoldAutoConfiguration to have a no-args constructor)
                final String noConstraintProviderErrorMsg = (scoreDirectorFactoryConfig != null)
                        ? "Cannot provision a ConstraintVerifier because there is no ConstraintProvider class."
                        : "Cannot provision a ConstraintVerifier because there is no PlanningSolution or PlanningEntity classes.";
                return new ConstraintVerifier<>() {
                    @Override
                    public ConstraintVerifier<ConstraintProvider_, SolutionClass_>
                            withConstraintStreamImplType(ConstraintStreamImplType constraintStreamImplType) {
                        throw new UnsupportedOperationException(noConstraintProviderErrorMsg);
                    }

                    @Override
                    public SingleConstraintVerification<SolutionClass_>
                            verifyThat(BiFunction<ConstraintProvider_, ConstraintFactory, Constraint> constraintFunction) {
                        throw new UnsupportedOperationException(noConstraintProviderErrorMsg);
                    }

                    @Override
                    public MultiConstraintVerification<SolutionClass_> verifyThat() {
                        throw new UnsupportedOperationException(noConstraintProviderErrorMsg);
                    }
                };
            }

            return ConstraintVerifier.create(solverConfig);
        }
    }

    // @Bean wrapped by static class to avoid classloading issues if dependencies are absent
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ Jackson2ObjectMapperBuilder.class, Score.class })
    static class TimefoldJacksonConfiguration {

        @Bean
        Module jacksonModule() {
            return TimefoldJacksonModule.createModule();
        }

    }
}
