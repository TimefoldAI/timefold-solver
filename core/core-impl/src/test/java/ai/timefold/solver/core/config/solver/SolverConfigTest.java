package ai.timefold.solver.core.config.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.impl.ai.TimefoldXmlSerializationException;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.core.impl.partitionedsearch.partitioner.SolutionPartitioner;
import ai.timefold.solver.core.impl.phase.custom.CustomPhaseCommand;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.extended.TestdataAnnotatedExtendedEntity;
import ai.timefold.solver.core.impl.testdata.domain.extended.TestdataAnnotatedExtendedSolution;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXParseException;

class SolverConfigTest {

    private static final String TEST_SOLVER_CONFIG_WITH_NAMESPACE = "testSolverConfigWithNamespace.xml";
    private static final String TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE = "testSolverConfigWithoutNamespace.xml";
    private final SolverConfigIO solverConfigIO = new SolverConfigIO();

    @ParameterizedTest
    @ValueSource(strings = { TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE, TEST_SOLVER_CONFIG_WITH_NAMESPACE })
    void xmlConfigRemainsSameAfterReadWrite(String solverConfigResource) throws IOException {
        SolverConfig jaxbSolverConfig = readSolverConfig(solverConfigResource);

        Writer stringWriter = new StringWriter();
        solverConfigIO.write(jaxbSolverConfig, stringWriter);
        String jaxbString = stringWriter.toString();

        String originalXml = IOUtils.toString(
                SolverConfigTest.class.getResourceAsStream(solverConfigResource), StandardCharsets.UTF_8);

        // During writing the solver config, the solver element's namespace is removed.
        String solverElementWithNamespace = SolverConfig.XML_ELEMENT_NAME + " xmlns=\"" + SolverConfig.XML_NAMESPACE + "\"";
        if (originalXml.contains(solverElementWithNamespace)) {
            originalXml = originalXml.replace(solverElementWithNamespace, SolverConfig.XML_ELEMENT_NAME);
        }
        assertThat(jaxbString).isXmlEqualTo(originalXml);
    }

    @Test
    void readXmlConfigWithNamespace() {
        SolverConfig solverConfig = readSolverConfig(TEST_SOLVER_CONFIG_WITH_NAMESPACE);

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
        try (Reader reader = new InputStreamReader(SolverConfigTest.class.getResourceAsStream(solverConfigResource))) {
            return solverConfigIO.read(reader);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    @Test
    void whiteCharsInClassName() {
        String solutionClassName = "ai.timefold.solver.core.impl.testdata.domain.TestdataSolution";
        String xmlFragment = String.format("<solver xmlns=\"https://timefold.ai/xsd/solver\">%n"
                + "  <solutionClass>  %s  %n" // Intentionally included white chars around the class name.
                + "  </solutionClass>%n"
                + "</solver>", solutionClassName);
        SolverConfig solverConfig = solverConfigIO.read(new StringReader(xmlFragment));
        assertThat(solverConfig.getSolutionClass().getName()).isEqualTo(solutionClassName);
    }

    @Test
    void readAndValidateInvalidSolverConfig_failsIndicatingTheIssue() {
        String solverConfigXml = "<solver xmlns=\"https://timefold.ai/xsd/solver\">\n"
                + "  <constructionHeuristic>\n"
                + "      <changeMoveSelector>\n"
                + "        <valueSelector>\n"
                // Intentionally wrong: variableName should be an attribute of the <valueSelector/>
                + "          <variableName>subValue</variableName>\n"
                + "        </valueSelector>\n"
                + "      </changeMoveSelector>\n"
                + "  </constructionHeuristic>\n"
                + "</solver>";

        StringReader stringReader = new StringReader(solverConfigXml);
        assertThatExceptionOfType(TimefoldXmlSerializationException.class)
                .isThrownBy(() -> solverConfigIO.read(stringReader))
                .withRootCauseExactlyInstanceOf(SAXParseException.class)
                .withMessageContaining("Node: variableName");
    }

    @Test
    void withEasyScoreCalculatorClass() {
        SolverConfig solverConfig = new SolverConfig();
        assertThat(solverConfig.getScoreDirectorFactoryConfig()).isNull();
        solverConfig.withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class);
        assertThat(solverConfig.getScoreDirectorFactoryConfig().getEasyScoreCalculatorClass())
                .isEqualTo(DummyEasyScoreCalculator.class);
    }

    @Test
    void withConstraintProviderClass() {
        SolverConfig solverConfig = new SolverConfig();
        assertThat(solverConfig.getScoreDirectorFactoryConfig()).isNull();
        solverConfig.withConstraintProviderClass(DummyConstraintProvider.class);
        assertThat(solverConfig.getScoreDirectorFactoryConfig().getConstraintProviderClass())
                .isEqualTo(DummyConstraintProvider.class);
    }

    @Test
    void withTerminationSpentLimit() {
        SolverConfig solverConfig = new SolverConfig();
        assertThat(solverConfig.getTerminationConfig()).isNull();
        solverConfig.withTerminationSpentLimit(Duration.ofMinutes(2));
        assertThat(solverConfig.getTerminationConfig().getSpentLimit())
                .isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void inherit() {
        SolverConfig originalSolverConfig = readSolverConfig(TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE);
        SolverConfig inheritedSolverConfig =
                new SolverConfig().inherit(originalSolverConfig);
        assertThat(inheritedSolverConfig).usingRecursiveComparison().isEqualTo(originalSolverConfig);
    }

    @Test
    void visitReferencedClasses() {
        SolverConfig solverConfig = readSolverConfig(TEST_SOLVER_CONFIG_WITHOUT_NAMESPACE);
        Consumer<Class<?>> classVisitor = mock(Consumer.class);
        solverConfig.visitReferencedClasses(classVisitor);
        verify(classVisitor, atLeastOnce()).accept(TestdataAnnotatedExtendedSolution.class);
        verify(classVisitor, atLeastOnce()).accept(TestdataEntity.class);
        verify(classVisitor, atLeastOnce()).accept(TestdataAnnotatedExtendedEntity.class);
        verify(classVisitor, atLeastOnce()).accept(DummyEasyScoreCalculator.class);
        verify(classVisitor, atLeastOnce()).accept(DummyConstraintProvider.class);
        verify(classVisitor, atLeastOnce()).accept(DummyIncrementalScoreCalculator.class);
        verify(classVisitor, atLeastOnce()).accept(DummyEntityFilter.class);
        verify(classVisitor, atLeastOnce()).accept(DummyValueFilter.class);
        verify(classVisitor, atLeastOnce()).accept(DummyChangeMoveFilter.class);
        verify(classVisitor, atLeastOnce()).accept(DummyMoveIteratorFactory.class);
        verify(classVisitor, atLeastOnce()).accept(DummyMoveListFactory.class);
        verify(classVisitor, atLeastOnce()).accept(CustomPhaseCommand.class);
    }

    /* Dummy classes below are referenced from the testSolverConfig.xml used in this test case. */

    public static abstract class DummySolutionPartitioner implements SolutionPartitioner<TestdataSolution> {
    }

    public static abstract class DummyEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {
    }

    public static abstract class DummyIncrementalScoreCalculator
            implements IncrementalScoreCalculator<TestdataSolution, SimpleScore> {
    }

    public static abstract class DummyConstraintProvider implements ConstraintProvider {
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

}
