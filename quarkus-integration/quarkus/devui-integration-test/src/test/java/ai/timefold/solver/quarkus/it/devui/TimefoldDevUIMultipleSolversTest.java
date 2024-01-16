package ai.timefold.solver.quarkus.it.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Path;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.quarkus.it.devui.domain.StringLengthVariableListener;
import ai.timefold.solver.quarkus.it.devui.domain.TestdataStringLengthShadowEntity;
import ai.timefold.solver.quarkus.it.devui.domain.TestdataStringLengthShadowSolution;
import ai.timefold.solver.quarkus.it.devui.solver.TestdataStringLengthConstraintProvider;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

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
        SolverManager<TestdataStringLengthShadowSolution, Long> solverManager2;
    }

    public TimefoldDevUIMultipleSolversTest() {
        super("Timefold Solver");
    }

    @Test
    void testSolverConfigPage() throws Exception {
        JsonNode configResponse = super.executeJsonRPCMethod("getConfig");
        List.of(Pair.of("solver1", "FULL_ASSERT"), Pair.of("solver2", "REPRODUCIBLE")).forEach(key -> {
            String solverConfig = configResponse.get("config").get(key.getLeft()).asText();
            assertThat(solverConfig).isEqualToIgnoringWhitespace(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<!--Properties that can be set at runtime are not included-->\n"
                            + "<solver>\n"
                            + "  <environmentMode>" + key.getRight() + "</environmentMode>\n"
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
        });
    }

    @Test
    void testModelPage() {
        List.of("solver1", "solver2").forEach(key -> {
            try {
                JsonNode modelResponse = super.executeJsonRPCMethod("getModelInfo");
                assertThat(modelResponse.get(key).get("solutionClass").asText())
                        .contains(TestdataStringLengthShadowSolution.class.getCanonicalName());
                assertThat(modelResponse.get(key).get("entityClassList"))
                        .containsExactly(
                                new TextNode(TestdataStringLengthShadowEntity.class.getCanonicalName()));
                assertThat(modelResponse.get(key).get("entityClassToGenuineVariableListMap")).hasSize(1);
                assertThat(modelResponse.get(key).get("entityClassToGenuineVariableListMap")
                        .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                        .containsExactly(new TextNode("value"));
                assertThat(modelResponse.get(key).get("entityClassToShadowVariableListMap")).hasSize(1);
                assertThat(modelResponse.get(key).get("entityClassToShadowVariableListMap")
                        .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                        .containsExactly(new TextNode("length"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testConstraintsPage() throws Exception {
        List.of("solver1", "solver2").forEach(key -> {
            try {
                JsonNode constraintsResponse = super.executeJsonRPCMethod("getConstraints");
                assertThat(constraintsResponse.get(key)).containsExactly(
                        new TextNode(TestdataStringLengthShadowSolution.class.getPackage()
                                .getName() + "/Don't assign 2 entities the same value."),
                        new TextNode(
                                TestdataStringLengthShadowSolution.class.getPackage().getName() + "/Maximize value length"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
