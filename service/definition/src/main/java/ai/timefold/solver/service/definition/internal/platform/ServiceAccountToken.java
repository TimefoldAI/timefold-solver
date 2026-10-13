package ai.timefold.solver.service.definition.internal.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.jspecify.annotations.Nullable;
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

    /**
     * Reads the token from where Kubernetes mounts it.
     */
    public ServiceAccountToken() {
        this(DEFAULT_TOKEN_FILE);
    }

    /**
     * Reads the token from given file instead of where Kubernetes mounts it; meant for tests.
     */
    public ServiceAccountToken(String file) {
        this(Path.of(file == null || file.isBlank() ? DEFAULT_TOKEN_FILE : file),
                Duration.ofSeconds(REFRESH_INTERVAL_SECONDS));
    }

    public ServiceAccountToken(Path file, Duration refreshInterval) {
        this.file = file;
        this.refreshIntervalNanos = refreshInterval.toNanos();
    }

    /**
     * The value of the {@value #AUTHORIZATION_HEADER} header to send, null when there is no token to send.
     */
    public @Nullable String authorization() {
        var current = reading;
        if (current != null && current.isFresh(System.nanoTime(), refreshIntervalNanos)) {
            return current.authorization();
        }
        return refresh().authorization();
    }

    /**
     * Serialized, so that two threads that both found the reading stale cannot finish out of order and have the one
     * that read the file first overwrite the newer token with its older one. Whoever comes second finds the reading
     * fresh already and uses it.
     */
    private synchronized Reading refresh() {
        var current = reading;
        if (current != null && current.isFresh(System.nanoTime(), refreshIntervalNanos)) {
            return current;
        }
        Reading read = read();
        if (read.authorization() == null && current != null && current.authorization() != null) {
            /*
             * A token was read before, so the file is there and failing to read it is not the normal state of a pod.
             * The previous token is kept rather than sending requests without one, which is safe as the kubelet
             * rotates the token well before it expires; the file is read again after the next interval.
             */
            LOGGER.warn("The service account token could not be read again from {}, the previous one is used until the "
                    + "next attempt", file);
            read = new Reading(current.authorization(), read.readAtNanos());
        }
        this.reading = read;
        return read;
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
    private record Reading(@Nullable String authorization, long readAtNanos) {

        boolean isFresh(long now, long refreshIntervalNanos) {
            return now - readAtNanos < refreshIntervalNanos;
        }
    }
}
