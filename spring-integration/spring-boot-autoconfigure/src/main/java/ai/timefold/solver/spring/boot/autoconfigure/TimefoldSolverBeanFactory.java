package ai.timefold.solver.spring.boot.autoconfigure;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreManager;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.jackson.api.TimefoldJacksonModule;
import ai.timefold.solver.spring.boot.autoconfigure.config.SolverManagerProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import ai.timefold.solver.test.api.score.stream.MultiConstraintVerification;
import ai.timefold.solver.test.api.score.stream.SingleConstraintVerification;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

/**
 * Must be seperated from {@link TimefoldSolverAutoConfiguration} since
 * {@link TimefoldSolverAutoConfiguration} will not be available at runtime
 * for a native image (since it is a {@link BeanFactoryInitializationAotProcessor}/
 * {@link BeanFactoryPostProcessor}).
 */
@Configuration
public class TimefoldSolverBeanFactory implements ApplicationContextAware, EnvironmentAware {
    private ApplicationContext context;
    private TimefoldProperties timefoldProperties;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public void setEnvironment(Environment environment) {
        // We need the environment to set run time properties of SolverFactory and SolverManager
        BindResult<TimefoldProperties> result = Binder.get(environment).bind("timefold", TimefoldProperties.class);
        this.timefoldProperties = result.orElseGet(TimefoldProperties::new);
    }

    private void failInjectionWithMultipleSolvers(String resourceName) {
        if (timefoldProperties.getSolver() != null && timefoldProperties.getSolver().size() > 1) {
            throw new BeanCreationException(
                    "No qualifying bean of type '%s' available".formatted(resourceName));
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
        if (solverConfig.getSolutionClass() == null) {
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
    /**
     * @deprecated Use {@link SolutionManager} instead.
     */
    public <Solution_, Score_ extends Score<Score_>> ScoreManager<Solution_, Score_> scoreManager() {
        failInjectionWithMultipleSolvers(ScoreManager.class.getName());
        SolverFactory<Solution_> solverFactory = context.getBean(SolverFactory.class);
        return ScoreManager.create(solverFactory);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_> solutionManager() {
        failInjectionWithMultipleSolvers(SolutionManager.class.getName());
        SolverFactory<Solution_> solverFactory = context.getBean(SolverFactory.class);
        return SolutionManager.create(solverFactory);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public <Solution_> ConstraintMetaModel constraintMetaModel() {
        failInjectionWithMultipleSolvers(ConstraintMetaModel.class.getName());
        var solverFactory = (DefaultSolverFactory<Solution_>) context.getBean(SolverFactory.class);
        var scoreDirectorFactory = solverFactory.getScoreDirectorFactory();
        if (scoreDirectorFactory instanceof AbstractConstraintStreamScoreDirectorFactory<Solution_, ?> castScoreDirectorFactory) {
            return castScoreDirectorFactory.getConstraintMetaModel();
        } else {
            throw new IllegalStateException(
                    "Cannot provide %s because the score director does not use the Constraint Streams API."
                            .formatted(ConstraintMetaModel.class.getSimpleName()));
        }
    }

    // @Bean wrapped by static class to avoid classloading issues if dependencies are absent
    @ConditionalOnClass({ ConstraintVerifier.class })
    @ConditionalOnMissingBean({ ConstraintVerifier.class })
    @AutoConfigureAfter(TimefoldSolverAutoConfiguration.class)
    class TimefoldConstraintVerifierConfiguration {

        private final ApplicationContext context;

        protected TimefoldConstraintVerifierConfiguration(ApplicationContext context) {
            this.context = context;
        }

        private static class UnsupportedConstraintVerifier<ConstraintProvider_ extends ConstraintProvider, SolutionClass_>
                implements ConstraintVerifier<ConstraintProvider_, SolutionClass_> {
            final String errorMessage;

            public UnsupportedConstraintVerifier(String errorMessage) {
                this.errorMessage = errorMessage;
            }

            @Override
            public ConstraintVerifier<ConstraintProvider_, SolutionClass_>
                    withConstraintStreamImplType(ConstraintStreamImplType constraintStreamImplType) {
                throw new UnsupportedOperationException(errorMessage);
            }

            @Override
            public SingleConstraintVerification<SolutionClass_>
                    verifyThat(BiFunction<ConstraintProvider_, ConstraintFactory, Constraint> constraintFunction) {
                throw new UnsupportedOperationException(errorMessage);
            }

            @Override
            public MultiConstraintVerification<SolutionClass_> verifyThat() {
                throw new UnsupportedOperationException(errorMessage);
            }
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
                //  requires TimefoldSolverAutoConfiguration to have a no-args constructor)
                final String noConstraintProviderErrorMsg = (scoreDirectorFactoryConfig != null)
                        ? "Cannot provision a ConstraintVerifier because there is no ConstraintProvider class."
                        : "Cannot provision a ConstraintVerifier because there is no PlanningSolution or PlanningEntity classes.";
                return new UnsupportedConstraintVerifier<>(noConstraintProviderErrorMsg);
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
