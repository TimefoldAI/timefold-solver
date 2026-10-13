package ai.timefold.solver.service.rest.impl.stats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import ai.timefold.solver.service.definition.impl.stats.StatisticsCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;

/**
 * Tracks in-flight HTTP requests at the Vert.x router level.
 * <p>
 * Completion is detected via {@link io.vertx.ext.web.RoutingContext#addEndHandler}, which fires once the response
 * has been sent, or the connection was closed or reset, regardless of how the request was processed.
 * Thanks to that a request can never stay registered as in-flight after it is gone.
 */
@ApplicationScoped
public class StatisticsCollectorImpl implements StatisticsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsCollectorImpl.class);

    // run before the body handler, so that the time spent uploading the request body counts as in-flight
    private static final int ROUTE_ORDER = Integer.MIN_VALUE;

    private static final String NON_APPLICATION_PATH = "/q";
    private static final String EVENT_STREAM_PATH_SUFFIX = "/events";

    private final AtomicInteger inflightRequests = new AtomicInteger();
    private final AtomicLong lastActivity = new AtomicLong(0);
    private final LongSupplier currentTimeMillis;

    public StatisticsCollectorImpl() {
        this(System::currentTimeMillis);
    }

    StatisticsCollectorImpl(LongSupplier currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    void registerTracking(@Observes Router router) {
        router.route().order(ROUTE_ORDER).handler(routingContext -> {
            if (isTracked(routingContext.normalizedPath())) {
                requestStarted();
                routingContext.addEndHandler(ignored -> requestEnded());
            }
            routingContext.next();
        });
    }

    /**
     * Health probes and metric scrapes (under {@code /q}) are periodic and must not keep the runtime alive.
     * Event streams stay open until the client disconnects and must not block the shutdown.
     */
    static boolean isTracked(String path) {
        if (path == null) {
            return true;
        }
        return !(path.equals(NON_APPLICATION_PATH)
                || path.startsWith(NON_APPLICATION_PATH + "/")
                || path.endsWith(EVENT_STREAM_PATH_SUFFIX)
                || path.endsWith(EVENT_STREAM_PATH_SUFFIX + "/"));
    }

    void requestStarted() {
        var inflight = inflightRequests.incrementAndGet();
        recordActivity();
        LOGGER.trace("Request started, {} in-flight", inflight);
    }

    void requestEnded() {
        recordActivity();
        var inflight = inflightRequests.decrementAndGet();
        LOGGER.trace("Request ended, {} in-flight", inflight);
    }

    private void recordActivity() {
        lastActivity.accumulateAndGet(currentTimeMillis.getAsLong(), Math::max);
    }

    @Override
    public long lastActivityTimestamp() {
        return lastActivity.get();
    }

    @Override
    public int inflightRequestCount() {
        return inflightRequests.get();
    }

}
