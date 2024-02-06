package ai.timefold.solver.quarkus.it.devui;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Path;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.it.devui.domain.StringLengthVariableListener;
import ai.timefold.solver.quarkus.it.devui.domain.TestdataStringLengthShadowEntity;
import ai.timefold.solver.quarkus.it.devui.domain.TestdataStringLengthShadowSolution;
import ai.timefold.solver.quarkus.it.devui.solver.TestdataStringLengthConstraintProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

@SuppressWarnings("java:S5786") //The public modifier is required when using QuarkusDevModeTest extension.
public class TimefoldDevUIMultipleSolversTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setBuildSystemProperty("quarkus.timefold.solver.\"solver1\".environment-mode", "FULL_ASSERT")
            .setBuildSystemProperty("quarkus.timefold.solver.\"solver2\".environment-mode", "REPRODUCIBLE")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(StringLengthVariableListener.class,
                            TestdataStringLengthShadowEntity.class, TestdataStringLengthShadowSolution.class,
                            TestdataStringLengthConstraintProvider.class, TimefoldTestMultipleResource.class));

    @Path("/timefold/test")
    public static class TimefoldTestMultipleResource {

        @Inject
        @Named("solver1")
        SolverManager<TestdataStringLengthShadowSolution, Long> solverManager;

        @Inject
        @Named("solver2")
        SolverManager<TestdataStringLengthShadowSolution, String> solverManager2;
    }

    public TimefoldDevUIMultipleSolversTest() {
        super("Timefold Solver");
    }

    @Test
    void testSolverConfigPage() throws Exception {
        JsonNode configResponse = super.executeJsonRPCMethod("getConfig");
        assertSolverConfigPage(configResponse.get("config").get("solver1").asText(), "FULL_ASSERT");
        assertSolverConfigPage(configResponse.get("config").get("solver2").asText(), "REPRODUCIBLE");
    }

    private void assertSolverConfigPage(String solverConfig, String environment) {
        assertThat(solverConfig).isEqualToIgnoringWhitespace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<!--Properties that can be set at runtime are not included-->\n"
                        + "<solver>\n"
                        + "  <environmentMode>" + environment + "</environmentMode>\n"
                        + "  <solutionClass>" + TestdataStringLengthShadowSolution.class.getCanonicalName()
                        + "</solutionClass>\n"
                        + "  <entityClass>" + TestdataStringLengthShadowEntity.class.getCanonicalName() + "</entityClass>\n"
                        + "  <domainAccessType>GIZMO</domainAccessType>\n"
                        + "  <scoreDirectorFactory>\n"
                        + "    <constraintProviderClass>" + TestdataStringLengthConstraintProvider.class.getCanonicalName()
                        + "</constraintProviderClass>\n"
                        + "  </scoreDirectorFactory>\n"
                        + "  <termination/>\n"
                        + "</solver>");
    }

    @Test
    void testModelPage() throws Exception {
        JsonNode modelResponse = super.executeJsonRPCMethod("getModelInfo");
        assertModelPage(modelResponse, "solver1");
        assertModelPage(modelResponse, "solver2");
    }

    private void assertModelPage(JsonNode modelResponse, String solverName) {
        assertThat(modelResponse.get(solverName).get("solutionClass").asText())
                .contains(TestdataStringLengthShadowSolution.class.getCanonicalName());
        assertThat(modelResponse.get(solverName).get("entityClassList"))
                .containsExactly(
                        new TextNode(TestdataStringLengthShadowEntity.class.getCanonicalName()));
        assertThat(modelResponse.get(solverName).get("entityClassToGenuineVariableListMap")).hasSize(1);
        assertThat(modelResponse.get(solverName).get("entityClassToGenuineVariableListMap")
                .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                .containsExactly(new TextNode("value"));
        assertThat(modelResponse.get(solverName).get("entityClassToShadowVariableListMap")).hasSize(1);
        assertThat(modelResponse.get(solverName).get("entityClassToShadowVariableListMap")
                .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                .containsExactly(new TextNode("length"));
    }

    @Test
    void testConstraintsPage() throws Exception {
        JsonNode constraintsResponse = super.executeJsonRPCMethod("getConstraints");
        assertConstraintsPage(constraintsResponse, "solver1");
        assertConstraintsPage(constraintsResponse, "solver2");
    }

    private void assertConstraintsPage(JsonNode constraintsResponse, String solverName) {
        assertThat(constraintsResponse.get(solverName)).containsExactly(
                new TextNode(TestdataStringLengthShadowSolution.class.getPackage()
                        .getName() + "/Don't assign 2 entities the same value."),
                new TextNode(
                        TestdataStringLengthShadowSolution.class.getPackage().getName() + "/Maximize value length"));
    }
}
