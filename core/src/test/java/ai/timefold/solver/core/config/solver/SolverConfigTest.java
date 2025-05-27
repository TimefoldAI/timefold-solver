package ai.timefold.solver.core.config.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.core.impl.io.jaxb.TimefoldXmlSerializationException;
import ai.timefold.solver.core.impl.partitionedsearch.partitioner.SolutionPartitioner;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childtoo.TestdataBothAnnotatedChildEntity;
import ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.childtoo.TestdataBothAnnotatedSolution;
import ai.timefold.solver.core.testdomain.interfaces.TestdataInterfaceConstraintProvider;
import ai.timefold.solver.core.testdomain.interfaces.TestdataInterfaceEntity;
import ai.timefold.solver.core.testdomain.interfaces.TestdataInterfaceSolution;
import ai.timefold.solver.core.testdomain.record.TestdataRecordEntity;
import ai.timefold.solver.core.testdomain.record.TestdataRecordSolution;

import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXParseException;

class SolverConfigTest {

    private static final String TEST_SOLVER_CONFIG_WITH_NAMESPACE = "testSolverConfigWithNamespace.xml";
    private static final String TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE = "testSolverConfigWithoutNamespace.xml";
    private static final String TEST_SOLVER_CONFIG_WITH_ENUM_SET = "testSolverConfigWithEnumSet.xml";
    private final SolverConfigIO solverConfigIO = new SolverConfigIO();

    @ParameterizedTest
    @ValueSource(strings = { TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE, TEST_SOLVER_CONFIG_WITH_NAMESPACE })
    void xmlConfigRemainsSameAfterReadWrite(String solverConfigResource) throws IOException {
        var jaxbSolverConfig = readSolverConfig(solverConfigResource);

        var stringWriter = new StringWriter();
        solverConfigIO.write(jaxbSolverConfig, stringWriter);
        var jaxbString = stringWriter.toString();

        var originalXml =
                IOUtils.toString(SolverConfigTest.class.getResourceAsStream(solverConfigResource), StandardCharsets.UTF_8);

        // During writing the solver config, the solver element's namespace is removed.
        var solverElementWithNamespace = """
                %s xmlns="%s"
                """.formatted(SolverConfig.XML_ELEMENT_NAME, SolverConfig.XML_NAMESPACE)
                .trim();
        if (originalXml.contains(solverElementWithNamespace)) {
            originalXml = originalXml.replace(solverElementWithNamespace, SolverConfig.XML_ELEMENT_NAME);
        }
        assertThat(jaxbString).isEqualToIgnoringWhitespace(originalXml);
    }

