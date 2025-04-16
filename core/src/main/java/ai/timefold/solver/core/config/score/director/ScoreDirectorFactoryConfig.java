package ai.timefold.solver.core.config.score.director;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbCustomPropertiesAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "easyScoreCalculatorClass",
        "easyScoreCalculatorCustomProperties",
        "constraintProviderClass",
        "constraintProviderCustomProperties",
        "constraintStreamImplType",
        "constraintStreamAutomaticNodeSharing",
        "incrementalScoreCalculatorClass",
        "incrementalScoreCalculatorCustomProperties",
        "scoreDrlList",
        "initializingScoreTrend",
        "assertionScoreDirectorFactory"
})
public class ScoreDirectorFactoryConfig extends AbstractConfig<ScoreDirectorFactoryConfig> {

    protected Class<? extends EasyScoreCalculator> easyScoreCalculatorClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> easyScoreCalculatorCustomProperties = null;

    protected Class<? extends ConstraintProvider> constraintProviderClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> constraintProviderCustomProperties = null;
    protected ConstraintStreamImplType constraintStreamImplType;
    protected Boolean constraintStreamAutomaticNodeSharing;

    protected Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> incrementalScoreCalculatorCustomProperties = null;

    @Deprecated(forRemoval = true)
    @XmlElement(name = "scoreDrl")
    protected List<String> scoreDrlList = null;

    // TODO: this should be rather an enum?
    protected String initializingScoreTrend = null;

    @XmlElement(name = "assertionScoreDirectorFactory")
    protected ScoreDirectorFactoryConfig assertionScoreDirectorFactory = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Class<? extends EasyScoreCalculator> getEasyScoreCalculatorClass() {
        return easyScoreCalculatorClass;
    }

    public void setEasyScoreCalculatorClass(@Nullable Class<? extends EasyScoreCalculator> easyScoreCalculatorClass) {
        this.easyScoreCalculatorClass = easyScoreCalculatorClass;
    }

    public @Nullable Map<@NonNull String, @NonNull String> getEasyScoreCalculatorCustomProperties() {
        return easyScoreCalculatorCustomProperties;
    }

    public void setEasyScoreCalculatorCustomProperties(
            @Nullable Map<@NonNull String, @NonNull String> easyScoreCalculatorCustomProperties) {
        this.easyScoreCalculatorCustomProperties = easyScoreCalculatorCustomProperties;
    }

    public @Nullable Class<? extends ConstraintProvider> getConstraintProviderClass() {
        return constraintProviderClass;
    }

    public void setConstraintProviderClass(@Nullable Class<? extends ConstraintProvider> constraintProviderClass) {
        this.constraintProviderClass = constraintProviderClass;
    }

    public @Nullable Map<@NonNull String, @NonNull String> getConstraintProviderCustomProperties() {
        return constraintProviderCustomProperties;
    }

    public void setConstraintProviderCustomProperties(
            @Nullable Map<@NonNull String, @NonNull String> constraintProviderCustomProperties) {
        this.constraintProviderCustomProperties = constraintProviderCustomProperties;
    }

    /**
     * @deprecated There is only one implementation, so this method is deprecated.
     *             This method no longer has any effect.
     */
    @Deprecated(forRemoval = true, since = "1.16.0")
    public @Nullable ConstraintStreamImplType getConstraintStreamImplType() {
        return constraintStreamImplType;
    }

    /**
     * @deprecated There is only one implementation, so this method is deprecated.
     *             This method no longer has any effect.
     */
    @Deprecated(forRemoval = true, since = "1.16.0")
    public void setConstraintStreamImplType(@Nullable ConstraintStreamImplType constraintStreamImplType) {
        this.constraintStreamImplType = constraintStreamImplType;
    }

    public @Nullable Boolean getConstraintStreamAutomaticNodeSharing() {
        return constraintStreamAutomaticNodeSharing;
    }

    public void setConstraintStreamAutomaticNodeSharing(@Nullable Boolean constraintStreamAutomaticNodeSharing) {
        this.constraintStreamAutomaticNodeSharing = constraintStreamAutomaticNodeSharing;
    }

    public @Nullable Class<? extends IncrementalScoreCalculator> getIncrementalScoreCalculatorClass() {
        return incrementalScoreCalculatorClass;
    }

    public void setIncrementalScoreCalculatorClass(
            @Nullable Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass) {
        this.incrementalScoreCalculatorClass = incrementalScoreCalculatorClass;
    }

    public @Nullable Map<@NonNull String, @NonNull String> getIncrementalScoreCalculatorCustomProperties() {
        return incrementalScoreCalculatorCustomProperties;
    }

    public void setIncrementalScoreCalculatorCustomProperties(
            @Nullable Map<@NonNull String, @NonNull String> incrementalScoreCalculatorCustomProperties) {
        this.incrementalScoreCalculatorCustomProperties = incrementalScoreCalculatorCustomProperties;
    }

