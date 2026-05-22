package ai.timefold.solver.core.config.score.director;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.JaxbCustomPropertiesAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "easyScoreCalculatorClass",
        "easyScoreCalculatorCustomProperties",
        "constraintProviderClass",
        "constraintProviderCustomProperties",
        "constraintStreamAutomaticNodeSharing",
        "constraintStreamProfilingEnabled",
        "incrementalScoreCalculatorClass",
        "incrementalScoreCalculatorCustomProperties",
        "initializingScoreTrend",
        "assertionScoreDirectorFactory"
})
public final class ScoreDirectorFactoryConfig extends AbstractConfig<ScoreDirectorFactoryConfig> {

    private String easyScoreCalculatorClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    private Map<String, String> easyScoreCalculatorCustomProperties = null;

    private String constraintProviderClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    private Map<String, String> constraintProviderCustomProperties = null;
    @Nullable
    private Boolean constraintStreamAutomaticNodeSharing;
    private Boolean constraintStreamProfilingEnabled;

    private String incrementalScoreCalculatorClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    private Map<String, String> incrementalScoreCalculatorCustomProperties = null;

    // TODO: this should be rather an enum?
    private String initializingScoreTrend = null;

    @XmlElement(name = "assertionScoreDirectorFactory")
    private ScoreDirectorFactoryConfig assertionScoreDirectorFactory = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Class<? extends EasyScoreCalculator> getEasyScoreCalculatorClass() {
        return ConfigUtils.resolveClass(easyScoreCalculatorClass, "easyScoreCalculatorClass", this);
    }

    public void setEasyScoreCalculatorClass(@Nullable Class<? extends EasyScoreCalculator> easyScoreCalculatorClass) {
        this.easyScoreCalculatorClass = easyScoreCalculatorClass == null ? null : easyScoreCalculatorClass.getName();
    }

    public @Nullable Map<@NonNull String, @NonNull String> getEasyScoreCalculatorCustomProperties() {
        return easyScoreCalculatorCustomProperties;
    }

    public void setEasyScoreCalculatorCustomProperties(
            @Nullable Map<@NonNull String, @NonNull String> easyScoreCalculatorCustomProperties) {
        this.easyScoreCalculatorCustomProperties = easyScoreCalculatorCustomProperties;
    }

    public @Nullable Class<? extends ConstraintProvider> getConstraintProviderClass() {
        return ConfigUtils.resolveClass(constraintProviderClass, "constraintProviderClass", this);
    }

    public void setConstraintProviderClass(@Nullable Class<? extends ConstraintProvider> constraintProviderClass) {
        this.constraintProviderClass = constraintProviderClass == null ? null : constraintProviderClass.getName();
    }

    public @Nullable Map<@NonNull String, @NonNull String> getConstraintProviderCustomProperties() {
        return constraintProviderCustomProperties;
    }

    public void setConstraintProviderCustomProperties(
            @Nullable Map<@NonNull String, @NonNull String> constraintProviderCustomProperties) {
        this.constraintProviderCustomProperties = constraintProviderCustomProperties;
    }

    public @Nullable Boolean getConstraintStreamAutomaticNodeSharing() {
        return constraintStreamAutomaticNodeSharing;
    }

    public void
            setConstraintStreamAutomaticNodeSharing(@Nullable Boolean constraintStreamAutomaticNodeSharing) {
        this.constraintStreamAutomaticNodeSharing = constraintStreamAutomaticNodeSharing;
    }

    public Boolean getConstraintStreamProfilingEnabled() {
        return constraintStreamProfilingEnabled;
    }

    public void setConstraintStreamProfilingEnabled(Boolean constraintStreamProfilingEnabled) {
        this.constraintStreamProfilingEnabled = constraintStreamProfilingEnabled;
    }

    public @Nullable Class<? extends IncrementalScoreCalculator> getIncrementalScoreCalculatorClass() {
        return ConfigUtils.resolveClass(incrementalScoreCalculatorClass, "incrementalScoreCalculatorClass", this);
    }

    public void setIncrementalScoreCalculatorClass(
            @Nullable Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass) {
        this.incrementalScoreCalculatorClass =
                incrementalScoreCalculatorClass == null ? null : incrementalScoreCalculatorClass.getName();
    }

    public @Nullable Map<@NonNull String, @NonNull String> getIncrementalScoreCalculatorCustomProperties() {
        return incrementalScoreCalculatorCustomProperties;
    }

    public void setIncrementalScoreCalculatorCustomProperties(
            @Nullable Map<@NonNull String, @NonNull String> incrementalScoreCalculatorCustomProperties) {
        this.incrementalScoreCalculatorCustomProperties = incrementalScoreCalculatorCustomProperties;
    }

    public @Nullable String getInitializingScoreTrend() {
        return initializingScoreTrend;
    }

    public void setInitializingScoreTrend(@Nullable String initializingScoreTrend) {
        this.initializingScoreTrend = initializingScoreTrend;
    }

