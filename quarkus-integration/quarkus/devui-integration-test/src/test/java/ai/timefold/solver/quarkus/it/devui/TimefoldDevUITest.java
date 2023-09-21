package ai.timefold.solver.quarkus.it.devui;

import static org.assertj.core.api.Assertions.assertThat;

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
                    .addPackages(true, TimefoldTestResource.class.getPackage().getName()));

    public TimefoldDevUITest() {
        super("Timefold Solver");
    }

    @Test
    void testSolverConfigPage() throws Exception {
        JsonNode configResponse = super.executeJsonRPCMethod("getConfig");
        String solverConfig = configResponse.get("config").asText();
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
                        + "</solver>");
    }

    @Test
    void testModelPage() throws Exception {
        JsonNode modelResponse = super.executeJsonRPCMethod("getModelInfo");
        assertThat(modelResponse.get("solutionClass").asText())
                .contains(TestdataStringLengthShadowSolution.class.getCanonicalName());
        assertThat(modelResponse.get("entityClassList"))
                .containsExactly(
                        new TextNode(TestdataStringLengthShadowEntity.class.getCanonicalName()));
        assertThat(modelResponse.get("entityClassToGenuineVariableListMap")).hasSize(1);
        assertThat(modelResponse.get("entityClassToGenuineVariableListMap")
                .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                .containsExactly(new TextNode("value"));
        assertThat(modelResponse.get("entityClassToShadowVariableListMap")).hasSize(1);
        assertThat(modelResponse.get("entityClassToShadowVariableListMap")
                .get(TestdataStringLengthShadowEntity.class.getCanonicalName()))
                .containsExactly(new TextNode("length"));
    }

    @Test
    void testConstraintsPage() throws Exception {
        JsonNode constraintsResponse = super.executeJsonRPCMethod("getConstraints");
        assertThat(constraintsResponse).containsExactly(
                new TextNode(TestdataStringLengthShadowSolution.class.getPackage()
                        .getName() + "/Don't assign 2 entities the same value."),
                new TextNode(TestdataStringLengthShadowSolution.class.getPackage().getName() + "/Maximize value length"));
    }
}
