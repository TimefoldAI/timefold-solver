package ai.timefold.solver.quarkus.it.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
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

public class TimefoldDevUITest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataStringLengthShadowEntity.class, TestdataStringLengthShadowSolution.class,
                            TestdataStringLengthConstraintProvider.class,
                            TimefoldTestResource.class));

    @Path("/timefold/test")
    public static class TimefoldTestResource {

        @Inject
        SolverManager<TestdataStringLengthShadowSolution, Long> solverManager;

        @POST
        @Path("/solver-factory")
        @Produces(MediaType.TEXT_PLAIN)
        public String solveWithSolverFactory() {
            TestdataStringLengthShadowSolution planningProblem = new TestdataStringLengthShadowSolution();
            planningProblem.setEntityList(Arrays.asList(
                    new TestdataStringLengthShadowEntity(),
                    new TestdataStringLengthShadowEntity()));
            planningProblem.setValueList(Arrays.asList("a", "bb", "ccc"));
            SolverJob<TestdataStringLengthShadowSolution, Long> solverJob = solverManager.solve(1L, planningProblem);
            try {
                return solverJob.getFinalBestSolution().getScore().toString();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Solving was interrupted.", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Solving failed.", e);
            }
        }
    }

    public TimefoldDevUITest() {
        super("Timefold Solver");
    }

    @Test
    void testSolverConfigPage() throws Exception {
        JsonNode configResponse = super.executeJsonRPCMethod("getConfig");
        String solverConfig = configResponse.get("config").get("default").asText();
        assertThat(solverConfig).isEqualToIgnoringWhitespace(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<!--Properties that can be set at runtime are not included-->\n"
                        + "<solver>\n"
                        + "  <solutionClass>" + TestdataStringLengthShadowSolution.class.getCanonicalName()
                        + "</solutionClass>\n"
                        + "  <entityClass>" + TestdataStringLengthShadowEntity.class.getCanonicalName() + "</entityClass>\n"
                        + "  <domainAccessType>GIZMO</domainAccessType>\n"
                        + "  <scoreDirectorFactory>\n"
                        + "    <constraintProviderClass>" + TestdataStringLengthConstraintProvider.class.getCanonicalName()
                        + "</constraintProviderClass>\n"
                        + "  </scoreDirectorFactory>\n"
                        + "  <termination>\n"
                        + "    <bestScoreLimit>0hard/5soft</bestScoreLimit>\n"
                        + "  </termination>\n"
                        + "</solver>");
    }

    @Test
    void testModelPage() throws Exception {
        JsonNode modelResponse = super.executeJsonRPCMethod("getModelInfo");
        assertThat(modelResponse.get("default").get("solutionClass").asText())
                .contains(TestdataStringLengthShadowSolution.class.getCanonicalName());
        assertThat(modelResponse.get("default").get("entityClassList"))
                .containsExactly(
                        new TextNode(TestdataStringLengthShadowEntity.class.getCanonicalName()));
        assertThat(modelResponse.get("default").get("entityClassToGenuineVariableListMap")).hasSize(1);
        assertThat(modelResponse.get("default").get("entityClassToGenuineVariableListMap")
                .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                .containsExactly(new TextNode("value"));
        assertThat(modelResponse.get("default").get("entityClassToShadowVariableListMap")).hasSize(1);
        assertThat(modelResponse.get("default").get("entityClassToShadowVariableListMap")
                .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                .containsExactly(new TextNode("length"));
    }

    @Test
    void testConstraintsPage() throws Exception {
        JsonNode constraintsResponse = super.executeJsonRPCMethod("getConstraints");
        assertThat(constraintsResponse.get("default")).containsExactly(
                new TextNode(TestdataStringLengthShadowSolution.class.getPackage()
                        .getName() + "/Don't assign 2 entities the same value."),
                new TextNode(TestdataStringLengthShadowSolution.class.getPackage().getName() + "/Maximize value length"));
    }
}
