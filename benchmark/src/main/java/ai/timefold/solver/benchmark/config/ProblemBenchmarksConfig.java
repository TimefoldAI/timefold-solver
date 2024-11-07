package ai.timefold.solver.benchmark.config;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "solutionFileIOClass",
        "writeOutputSolutionEnabled",
        "inputSolutionFileList",
        "problemStatisticEnabled",
        "problemStatisticTypeList",
        "singleStatisticTypeList"
})
public class ProblemBenchmarksConfig extends AbstractConfig<ProblemBenchmarksConfig> {

    private Class<? extends SolutionFileIO<?>> solutionFileIOClass = null;

    private Boolean writeOutputSolutionEnabled = null;

    @XmlElement(name = "inputSolutionFile")
    private List<File> inputSolutionFileList = null;

    private Boolean problemStatisticEnabled = null;

    @XmlElement(name = "problemStatisticType")
    private List<ProblemStatisticType> problemStatisticTypeList = null;

    @XmlElement(name = "singleStatisticType")
    private List<SingleStatisticType> singleStatisticTypeList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Class<? extends SolutionFileIO<?>> getSolutionFileIOClass() {
        return solutionFileIOClass;
    }

    public void setSolutionFileIOClass(@Nullable Class<? extends SolutionFileIO<?>> solutionFileIOClass) {
        this.solutionFileIOClass = solutionFileIOClass;
    }

    public @Nullable Boolean getWriteOutputSolutionEnabled() {
        return writeOutputSolutionEnabled;
    }

    public void setWriteOutputSolutionEnabled(@Nullable Boolean writeOutputSolutionEnabled) {
        this.writeOutputSolutionEnabled = writeOutputSolutionEnabled;
    }

    public @Nullable List<@NonNull File> getInputSolutionFileList() {
        return inputSolutionFileList;
    }

    public void setInputSolutionFileList(@Nullable List<@NonNull File> inputSolutionFileList) {
        this.inputSolutionFileList = inputSolutionFileList;
    }

    public @Nullable Boolean getProblemStatisticEnabled() {
        return problemStatisticEnabled;
    }

    public void setProblemStatisticEnabled(@Nullable Boolean problemStatisticEnabled) {
        this.problemStatisticEnabled = problemStatisticEnabled;
    }

    public @Nullable List<@NonNull ProblemStatisticType> getProblemStatisticTypeList() {
        return problemStatisticTypeList;
    }

    public void setProblemStatisticTypeList(@Nullable List<@NonNull ProblemStatisticType> problemStatisticTypeList) {
        this.problemStatisticTypeList = problemStatisticTypeList;
    }

    public @Nullable List<@NonNull SingleStatisticType> getSingleStatisticTypeList() {
        return singleStatisticTypeList;
    }

    public void setSingleStatisticTypeList(@Nullable List<@NonNull SingleStatisticType> singleStatisticTypeList) {
        this.singleStatisticTypeList = singleStatisticTypeList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull ProblemBenchmarksConfig
            withSolutionFileIOClass(@NonNull Class<? extends SolutionFileIO<?>> solutionFileIOClass) {
        this.setSolutionFileIOClass(solutionFileIOClass);
        return this;
    }

    public @NonNull ProblemBenchmarksConfig withWriteOutputSolutionEnabled(@NonNull Boolean writeOutputSolutionEnabled) {
        this.setWriteOutputSolutionEnabled(writeOutputSolutionEnabled);
        return this;
    }

    public @NonNull ProblemBenchmarksConfig withInputSolutionFileList(@NonNull List<@NonNull File> inputSolutionFileList) {
        this.setInputSolutionFileList(inputSolutionFileList);
        return this;
    }

    public @NonNull ProblemBenchmarksConfig withInputSolutionFiles(@NonNull File... inputSolutionFiles) {
        this.setInputSolutionFileList(List.of(inputSolutionFiles));
        return this;
    }

    public @NonNull ProblemBenchmarksConfig withProblemStatisticsEnabled(@NonNull Boolean problemStatisticEnabled) {
        this.setProblemStatisticEnabled(problemStatisticEnabled);
        return this;
    }

    public @NonNull ProblemBenchmarksConfig
            withProblemStatisticTypeList(@NonNull List<@NonNull ProblemStatisticType> problemStatisticTypeList) {
        this.setProblemStatisticTypeList(problemStatisticTypeList);
        return this;
    }

    public @NonNull ProblemBenchmarksConfig
            withProblemStatisticTypes(@NonNull ProblemStatisticType... problemStatisticTypes) {
        this.setProblemStatisticTypeList(List.of(problemStatisticTypes));
        return this;
    }

    public @NonNull ProblemBenchmarksConfig
            withSingleStatisticTypeList(@NonNull List<@NonNull SingleStatisticType> singleStatisticTypeList) {
        this.setSingleStatisticTypeList(singleStatisticTypeList);
        return this;
    }

    public @NonNull ProblemBenchmarksConfig
            withSingleStatisticTypes(@NonNull SingleStatisticType... singleStatisticTypes) {
        this.setSingleStatisticTypeList(List.of(singleStatisticTypes));
        return this;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    /**
     * Return the problem statistic type list, or a list containing default metrics if problemStatisticEnabled
     * is not false. If problemStatisticEnabled is false, an empty list is returned.
     */
    public @NonNull List<@NonNull ProblemStatisticType> determineProblemStatisticTypeList() {
        if (problemStatisticEnabled != null && !problemStatisticEnabled) {
            return Collections.emptyList();
        }

        if (problemStatisticTypeList == null || problemStatisticTypeList.isEmpty()) {
            return ProblemStatisticType.defaultList();
        }

        return problemStatisticTypeList;
    }

    /**
     * Return the single statistic type list, or an empty list if it is null
     */
    public @NonNull List<@NonNull SingleStatisticType> determineSingleStatisticTypeList() {
        return Objects.requireNonNullElse(singleStatisticTypeList, Collections.emptyList());
    }

    @Override
    public @NonNull ProblemBenchmarksConfig inherit(@NonNull ProblemBenchmarksConfig inheritedConfig) {
        solutionFileIOClass = ConfigUtils.inheritOverwritableProperty(solutionFileIOClass,
                inheritedConfig.getSolutionFileIOClass());
        writeOutputSolutionEnabled = ConfigUtils.inheritOverwritableProperty(writeOutputSolutionEnabled,
                inheritedConfig.getWriteOutputSolutionEnabled());
        inputSolutionFileList = ConfigUtils.inheritMergeableListProperty(inputSolutionFileList,
                inheritedConfig.getInputSolutionFileList());
        problemStatisticEnabled = ConfigUtils.inheritOverwritableProperty(problemStatisticEnabled,
                inheritedConfig.getProblemStatisticEnabled());
        problemStatisticTypeList = ConfigUtils.inheritUniqueMergeableListProperty(problemStatisticTypeList,
                inheritedConfig.getProblemStatisticTypeList());
        singleStatisticTypeList = ConfigUtils.inheritUniqueMergeableListProperty(singleStatisticTypeList,
                inheritedConfig.getSingleStatisticTypeList());
        return this;
    }

    @Override
    public @NonNull ProblemBenchmarksConfig copyConfig() {
        return new ProblemBenchmarksConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(solutionFileIOClass);
    }

}