    @Test
    void readXmlConfigWithNamespace() {
        var solverConfig = readSolverConfig(TEST_SOLVER_CONFIG_WITH_NAMESPACE);

        assertThat(solverConfig).isNotNull();
        assertThat(solverConfig.getPhaseConfigList())
                .hasSize(2)
                .hasOnlyElementsOfTypes(ConstructionHeuristicPhaseConfig.class, LocalSearchPhaseConfig.class);
        assertThat(solverConfig.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(solverConfig.getSolutionClass()).isAssignableFrom(TestdataSolution.class);
        assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                .isAssignableFrom(DummyConstraintProvider.class);
    }

    private SolverConfig readSolverConfig(String solverConfigResource) {
        try (var reader = new InputStreamReader(SolverConfigTest.class.getResourceAsStream(solverConfigResource))) {
            return solverConfigIO.read(reader);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    @Test
    void whiteCharsInClassName() {
        var solutionClassName = "ai.timefold.solver.core.testdomain.TestdataSolution";
        // Intentionally included white chars around the class name.
        var xmlFragment = """
                <solver xmlns="https://timefold.ai/xsd/solver">
                    <solutionClass>  %s\s
                    </solutionClass>
                </solver>""".formatted(solutionClassName);
        var solverConfig = solverConfigIO.read(new StringReader(xmlFragment));
        assertThat(solverConfig.getSolutionClass().getName()).isEqualTo(solutionClassName);
    }

    @Test
    void readAndValidateInvalidSolverConfig_failsIndicatingTheIssue() {
        // Intentionally wrong: variableName should be an attribute of the <valueSelector/>
        var solverConfigXml = """
                <solver xmlns="https://timefold.ai/xsd/solver">
                    <constructionHeuristic>
                        <changeMoveSelector>
                            <valueSelector>
                                <variableName>subValue</variableName>
                            </valueSelector>
                        </changeMoveSelector>
                    </constructionHeuristic>
                </solver>""";

        var stringReader = new StringReader(solverConfigXml);
        assertThatExceptionOfType(TimefoldXmlSerializationException.class)
                .isThrownBy(() -> solverConfigIO.read(stringReader))
                .withRootCauseExactlyInstanceOf(SAXParseException.class)
                .withMessageContaining("Node: variableName");
    }

    @Test
    void withEasyScoreCalculatorClass() {
        var solverConfig = new SolverConfig();
        assertThat(solverConfig.getScoreDirectorFactoryConfig()).isNull();
        solverConfig.withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class);
        assertThat(solverConfig.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass())
                .isEqualTo(DummyEasyScoreCalculator.class);
    }

    @Test
    void withConstraintProviderClass() {
        var solverConfig = new SolverConfig();
        assertThat(solverConfig.getScoreDirectorFactoryConfig()).isNull();
        solverConfig.withConstraintProviderClass(DummyConstraintProvider.class);
        assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                .isEqualTo(DummyConstraintProvider.class);
    }

    @Test
    void withEnablePreviewFeatureSet() {
        var solverConfig = new SolverConfig();
        assertThat(solverConfig.getEnablePreviewFeatureSet()).isNull();
        solverConfig.withPreviewFeature(PreviewFeature.DIVERSIFIED_LATE_ACCEPTANCE);
        assertThat(solverConfig.getEnablePreviewFeatureSet())
                .hasSameElementsAs(List.of(PreviewFeature.DIVERSIFIED_LATE_ACCEPTANCE));
    }

    @Test
    void withTerminationSpentLimit() {
        var solverConfig = new SolverConfig();
        var duration = Duration.ofMinutes(2);
        assertThat(solverConfig.getTerminationConfig()).isNull();
        solverConfig.withTerminationSpentLimit(duration);
        assertThat(solverConfig.getTerminationConfig().getSpentLimit())
                .isEqualTo(duration);
    }

    @Test
    void withTerminationUnimprovedSpentLimit() {
        var solverConfig = new SolverConfig();
        var duration = Duration.ofMinutes(2);
        assertThat(solverConfig.getTerminationConfig()).isNull();
        solverConfig.withTerminationUnimprovedSpentLimit(duration);
        assertThat(solverConfig.getTerminationConfig().getUnimprovedSpentLimit())
                .isEqualTo(duration);
    }

    @Test
    void inherit() {
        var originalSolverConfig = readSolverConfig(TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE);
        var inheritedSolverConfig =
                new SolverConfig().inherit(originalSolverConfig);
        assertThat(inheritedSolverConfig).usingRecursiveComparison().isEqualTo(originalSolverConfig);
    }

    @Test
    void inheritEnumSet() {
        var originalSolverConfig = readSolverConfig(TEST_SOLVER_CONFIG_WITH_ENUM_SET);
        var inheritedSolverConfig =
                new SolverConfig().inherit(originalSolverConfig);
        assertThat(inheritedSolverConfig).usingRecursiveComparison().isEqualTo(originalSolverConfig);
    }

    @Test
    void visitReferencedClasses() {
        var solverConfig = readSolverConfig(TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE);
        var classVisitor = (Consumer<Class<?>>) mock(Consumer.class);
        solverConfig.visitReferencedClasses(classVisitor);
        verify(classVisitor, atLeastOnce()).accept(TestdataBothAnnotatedSolution.class);
        verify(classVisitor, atLeastOnce()).accept(TestdataEntity.class);
        verify(classVisitor, atLeastOnce()).accept(TestdataBothAnnotatedChildEntity.class);
        verify(classVisitor, atLeastOnce()).accept(DummyEasyScoreCalculator.class);
        verify(classVisitor, atLeastOnce()).accept(DummyConstraintProvider.class);
        verify(classVisitor, atLeastOnce()).accept(DummyIncrementalScoreCalculator.class);
        verify(classVisitor, atLeastOnce()).accept(DummyNearbyDistanceClass.class);
        verify(classVisitor, atLeastOnce()).accept(DummyEntityFilter.class);
        verify(classVisitor, atLeastOnce()).accept(DummyValueFilter.class);
        verify(classVisitor, atLeastOnce()).accept(DummyChangeMoveFilter.class);
        verify(classVisitor, atLeastOnce()).accept(DummyMoveIteratorFactory.class);
        verify(classVisitor, atLeastOnce()).accept(DummyMoveListFactory.class);
        verify(classVisitor, atLeastOnce()).accept(PhaseCommand.class);
    }

    @Test
    void solutionIsARecord() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(DummyRecordSolution.class)
                .withEntityClasses(TestdataEntity.class);
        assertThatThrownBy(() -> SolverFactory.create(solverConfig))
                .hasMessageContaining(DummyRecordSolution.class.getSimpleName())
                .hasMessageContaining("record");
    }

    @Test
    void entityIsARecord() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(DummySolutionWithRecordEntity.class)
                .withEntityClasses(DummyRecordEntity.class);
        assertThatThrownBy(() -> SolverFactory.create(solverConfig))
                .hasMessageContaining(DummyRecordEntity.class.getSimpleName())
                .hasMessageContaining("record");
    }

