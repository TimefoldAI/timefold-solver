package ai.timefold.solver.service.definition.internal.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Kubernetes service account token of the pod, as it is mounted into it, for the clients that have to prove to
 * the platform services who they are.
 * <p>
 * The token is a JWT the cluster issues for the service account the pod runs as. It carries the namespace, the pod
 * and the service account it was issued for, which is what lets the service on the other end tell one caller from
 * another. The kubelet rotates the token well before it expires and rewrites the file in place, so it is read again
 * every {@value #REFRESH_INTERVAL_SECONDS} seconds rather than kept for the lifetime of the client.
 * <p>
 * Outside a cluster there is no such file, which is not an error: the token is then simply not available and the
 * clients send their requests without one, the same way they did before the platform asked for it.
 */
public final class ServiceAccountToken {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceAccountToken.class);

    /**
     * File the token is read from, for the deployments that mount it somewhere else than Kubernetes does by default.
     */
    public static final String TOKEN_FILE_PROPERTY = "timefold.platform.service-account.token-file";

    /**
     * Where Kubernetes mounts the token of the service account the pod runs as.
     */
    public static final String DEFAULT_TOKEN_FILE = "/var/run/secrets/kubernetes.io/serviceaccount/token";

    /**
     * Header the token is sent in, as a bearer credential.
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final long REFRESH_INTERVAL_SECONDS = 60;

    private final Path file;

    private final long refreshIntervalNanos;

    /**
     * What was last read, replaced as a whole so that a reader never sees a half-written one.
     */
    private volatile Reading reading;

    public ServiceAccountToken(String file) {
        this(Path.of(file == null || file.isBlank() ? DEFAULT_TOKEN_FILE : file),
                Duration.ofSeconds(REFRESH_INTERVAL_SECONDS));
    }

    public ServiceAccountToken(Path file, Duration refreshInterval) {
        this.file = file;
        this.refreshIntervalNanos = refreshInterval.toNanos();
    }

    /**
     * The value of the {@value #AUTHORIZATION_HEADER} header to send, empty when there is no token to send.
     */
    public Optional<String> authorization() {
        Reading current = reading;
        if (current != null && current.isFresh(System.nanoTime(), refreshIntervalNanos)) {
            return Optional.ofNullable(current.authorization());
        }
        Reading read = read();
        this.reading = read;
        return Optional.ofNullable(read.authorization());
    }

    private Reading read() {
        try {
            String token = Files.readString(file).trim();
            if (token.isEmpty()) {
                LOGGER.warn("The service account token file {} is empty, requests are sent without a token", file);
                return new Reading(null, System.nanoTime());
            }
            return new Reading(BEARER_PREFIX + token, System.nanoTime());
        } catch (IOException | RuntimeException e) {
            /*
             * Only worth mentioning once in a while, as this is the normal state of anything that does not run in a
             * cluster, and the service on the other end is the one that decides whether a caller without a token is
             * served.
             */
            LOGGER.debug("No service account token could be read from {} ({}), requests are sent without one", file,
                    e.getMessage());
            return new Reading(null, System.nanoTime());
        }
    }

    /**
     * @param authorization the header value that was built out of the token, null when there was no token to read
     * @param readAtNanos when the file was last looked at
     */
    private record Reading(String authorization, long readAtNanos) {

        boolean isFresh(long now, long refreshIntervalNanos) {
            return now - readAtNanos < refreshIntervalNanos;
        }
    }
}
