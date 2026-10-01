package ai.timefold.solver.tools.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

/**
 * Resolves the personal access token that authenticates the build against Timefold Platform, either from the
 * environment, or from a {@code <server>} entry of the Maven settings.
 * <p>
 * The latter exists so that the token does not have to be exported into every shell the build runs in, without having
 * to keep it in clear text on disk either: it is stored the same way as any other Maven credential, encrypted with
 * {@code mvn --encrypt-password} and decrypted here through Maven's own {@link SettingsDecrypter}.
 *
 * @see <a href="https://maven.apache.org/guides/mini/guide-encryption.html">Password Encryption</a>
 */
public class AccessTokenProvider {

    public static final String PAT_ENV_VARIABLE = "TIMEFOLD_PAT";

    /**
     * Id of the {@code <server>} entry the token is read from, unless the build configures a different one.
     */
    public static final String DEFAULT_SERVER_ID = "timefold-platform";

    protected static final String ENCRYPTION_GUIDE_URL = "https://maven.apache.org/guides/mini/guide-encryption.html";

    private final Settings settings;

    private final SettingsDecrypter settingsDecrypter;

    private final String serverId;

    private final Log log;

    /**
     * Only meant for test doubles, which override {@link #getAccessToken()} and therefore never read either source.
     */
    protected AccessTokenProvider() {
        this(null, null, DEFAULT_SERVER_ID, null);
    }

    public AccessTokenProvider(Settings settings, SettingsDecrypter settingsDecrypter, String serverId, Log log) {
        this.settings = settings;
        this.settingsDecrypter = settingsDecrypter;
        this.serverId = serverId == null || serverId.isBlank() ? DEFAULT_SERVER_ID : serverId.trim();
        this.log = log == null ? new SystemStreamLog() : log;
    }

    /**
     * The id of the {@code <server>} entry that is actually read, i.e. the configured one once normalized, or the
     * default when the build configures none. Reporting anything else would point at an entry that was never
     * consulted.
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * The environment takes precedence, so that a build which already exports the token, typically on CI, keeps
     * authenticating with it even when the machine also has a server entry configured.
     * <p>
     * Not finding a token and finding one that cannot be read are told apart: a build that configures none simply has
     * none, whereas one that stored an encrypted token clearly meant to authenticate with it, so leaving that token
     * unread would only surface later as an authentication error that says nothing about the real problem.
     *
     * @return null when neither source provides a token
     * @throws MojoExecutionException when the server entry holds a token that cannot be turned into a usable one,
     *         i.e. decryption fails or leaves it in its encrypted form
     */
    public String getAccessToken() throws MojoExecutionException {
        String environmentToken = readEnvironmentToken();
        if (environmentToken != null && !environmentToken.isBlank()) {
            log.debug("Personal access token read from the " + PAT_ENV_VARIABLE + " environment variable");
            return environmentToken.trim();
        }
        return readSettingsToken();
    }

    /**
     * Overridable so that tests do not have to change the environment of the process they run in.
     */
    protected String readEnvironmentToken() {
        return System.getenv(PAT_ENV_VARIABLE);
    }

    private String readSettingsToken() throws MojoExecutionException {
        if (settings == null) {
            return null;
        }
        Server configuredServer = settings.getServer(serverId);
        if (configuredServer == null) {
            log.warn("No server '" + serverId + "' is configured in the Maven settings");
            return null;
        }
        if (configuredServer.getPassword() == null || configuredServer.getPassword().isBlank()) {
            log.warn("Server '" + serverId + "' in the Maven settings has no <password>, so it does not provide a "
                    + "personal access token for Timefold Platform");
            return null;
        }
        // Maven's own settings reader trims the value, but leaving that to it would let whitespace around the token
        // decide whether it reads as cipher text. Trimmed on a copy, so that the settings of the session, which the
        // rest of the build shares, stay as they are. Kept around to tell a token that is stored unencrypted from
        // one that decryption left untouched.
        String storedPassword = configuredServer.getPassword().trim();
        Server server = configuredServer.clone();
        server.setPassword(storedPassword);
        String token = decrypt(server, storedPassword);
        if (isEncrypted(storedPassword) && token.equals(storedPassword)) {
            // Decryption left the cipher text untouched, so the token was never decrypted at all. Sending it would
            // only produce an authentication error that points at the token being wrong, rather than at it being
            // unreadable.
            throw new MojoExecutionException("""
                    The personal access token in server '%s' of the Maven settings could not be decrypted, so it is \
                    still in its encrypted form and cannot authenticate against Timefold Platform.
                    Check that your master password, in ~/.m2/settings-security.xml unless configured elsewhere, is \
                    the one the token was encrypted with; encrypt the token again with 'mvn --encrypt-password' if it \
                    is not.
                    See %s""".formatted(serverId, ENCRYPTION_GUIDE_URL));
        }
        if (!isEncrypted(storedPassword)) {
            log.warn("The personal access token in server '" + serverId + "' of the Maven settings is stored "
                    + "unencrypted; encrypt it with 'mvn --encrypt-password'. See " + ENCRYPTION_GUIDE_URL);
        }
        log.debug("Personal access token read from server '" + serverId + "' of the Maven settings");
        return token.trim();
    }

