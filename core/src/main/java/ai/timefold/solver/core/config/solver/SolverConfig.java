package ai.timefold.solver.core.config.solver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.NoChangePhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.random.RandomType;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.core.impl.io.jaxb.TimefoldXmlSerializationException;
import ai.timefold.solver.core.impl.phase.PhaseFactory;
import ai.timefold.solver.core.impl.solver.random.RandomFactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * To read it from XML, use {@link #createFromXmlResource(String)}.
 * To build a {@link SolverFactory} with it, use {@link SolverFactory#create(SolverConfig)}.
 */
@XmlRootElement(name = SolverConfig.XML_ELEMENT_NAME)
@XmlType(name = SolverConfig.XML_TYPE_NAME, propOrder = {
        "enablePreviewFeatureSet",
        "environmentMode",
        "daemon",
        "randomType",
        "randomSeed",
        "randomFactoryClass",
        "moveThreadCount",
        "moveThreadBufferSize",
        "threadFactoryClass",
        "monitoringConfig",
        "solutionClass",
        "entityClassList",
        "domainAccessType",
        "scoreDirectorFactoryConfig",
        "terminationConfig",
        "nearbyDistanceMeterClass",
        "phaseConfigList",
})
public class SolverConfig extends AbstractConfig<SolverConfig> {

    public static final String XML_ELEMENT_NAME = "solver";
    public static final String XML_NAMESPACE = "https://timefold.ai/xsd/solver";
    public static final String XML_TYPE_NAME = "solverConfig";

    /**
     * Reads an XML solver configuration from the classpath.
     *
     * @param solverConfigResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull SolverConfig createFromXmlResource(@NonNull String solverConfigResource) {
        return createFromXmlResource(solverConfigResource, null);
    }

    /**
     * As defined by {@link #createFromXmlResource(String)}.
     *
     * @param solverConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull SolverConfig createFromXmlResource(@NonNull String solverConfigResource,
            @Nullable ClassLoader classLoader) {
        ClassLoader actualClassLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        try (InputStream in = actualClassLoader.getResourceAsStream(solverConfigResource)) {
            if (in == null) {
                String errorMessage = "The solverConfigResource (" + solverConfigResource
                        + ") does not exist as a classpath resource in the classLoader (" + actualClassLoader + ").";
                if (solverConfigResource.startsWith("/")) {
                    errorMessage += "\nA classpath resource should not start with a slash (/)."
                            + " A solverConfigResource adheres to ClassLoader.getResource(String)."
                            + " Maybe remove the leading slash from the solverConfigResource.";
                }
                throw new IllegalArgumentException(errorMessage);
            }
            return createFromXmlInputStream(in, classLoader);
        } catch (TimefoldXmlSerializationException e) {
            throw new IllegalArgumentException("Unmarshalling of solverConfigResource (" + solverConfigResource + ") fails.",
                    e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the solverConfigResource (" + solverConfigResource + ") fails.", e);
        }
    }

    /**
     * Reads an XML solver configuration from the file system.
     * <p>
     * Warning: this leads to platform dependent code,
     * it's recommend to use {@link #createFromXmlResource(String)} instead.
     *
     */
    public static @NonNull SolverConfig createFromXmlFile(@NonNull File solverConfigFile) {
        return createFromXmlFile(solverConfigFile, null);
    }

    /**
     * As defined by {@link #createFromXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull SolverConfig createFromXmlFile(@NonNull File solverConfigFile, @Nullable ClassLoader classLoader) {
        try (InputStream in = new FileInputStream(solverConfigFile)) {
            return createFromXmlInputStream(in, classLoader);
        } catch (TimefoldXmlSerializationException e) {
            throw new IllegalArgumentException("Unmarshalling the solverConfigFile (" + solverConfigFile + ") fails.", e);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The solverConfigFile (" + solverConfigFile + ") was not found.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the solverConfigFile (" + solverConfigFile + ") fails.", e);
        }
    }

    /**
     * @param in gets closed
     */
    public static @NonNull SolverConfig createFromXmlInputStream(@NonNull InputStream in) {
        return createFromXmlInputStream(in, null);
    }

    /**
     * As defined by {@link #createFromXmlInputStream(InputStream)}.
     *
     * @param in gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull SolverConfig createFromXmlInputStream(@NonNull InputStream in, @Nullable ClassLoader classLoader) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return createFromXmlReader(reader, classLoader);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("This vm does not support the charset (" + StandardCharsets.UTF_8 + ").", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading solverConfigInputStream fails.", e);
        }
    }

    /**
     * @param reader gets closed
     */
    public static @NonNull SolverConfig createFromXmlReader(@NonNull Reader reader) {
        return createFromXmlReader(reader, null);
    }

    /**
     * As defined by {@link #createFromXmlReader(Reader)}.
     *
     * @param reader gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull SolverConfig createFromXmlReader(@NonNull Reader reader, @Nullable ClassLoader classLoader) {
        SolverConfigIO solverConfigIO = new SolverConfigIO();
        SolverConfig solverConfig = solverConfigIO.read(reader);
        solverConfig.setClassLoader(classLoader);
        return solverConfig;
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    public static final String MOVE_THREAD_COUNT_NONE = "NONE";
    public static final String MOVE_THREAD_COUNT_AUTO = "AUTO";

    @XmlTransient
    private Clock clock = null;
    @XmlTransient
    private ClassLoader classLoader = null;

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file
    @XmlElement(name = "enablePreviewFeature")
    protected Set<PreviewFeature> enablePreviewFeatureSet = null;
    protected EnvironmentMode environmentMode = null;
    protected Boolean daemon = null;
    protected RandomType randomType = null;
    protected Long randomSeed = null;
    protected Class<? extends RandomFactory> randomFactoryClass = null;
    protected String moveThreadCount = null;
    protected Integer moveThreadBufferSize = null;
    protected Class<? extends ThreadFactory> threadFactoryClass = null;

    protected Class<?> solutionClass = null;

    @XmlElement(name = "entityClass")
    protected List<Class<?>> entityClassList = null;
    protected DomainAccessType domainAccessType = null;
    @XmlTransient
    protected Map<String, MemberAccessor> gizmoMemberAccessorMap = null;
    @XmlTransient
    protected Map<String, SolutionCloner> gizmoSolutionClonerMap = null;

    @XmlElement(name = "scoreDirectorFactory")
    protected ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = null;

    @XmlElement(name = "termination")
    private TerminationConfig terminationConfig;

    protected Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass = null;

    @XmlElements({
            @XmlElement(name = ConstructionHeuristicPhaseConfig.XML_ELEMENT_NAME,
                    type = ConstructionHeuristicPhaseConfig.class),
            @XmlElement(name = CustomPhaseConfig.XML_ELEMENT_NAME, type = CustomPhaseConfig.class),
            @XmlElement(name = ExhaustiveSearchPhaseConfig.XML_ELEMENT_NAME, type = ExhaustiveSearchPhaseConfig.class),
            @XmlElement(name = LocalSearchPhaseConfig.XML_ELEMENT_NAME, type = LocalSearchPhaseConfig.class),
            @XmlElement(name = NoChangePhaseConfig.XML_ELEMENT_NAME, type = NoChangePhaseConfig.class),
            @XmlElement(name = PartitionedSearchPhaseConfig.XML_ELEMENT_NAME, type = PartitionedSearchPhaseConfig.class)
    })
    protected List<PhaseConfig> phaseConfigList = null;

    @XmlElement(name = "monitoring")
    protected MonitoringConfig monitoringConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    /**
     * Create an empty solver config.
     */
    public SolverConfig() {
    }

    /**
     * For testing purposes only.
     */
    public SolverConfig(@NonNull Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public SolverConfig(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Allows you to programmatically change the {@link SolverConfig} per concurrent request,
     * based on a template solver config,
     * by building a separate {@link SolverFactory} with {@link SolverFactory#create(SolverConfig)}
     * and a separate {@link Solver} per request to avoid race conditions.
     *
     */
    public SolverConfig(@NonNull SolverConfig inheritedConfig) {
        inherit(inheritedConfig);
    }

    /**
     * For testing purposes only.
     *
     * @return null if system default should be used
     */
    public @Nullable Clock getClock() {
        return clock;
    }

    /**
     * For testing purposes only.
     */
    public void setClock(@Nullable Clock clock) {
        this.clock = clock;
    }

    public @Nullable ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public @Nullable Set<PreviewFeature> getEnablePreviewFeatureSet() {
        return enablePreviewFeatureSet;
    }

    public void setEnablePreviewFeatureSet(@Nullable Set<PreviewFeature> enablePreviewFeatureSet) {
        this.enablePreviewFeatureSet = enablePreviewFeatureSet;
    }

    public @Nullable EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    public void setEnvironmentMode(@Nullable EnvironmentMode environmentMode) {
        this.environmentMode = environmentMode;
    }

    public @Nullable Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(@Nullable Boolean daemon) {
        this.daemon = daemon;
    }

    public @Nullable RandomType getRandomType() {
        return randomType;
    }

    public void setRandomType(@Nullable RandomType randomType) {
        this.randomType = randomType;
    }

    public @Nullable Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(@Nullable Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public @Nullable Class<? extends RandomFactory> getRandomFactoryClass() {
        return randomFactoryClass;
    }

    public void setRandomFactoryClass(@Nullable Class<? extends RandomFactory> randomFactoryClass) {
        this.randomFactoryClass = randomFactoryClass;
    }

    public @Nullable String getMoveThreadCount() {
        return moveThreadCount;
    }

    public void setMoveThreadCount(@Nullable String moveThreadCount) {
        this.moveThreadCount = moveThreadCount;
    }

    public @Nullable Integer getMoveThreadBufferSize() {
        return moveThreadBufferSize;
    }

    public void setMoveThreadBufferSize(@Nullable Integer moveThreadBufferSize) {
        this.moveThreadBufferSize = moveThreadBufferSize;
    }

    public @Nullable Class<? extends ThreadFactory> getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(@Nullable Class<? extends ThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
    }

    public @Nullable Class<?> getSolutionClass() {
        return solutionClass;
    }

    public void setSolutionClass(@Nullable Class<?> solutionClass) {
        this.solutionClass = solutionClass;
    }

    public @Nullable List<Class<?>> getEntityClassList() {
        return entityClassList;
    }

    public void setEntityClassList(@Nullable List<Class<?>> entityClassList) {
        this.entityClassList = entityClassList;
    }

    public @Nullable DomainAccessType getDomainAccessType() {
        return domainAccessType;
    }

    public void setDomainAccessType(@Nullable DomainAccessType domainAccessType) {
        this.domainAccessType = domainAccessType;
    }

    public @Nullable Map<@NonNull String, @NonNull MemberAccessor> getGizmoMemberAccessorMap() {
        return gizmoMemberAccessorMap;
    }

    public void setGizmoMemberAccessorMap(@Nullable Map<@NonNull String, @NonNull MemberAccessor> gizmoMemberAccessorMap) {
        this.gizmoMemberAccessorMap = gizmoMemberAccessorMap;
    }

    public @Nullable Map<@NonNull String, @NonNull SolutionCloner> getGizmoSolutionClonerMap() {
        return gizmoSolutionClonerMap;
    }

    public void setGizmoSolutionClonerMap(@Nullable Map<@NonNull String, @NonNull SolutionCloner> gizmoSolutionClonerMap) {
        this.gizmoSolutionClonerMap = gizmoSolutionClonerMap;
    }

    public @Nullable ScoreDirectorFactoryConfig getScoreDirectorFactoryConfig() {
        return scoreDirectorFactoryConfig;
    }

    public void setScoreDirectorFactoryConfig(@Nullable ScoreDirectorFactoryConfig scoreDirectorFactoryConfig) {
        this.scoreDirectorFactoryConfig = scoreDirectorFactoryConfig;
    }

    public @Nullable TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    public void setTerminationConfig(@Nullable TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
    }

    public @Nullable Class<? extends NearbyDistanceMeter<?, ?>> getNearbyDistanceMeterClass() {
        return nearbyDistanceMeterClass;
    }

    public void setNearbyDistanceMeterClass(@Nullable Class<? extends NearbyDistanceMeter<?, ?>> nearbyDistanceMeterClass) {
        this.nearbyDistanceMeterClass = nearbyDistanceMeterClass;
    }

    public @Nullable List<@NonNull PhaseConfig> getPhaseConfigList() {
        return phaseConfigList;
    }

    public void setPhaseConfigList(@Nullable List<@NonNull PhaseConfig> phaseConfigList) {
        this.phaseConfigList = phaseConfigList;
    }

    public @Nullable MonitoringConfig getMonitoringConfig() {
        return monitoringConfig;
    }

    public void setMonitoringConfig(@Nullable MonitoringConfig monitoringConfig) {
        this.monitoringConfig = monitoringConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SolverConfig withPreviewFeature(@NonNull PreviewFeature... previewFeature) {
        enablePreviewFeatureSet = EnumSet.copyOf(Arrays.asList(previewFeature));
        return this;
    }

    public @NonNull SolverConfig withEnvironmentMode(@NonNull EnvironmentMode environmentMode) {
        this.environmentMode = environmentMode;
        return this;
    }

    public @NonNull SolverConfig withDaemon(@NonNull Boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public @NonNull SolverConfig withRandomType(@NonNull RandomType randomType) {
        this.randomType = randomType;
        return this;
    }

    public @NonNull SolverConfig withRandomSeed(@NonNull Long randomSeed) {
        this.randomSeed = randomSeed;
        return this;
    }

    public @NonNull SolverConfig withRandomFactoryClass(@NonNull Class<? extends RandomFactory> randomFactoryClass) {
        this.randomFactoryClass = randomFactoryClass;
        return this;
    }

    public @NonNull SolverConfig withMoveThreadCount(@NonNull String moveThreadCount) {
        this.moveThreadCount = moveThreadCount;
        return this;
    }

    public @NonNull SolverConfig withMoveThreadBufferSize(@NonNull Integer moveThreadBufferSize) {
        this.moveThreadBufferSize = moveThreadBufferSize;
        return this;
    }

    public @NonNull SolverConfig withThreadFactoryClass(@NonNull Class<? extends ThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
        return this;
    }

    public @NonNull SolverConfig withSolutionClass(@NonNull Class<?> solutionClass) {
        this.solutionClass = solutionClass;
        return this;
    }

    public @NonNull SolverConfig withEntityClassList(@NonNull List<Class<?>> entityClassList) {
        this.entityClassList = entityClassList;
        return this;
    }

    public @NonNull SolverConfig withEntityClasses(@NonNull Class<?>... entityClasses) {
        this.entityClassList = Arrays.asList(entityClasses);
        return this;
    }

    public @NonNull SolverConfig withDomainAccessType(@NonNull DomainAccessType domainAccessType) {
        this.domainAccessType = domainAccessType;
        return this;
    }

    public @NonNull SolverConfig
            withGizmoMemberAccessorMap(@NonNull Map<@NonNull String, @NonNull MemberAccessor> memberAccessorMap) {
        this.gizmoMemberAccessorMap = memberAccessorMap;
        return this;
    }

    public @NonNull SolverConfig
            withGizmoSolutionClonerMap(@NonNull Map<@NonNull String, @NonNull SolutionCloner> solutionClonerMap) {
        this.gizmoSolutionClonerMap = solutionClonerMap;
        return this;
    }

    public @NonNull SolverConfig withScoreDirectorFactory(@NonNull ScoreDirectorFactoryConfig scoreDirectorFactoryConfig) {
        this.scoreDirectorFactoryConfig = scoreDirectorFactoryConfig;
        return this;
    }

    public @NonNull SolverConfig withClassLoader(@NonNull ClassLoader classLoader) {
        this.setClassLoader(classLoader);
        return this;
    }

    /**
     * As defined by {@link ScoreDirectorFactoryConfig#withEasyScoreCalculatorClass(Class)}, but returns this.
     */
    public @NonNull SolverConfig withEasyScoreCalculatorClass(
            @NonNull Class<? extends EasyScoreCalculator> easyScoreCalculatorClass) {
        if (scoreDirectorFactoryConfig == null) {
            scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        }
        scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(easyScoreCalculatorClass);
        return this;
    }

    /**
     * As defined by {@link ScoreDirectorFactoryConfig#withConstraintProviderClass(Class)}, but returns this.
     */
    public @NonNull SolverConfig withConstraintProviderClass(
            @NonNull Class<? extends ConstraintProvider> constraintProviderClass) {
        if (scoreDirectorFactoryConfig == null) {
            scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        }
        scoreDirectorFactoryConfig.setConstraintProviderClass(constraintProviderClass);
        return this;
    }

    public @NonNull SolverConfig withConstraintStreamImplType(@NonNull ConstraintStreamImplType constraintStreamImplType) {
        if (scoreDirectorFactoryConfig == null) {
            scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        }
        scoreDirectorFactoryConfig.setConstraintStreamImplType(constraintStreamImplType);
        return this;
    }

    public @NonNull SolverConfig withTerminationConfig(@NonNull TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
        return this;
    }

    /**
     * As defined by {@link TerminationConfig#withSpentLimit(Duration)}, but returns this.
     */
    public @NonNull SolverConfig withTerminationSpentLimit(@NonNull Duration spentLimit) {
        if (terminationConfig == null) {
            terminationConfig = new TerminationConfig();
        }
        terminationConfig.setSpentLimit(spentLimit);
        return this;
    }

    /**
     * As defined by {@link TerminationConfig#withUnimprovedSpentLimit(Duration)}, but returns this.
     */
    public @NonNull SolverConfig withTerminationUnimprovedSpentLimit(@NonNull Duration unimprovedSpentLimit) {
        if (terminationConfig == null) {
            terminationConfig = new TerminationConfig();
        }
        terminationConfig.setUnimprovedSpentLimit(unimprovedSpentLimit);
        return this;
    }

    public @NonNull SolverConfig
            withNearbyDistanceMeterClass(@NonNull Class<? extends NearbyDistanceMeter<?, ?>> distanceMeterClass) {
        this.nearbyDistanceMeterClass = distanceMeterClass;
        return this;
    }

    public @NonNull SolverConfig withPhaseList(@NonNull List<@NonNull PhaseConfig> phaseConfigList) {
        this.phaseConfigList = phaseConfigList;
        return this;
    }

    public @NonNull SolverConfig withPhases(@NonNull PhaseConfig... phaseConfigs) {
        this.phaseConfigList = Arrays.asList(phaseConfigs);
        return this;
    }

    public @NonNull SolverConfig withMonitoringConfig(@NonNull MonitoringConfig monitoringConfig) {
        this.monitoringConfig = monitoringConfig;
        return this;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    /**
     *
     * @return true if the solver has either a global termination configured,
     *         or all of its phases have a termination configured
     */
    public boolean canTerminate() {
        if (terminationConfig != null && terminationConfig.isConfigured()) {
            return true;
        }
        var configList = getPhaseConfigList();
        if (configList == null) {
            return true;
        }
        return configList.stream()
                .allMatch(PhaseFactory::canTerminate);
    }

    public @NonNull EnvironmentMode determineEnvironmentMode() {
        return Objects.requireNonNullElse(environmentMode, EnvironmentMode.PHASE_ASSERT);
    }

    public @NonNull DomainAccessType determineDomainAccessType() {
        return Objects.requireNonNullElse(domainAccessType, DomainAccessType.REFLECTION);
    }

    public @NonNull MonitoringConfig determineMetricConfig() {
        return Objects.requireNonNullElse(monitoringConfig,
                new MonitoringConfig().withSolverMetricList(Arrays.asList(SolverMetric.SOLVE_DURATION, SolverMetric.ERROR_COUNT,
                        SolverMetric.SCORE_CALCULATION_COUNT, SolverMetric.MOVE_EVALUATION_COUNT,
                        SolverMetric.PROBLEM_ENTITY_COUNT, SolverMetric.PROBLEM_VARIABLE_COUNT,
                        SolverMetric.PROBLEM_VALUE_COUNT, SolverMetric.PROBLEM_SIZE_LOG)));
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public void offerRandomSeedFromSubSingleIndex(long subSingleIndex) {
        if ((environmentMode == null || environmentMode.isReproducible()) && randomFactoryClass == null && randomSeed == null) {
            randomSeed = subSingleIndex;
        }
    }

    /**
     * Do not use this method, it is an internal method.
     * Use {@link #SolverConfig(SolverConfig)} instead.
     */
    @Override
    public @NonNull SolverConfig inherit(@NonNull SolverConfig inheritedConfig) {
        clock = ConfigUtils.inheritOverwritableProperty(clock, inheritedConfig.getClock());
        classLoader = ConfigUtils.inheritOverwritableProperty(classLoader, inheritedConfig.getClassLoader());
        enablePreviewFeatureSet = ConfigUtils.inheritMergeableEnumSetProperty(enablePreviewFeatureSet,
                inheritedConfig.getEnablePreviewFeatureSet());
        environmentMode = ConfigUtils.inheritOverwritableProperty(environmentMode, inheritedConfig.getEnvironmentMode());
        daemon = ConfigUtils.inheritOverwritableProperty(daemon, inheritedConfig.getDaemon());
        randomType = ConfigUtils.inheritOverwritableProperty(randomType, inheritedConfig.getRandomType());
        randomSeed = ConfigUtils.inheritOverwritableProperty(randomSeed, inheritedConfig.getRandomSeed());
        randomFactoryClass = ConfigUtils.inheritOverwritableProperty(randomFactoryClass,
                inheritedConfig.getRandomFactoryClass());
        moveThreadCount = ConfigUtils.inheritOverwritableProperty(moveThreadCount,
                inheritedConfig.getMoveThreadCount());
        moveThreadBufferSize = ConfigUtils.inheritOverwritableProperty(moveThreadBufferSize,
                inheritedConfig.getMoveThreadBufferSize());
        threadFactoryClass = ConfigUtils.inheritOverwritableProperty(threadFactoryClass,
                inheritedConfig.getThreadFactoryClass());
        solutionClass = ConfigUtils.inheritOverwritableProperty(solutionClass, inheritedConfig.getSolutionClass());
        entityClassList = ConfigUtils.inheritMergeableListProperty(entityClassList,
                inheritedConfig.getEntityClassList());
        domainAccessType = ConfigUtils.inheritOverwritableProperty(domainAccessType, inheritedConfig.getDomainAccessType());
        gizmoMemberAccessorMap = ConfigUtils.inheritMergeableMapProperty(
                gizmoMemberAccessorMap, inheritedConfig.getGizmoMemberAccessorMap());
        gizmoSolutionClonerMap = ConfigUtils.inheritMergeableMapProperty(
                gizmoSolutionClonerMap, inheritedConfig.getGizmoSolutionClonerMap());

        scoreDirectorFactoryConfig = ConfigUtils.inheritConfig(scoreDirectorFactoryConfig,
                inheritedConfig.getScoreDirectorFactoryConfig());
        terminationConfig = ConfigUtils.inheritConfig(terminationConfig, inheritedConfig.getTerminationConfig());
        nearbyDistanceMeterClass = ConfigUtils.inheritOverwritableProperty(nearbyDistanceMeterClass,
                inheritedConfig.getNearbyDistanceMeterClass());
        phaseConfigList = ConfigUtils.inheritMergeableListConfig(phaseConfigList, inheritedConfig.getPhaseConfigList());
        monitoringConfig = ConfigUtils.inheritConfig(monitoringConfig, inheritedConfig.getMonitoringConfig());
        return this;
    }

    @Override
    public @NonNull SolverConfig copyConfig() {
        return new SolverConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(randomFactoryClass);
        classVisitor.accept(threadFactoryClass);
        classVisitor.accept(solutionClass);
        if (entityClassList != null) {
            entityClassList.forEach(classVisitor);
        }
        if (scoreDirectorFactoryConfig != null) {
            scoreDirectorFactoryConfig.visitReferencedClasses(classVisitor);
        }
        if (nearbyDistanceMeterClass != null) {
            classVisitor.accept(nearbyDistanceMeterClass);
        }
        if (terminationConfig != null) {
            terminationConfig.visitReferencedClasses(classVisitor);
        }
        if (phaseConfigList != null) {
            phaseConfigList.forEach(pc -> pc.visitReferencedClasses(classVisitor));
        }
    }
}
