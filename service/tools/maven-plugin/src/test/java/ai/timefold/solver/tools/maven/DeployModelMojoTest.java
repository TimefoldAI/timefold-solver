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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import ai.timefold.solver.tools.maven.http.UploadProgressSettings;
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

    private static final long MIB = 1024L * 1024L;

    @RegisterExtension
    static WireMockExtension wm1 = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private InMemoryMojoLog log = new InMemoryMojoLog();

    @BeforeEach
    void setUp() throws IOException {
        log.clear();
        wm1.resetAll();

        // represents a model already registered under the same registration key so patch would be required
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("existing"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"id\":\"1\",\"code\":\"TFP-14001\",\"message\":\"Registered model conflicts with existing model (model_v1), unique model registration key is required\"}")));

        wm1.stubFor(patch(urlPathEqualTo("/api/platform/v1/models/existing"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // represents a conflict the platform did not identify
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("unknown-conflict"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"id\":\"1\",\"code\":\"TFP-99999\",\"message\":\"Registered model conflicts with existing model (model_v1)\"}")));

        // represents a conflict on the registration key where the registration cannot be updated afterwards
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("failing-update"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"id\":\"1\",\"code\":\"TFP-14001\",\"message\":\"Registered model conflicts with existing model (model_v1), unique model registration key is required\"}")));

        wm1.stubFor(patch(urlPathEqualTo("/api/platform/v1/models/failing-update"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"id\":\"1\",\"code\":\"TFP-99999\",\"message\":\"Model with registration key 'failing-update' was not found\"}")));

        // represents a conflict reported with a body that is not the expected JSON, e.g. produced by a proxy
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("malformed-conflict"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("<html>Conflict</html>")));

        // represents a conflict reported without any body
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("empty-conflict"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")));

        // represents the same model version already being the latest release of that registration key
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("existing-version"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"id\":\"1\",\"code\":\"TFP-14005\",\"message\":\"Model build version '1.0.0' already matches the current latest release for registration key 'existing-version'; override the existing release instead of registering a duplicate.\"}")));

        wm1.stubFor(patch(urlPathEqualTo("/api/platform/v1/models/existing-version"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // represents the same model id already registered under a different registration key
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("existing-model-id"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"id\":\"1\",\"code\":\"TFP-14004\",\"message\":\"Existing private model (model_v1), already exists for tenants\"}")));

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
    @MojoParameter(name = "key", value = "existing-version")
    @MojoParameter(name = "overwrite", value = "true")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testRegisterModelWithPatchOnModelVersionConflict(DeployModelMojo mojo) throws Exception {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        mojo.execute();

        // conflict on the model version of the same registration key is resolved by overriding that release
        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("existing-version")));

        wm1.verify(1, patchRequestedFor(urlPathEqualTo("/api/platform/v1/models/existing-version")));

        log.assertContains("Model .* has been successfully updated on platform.*", Level.INFO);
    }

    @Test
    @MojoParameter(name = "key", value = "existing-model-id")
    @MojoParameter(name = "overwrite", value = "true")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailWithoutPatchOnModelIdConflict(DeployModelMojo mojo) {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).rootCause().isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Model deployment of timefold-test-model_v2-beta failed due to conflict (TFP-14004) that cannot be resolved by updating the registration with key existing-model-id: Existing private model (model_v1), already exists for tenants");

        // the registration key was never registered, so it must not be updated even with overwrite enabled
        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("existing-model-id")));

        wm1.verify(0, patchRequestedFor(urlPathEqualTo("/api/platform/v1/models/existing-model-id")));

        log.assertContains(".*Existing private model \\(model_v1\\), already exists for tenants.*", Level.ERROR);
    }

    @Test
    @MojoParameter(name = "key", value = "unknown-conflict")
    @MojoParameter(name = "overwrite", value = "true")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailWithoutPatchOnUnidentifiedConflict(DeployModelMojo mojo) {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        assertThatThrownBy(mojo::execute).rootCause().isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Model deployment of timefold-test-model_v2-beta failed due to conflict (TFP-99999) that cannot be resolved by updating the registration with key unknown-conflict: Registered model conflicts with existing model (model_v1)");

        // a conflict that the platform does not identify is not assumed to be on the registration key
        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("unknown-conflict")));

        wm1.verify(0, patchRequestedFor(urlPathEqualTo("/api/platform/v1/models/unknown-conflict")));

        log.assertContains(".*Registered model conflicts with existing model \\(model_v1\\).*", Level.ERROR);
    }

    @Test
    @MojoParameter(name = "key", value = "failing-update")
    @MojoParameter(name = "overwrite", value = "true")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailOnUpdateReportsPlatformError(DeployModelMojo mojo) {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        // the reason reported by the platform is part of the failure and not only of the build log
        assertThatThrownBy(mojo::execute).rootCause().isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Model deployment (override) failed with 404 status code: Model with registration key 'failing-update' was not found");

        wm1.verify(1, patchRequestedFor(urlPathEqualTo("/api/platform/v1/models/failing-update")));

        log.assertContains(".*Model with registration key 'failing-update' was not found.*", Level.ERROR);
    }

    @Test
    @MojoParameter(name = "key", value = "malformed-conflict")
    @MojoParameter(name = "overwrite", value = "true")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailWithoutPatchOnMalformedConflictBody(DeployModelMojo mojo) {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        // a body that cannot be read as JSON reports no error code, so the conflict is not resolvable
        assertThatThrownBy(mojo::execute).rootCause().isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Model deployment of timefold-test-model_v2-beta failed due to conflict (TFP-99999) that cannot be resolved by updating the registration with key malformed-conflict: <html>Conflict</html>");

        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("malformed-conflict")));

        wm1.verify(0, patchRequestedFor(urlPathEqualTo("/api/platform/v1/models/malformed-conflict")));

        log.assertContains("Unable to read error code from response body <html>Conflict</html>", Level.DEBUG);
        log.assertContains("<html>Conflict</html>", Level.ERROR);
    }

    @Test
    @MojoParameter(name = "key", value = "empty-conflict")
    @MojoParameter(name = "overwrite", value = "true")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailWithoutPatchOnEmptyConflictBody(DeployModelMojo mojo) {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();

        // a conflict without a body reports no error code, so the conflict is not resolvable
        assertThatThrownBy(mojo::execute).rootCause().isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Model deployment of timefold-test-model_v2-beta failed due to conflict (TFP-99999) that cannot be resolved by updating the registration with key empty-conflict: no error message reported by the platform");

        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models"))
                .withQueryParam("registrationKey", equalTo("empty-conflict")));

        wm1.verify(0, patchRequestedFor(urlPathEqualTo("/api/platform/v1/models/empty-conflict")));

        // there is nothing to report from an empty body
        assertThat(log.getEvents()).noneMatch(event -> event.level == Level.ERROR);
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

    @Test
    @MojoParameter(name = "key", value = "slowplatform")
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testHeartbeatWhilePlatformIsProcessing(DeployModelMojo mojo) throws Exception {
        // atPriority(1) because setUp already registers a catch-all POST stub at priority 15
        wm1.stubFor(post(urlPathEqualTo("/api/platform/v1/models"))
                .atPriority(1)
                .withQueryParam("registrationKey", equalTo("slowplatform"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(1_000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        // a heartbeat and silence budget of 100 ms against a 1 s server delay leaves roughly ten chances to log
        mojo.setProgressSettings(new UploadProgressSettings(Duration.ofMillis(100), Duration.ofMillis(100), 5, MIB));
        mojo.execute();

        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models")));

        // only presence is asserted, never elapsed times or line counts, so this cannot become time sensitive
        log.assertContains("waiting for the Timefold Platform to process it", Level.INFO);
        log.assertContains("transfer finished", Level.INFO);
        log.assertContains("Model .* has been successfully deployed into platform.*", Level.INFO);
    }

    @Test
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testAnnouncesArchiveSizeForLargeArchives(DeployModelMojo mojo) throws Exception {
        createLargeModelDescriptorArchive(2 * (int) MIB);

        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        // a heartbeat longer than the test can possibly take, so only the up-front announcement can appear
        mojo.setProgressSettings(new UploadProgressSettings(Duration.ofMinutes(5), Duration.ofMinutes(5), 5, MIB));
        mojo.execute();

        wm1.verify(1, postRequestedFor(urlPathEqualTo("/api/platform/v1/models")));
        log.assertContains("Uploading model descriptor archive \\(2\\.\\d MiB", Level.INFO);
    }

    @Test
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testNoProgressOutputForTheTypicalSmallArchive(DeployModelMojo mojo) throws Exception {
        mojo.setLog(log);
        mojo.platformUrl = wm1.getRuntimeInfo().getHttpBaseUrl();
        // explicitly longer than the default, rather than relying on 5 s outrunning a loaded CI agent's loopback
        mojo.setProgressSettings(new UploadProgressSettings(Duration.ofMinutes(5), Duration.ofMinutes(5), 5, MIB));
        mojo.execute();

        assertThat(log.contains("Uploading model descriptor archive", Level.INFO)).isFalse();
        assertThat(log.contains("waiting for the Timefold Platform", Level.INFO)).isFalse();
        assertThat(log.contains("transfer finished", Level.INFO)).isFalse();
        log.assertContains("Model .* has been successfully deployed into platform.*", Level.INFO);
    }

    @Test
    @MojoParameter(name = "descriptorOnly", value = "true")
    @InjectMojo(goal = "deploy", pom = "src/test/resources/project-to-test/pom.xml")
    public void testFailureCauseIsUnwrapped(DeployModelMojo mojo) {
        mojo.setLog(log);
        // nothing listens on port 1, so the connection is refused immediately; a request timeout would also exercise
        // this branch but would make the test wait for it
        mojo.platformUrl = "http://localhost:1";

        // sendAsync reports failures as an ExecutionException; sendWithProgress unwraps it so that the cause stays
        // exactly what the previously blocking send() would have thrown
        assertThatThrownBy(mojo::execute)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected error while deploying model")
                .hasCauseInstanceOf(ConnectException.class);
    }

    /**
     * Copies every entry of the test fixture, so that {@code readModelDescriptor} still finds
     * {@code timefold-model-descriptor.json} in the archive, then appends incompressible filler to grow it past the
     * announcement threshold. Only writes under {@code target/}, which {@code setUp} overwrites for every other test.
     */
    private static void createLargeModelDescriptorArchive(int fillerBytes) throws IOException {
        byte[] filler = new byte[fillerBytes];
        new Random(42).nextBytes(filler); // random data does not deflate, so the archive really is this big
        Path source = Paths.get("src", "test", "resources", "model-descriptor.zip");
        Path target = Paths.get("target", "model-descriptor.zip");
        try (ZipFile sourceZip = new ZipFile(source.toFile());
                ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target))) {
            Enumeration<? extends ZipEntry> entries = sourceZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                out.putNextEntry(new ZipEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    try (InputStream in = sourceZip.getInputStream(entry)) {
                        in.transferTo(out);
                    }
                }
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry("filler.bin"));
            out.write(filler);
            out.closeEntry();
        }
    }
}
