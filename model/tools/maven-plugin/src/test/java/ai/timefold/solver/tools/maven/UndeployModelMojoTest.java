package ai.timefold.solver.tools.maven;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
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
public class UndeployModelMojoTest {

    @RegisterExtension
    static WireMockExtension wm1 = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private InMemoryMojoLog log = new InMemoryMojoLog();

    @BeforeEach
    void setUp() throws IOException {
        log.clear();
        wm1.resetAll();

        wm1.stubFor(delete(urlPathEqualTo("/api/platform/v1/models/existing"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        wm1.stubFor(delete(urlPathEqualTo("/api/platform/v1/models/notexisting"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // copy sample model descriptor
        Path modelDescriptor = Paths.get("src", "test", "resources", "model-descriptor.zip");
        Path targetModelDescriptor = Paths.get("target", "model-descriptor.zip");

        Files.copy(modelDescriptor, targetModelDescriptor, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    @MojoParameter(name = "skip", value = "true")
    @InjectMojo(goal = "undeploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testSkipByParameter(UndeployModelMojo mojo) throws Exception {

        mojo.setLog(log);
        mojo.execute();
        // assert that plugin executed and produced expected logs
        log.assertContains("Model undeployment skipped by configuration", Level.INFO);
    }

    @Test
    @MojoParameter(name = "key", value = "existing")
    @InjectMojo(goal = "undeploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testUndeploy(UndeployModelMojo mojo) throws Exception {

        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        // verify plugin performed expected HTTP call
        wm1.verify(1, deleteRequestedFor(urlPathEqualTo("/api/platform/v1/models/existing")));

        // assert that plugin executed and produced expected logs
        log.assertContains("Model .* has been successfully undeployed from platform.*", Level.INFO);
    }

    @Test
    @MojoParameter(name = "key", value = "notexisting")
    @InjectMojo(goal = "undeploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testUndeployNotExisting(UndeployModelMojo mojo) throws Exception {

        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        assertThatThrownBy(() -> mojo.execute()).isInstanceOf(IllegalStateException.class)
                .hasMessage("Model undeploy failed with 404 status code");

        // verify plugin performed expected HTTP call
        wm1.verify(1, deleteRequestedFor(urlPathEqualTo("/api/platform/v1/models/notexisting")));

    }
}
