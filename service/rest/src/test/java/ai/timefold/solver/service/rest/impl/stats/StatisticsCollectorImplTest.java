package ai.timefold.solver.service.rest.impl.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

class StatisticsCollectorImplTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final AtomicLong clock = new AtomicLong(1_000);
    private final StatisticsCollectorImpl collector = new StatisticsCollectorImpl(clock::get);

    private final CountDownLatch hangingRequestStarted = new CountDownLatch(1);

    private Vertx vertx;
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        vertx = Vertx.vertx();
        var router = Router.router(vertx);
        collector.registerTracking(router);
        router.get("/v1/ok").handler(routingContext -> routingContext.end("ok"));
        router.get("/v1/failing").handler(routingContext -> {
            throw new IllegalStateException("Simulated failure");
        });
        // never responds, so it completes only when the client goes away
        router.get("/v1/hanging").handler(routingContext -> hangingRequestStarted.countDown());
        router.get("/q/health").handler(routingContext -> routingContext.end("UP"));
        server = vertx.createHttpServer()
                .requestHandler(router)
                .listen(0)
                .toCompletionStage().toCompletableFuture()
                .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    @AfterEach
    void stopServer() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    @Test
    void noActivityInitially() {
        assertThat(collector.inflightRequestCount()).isZero();
        assertThat(collector.lastActivityTimestamp()).isZero();
    }

    @Test
    void completedRequest() throws Exception {
        assertThat(get("/v1/ok")).startsWith("HTTP/1.1 200");
        awaitUntil(() -> collector.inflightRequestCount() == 0 && collector.lastActivityTimestamp() == clock.get());
    }

    @Test
    void failedRequest() throws Exception {
        assertThat(get("/v1/failing")).startsWith("HTTP/1.1 500");
        awaitUntil(() -> collector.inflightRequestCount() == 0 && collector.lastActivityTimestamp() == clock.get());
    }

    @Test
    void untrackedRequest() throws IOException {
        assertThat(get("/q/health")).startsWith("HTTP/1.1 200");
        assertThat(collector.inflightRequestCount()).isZero();
        assertThat(collector.lastActivityTimestamp()).isZero();
    }

    @Test
    void requestAbandonedByClient() throws Exception {
        try (var socket = new Socket("localhost", server.actualPort())) {
            sendGet(socket.getOutputStream(), "/v1/hanging");
            assertThat(hangingRequestStarted.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();
            assertThat(collector.inflightRequestCount()).isOne();
            clock.set(5_000);
        }
        awaitUntil(() -> collector.inflightRequestCount() == 0);
        assertThat(collector.lastActivityTimestamp()).isEqualTo(5_000);
    }

    @Test
    void lastActivityNeverMovesBackwards() {
        clock.set(5_000);
        collector.requestStarted();
        clock.set(3_000);
        collector.requestEnded();
        assertThat(collector.lastActivityTimestamp()).isEqualTo(5_000);
        assertThat(collector.inflightRequestCount()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "/v1/route-plans, true",
            "/v1/route-plans/123, true",
            "/v1/route-plans/123/events, false",
            "/q, false",
            "/q/health/live, false",
            "/q/metrics, false",
            "/quality, true",
    })
    void isTracked(String path, boolean expected) {
        assertThat(StatisticsCollectorImpl.isTracked(path)).isEqualTo(expected);
    }

    private String get(String path) throws IOException {
        try (var socket = new Socket("localhost", server.actualPort())) {
            sendGet(socket.getOutputStream(), path);
            return readStatusLine(socket.getInputStream());
        }
    }

    private static void sendGet(OutputStream outputStream, String path) throws IOException {
        outputStream.write("GET %s HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".formatted(path)
                .getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();
    }

    private static String readStatusLine(InputStream inputStream) throws IOException {
        var statusLine = new StringBuilder();
        int c;
        while ((c = inputStream.read()) != -1 && c != '\r') {
            statusLine.append((char) c);
        }
        // drain the rest, so that the response has been fully sent before asserting on it
        inputStream.readAllBytes();
        return statusLine.toString();
    }

    private static void awaitUntil(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within " + TIMEOUT);
            }
            Thread.sleep(10);
        }
    }
}
