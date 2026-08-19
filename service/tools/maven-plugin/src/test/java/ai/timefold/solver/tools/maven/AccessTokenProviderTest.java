package ai.timefold.solver.tools.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog;
import ai.timefold.solver.tools.maven.utils.InMemoryMojoLog.Level;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

class AccessTokenProviderTest {

    private static final String TOKEN = "dummy-access-token";

    @TempDir
    private Path tempDir;

    private InMemoryMojoLog log = new InMemoryMojoLog();

    @BeforeEach
    void setUp() throws Exception {
        log.clear();
    }

    @Test
    void environmentTokenTakesPrecedenceOverTheSettings() throws Exception {
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, "from-settings");

        AccessTokenProvider provider = provider("  from-environment  ", settings, plainDecrypter());

        // trimmed, as an exported value easily picks up trailing whitespace
        assertThat(provider.getAccessToken()).isEqualTo("from-environment");
    }

    @Test
    void blankEnvironmentTokenFallsBackToTheSettings() throws Exception {
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, "from-settings");

        AccessTokenProvider provider = provider("   ", settings, plainDecrypter());

        assertThat(provider.getAccessToken()).isEqualTo("from-settings");
    }

    @Test
    void encryptedTokenInTheSettingsIsDecrypted() throws Exception {
        String masterPassword = "the-master-password";
        SettingsDecrypter decrypter = mavenDecrypter(masterPassword);
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID,
                new DefaultPlexusCipher().encryptAndDecorate(TOKEN, masterPassword));

        AccessTokenProvider provider = provider(null, settings, decrypter);

        assertThat(provider.getAccessToken()).isEqualTo(TOKEN);
        // an encrypted token is the expected way of storing it, so nothing is reported
        assertThat(log.contains("unencrypted", Level.WARN)).isFalse();
    }

    @Test
    void tokenIsReadFromTheConfiguredServerId() throws Exception {
        Settings settings = settingsWithServer("my-platform", "from-settings");

        assertThat(provider(null, settings, plainDecrypter(), "my-platform").getAccessToken()).isEqualTo("from-settings");
        // the default server id is not configured, so there is nothing to read
        assertThat(provider(null, settings, plainDecrypter()).getAccessToken()).isNull();
    }

    @Test
    void unencryptedTokenInTheSettingsIsUsedButReported() throws Exception {
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, TOKEN);

        AccessTokenProvider provider = provider(null, settings, plainDecrypter());

        assertThat(provider.getAccessToken()).isEqualTo(TOKEN);
        log.assertContains("stored unencrypted", Level.WARN);
        log.assertContains("mvn --encrypt-password", Level.WARN);
    }

    @Test
    void serverWithoutPasswordProvidesNoToken() throws Exception {
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, null);

        AccessTokenProvider provider = provider(null, settings, plainDecrypter());

        assertThat(provider.getAccessToken()).isNull();
        log.assertContains("has no <password>", Level.WARN);
    }

    @Test
    void noTokenAtAllResolvesToNull() throws Exception {
        assertThat(provider(null, null, plainDecrypter()).getAccessToken()).isNull();
        assertThat(provider(null, new Settings(), plainDecrypter()).getAccessToken()).isNull();
    }

    /**
     * A token that cannot be decrypted must fail the build rather than reach the platform in its encrypted form, where
     * it would only produce an authentication error that says nothing about the actual problem.
     */
    @Test
    void failedDecryptionFailsWithTheEncryptedTokenKeptOut() throws Exception {
        String encryptedToken = "{dummy-cipher-text}";
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, encryptedToken);
        SettingsDecrypter failing = request -> new StubDecryptionResult(request.getServers().get(0),
                List.of(new DefaultSettingsProblem("master password is not set", Severity.ERROR, "settings.xml", -1, -1,
                        null)));

        AccessTokenProvider provider = provider(null, settings, failing);

        assertThatThrownBy(provider::getAccessToken).isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Unable to decrypt the personal access token in server 'timefold-platform'")
                .hasMessageContaining("master password is not set")
                .hasMessageContaining("~/.m2/settings-security.xml")
                .hasMessageContaining("mvn --encrypt-password")
                .hasMessageContaining(AccessTokenProvider.ENCRYPTION_GUIDE_URL)
                .hasMessageNotContaining(encryptedToken);
    }

    @Test
    void decryptionWarningsAreReportedWithoutFailing() throws Exception {
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, "{encrypted}");
        SettingsDecrypter warning = request -> {
            Server decrypted = request.getServers().get(0).clone();
            decrypted.setPassword(TOKEN);
            return new StubDecryptionResult(decrypted, List.of(new DefaultSettingsProblem("deprecated encryption",
                    Severity.WARNING, "settings.xml", -1, -1, null)));
        };

        assertThat(provider(null, settings, warning).getAccessToken()).isEqualTo(TOKEN);
        log.assertContains("deprecated encryption", Level.WARN);
    }

    /**
     * Maven treats braces escaped as {@code \{} and {@code \}} as characters of the password and hands the value
     * back untouched, so it is a token stored in clear text rather than one that failed to decrypt.
     */
    @Test
    void escapedBracesAreNotMistakenForCipherText() throws Exception {
        String escapedToken = "dummy\\{abc\\}value";
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, escapedToken);

        // Maven leaves the escaping in place, so the token is used exactly as it was stored
        assertThat(provider(null, settings, mavenDecrypter("the-master-password")).getAccessToken())
                .isEqualTo(escapedToken);
        log.assertContains("stored unencrypted", Level.WARN);
    }

    /**
     * Maven allows a comment outside the braces, so a token stored that way is encrypted and must not be reported as
     * if it were sitting there in clear text.
     */
    @Test
    void encryptedTokenWithACommentIsNotReportedAsUnencrypted() throws Exception {
        String masterPassword = "the-master-password";
        SettingsDecrypter decrypter = mavenDecrypter(masterPassword);
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID,
                "rotate before 2026-12-01 " + new DefaultPlexusCipher().encryptAndDecorate(TOKEN, masterPassword));

        assertThat(provider(null, settings, decrypter).getAccessToken()).isEqualTo(TOKEN);
        assertThat(log.contains("unencrypted", Level.WARN)).isFalse();
    }

    /**
     * The check for an undecrypted token compares against what was stored, rather than looking for curly braces in
     * the result, so that a token that legitimately contains them is not rejected after being decrypted correctly.
     */
    @Test
    void decryptedTokenMayContainCurlyBraces() throws Exception {
        String masterPassword = "the-master-password";
        String bracedToken = "dummy-{not}-cipher-text";
        SettingsDecrypter decrypter = mavenDecrypter(masterPassword);
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID,
                new DefaultPlexusCipher().encryptAndDecorate(bracedToken, masterPassword));

        assertThat(provider(null, settings, decrypter).getAccessToken()).isEqualTo(bracedToken);
    }

    /**
     * Without a decrypter there is nothing that could turn the cipher text into a usable token, so it must not be
     * handed on as if it were one.
     */
    @Test
    void encryptedTokenWithoutADecrypterFails() throws Exception {
        String encryptedToken = "{dummy-cipher-text}";
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, encryptedToken);

        assertThatThrownBy(provider(null, settings, null)::getAccessToken).isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("could not be decrypted")
                .hasMessageContaining("still in its encrypted form")
                .hasMessageContaining("mvn --encrypt-password")
                .hasMessageNotContaining(encryptedToken);
    }

    /**
     * A build that configures a blank server id falls back to the default entry, so that is the id to report; naming
     * the blank value would point at an entry that is never looked up.
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    void aBlankServerIdReadsTheDefaultEntry(String configured) throws Exception {
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, TOKEN);
        AccessTokenProvider provider = provider(null, settings, plainDecrypter(), configured);

        assertThat(provider.getServerId()).isEqualTo(AccessTokenProvider.DEFAULT_SERVER_ID);
        assertThat(provider.getAccessToken()).isEqualTo(TOKEN);
    }

    @Test
    void aPaddedServerIdReadsTheEntryItNames() throws Exception {
        Settings settings = settingsWithServer("my-platform", TOKEN);
        AccessTokenProvider provider = provider(null, settings, plainDecrypter(), "  my-platform  ");

        assertThat(provider.getServerId()).isEqualTo("my-platform");
        assertThat(provider.getAccessToken()).isEqualTo(TOKEN);
    }

    /**
     * Whitespace around the stored token, which an XML element that holds it on a line of its own carries, must not
     * decide whether it reads as cipher text, or the safeguard above would let the cipher text through.
     */
    @Test
    void whitespaceAroundAnEncryptedTokenDoesNotHideIt() throws Exception {
        String encryptedToken = "{dummy-cipher-text}";
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, "\n  " + encryptedToken + "\n  ");

        assertThatThrownBy(provider(null, settings, null)::getAccessToken).isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("could not be decrypted")
                .hasMessageNotContaining(encryptedToken);
    }

    /**
     * The settings belong to the session that the rest of the build shares, so reading the token must leave them be.
     */
    @Test
    void readingTheTokenLeavesTheSettingsAlone() throws Exception {
        String storedPassword = "  " + TOKEN + "  ";
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, storedPassword);

        assertThat(provider(null, settings, plainDecrypter()).getAccessToken()).isEqualTo(TOKEN);

        assertThat(settings.getServer(AccessTokenProvider.DEFAULT_SERVER_ID).getPassword()).isEqualTo(storedPassword);
    }

    /**
     * A token that is not encrypted needs no decrypter, so the absence of one is not a failure in itself.
     */
    @Test
    void unencryptedTokenWithoutADecrypterIsStillUsable() throws Exception {
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, TOKEN);

        assertThat(provider(null, settings, null).getAccessToken()).isEqualTo(TOKEN);
        log.assertContains("stored unencrypted", Level.WARN);
    }

    /**
     * A decrypter that reports neither a result nor a problem must not be taken as a successful decryption either.
     */
    @Test
    void decrypterThatLeavesTheTokenEncryptedFails() throws Exception {
        String encryptedToken = "{dummy-cipher-text}";
        Settings settings = settingsWithServer(AccessTokenProvider.DEFAULT_SERVER_ID, encryptedToken);
        SettingsDecrypter reportsNothing = request -> new StubDecryptionResult(null, List.of());

        assertThatThrownBy(provider(null, settings, reportsNothing)::getAccessToken)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("could not be decrypted")
                .hasMessageNotContaining(encryptedToken);
    }

    /**
     * Reading a value as cipher text only means anything while it agrees with what Maven itself decides to decrypt,
     * so it is held against Maven's own expression for every value that the characters involved can spell.
     */
    @Test
    void cipherTextIsRecognizedTheSameWayMavenDoes() throws Exception {
        DefaultPlexusCipher maven = new DefaultPlexusCipher();

        List<String> disagreed = everyValueUpTo(6, '{', '}', '\\', 'a').stream()
                .filter(value -> AccessTokenProvider.isEncrypted(value) != maven.isEncryptedString(value))
                .toList();

        assertThat(disagreed).isEmpty();
    }

    /**
     * Maven's expression lets a single line terminator sit right in front of the opening brace, which this check does
     * not follow: it reads such a value as clear text, so that the token is used rather than rejected as one that
     * failed to decrypt.
     */
    @Test
    void aValueHoldingALineTerminatorIsNeverReadAsCipherText() throws Exception {
        DefaultPlexusCipher maven = new DefaultPlexusCipher();

        assertThat(AccessTokenProvider.isEncrypted("{a}\n")).isFalse();
        assertThat(maven.isEncryptedString("{a}\n")).isFalse();
        assertThat(AccessTokenProvider.isEncrypted("{a\nb}")).isFalse();
        assertThat(maven.isEncryptedString("{a\nb}")).isFalse();

        // the one value the two read differently, and only ever in the direction that keeps the token usable
        assertThat(AccessTokenProvider.isEncrypted("\n{a}")).isFalse();
        assertThat(maven.isEncryptedString("\n{a}")).isTrue();
    }

    private static List<String> everyValueUpTo(int maxLength, char... alphabet) {
        List<String> values = new ArrayList<>(List.of(""));
        List<String> shorter = List.of("");
        for (int length = 1; length <= maxLength; length++) {
            List<String> current = new ArrayList<>();
            for (String value : shorter) {
                for (char character : alphabet) {
                    current.add(value + character);
                }
            }
            values.addAll(current);
            shorter = current;
        }
        return values;
    }

    /**
     * Mimics how Maven decrypts a server password: an encrypted master password in {@code settings-security.xml},
     * against which the stored token was encrypted.
     */
    private SettingsDecrypter mavenDecrypter(String masterPassword) throws Exception {
        DefaultPlexusCipher cipher = new DefaultPlexusCipher();
        Path securitySettings = tempDir.resolve("settings-security.xml");
        Files.writeString(securitySettings, """
                <settingsSecurity>
                  <master>%s</master>
                </settingsSecurity>
                """.formatted(
                cipher.encryptAndDecorate(masterPassword, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION)));
        DefaultSecDispatcher secDispatcher = new DefaultSecDispatcher(cipher);
        secDispatcher.setConfigurationFile(securitySettings.toString());
        return new DefaultSettingsDecrypter(secDispatcher);
    }

    /**
     * Stands in for a Maven that has nothing to decrypt, i.e. hands the stored value back unchanged.
     */
    private static SettingsDecrypter plainDecrypter() {
        return request -> new StubDecryptionResult(request.getServers().get(0), List.of());
    }

    private static Settings settingsWithServer(String serverId, String password) {
        Server server = new Server();
        server.setId(serverId);
        server.setPassword(password);
        Settings settings = new Settings();
        settings.addServer(server);
        return settings;
    }

    private AccessTokenProvider provider(String environmentToken, Settings settings, SettingsDecrypter decrypter) {
        return provider(environmentToken, settings, decrypter, AccessTokenProvider.DEFAULT_SERVER_ID);
    }

    private AccessTokenProvider provider(String environmentToken, Settings settings, SettingsDecrypter decrypter,
            String serverId) {
        return new TestableAccessTokenProvider(settings, decrypter, serverId, log, environmentToken);
    }

    private static final class TestableAccessTokenProvider extends AccessTokenProvider {

        private final String environmentToken;

        private TestableAccessTokenProvider(Settings settings, SettingsDecrypter settingsDecrypter, String serverId,
                Log log, String environmentToken) {
            super(settings, settingsDecrypter, serverId, log);
            this.environmentToken = environmentToken;
        }

        @Override
        protected String readEnvironmentToken() {
            return environmentToken;
        }
    }

    private record StubDecryptionResult(Server server, List<SettingsProblem> problems)
            implements
                SettingsDecryptionResult {

        @Override
        public Server getServer() {
            return server;
        }

        @Override
        public List<Server> getServers() {
            return server == null ? List.of() : List.of(server);
        }

        @Override
        public Proxy getProxy() {
            return null;
        }

        @Override
        public List<Proxy> getProxies() {
            return List.of();
        }

        @Override
        public List<SettingsProblem> getProblems() {
            return problems;
        }
    }

}
