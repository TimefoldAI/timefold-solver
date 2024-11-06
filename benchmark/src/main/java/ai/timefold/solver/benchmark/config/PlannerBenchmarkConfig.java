package ai.timefold.solver.benchmark.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.blueprint.SolverBenchmarkBluePrintConfig;
import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.benchmark.impl.io.PlannerBenchmarkConfigIO;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.io.jaxb.TimefoldXmlSerializationException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * To read it from XML, use {@link #createFromXmlResource(String)}.
 * To build a {@link PlannerBenchmarkFactory} with it, use {@link PlannerBenchmarkFactory#create(PlannerBenchmarkConfig)}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = PlannerBenchmarkConfig.XML_ELEMENT_NAME)
@XmlType(propOrder = {
        "name",
        "benchmarkDirectory",
        "threadFactoryClass",
        "parallelBenchmarkCount",
        "warmUpMillisecondsSpentLimit",
        "warmUpSecondsSpentLimit",
        "warmUpMinutesSpentLimit",
        "warmUpHoursSpentLimit",
        "warmUpDaysSpentLimit",
        "benchmarkReportConfig",
        "inheritedSolverBenchmarkConfig",
        "solverBenchmarkBluePrintConfigList",
        "solverBenchmarkConfigList"
})
public class PlannerBenchmarkConfig {
    public static final String SOLVER_NAMESPACE_PREFIX = "solver";
    public static final String XML_ELEMENT_NAME = "plannerBenchmark";
    public static final String XML_NAMESPACE = "https://timefold.ai/xsd/benchmark";

    // ************************************************************************
    // Static creation methods: SolverConfig
    // ************************************************************************

    public static @NonNull PlannerBenchmarkConfig createFromSolverConfig(@NonNull SolverConfig solverConfig) {
        return createFromSolverConfig(solverConfig, new File("local/benchmarkReport"));
    }

    public static @NonNull PlannerBenchmarkConfig createFromSolverConfig(@NonNull SolverConfig solverConfig,
            @NonNull File benchmarkDirectory) {
        PlannerBenchmarkConfig plannerBenchmarkConfig = new PlannerBenchmarkConfig();
        plannerBenchmarkConfig.setBenchmarkDirectory(benchmarkDirectory);
        SolverBenchmarkConfig solverBenchmarkConfig = new SolverBenchmarkConfig();
        // Defensive copy of solverConfig
        solverBenchmarkConfig.setSolverConfig(new SolverConfig(solverConfig));
        plannerBenchmarkConfig.setInheritedSolverBenchmarkConfig(solverBenchmarkConfig);
        plannerBenchmarkConfig.setSolverBenchmarkConfigList(Collections.singletonList(new SolverBenchmarkConfig()));
        return plannerBenchmarkConfig;
    }

    // ************************************************************************
    // Static creation methods: XML
    // ************************************************************************

    /**
     * Reads an XML benchmark configuration from the classpath.
     *
     * @param benchmarkConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlResource(@NonNull String benchmarkConfigResource) {
        return createFromXmlResource(benchmarkConfigResource, null);
    }

    /**
     * As defined by {@link #createFromXmlResource(String)}.
     *
     * @param benchmarkConfigResource a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlResource(@NonNull String benchmarkConfigResource,
            @Nullable ClassLoader classLoader) {
        ClassLoader actualClassLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        try (InputStream in = actualClassLoader.getResourceAsStream(benchmarkConfigResource)) {
            if (in == null) {
                String errorMessage = "The benchmarkConfigResource (" + benchmarkConfigResource
                        + ") does not exist as a classpath resource in the classLoader (" + actualClassLoader + ").";
                if (benchmarkConfigResource.startsWith("/")) {
                    errorMessage += "\nA classpath resource should not start with a slash (/)."
                            + " A benchmarkConfigResource adheres to ClassLoader.getResource(String)."
                            + " Maybe remove the leading slash from the benchmarkConfigResource.";
                }
                throw new IllegalArgumentException(errorMessage);
            }
            return createFromXmlInputStream(in, classLoader);
        } catch (TimefoldXmlSerializationException e) {
            throw new IllegalArgumentException("Unmarshalling of benchmarkConfigResource (" + benchmarkConfigResource
                    + ") fails.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the benchmarkConfigResource (" + benchmarkConfigResource + ") fails.",
                    e);
        }
    }

    /**
     * Reads an XML benchmark configuration from the file system.
     * <p>
     * Warning: this leads to platform dependent code,
     * it's recommend to use {@link #createFromXmlResource(String)} instead.
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlFile(@NonNull File benchmarkConfigFile) {
        return createFromXmlFile(benchmarkConfigFile, null);
    }

    /**
     * As defined by {@link #createFromXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlFile(@NonNull File benchmarkConfigFile,
            @Nullable ClassLoader classLoader) {
        try (InputStream in = new FileInputStream(benchmarkConfigFile)) {
            return createFromXmlInputStream(in, classLoader);
        } catch (TimefoldXmlSerializationException e) {
            throw new IllegalArgumentException("Unmarshalling the benchmarkConfigFile (" + benchmarkConfigFile + ") fails.", e);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The benchmarkConfigFile (" + benchmarkConfigFile + ") was not found.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the benchmarkConfigFile (" + benchmarkConfigFile + ") fails.", e);
        }
    }

    /**
     * @param in gets closed
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlInputStream(@NonNull InputStream in) {
        return createFromXmlInputStream(in, null);
    }

    /**
     * @param in gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlInputStream(@NonNull InputStream in,
            @Nullable ClassLoader classLoader) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return createFromXmlReader(reader, classLoader);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("This vm does not support the charset (" + StandardCharsets.UTF_8 + ").", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading fails.", e);
        }
    }

    /**
     * @param reader gets closed
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlReader(@NonNull Reader reader) {
        return createFromXmlReader(reader, null);
    }

    /**
     * @param reader gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromXmlReader(@NonNull Reader reader,
            @Nullable ClassLoader classLoader) {
        PlannerBenchmarkConfigIO xmlIO = new PlannerBenchmarkConfigIO();
        Object benchmarkConfigObject = xmlIO.read(reader);
        if (!(benchmarkConfigObject instanceof PlannerBenchmarkConfig)) {
            throw new IllegalArgumentException("The " + PlannerBenchmarkConfig.class.getSimpleName()
                    + "'s XML root element resolves to a different type ("
                    + (benchmarkConfigObject == null ? null : benchmarkConfigObject.getClass().getSimpleName()) + ")."
                    + (benchmarkConfigObject instanceof SolverConfig
                            ? "\nMaybe use " + PlannerBenchmarkFactory.class.getSimpleName()
                                    + ".createFromSolverConfigXmlResource() instead."
                            : ""));
        }
        PlannerBenchmarkConfig benchmarkConfig = (PlannerBenchmarkConfig) benchmarkConfigObject;
        benchmarkConfig.setClassLoader(classLoader);
        return benchmarkConfig;
    }

    // ************************************************************************
    // Static creation methods: Freemarker XML
    // ************************************************************************

    /**
     * Reads a Freemarker XML benchmark configuration from the classpath.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlResource(@NonNull String templateResource) {
        return createFromFreemarkerXmlResource(templateResource, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlResource(String)}.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlResource(@NonNull String templateResource,
            @Nullable ClassLoader classLoader) {
        return createFromFreemarkerXmlResource(templateResource, null, classLoader);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlResource(String)}.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlResource(@NonNull String templateResource,
            @Nullable Object model) {
        return createFromFreemarkerXmlResource(templateResource, model, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlResource(String)}.
     *
     * @param templateResource a classpath resource as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlResource(@NonNull String templateResource,
            @Nullable Object model,
            @Nullable ClassLoader classLoader) {
        ClassLoader actualClassLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        try (InputStream templateIn = actualClassLoader.getResourceAsStream(templateResource)) {
            if (templateIn == null) {
                String errorMessage = "The templateResource (" + templateResource
                        + ") does not exist as a classpath resource in the classLoader (" + actualClassLoader + ").";
                if (templateResource.startsWith("/")) {
                    errorMessage += "\nA classpath resource should not start with a slash (/)."
                            + " A templateResource adheres to ClassLoader.getResource(String)."
                            + " Maybe remove the leading slash from the templateResource.";
                }
                throw new IllegalArgumentException(errorMessage);
            }
            return createFromFreemarkerXmlInputStream(templateIn, model, classLoader);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the templateResource (" + templateResource + ") fails.", e);
        }
    }

    /**
     * Reads a Freemarker XML benchmark configuration from the file system.
     * <p>
     * Warning: this leads to platform dependent code,
     * it's recommend to use {@link #createFromFreemarkerXmlResource(String)} instead.
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlFile(@NonNull File templateFile) {
        return createFromFreemarkerXmlFile(templateFile, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlFile(@NonNull File templateFile,
            @Nullable ClassLoader classLoader) {
        return createFromFreemarkerXmlFile(templateFile, null, classLoader);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlFile(File)}.
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlFile(@NonNull File templateFile,
            @Nullable Object model) {
        return createFromFreemarkerXmlFile(templateFile, model, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlFile(File)}.
     *
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlFile(@NonNull File templateFile,
            @Nullable Object model, @Nullable ClassLoader classLoader) {
        try (FileInputStream templateIn = new FileInputStream(templateFile)) {
            return createFromFreemarkerXmlInputStream(templateIn, model, classLoader);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The templateFile (" + templateFile + ") was not found.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the templateFile (" + templateFile + ") fails.", e);
        }
    }

    /**
     * @param templateIn gets closed
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlInputStream(@NonNull InputStream templateIn) {
        return createFromFreemarkerXmlInputStream(templateIn, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlInputStream(InputStream)}.
     *
     * @param templateIn gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlInputStream(@NonNull InputStream templateIn,
            @Nullable ClassLoader classLoader) {
        return createFromFreemarkerXmlInputStream(templateIn, null, classLoader);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlInputStream(InputStream)}.
     *
     * @param templateIn gets closed
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlInputStream(@NonNull InputStream templateIn,
            @Nullable Object model) {
        return createFromFreemarkerXmlInputStream(templateIn, model, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlInputStream(InputStream)}.
     *
     * @param templateIn gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlInputStream(@NonNull InputStream templateIn,
            @Nullable Object model, @Nullable ClassLoader classLoader) {
        try (Reader reader = new InputStreamReader(templateIn, StandardCharsets.UTF_8)) {
            return createFromFreemarkerXmlReader(reader, model, classLoader);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("This vm does not support the charset (" + StandardCharsets.UTF_8 + ").", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading fails.", e);
        }
    }

    /**
     * @param templateReader gets closed
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlReader(@NonNull Reader templateReader) {
        return createFromFreemarkerXmlReader(templateReader, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlReader(Reader)}.
     *
     * @param templateReader gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlReader(@NonNull Reader templateReader,
            @Nullable ClassLoader classLoader) {
        return createFromFreemarkerXmlReader(templateReader, null, classLoader);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlReader(Reader)}.
     *
     * @param templateReader gets closed
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlReader(@NonNull Reader templateReader,
            @Nullable Object model) {
        return createFromFreemarkerXmlReader(templateReader, model, null);
    }

    /**
     * As defined by {@link #createFromFreemarkerXmlReader(Reader)}.
     *
     * @param templateReader gets closed
     * @param classLoader the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     */
    public static @NonNull PlannerBenchmarkConfig createFromFreemarkerXmlReader(@NonNull Reader templateReader,
            @Nullable Object model, @Nullable ClassLoader classLoader) {
        Configuration freemarkerConfiguration = BenchmarkReport.createFreeMarkerConfiguration();
        freemarkerConfiguration.setNumberFormat("computer");
        freemarkerConfiguration.setDateFormat("yyyy-mm-dd");
        freemarkerConfiguration.setDateTimeFormat("yyyy-mm-dd HH:mm:ss.SSS z");
        freemarkerConfiguration.setTimeFormat("HH:mm:ss.SSS");
        String xmlContent;
        try (StringWriter xmlContentWriter = new StringWriter()) {
            Template template = new Template("benchmarkTemplate.ftl", templateReader, freemarkerConfiguration,
                    freemarkerConfiguration.getDefaultEncoding());
            template.process(model, xmlContentWriter);
            xmlContent = xmlContentWriter.toString();
        } catch (TemplateException | IOException e) {
            throw new IllegalArgumentException("Can not process the Freemarker template into xmlContentWriter.", e);
        }
        try (StringReader configReader = new StringReader(xmlContent)) {
            return createFromXmlReader(configReader, classLoader);
        }
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    public static final String PARALLEL_BENCHMARK_COUNT_AUTO = "AUTO";

    @XmlTransient
    private ClassLoader classLoader = null;

    private String name = null;
    private File benchmarkDirectory = null;

    private Class<? extends ThreadFactory> threadFactoryClass = null;
    private String parallelBenchmarkCount = null;
    private Long warmUpMillisecondsSpentLimit = null;
    private Long warmUpSecondsSpentLimit = null;
    private Long warmUpMinutesSpentLimit = null;
    private Long warmUpHoursSpentLimit = null;
    private Long warmUpDaysSpentLimit = null;

    @XmlElement(name = "benchmarkReport")
    private BenchmarkReportConfig benchmarkReportConfig = null;

    @XmlElement(name = "inheritedSolverBenchmark")
    private SolverBenchmarkConfig inheritedSolverBenchmarkConfig = null;

    @XmlElement(name = "solverBenchmarkBluePrint")
    private List<SolverBenchmarkBluePrintConfig> solverBenchmarkBluePrintConfigList = null;

    @XmlElement(name = "solverBenchmark")
    private List<SolverBenchmarkConfig> solverBenchmarkConfigList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    /**
     * Create an empty benchmark config.
     */
    public PlannerBenchmarkConfig() {
    }

    public PlannerBenchmarkConfig(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public @Nullable ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable File getBenchmarkDirectory() {
        return benchmarkDirectory;
    }

    public void setBenchmarkDirectory(@Nullable File benchmarkDirectory) {
        this.benchmarkDirectory = benchmarkDirectory;
    }

    public @Nullable Class<? extends ThreadFactory> getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(@Nullable Class<? extends ThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
    }

    /**
     * Using multiple parallel benchmarks can decrease the reliability of the results.
     * <p>
     * If there aren't enough processors available, it will be decreased.
     *
     * @return null, a number or {@value #PARALLEL_BENCHMARK_COUNT_AUTO}.
     */
    public @Nullable String getParallelBenchmarkCount() {
        return parallelBenchmarkCount;
    }

    public void setParallelBenchmarkCount(@Nullable String parallelBenchmarkCount) {
        this.parallelBenchmarkCount = parallelBenchmarkCount;
    }

    public @NonNull Long getWarmUpMillisecondsSpentLimit() {
        return warmUpMillisecondsSpentLimit;
    }

    public void setWarmUpMillisecondsSpentLimit(@NonNull Long warmUpMillisecondsSpentLimit) {
        this.warmUpMillisecondsSpentLimit = warmUpMillisecondsSpentLimit;
    }

    public @NonNull Long getWarmUpSecondsSpentLimit() {
        return warmUpSecondsSpentLimit;
    }

    public void setWarmUpSecondsSpentLimit(@NonNull Long warmUpSecondsSpentLimit) {
        this.warmUpSecondsSpentLimit = warmUpSecondsSpentLimit;
    }

    public @NonNull Long getWarmUpMinutesSpentLimit() {
        return warmUpMinutesSpentLimit;
    }

    public void setWarmUpMinutesSpentLimit(@NonNull Long warmUpMinutesSpentLimit) {
        this.warmUpMinutesSpentLimit = warmUpMinutesSpentLimit;
    }

    public @NonNull Long getWarmUpHoursSpentLimit() {
        return warmUpHoursSpentLimit;
    }

    public void setWarmUpHoursSpentLimit(@NonNull Long warmUpHoursSpentLimit) {
        this.warmUpHoursSpentLimit = warmUpHoursSpentLimit;
    }

    public @NonNull Long getWarmUpDaysSpentLimit() {
        return warmUpDaysSpentLimit;
    }

    public void setWarmUpDaysSpentLimit(@NonNull Long warmUpDaysSpentLimit) {
        this.warmUpDaysSpentLimit = warmUpDaysSpentLimit;
    }

    public @NonNull BenchmarkReportConfig getBenchmarkReportConfig() {
        return benchmarkReportConfig;
    }

    public void setBenchmarkReportConfig(@NonNull BenchmarkReportConfig benchmarkReportConfig) {
        this.benchmarkReportConfig = benchmarkReportConfig;
    }

    public @NonNull SolverBenchmarkConfig getInheritedSolverBenchmarkConfig() {
        return inheritedSolverBenchmarkConfig;
    }

    public void setInheritedSolverBenchmarkConfig(@NonNull SolverBenchmarkConfig inheritedSolverBenchmarkConfig) {
        this.inheritedSolverBenchmarkConfig = inheritedSolverBenchmarkConfig;
    }

    public @Nullable List<@NonNull SolverBenchmarkBluePrintConfig> getSolverBenchmarkBluePrintConfigList() {
        return solverBenchmarkBluePrintConfigList;
    }

    public void setSolverBenchmarkBluePrintConfigList(
            @Nullable List<@NonNull SolverBenchmarkBluePrintConfig> solverBenchmarkBluePrintConfigList) {
        this.solverBenchmarkBluePrintConfigList = solverBenchmarkBluePrintConfigList;
    }

    public @Nullable List<@NonNull SolverBenchmarkConfig> getSolverBenchmarkConfigList() {
        return solverBenchmarkConfigList;
    }

    public void setSolverBenchmarkConfigList(@Nullable List<@NonNull SolverBenchmarkConfig> solverBenchmarkConfigList) {
        this.solverBenchmarkConfigList = solverBenchmarkConfigList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull PlannerBenchmarkConfig withClassLoader(@NonNull ClassLoader classLoader) {
        this.setClassLoader(classLoader);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withName(@NonNull String name) {
        this.setName(name);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withBenchmarkDirectory(@NonNull File benchmarkDirectory) {
        this.setBenchmarkDirectory(benchmarkDirectory);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withThreadFactoryClass(@NonNull Class<? extends ThreadFactory> threadFactoryClass) {
        this.setThreadFactoryClass(threadFactoryClass);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withParallelBenchmarkCount(@NonNull String parallelBenchmarkCount) {
        this.setParallelBenchmarkCount(parallelBenchmarkCount);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withWarmUpMillisecondsSpentLimit(@NonNull Long warmUpMillisecondsSpentLimit) {
        this.setWarmUpMillisecondsSpentLimit(warmUpMillisecondsSpentLimit);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withWarmUpSecondsSpentLimit(@NonNull Long warmUpSecondsSpentLimit) {
        this.setWarmUpSecondsSpentLimit(warmUpSecondsSpentLimit);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withWarmUpMinutesSpentLimit(@NonNull Long warmUpMinutesSpentLimit) {
        this.setWarmUpMinutesSpentLimit(warmUpMinutesSpentLimit);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withWarmUpHoursSpentLimit(@NonNull Long warmUpHoursSpentLimit) {
        this.setWarmUpHoursSpentLimit(warmUpHoursSpentLimit);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withWarmUpDaysSpentLimit(@NonNull Long warmUpDaysSpentLimit) {
        this.setWarmUpDaysSpentLimit(warmUpDaysSpentLimit);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withBenchmarkReportConfig(@NonNull BenchmarkReportConfig benchmarkReportConfig) {
        this.setBenchmarkReportConfig(benchmarkReportConfig);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig
            withInheritedSolverBenchmarkConfig(@NonNull SolverBenchmarkConfig inheritedSolverBenchmarkConfig) {
        this.setInheritedSolverBenchmarkConfig(inheritedSolverBenchmarkConfig);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withSolverBenchmarkBluePrintConfigList(
            @NonNull List<@NonNull SolverBenchmarkBluePrintConfig> solverBenchmarkBluePrintConfigList) {
        this.setSolverBenchmarkBluePrintConfigList(solverBenchmarkBluePrintConfigList);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig withSolverBenchmarkBluePrintConfigs(
            @NonNull SolverBenchmarkBluePrintConfig... solverBenchmarkBluePrintConfigs) {
        this.setSolverBenchmarkBluePrintConfigList(List.of(solverBenchmarkBluePrintConfigs));
        return this;
    }

    public @NonNull PlannerBenchmarkConfig
            withSolverBenchmarkConfigList(@NonNull List<@NonNull SolverBenchmarkConfig> solverBenchmarkConfigList) {
        this.setSolverBenchmarkConfigList(solverBenchmarkConfigList);
        return this;
    }

    public @NonNull PlannerBenchmarkConfig
            withSolverBenchmarkConfigs(@NonNull SolverBenchmarkConfig... solverBenchmarkConfigs) {
        this.setSolverBenchmarkConfigList(List.of(solverBenchmarkConfigs));
        return this;
    }
}
