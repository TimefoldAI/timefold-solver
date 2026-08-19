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
import java.util.Set;

import javax.inject.Inject;

import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog;
import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog.Level;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.project.MavenProject;
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

        // the token is allowed to deploy models, but is not associated with any account
        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer noaccounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                "user" : "test@email.com",
                                "scopes" : ["registered-model:create"],
                                "tenants" : [],
                                "accountIds" : [],
                                "config" : {
                                    "containerRegistry" : "test.registry.com"
                                }
                                }
                                """)));

        // the platform does not report the accountIds field at all
        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer noaccountids"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                "user" : "test@email.com",
                                "scopes" : ["registered-model:create"],
                                "tenants" : [],
                                "config" : {
                                    "containerRegistry" : "test.registry.com"
                                }
                                }
                                """)));

        // the token is associated with several accounts, so the account id cannot be derived from it
        wm1.stubFor(get(urlPathEqualTo("/api/platform/v1/aboutme"))
                .withHeader("Authorization", equalTo("Bearer multipleaccounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                "user" : "test@email.com",
                                "scopes" : ["registered-model:create"],
                                "tenants" : [],
                                "accountIds" : ["test", "company"],
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
        setEnterpriseModel(mojo);

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
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureSuccessfullyWithTrailingSlashInPlatformUrl(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl() + "/";
        mojo.execute();

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));

        // assert that plugin executed and produced expected logs
        log.assertContains("Configured Timefold Platform integration", Level.INFO);
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsWithBlankPlatformUrl(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = "///";

        assertThatThrownBy(mojo::execute).isInstanceOf(IllegalStateException.class)
                .hasMessage("Platform Url is mandatory");

        wm1.verify(0, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @MojoParameter(name = "nativeSupported", value = "true")
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureSuccessfullyNativeSupported(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

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
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("wrong"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(IllegalStateException.class)
                .hasMessage("Platform authentication failed with 401 status code");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureMissingAccessToken(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider(null));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Personal Access Token for Timefold Platform is required. Set this via TIMEFOLD_PAT environment variable");

        wm1.verify(0, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @MojoParameter(name = "accountId", value = "company")
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureNotAuthorizaedForAccountId(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(RuntimeException.class)
                .hasMessage("No access to configured account id company or account not configured");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsWhenTokenHasNoAccount(ConfigureMojo mojo) {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("noaccounts"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("Unable to resolve the Timefold Platform account id")
                .hasMessageContaining("not associated with any account");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsWhenPlatformReportsNoAccountIds(ConfigureMojo mojo) {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("noaccountids"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("not associated with any account");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsWhenAccountIdIsAmbiguous(ConfigureMojo mojo) {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("multipleaccounts"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("associated with 2 accounts (company, test)")
                .hasMessageContaining("-Dtimefold.accountId=<account id>");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @MojoParameter(name = "accountId", value = "company")
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureUsesConfiguredAccountIdWhenSeveralAreAvailable(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("multipleaccounts"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));

        log.assertContains("Configured Timefold Platform integration", Level.INFO);

        Path buildProperties = Paths.get("target", "generated-resources", "timefold-build.properties");

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(buildProperties)) {
            props.load(in);
        }
        assertThat(props).containsEntry("quarkus.container-image.group", "company");
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureWrongScopes(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setEnterpriseModel(mojo);

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("noaccess"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(RuntimeException.class)
                .hasMessage("No access to deploy model on Timefold Platform");

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsForCommunityEditionBuild(ConfigureMojo mojo) {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setServiceParent(mojo);
        // no ai.timefold.solver.enterprise artifact resolved, i.e. the 'enterprise' profile was not activated

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("built with the Community Edition of Timefold Solver")
                .hasMessageContaining("-Denterprise=true");

        // the build fails before any platform call is made
        wm1.verify(0, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @MojoParameter(name = "skip", value = "true")
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsForCommunityEditionBuildEvenWhenConfigurationSkipped(ConfigureMojo mojo) {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setServiceParent(mojo);

        mojo.setLog(log);

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("built with the Community Edition of Timefold Solver");
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureSucceedsForEnterpriseEditionBuild(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        setServiceParent(mojo);
        setArtifacts(mojo, "ai.timefold.solver.enterprise", "timefold-solver-enterprise-service-storage-s3");

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        wm1.verify(1, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
        log.assertContains("Configured Timefold Platform integration", Level.INFO);
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsWhenServiceParentIsMissing(ConfigureMojo mojo) {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        // no parent declared at all, even though the enterprise artifacts are present
        setArtifacts(mojo, "ai.timefold.solver.enterprise", "timefold-solver-enterprise-service");

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("does not inherit from ai.timefold.solver:timefold-solver-service-parent");

        wm1.verify(0, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureFailsWhenServiceParentIsNotAnAncestor(ConfigureMojo mojo) {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        mojo.getProject().getModel().setParent(parent("com.example", "some-other-parent"));
        setArtifacts(mojo, "ai.timefold.solver.enterprise", "timefold-solver-enterprise-service");

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("does not inherit from ai.timefold.solver:timefold-solver-service-parent");
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureAcceptsServiceParentAsIndirectAncestor(ConfigureMojo mojo) throws Exception {

        session.getRequest().setGoals(List.of("timefold:deploy"));
        // a company aggregator in between, which itself inherits from timefold-solver-service-parent
        MavenProject serviceParent = new MavenProject();
        serviceParent.setGroupId("ai.timefold.solver");
        serviceParent.setArtifactId("timefold-solver-service-parent");
        MavenProject aggregator = new MavenProject();
        aggregator.setGroupId("com.example");
        aggregator.setArtifactId("company-parent");
        aggregator.setParent(serviceParent);
        mojo.getProject().setParent(aggregator);
        mojo.getProject().getModel().setParent(parent("com.example", "company-parent"));
        setArtifacts(mojo, "ai.timefold.solver.enterprise", "timefold-solver-enterprise-service");

        mojo.setAccessTokenProvider(new TestAccessTokenProvider("xxxx"));
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        log.assertContains("Configured Timefold Platform integration", Level.INFO);
    }

    @Test
    @InjectMojo(goal = "configure", pom = "src/test/resources/project-to-test/pom.xml")
    public void testConfigureSkipsEnterpriseCheckWhenDeployNotRequested(ConfigureMojo mojo) throws Exception {

        // neither the parent nor the enterprise artifacts are set, yet nothing fails
        mojo.setLog(log);
        mojo.execute();

        assertThat(log.getEvents()).isEmpty();
        wm1.verify(0, getRequestedFor(urlPathEqualTo("/api/platform/v1/aboutme")));
    }

    /**
     * Mimics a correctly set up platform model: it inherits from {@code timefold-solver-service-parent} and was built
     * with the {@code enterprise} profile active.
     */
    private static void setEnterpriseModel(ConfigureMojo mojo) {
        setServiceParent(mojo);
        setArtifacts(mojo, "ai.timefold.solver.enterprise", "timefold-solver-enterprise-service");
    }

    /**
     * Mimics a model inheriting from {@code timefold-solver-service-parent}, as every platform model does.
     */
    private static void setServiceParent(ConfigureMojo mojo) {
        mojo.getProject().getModel().setParent(parent("ai.timefold.solver", "timefold-solver-service-parent"));
    }

    /**
     * Mimics the dependencies that the {@code enterprise} profile of {@code timefold-solver-service-parent} pulls in.
     */
    private static void setArtifacts(ConfigureMojo mojo, String groupId, String artifactId) {
        ArtifactStub artifact = new ArtifactStub();
        artifact.setGroupId(groupId);
        artifact.setArtifactId(artifactId);
        artifact.setVersion("1.0.0");
        mojo.getProject().setArtifacts(Set.of(artifact));
    }

    private static Parent parent(String groupId, String artifactId) {
        Parent parent = new Parent();
        parent.setGroupId(groupId);
        parent.setArtifactId(artifactId);
        parent.setVersion("1.0.0");
        return parent;
    }
}
