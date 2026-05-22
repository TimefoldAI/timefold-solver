package ai.timefold.solver.tools.maven;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog;
import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog.Level;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

@MojoTest
public class ConfigureMojoTest {

    @RegisterExtension
    static WireMockExtension wm1 = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private InMemoryMojoLog log = new InMemoryMojoLog();

    @Inject
    private MavenSession session;

    @BeforeEach
    void setUp() throws IOException {
        log.clear();
        wm1.resetAll();

        // successful authentication
        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer xxxx"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                "user" : "test@email.com",
                                "scopes" : ["registered-model:create"],
                                "tenants" : [],
                                "accountIds" : ["test"],
                                "config" : {
                                    "containerRegistry" : "test.registry.com"
                                }
                                }
                                """)));

        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer wrong"))
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(401)));

        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer noaccess"))
                .atPriority(5)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                "user" : "test@email.com",
                                "scopes" : [],
                                "tenants" : [],
                                "accountIds" : ["test"],
                                "config" : {
                                    "containerRegistry" : "test.registry.com"
                                }
                                }
                                """)));
    }

    @Test
    @MojoParameter(name = "skip", value = "true")
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testSkipByParameter(ConfigureMojo mojo) throws Exception {

        mojo.setLog(log);
        mojo.execute();
        // assert that plugin executed and produced expected logs
        log.assertContains("Timefold Platform configuration skipped", Level.INFO);
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureSuccessfully(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));

        // assert that plugin executed and produced expected logs
        log.assertContains("Configured Timefold Platform integration", Level.INFO);

        Path buildProperties = Paths.get("target", "generated-resources", "timefold-build.properties");

        // assert the build properties file exists
        assertThat(Files.exists(buildProperties)).isTrue();

        // load configured build properties and assert expected entry
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(buildProperties)) {
            props.load(in);
        }
        assertThat(props)
                .containsEntry("quarkus.container-image.group", "test")// test is returned from aboutme endpoint as this is the account that access token grants
                .containsEntry("quarkus.container-image.registry", "test.registry.com")
                .containsEntry("quarkus.container-image.push", "true")
                .containsEntry("image.native-suffix", "");
    }

    @Test
    @MojoParameter(name = "nativeSupported", value = "true")
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureSuccessfullyNativeSupported(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));

        // assert that plugin executed and produced expected logs
        log.assertContains("Configured Timefold Platform integration", Level.INFO);

        Path buildProperties = Paths.get("target", "generated-resources", "timefold-build.properties");

        // assert the build properties file exists
        assertThat(Files.exists(buildProperties)).isTrue();

        // load configured build properties and assert expected entry
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(buildProperties)) {
            props.load(in);
        }
        assertThat(props)
                .containsEntry("quarkus.container-image.group", "test")// test is returned from aboutme endpoint as this is the account that access token grants
                .containsEntry("quarkus.container-image.registry", "test.registry.com")
                .containsEntry("quarkus.container-image.push", "true")
                .doesNotContainKey("image.native-suffix");
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureNotAuthorizaed(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("wrong"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(() -> mojo.execute()).isInstanceOf(IllegalStateException.class)
                .hasMessage("Platform authentication failed with 401 status code");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureMissingAccessToken(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));

        mojo.setAccessTokenProvider(new TestAccessTokenProvider(null));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(() -> mojo.execute()).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Personal Access Token for Timefold Platform is required. Set this via TIMEFOLD_PAT environment variable");

        wm1.verify(0, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @MojoParameter(name = "accountId", value = "company")
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureNotAuthorizaedForAccountId(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(() -> mojo.execute()).isInstanceOf(RuntimeException.class)
                .hasMessage("No access to configured account id company or account not configured");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureWrongScopes(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("noaccess"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(() -> mojo.execute()).isInstanceOf(RuntimeException.class)
                .hasMessage("No access to deploy model on Timefold Platform");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }
}