    /**
     * Decrypts the stored token the standard Maven way, which transparently covers both the Maven 3 and the Maven 4
     * encryption formats, as well as a token that is not encrypted at all.
     *
     * @throws MojoExecutionException when the token cannot be decrypted; sending its encrypted form to the platform
     *         would only fail with an authentication error that does not point at the actual problem
     */
    private String decrypt(Server server, String storedPassword) throws MojoExecutionException {
        if (settingsDecrypter == null) {
            // Outside a Maven build there is nothing to decrypt with; an encrypted token is caught by the caller.
            return storedPassword;
        }
        SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
        for (SettingsProblem problem : result.getProblems()) {
            if (problem.getSeverity() == Severity.WARNING) {
                log.warn(problem.getMessage());
            } else {
                throw new MojoExecutionException("""
                        Unable to decrypt the personal access token in server '%s' of the Maven settings: %s
                        Check that your master password, in ~/.m2/settings-security.xml unless configured elsewhere, \
                        is the one the token was encrypted with; encrypt the token again with 'mvn --encrypt-password' \
                        if it is not.
                        See %s""".formatted(serverId, problem.getMessage(), ENCRYPTION_GUIDE_URL));
            }
        }
        Server decryptedServer = result.getServer();
        String decryptedPassword = decryptedServer == null ? null : decryptedServer.getPassword();
        // A decrypter that reports nothing usable leaves the stored token as it is; the caller rejects it when that
        // means handing on cipher text.
        return decryptedPassword == null || decryptedPassword.isBlank() ? storedPassword : decryptedPassword;
    }

    /**
     * Whether Maven reads the stored value as cipher text, and therefore whether it attempts to decrypt it at all.
     * That is what tells a token stored in clear text from one that failed to decrypt, so it has to agree with
     * {@code DefaultPlexusCipher.ENCRYPTED_STRING_PATTERN}: the value carries a non-empty {@code {...}} whose closing
     * brace is not escaped as {@code \}}. It therefore also accepts a comment outside the braces.
     * <p>
     * Spelled out rather than copied as that expression, whose backtracking is needlessly super-linear. The two agree
     * on every value that holds no line terminator, which {@code AccessTokenProviderTest} pins for every value that
     * can be built from the characters involved. On one that does hold a line terminator this is only ever the more
     * careful of the two, so that a token is never wrongly rejected as undecryptable.
     */
    static boolean isEncrypted(String password) {
        if (containsLineTerminator(password)) {
            return false;
        }
        int open = password.indexOf('{');
        if (open < 0) {
            return false;
        }
        // Looking at the first opening brace is enough: whatever closing brace a later one pairs with, the first one
        // pairs with it too, as it only leaves more content in between.
        for (int close = open + 2; close < password.length(); close++) {
            if (password.charAt(close) == '}' && password.charAt(close - 1) != '\\') {
                return true;
            }
        }
        return false;
    }

    /**
     * Maven matches the parts around the cipher text with {@code .}, which no line terminator is.
     */
    private static boolean containsLineTerminator(String password) {
        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            if (character == '\n' || character == '\r' || character == '\u0085' || character == '\u2028'
                    || character == '\u2029') {
                return true;
            }
        }
        return false;
    }

}
