package ai.timefold.solver.spring.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.databind.Module;

@Configuration
@ConditionalOnClass({ SolverConfig.class, SolverFactory.class, ScoreManager.class, SolutionManager.class, SolverManager.class })
@ConditionalOnMissingBean({ SolverConfig.class, SolverFactory.class, ScoreManager.class, SolutionManager.class,
        SolverManager.class })
@EnableConfigurationProperties({ TimefoldProperties.class })
public class TimefoldAutoConfiguration implements BeanClassLoaderAware {

    private final ApplicationContext context;
    private final TimefoldProperties timefoldProperties;
    private ClassLoader beanClassLoader;

    protected TimefoldAutoConfiguration(ApplicationContext context,
            TimefoldProperties timefoldProperties) {
        this.context = context;
        this.timefoldProperties = timefoldProperties;
    }

    @Override
    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
    }

    @Bean
    @ConditionalOnMissingBean
    public <Solution_, ProblemId_> SolverManager<Solution_, ProblemId_> solverManager(SolverFactory solverFactory) {
        // TODO supply ThreadFactory
        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
        SolverManagerProperties solverManagerProperties = timefoldProperties.getSolverManager();
        if (solverManagerProperties != null) {
            if (solverManagerProperties.getParallelSolverCount() != null) {
                solverManagerConfig.setParallelSolverCount(solverManagerProperties.getParallelSolverCount());
            }
        }
        return SolverManager.create(solverFactory, solverManagerConfig);
    }

    @Deprecated(forRemoval = true)
    @Bean
    @ConditionalOnMissingBean
    public <Solution_, Score_ extends Score<Score_>> ScoreManager<Solution_, Score_> scoreManager(
            SolverFactory solverFactory) {
        return ScoreManager.create(solverFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_> solutionManager(
            SolverFactory solverFactory) {
        return SolutionManager.create(solverFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public <Solution_> SolverFactory<Solution_> solverFactory(SolverConfig solverConfig) {
        return SolverFactory.create(solverConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public SolverConfig solverConfig() {
        String solverConfigXml = timefoldProperties.getSolverConfigXml();
        SolverConfig solverConfig;
        if (solverConfigXml != null) {
            if (beanClassLoader.getResource(solverConfigXml) == null) {
                throw new IllegalStateException("Invalid timefold.solverConfigXml property (" + solverConfigXml
                        + "): that classpath resource does not exist.");
            }
            solverConfig = SolverConfig.createFromXmlResource(solverConfigXml, beanClassLoader);
        } else if (beanClassLoader.getResource(TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL) != null) {
            solverConfig = SolverConfig.createFromXmlResource(
                    TimefoldProperties.DEFAULT_SOLVER_CONFIG_URL, beanClassLoader);
        } else {
            solverConfig = new SolverConfig(beanClassLoader);
        }

        applySolverProperties(solverConfig);
        return solverConfig;
    }

    // @Bean wrapped by static class to avoid classloading issues if dependencies are absent
    @ConditionalOnClass({ ConstraintVerifier.class })
    @ConditionalOnMissingBean({ ConstraintVerifier.class })
    static class TimefoldConstraintVerifierConfiguration {

        @Bean
        @SuppressWarnings("unchecked")
        <ConstraintProvider_ extends ConstraintProvider, SolutionClass_>
                ConstraintVerifier<ConstraintProvider_, SolutionClass_> constraintVerifier(SolverConfig solverConfig) {
            ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = solverConfig.getScoreDirectorFactoryConfig();
            if (scoreDirectorFactoryConfig.getConstraintProviderClass() == null) {
                // Return a mock ConstraintVerifier so not having ConstraintProvider doesn't crash tests
                // (Cannot create custom condition that checks SolverConfig, since that
                //  requires TimefoldAutoConfiguration to have a no-args constructor)
                final String noConstraintProviderErrorMsg =
                        "Cannot provision a ConstraintVerifier because there is no ConstraintProvider class.";
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

    private void applySolverProperties(SolverConfig solverConfig) {
        IncludeAbstractClassesEntityScanner entityScanner = new IncludeAbstractClassesEntityScanner(this.context);
        if (solverConfig.getSolutionClass() == null) {
            solverConfig.setSolutionClass(findSolutionClass(entityScanner));
        }
        if (solverConfig.getEntityClassList() == null) {
            solverConfig.setEntityClassList(findEntityClassList(entityScanner));
        } else {
            long entityClassCount = solverConfig.getEntityClassList().stream()
                    .filter(Objects::nonNull)
                    .count();
            if (entityClassCount == 0L) {
                throw new IllegalStateException("The solverConfig's entityClassList (" + solverConfig.getEntityClassList()
                        + ") does not contain any non-null entries.\n"
                        + "Maybe the classes listed there do not actually exist and therefore deserialization turned them to null?\n");
            }
        }
        applyScoreDirectorFactoryProperties(solverConfig);
        SolverProperties solverProperties = timefoldProperties.getSolver();
        if (solverProperties != null) {
            if (solverProperties.getEnvironmentMode() != null) {
                solverConfig.setEnvironmentMode(solverProperties.getEnvironmentMode());
            }
            if (solverProperties.getDomainAccessType() != null) {
                solverConfig.setDomainAccessType(solverProperties.getDomainAccessType());
            }
            if (solverProperties.getDaemon() != null) {
                solverConfig.setDaemon(solverProperties.getDaemon());
            }
            if (solverProperties.getMoveThreadCount() != null) {
                solverConfig.setMoveThreadCount(solverProperties.getMoveThreadCount());
            }
            applyTerminationProperties(solverConfig, solverProperties.getTermination());
        }
    }

    private Class<?> findSolutionClass(IncludeAbstractClassesEntityScanner entityScanner) {
        Set<Class<?>> solutionClassSet;
        try {
            solutionClassSet = entityScanner.scan(PlanningSolution.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @" + PlanningSolution.class.getSimpleName()
                    + " annotations failed.", e);
        }
        if (solutionClassSet.size() > 1) {
            throw new IllegalStateException("Multiple classes (" + solutionClassSet
                    + ") found with a @" + PlanningSolution.class.getSimpleName() + " annotation.");
        }
        if (solutionClassSet.isEmpty()) {
            throw new IllegalStateException("No classes (" + solutionClassSet
                    + ") found with a @" + PlanningSolution.class.getSimpleName() + " annotation.\n"
                    + "Maybe your @" + PlanningSolution.class.getSimpleName() + " annotated class "
                    + " is not in a subpackage of your @" + SpringBootApplication.class.getSimpleName()
                    + " annotated class's package.\n"
                    + "Maybe move your planning solution class to your application class's (sub)package"
                    + " (or use @" + EntityScan.class.getSimpleName() + ").");
        }
        return solutionClassSet.iterator().next();
    }

    private List<Class<?>> findEntityClassList(IncludeAbstractClassesEntityScanner entityScanner) {
        Set<Class<?>> entityClassSet;
        try {
            entityClassSet = entityScanner.scan(PlanningEntity.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Scanning for @" + PlanningEntity.class.getSimpleName() + " failed.", e);
        }
        if (entityClassSet.isEmpty()) {
            throw new IllegalStateException("No classes (" + entityClassSet
                    + ") found with a @" + PlanningEntity.class.getSimpleName() + " annotation.\n"
                    + "Maybe your @" + PlanningEntity.class.getSimpleName() + " annotated class(es) "
                    + " are not in a subpackage of your @" + SpringBootApplication.class.getSimpleName()
                    + " annotated class's package.\n"
                    + "Maybe move your planning entity classes to your application class's (sub)package"
                    + " (or use @" + EntityScan.class.getSimpleName() + ").");
        }
        return new ArrayList<>(entityClassSet);
    }

    protected void applyScoreDirectorFactoryProperties(SolverConfig solverConfig) {
        if (solverConfig.getScoreDirectorFactoryConfig() == null) {
            solverConfig.setScoreDirectorFactoryConfig(defaultScoreDirectoryFactoryConfig());
        }
    }

    private ScoreDirectorFactoryConfig defaultScoreDirectoryFactoryConfig() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(findImplementingClass(EasyScoreCalculator.class));
        scoreDirectorFactoryConfig.setConstraintProviderClass(findImplementingClass(ConstraintProvider.class));
        scoreDirectorFactoryConfig
                .setIncrementalScoreCalculatorClass(findImplementingClass(IncrementalScoreCalculator.class));

        if (scoreDirectorFactoryConfig.getEasyScoreCalculatorClass() == null
                && scoreDirectorFactoryConfig.getConstraintProviderClass() == null
                && scoreDirectorFactoryConfig.getIncrementalScoreCalculatorClass() == null) {
            throw new IllegalStateException("No classes found that implement "
                    + EasyScoreCalculator.class.getSimpleName() + ", "
                    + ConstraintProvider.class.getSimpleName() + " or "
                    + IncrementalScoreCalculator.class.getSimpleName() + ".\n"
                    + "Maybe your " + ConstraintProvider.class.getSimpleName() + " class "
                    + " is not in a subpackage of your @" + SpringBootApplication.class.getSimpleName()
                    + " annotated class's package.\n"
                    + "Maybe move your " + ConstraintProvider.class.getSimpleName()
                    + " class to your application class's (sub)package"
                    + " (or use @" + EntityScan.class.getSimpleName() + ").");
        }
        return scoreDirectorFactoryConfig;
    }

    private <T> Class<? extends T> findImplementingClass(Class<T> targetClass) {
        if (!AutoConfigurationPackages.has(context)) {
            return null;
        }
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.setEnvironment(context.getEnvironment());
        scanner.setResourceLoader(context);
        scanner.addIncludeFilter(new AssignableTypeFilter(targetClass));

        EntityScanPackages entityScanPackages = EntityScanPackages.get(context);

        Set<String> packages = new HashSet<>();
        packages.addAll(AutoConfigurationPackages.get(context));
        packages.addAll(entityScanPackages.getPackageNames());
        List<Class<? extends T>> classList = packages.stream()
                .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
                // findCandidateComponents can return the same package for different base packages
                .distinct()
                .sorted(Comparator.comparing(BeanDefinition::getBeanClassName))
                .map(candidate -> {
                    try {
                        Class<? extends T> clazz = ClassUtils.forName(candidate.getBeanClassName(), context.getClassLoader())
                                .asSubclass(targetClass);
                        return clazz;
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("The " + targetClass.getSimpleName() + " class ("
                                + candidate.getBeanClassName() + ") cannot be found.", e);
                    }
                })
                .collect(Collectors.toList());
        if (classList.size() > 1) {
            throw new IllegalStateException("Multiple classes (" + classList
                    + ") found that implement the interface " + targetClass.getSimpleName() + ".");
        }
        if (classList.isEmpty()) {
            return null;
        }
        return classList.get(0);
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

    // @Bean wrapped by static class to avoid classloading issues if dependencies are absent
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ Jackson2ObjectMapperBuilder.class, Score.class })
    static class TimefoldJacksonConfiguration {

        @Bean
        Module jacksonModule() {
            return TimefoldJacksonModule.createModule();
        }

    }

    private static class IncludeAbstractClassesEntityScanner extends EntityScanner {

        public IncludeAbstractClassesEntityScanner(ApplicationContext context) {
            super(context);
        }

        @Override
        protected ClassPathScanningCandidateComponentProvider
                createClassPathScanningCandidateComponentProvider(ApplicationContext context) {
            return new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    AnnotationMetadata metadata = beanDefinition.getMetadata();
                    // Do not exclude abstract classes nor interfaces
                    return metadata.isIndependent();
                }
            };
        }

    }

}