    /**
     * @deprecated All support for Score DRL was removed when Timefold was forked from OptaPlanner.
     *             See <a href="https://timefold.ai/blog/migrating-score-drl-to-constraint-streams">DRL to Constraint Streams
     *             migration recipe</a>.
     */
    @Deprecated(forRemoval = true)
    public List<String> getScoreDrlList() {
        return scoreDrlList;
    }

    /**
     * @deprecated All support for Score DRL was removed when Timefold was forked from OptaPlanner.
     *             See <a href="https://timefold.ai/blog/migrating-score-drl-to-constraint-streams">DRL to Constraint Streams
     *             migration recipe</a>.
     */
    @Deprecated(forRemoval = true)
    public void setScoreDrlList(List<String> scoreDrlList) {
        this.scoreDrlList = scoreDrlList;
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
        this.easyScoreCalculatorClass = easyScoreCalculatorClass;
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
        this.constraintProviderClass = constraintProviderClass;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withConstraintProviderCustomProperties(
                    @NonNull Map<@NonNull String, @NonNull String> constraintProviderCustomProperties) {
        this.constraintProviderCustomProperties = constraintProviderCustomProperties;
        return this;
    }

    /**
     * @deprecated There is only one implementation, so this method is deprecated.
     *             This method no longer has any effect.
     */
    @Deprecated(forRemoval = true, since = "1.16.0")
    public @NonNull ScoreDirectorFactoryConfig
            withConstraintStreamImplType(@NonNull ConstraintStreamImplType constraintStreamImplType) {
        this.constraintStreamImplType = constraintStreamImplType;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withConstraintStreamAutomaticNodeSharing(@NonNull Boolean constraintStreamAutomaticNodeSharing) {
        this.constraintStreamAutomaticNodeSharing = constraintStreamAutomaticNodeSharing;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withIncrementalScoreCalculatorClass(
                    @NonNull Class<? extends IncrementalScoreCalculator> incrementalScoreCalculatorClass) {
        this.incrementalScoreCalculatorClass = incrementalScoreCalculatorClass;
        return this;
    }

    public @NonNull ScoreDirectorFactoryConfig
            withIncrementalScoreCalculatorCustomProperties(
                    @NonNull Map<@NonNull String, @NonNull String> incrementalScoreCalculatorCustomProperties) {
        this.incrementalScoreCalculatorCustomProperties = incrementalScoreCalculatorCustomProperties;
        return this;
    }

    /**
     * @deprecated All support for Score DRL was removed when Timefold was forked from OptaPlanner.
     *             See <a href="https://timefold.ai/blog/migrating-score-drl-to-constraint-streams">DRL to Constraint Streams
     *             migration recipe</a>.
     */
    @Deprecated(forRemoval = true)
    public ScoreDirectorFactoryConfig withScoreDrlList(List<String> scoreDrlList) {
        this.scoreDrlList = scoreDrlList;
        return this;
    }

    /**
     * @deprecated All support for Score DRL was removed when Timefold was forked from OptaPlanner.
     *             See <a href="https://timefold.ai/blog/migrating-score-drl-to-constraint-streams">DRL to Constraint Streams
     *             migration recipe</a>.
     */
    @Deprecated(forRemoval = true)
    public ScoreDirectorFactoryConfig withScoreDrls(String... scoreDrls) {
        this.scoreDrlList = Arrays.asList(scoreDrls);
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
                easyScoreCalculatorClass, inheritedConfig.getEasyScoreCalculatorClass());
        easyScoreCalculatorCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                easyScoreCalculatorCustomProperties, inheritedConfig.getEasyScoreCalculatorCustomProperties());
        constraintProviderClass = ConfigUtils.inheritOverwritableProperty(
                constraintProviderClass, inheritedConfig.getConstraintProviderClass());
        constraintProviderCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                constraintProviderCustomProperties, inheritedConfig.getConstraintProviderCustomProperties());
        constraintStreamImplType = ConfigUtils.inheritOverwritableProperty(
                constraintStreamImplType, inheritedConfig.getConstraintStreamImplType());
        constraintStreamAutomaticNodeSharing = ConfigUtils.inheritOverwritableProperty(constraintStreamAutomaticNodeSharing,
                inheritedConfig.getConstraintStreamAutomaticNodeSharing());
        incrementalScoreCalculatorClass = ConfigUtils.inheritOverwritableProperty(
                incrementalScoreCalculatorClass, inheritedConfig.getIncrementalScoreCalculatorClass());
        incrementalScoreCalculatorCustomProperties = ConfigUtils.inheritMergeableMapProperty(
                incrementalScoreCalculatorCustomProperties, inheritedConfig.getIncrementalScoreCalculatorCustomProperties());
        scoreDrlList = ConfigUtils.inheritMergeableListProperty(
                scoreDrlList, inheritedConfig.getScoreDrlList());
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
        classVisitor.accept(easyScoreCalculatorClass);
        classVisitor.accept(constraintProviderClass);
        classVisitor.accept(incrementalScoreCalculatorClass);
        if (assertionScoreDirectorFactory != null) {
            assertionScoreDirectorFactory.visitReferencedClasses(classVisitor);
        }
    }

}