    public @Nullable ScoreDirectorFactoryConfig getAssertionScoreDirectorFactory() {
        return assertionScoreDirectorFactory;
    }

    public void setAssertionScoreDirectorFactory(@Nullable ScoreDirectorFactoryConfig assertionScoreDirectorFactory) {
        this.assertionScoreDirectorFactory = assertionScoreDirectorFactory;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull ScoreDirectorFactoryConfig
            withEasyScoreCalculatorClass(@NonNull Class<? extends EasyScoreCalculator> easyScoreCalculatorClass) {
        this.easyScoreCalculatorClass = easyScoreCalculatorClass.getName();
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withEasyScoreCalculatorCustomProperties(
                    @NonNull Map<@NonNull String, @NonNull String> easyScoreCalculatorCustomProperties) {
        this.easyScoreCalculatorCustomProperties = easyScoreCalculatorCustomProperties;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withConstraintProviderClass(@NonNull Class<? extends ConstraintProvider> constraintProviderClass) {
        this.constraintProviderClass = constraintProviderClass.getName();
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withConstraintProviderCustomProperties(
                    @NonNull Map<@NonNull String, @NonNull String> constraintProviderCustomProperties) {
        this.constraintProviderCustomProperties = constraintProviderCustomProperties;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withConstraintStreamAutomaticNodeSharing(@NonNull Boolean constraintStreamAutomaticNodeSharing) {
        this.constraintStreamAutomaticNodeSharing = constraintStreamAutomaticNodeSharing;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withConstraintStreamProfilingEnabled(Boolean constraintStreamProfiling) {
        this.constraintStreamProfilingEnabled = constraintStreamProfiling;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withIncrementalScoreCalculatorClass(
                    @NonNull Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass) {
        this.incrementalScoreCalculatorClass = incrementalScoreCalculatorClass.getName();
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withIncrementalScoreCalculatorCustomProperties(
                    @NonNull Map<@NonNull String, @NonNull String> incrementalScoreCalculatorCustomProperties) {
        this.incrementalScoreCalculatorCustomProperties = incrementalScoreCalculatorCustomProperties;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig withInitializingScoreTrend(@NonNull String initializingScoreTrend) {
        this.initializingScoreTrend = initializingScoreTrend;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig withAssertionScoreDirectorFactory(
            @NonNull ScoreDirectorFactoryConfig assertionScoreDirectorFactory) {
        this.assertionScoreDirectorFactory = assertionScoreDirectorFactory;
        return this;
    }

    @Override
    public @NonNull ScoreDirectorFactoryConfig inherit(@NonNull ScoreDirectorFactoryConfig inheritedConfig) {
        easyScoreCalculatorClass = ConfigUtils.inheritOverwritableProperty(
                easyScoreCalculatorClass, inheritedConfig.easyScoreCalculatorClass);
        easyScoreCalculatorCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                easyScoreCalculatorCustomProperties, inheritedConfig.getEasyScoreCalculatorCustomProperties());
        constraintProviderClass = ConfigUtils.inheritOverwritableProperty(
                constraintProviderClass, inheritedConfig.constraintProviderClass);
        constraintProviderCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                constraintProviderCustomProperties, inheritedConfig.getConstraintProviderCustomProperties());
        constraintStreamAutomaticNodeSharing = ConfigUtils.inheritOverwritableProperty(constraintStreamAutomaticNodeSharing,
                inheritedConfig.getConstraintStreamAutomaticNodeSharing());
        constraintStreamProfilingEnabled = ConfigUtils.inheritOverwritableProperty(constraintStreamProfilingEnabled,
                inheritedConfig.getConstraintStreamProfilingEnabled());
        incrementalScoreCalculatorClass = ConfigUtils.inheritOverwritableProperty(
                incrementalScoreCalculatorClass, inheritedConfig.incrementalScoreCalculatorClass);
        incrementalScoreCalculatorCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                incrementalScoreCalculatorCustomProperties, inheritedConfig.getIncrementalScoreCalculatorCustomProperties());
        initializingScoreTrend = ConfigUtils.inheritOverwritableProperty(
                initializingScoreTrend, inheritedConfig.getInitializingScoreTrend());
        assertionScoreDirectorFactory = ConfigUtils.inheritOverwritableProperty(
                assertionScoreDirectorFactory, inheritedConfig.getAssertionScoreDirectorFactory());
        return this;
    }

    @Override
    public @NonNull ScoreDirectorFactoryConfig copyConfig() {
        return new ScoreDirectorFactoryConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(getEasyScoreCalculatorClass());
        classVisitor.accept(getConstraintProviderClass());
        classVisitor.accept(getIncrementalScoreCalculatorClass());
        if (assertionScoreDirectorFactory != null) {
            assertionScoreDirectorFactory.visitReferencedClasses(classVisitor);
        }
    }

}
