package ai.timefold.solver.tools.maven;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog;
import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog.Level;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

@MojoTest
class PermissionsMojoTest {

    @RegisterExtension
    static WireMockExtension wm1 = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private InMemoryMojoLog log = new InMemoryMojoLog();

    @BeforeEach
    void setUp() {
        log.clear();
        wm1.resetAll();

        // token with push access rights
        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer xxxx"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                "user" : "test@email.com",
                                "scopes" : ["registered-model:create", "registered-model:update"],
                                "tenants" : ["007f172e-353f-440f-bf30-46321f6d6733"],
                                "accountIds" : ["test"]
                                }
                                """)));

        // token without any push access rights
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
                                "accountIds" : ["test"]
                                }
                                """)));

        // invalid/unknown token
        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer wrong"))
                .atPriority(10)
                .willReturn(aResponse()
                        .withStatus(401)));
    }

    @Test
    @InjectMojo(goal = "permissions", pom = "src/test/resources/project-to-test/pom.xml")
    void testReportsPermissions(PermissionsMojo mojo) throws Exception {
        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));

        log.assertContains("test@email.com", Level.INFO);
        log.assertContains("registered-model:create, registered-model:update", Level.INFO);
        log.assertContains("007f172e-353f-440f-bf30-46321f6d6733", Level.INFO);
        // tenant selected via the plugin configuration in the test pom
        log.assertContains("Selected tenant : 007f172e-353f-440f-bf30-46321f6d6733", Level.INFO);
        log.assertContains("token can register/update models", Level.INFO);
    }

    @Test
    @InjectMojo(goal = "permissions", pom = "src/test/resources/project-to-test/pom.xml")
    void testWarnsWhenTokenLacksPushAccess(PermissionsMojo mojo) throws Exception {
        mojo.setAccessTokenProvider(new TestAccessTokenProvider("noaccess"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));

        log.assertContains("Scopes +: \\(none\\)", Level.INFO);
        log.assertContains("token CANNOT register/update models", Level.WARN);
    }

    @Test
    @InjectMojo(goal = "permissions", pom = "src/test/resources/project-to-test/pom.xml")
    void testFailsWhenNotAuthorized(PermissionsMojo mojo) {
        mojo.setAccessTokenProvider(new TestAccessTokenProvider("wrong"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
                .hasMessage(
                        "Platform authentication failed — please verify your PAT and tenant access, or contact support if the problem persists");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "permissions", pom = "src/test/resources/project-to-test/pom.xml")
    void testFailsWhenAccessTokenMissing(PermissionsMojo mojo) {
        mojo.setAccessTokenProvider(new TestAccessTokenProvider(null));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Personal Access Token for Timefold Platform is required")
                .hasMessageContaining("export TIMEFOLD_PAT=<your token>")
                .hasMessageContaining("<id>timefold-platform</id>")
                .hasMessageContaining("mvn --encrypt-password");

        wm1.verify(0, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }
}