    @Test
    void variableWithPlanningIdIsARecord() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataRecordSolution.class)
                .withEntityClasses(TestdataRecordEntity.class)
                .withEasyScoreCalculatorClass(DummyRecordEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()); // Run CH and finish.
        var solver = SolverFactory.create(solverConfig)
                .buildSolver();

        var solution = TestdataRecordSolution.generateSolution();
        assertThatNoException().isThrownBy(() -> solver.solve(solution));
    }

    @Test
    void domainClassesAreInterfaces() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataInterfaceSolution.class)
                .withEntityClasses(TestdataInterfaceEntity.class)
                .withConstraintProviderClass(TestdataInterfaceConstraintProvider.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()); // Run CH and finish.
        var solver = SolverFactory.create(solverConfig)
                .buildSolver();

        var solution = TestdataInterfaceSolution.generateSolution();
        assertThatNoException().isThrownBy(() -> solver.solve(solution));
    }

    @Test
    void entityWithTwoPlanningListVariables() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(DummySolutionWithTwoListVariablesEntity.class)
                .withEntityClasses(DummyEntityWithTwoListVariables.class)
                .withEasyScoreCalculatorClass(DummyRecordEasyScoreCalculator.class);
        assertThatThrownBy(() -> SolverFactory.create(solverConfig))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(DummyEntityWithTwoListVariables.class.getSimpleName())
                .hasMessageContaining("firstListVariable")
                .hasMessageContaining("secondListVariable");
    }

    @PlanningSolution
    private record DummyRecordSolution(
            @PlanningEntityCollectionProperty List<TestdataEntity> entities,
            @PlanningScore SimpleScore score) {

    }

    @PlanningSolution
    private static class DummySolutionWithRecordEntity {

        @PlanningEntityCollectionProperty
        List<DummyRecordEntity> entities;
        @PlanningScore
        SimpleScore score;

    }

    @PlanningEntity
    private record DummyRecordEntity(
            @PlanningVariable String variable) {

    }

    @PlanningSolution
    private static class DummySolutionWithMixedSimpleAndListVariableEntity {

        @PlanningEntityCollectionProperty
        List<DummyEntityWithMixedSimpleAndListVariable> entities;

        @ValueRangeProvider(id = "listValueRange")
        ValueRange<DummyEntityForListVariable> listValueRange;

        @ValueRangeProvider(id = "basicValueRange")
        ValueRange<Integer> basicValueRange;

        @PlanningScore
        SimpleScore score;

    }

    @PlanningEntity
    private static class DummyEntityWithMixedSimpleAndListVariable {

        @PlanningListVariable(valueRangeProviderRefs = "listValueRange")
        private List<DummyEntityForListVariable> listVariable;

        @PlanningVariable(valueRangeProviderRefs = "basicValueRange")
        Integer basicVariable;

    }

    @PlanningSolution
    private static class DummySolutionWithTwoListVariablesEntity {

        @PlanningEntityCollectionProperty
        List<DummyEntityWithTwoListVariables> entities;

        @ValueRangeProvider(id = "firstListValueRange")
        ValueRange<DummyEntityForListVariable> firstListValueRange;

        @ValueRangeProvider(id = "secondListValueRange")
        ValueRange<DummyEntityForListVariable> secondListValueRange;

        @PlanningScore
        SimpleScore score;

    }

    @PlanningEntity
    private static class DummyEntityWithTwoListVariables {

        @PlanningListVariable(valueRangeProviderRefs = "firstListValueRange")
        private List<DummyEntityForListVariable> firstListVariable;

        @PlanningListVariable(valueRangeProviderRefs = "secondListValueRange")
        private List<DummyEntityForListVariable> secondListVariable;

    }

    @PlanningEntity
    private static class DummyEntityForListVariable {

    }

    public static class DummyRecordEasyScoreCalculator implements EasyScoreCalculator<TestdataRecordSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataRecordSolution solution) {
            return SimpleScore.of(solution.getEntityList().size());
        }
    }

    /* Dummy classes below are referenced from the testSolverConfig.xml used in this test case. */

    public abstract static class DummySolutionPartitioner implements SolutionPartitioner<TestdataSolution> {
    }

    public abstract static class DummyEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {
    }

    public abstract static class DummyIncrementalScoreCalculator
            implements IncrementalScoreCalculator<TestdataSolution, SimpleScore> {
    }

    public abstract static class DummyConstraintProvider implements ConstraintProvider {
    }

    public abstract static class DummyValueFilter implements SelectionFilter<TestdataSolution, TestdataValue> {
    }

    public abstract static class DummyEntityFilter implements SelectionFilter<TestdataSolution, TestdataEntity> {
    }

    public abstract static class DummyChangeMoveFilter
            implements SelectionFilter<TestdataSolution, ChangeMove<TestdataSolution>> {
    }

    public abstract static class DummyMoveIteratorFactory implements MoveIteratorFactory<TestdataSolution, DummyMove> {
    }

    public abstract static class DummyMoveListFactory implements MoveListFactory<TestdataSolution> {
    }

    public static class DummyNearbyDistanceClass implements NearbyDistanceMeter<String, String> {

        @Override
        public double getNearbyDistance(String origin, String destination) {
            return 0;
        }
    }

}
