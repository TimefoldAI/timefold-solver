package ai.timefold.solver.tools.maven;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog;
import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog.Level;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

@MojoTest
public class DeployModelMojoTest {

    @RegisterExtension
    static WireMockExtension wm1 = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private InMemoryMojoLog log = new InMemoryMojoLog();

    @BeforeEach
    void setUp() throws IOException {
        log.clear();
        wm1.resetAll();

        // represents already existing model so patch would be required
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("existing"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        wm1.stubFor(patch(urlPathEqualTo("/api/platform/v1/models/existing"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // represents successful registration on first call - no other model with given id existed
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .atPriority(15)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // Fallback: for other requests to the same path return 401 Unauthorized
        wm1.stubFor(get(urlEqualTo("/api/platform/v1/aboutme"))
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Unauthorized\"}")));

        // copy sample model descriptor
        Path modelDescriptor = Paths.get("src", "test", "resources", "model-descriptor.zip");
        Path targetModelDescriptor = Paths.get("target", "model-descriptor.zip");

        Files.copy(modelDescriptor, targetModelDescriptor, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    @MojoParameter(name = "skip", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testSkipByParameter(DeployModelMojo mojo) throws Exception {

        mojo.setLog(log);
        mojo.execute();
        // assert that plugin executed and produced expected logs
        log.assertContains("Model deployment skipped by configuration", Level.INFO);
    }

    @Test
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testRegisterModel(DeployModelMojo mojo) throws Exception {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        // verify plugin performed expected HTTP call
        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("model-key")));

        // assert that plugin executed and produced expected logs
        log.assertContains("Type is not explicitly specified so it was computed based on tenants and is set to Private",
                Level.DEBUG);
        log.assertContains("Model .* has been successfully deployed into platform.*", Level.INFO);
    }

    @Test
    @MojoParameter(name = "key", value = "existing")
    @MojoParameter(name = "overwrite", value = "true")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testRegisterModelWithPatch(DeployModelMojo mojo) throws Exception {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        // verify plugin performed expected HTTP call
        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("existing")));

        wm1.verify(1, patchRequestedFor(urlPathEqualTo("/api/platform/v1/models/existing")));

        // assert that plugin executed and produced expected logs
        log.assertContains("Model .* has been successfully updated on platform.*", Level.INFO);
    }

    @Test
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailOnMissingModelDescriptor(DeployModelMojo mojo) throws Exception {
        // delete model descriptor to simulate failure on missing
        Path targetModelDescriptor = Paths.get("target", "model-descriptor.zip");
        Files.deleteIfExists(targetModelDescriptor);

        assertThatThrownBy(() -> mojo.execute()).isInstanceOf(IllegalStateException.class)
                .hasMessage("Model descriptor not found in target folder");

    }

    @Test
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom-shared.xml")
    public void testRegisterModelSharedType(DeployModelMojo mojo) throws Exception {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        // verify plugin performed expected HTTP call
        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("model-key")));

        // assert that plugin executed and produced expected logs
        log.assertContains("Type is not explicitly specified so it was computed based on tenants and is set to Shared",
                Level.DEBUG);
        log.assertContains("Model .* has been successfully deployed into platform.*", Level.INFO);
    }

    @Test
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailOnIncompleteDeploy(DeployModelMojo mojo) throws Exception {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(() -> mojo.execute()).isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "'package' goal was not requested, deploy of timefold model might not be complete, make sure to use 'clean package timefold:deploy' or set '-Dtimefold.model.deploy.descriptorOnly=true'");

    }
}
