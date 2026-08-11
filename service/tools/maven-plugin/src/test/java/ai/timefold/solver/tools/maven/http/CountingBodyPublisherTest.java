package ai.timefold.solver.tools.maven.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class CountingBodyPublisherTest {

    private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(10);

    @Test
    void delegatesContentLengthAndStartsAtZero() {
        CountingBodyPublisher publisher = new CountingBodyPublisher(BodyPublishers.ofByteArray(new byte[123]));

        // must be delegated: returning -1 would switch the request to chunked transfer encoding
        assertThat(publisher.contentLength()).isEqualTo(123);
        assertThat(publisher.getTransferredBytes()).isZero();
    }

    @Test
    void countsBytesEvenWhenTheSubscriberDrainsTheBuffer() throws Exception {
        // more than one 16 KiB chunk, so that onNext is called repeatedly
        byte[] payload = new byte[100_000];
        CountingBodyPublisher publisher = new CountingBodyPublisher(BodyPublishers.ofByteArray(payload));
        DrainingSubscriber subscriber = new DrainingSubscriber();

        publisher.subscribe(subscriber);

        assertThat(subscriber.awaitCompletion(COMPLETION_TIMEOUT)).isTrue();
        assertThat(subscriber.getDrainedBytes()).isEqualTo(payload.length);
        assertThat(publisher.getTransferredBytes()).isEqualTo(payload.length);
    }

    @Test
    void resubscribingRestartsTheCounter() throws Exception {
        Path archive = Paths.get("src", "test", "resources", "model-descriptor.zip");
        long size = Files.size(archive);
        CountingBodyPublisher publisher = new CountingBodyPublisher(BodyPublishers.ofFile(archive));

        DrainingSubscriber first = new DrainingSubscriber();
        publisher.subscribe(first);
        assertThat(first.awaitCompletion(COMPLETION_TIMEOUT)).isTrue();
        assertThat(publisher.getTransferredBytes()).isEqualTo(size);

        // the client resubscribes when it resends the request, for example after a redirect
        DrainingSubscriber second = new DrainingSubscriber();
        publisher.subscribe(second);
        assertThat(second.awaitCompletion(COMPLETION_TIMEOUT)).isTrue();
        assertThat(publisher.getTransferredBytes()).isEqualTo(size); // not twice the size
    }

    /**
     * Deliberately consumes every buffer it is given, exactly like the real downstream subscriber does when it writes
     * to the socket. That is what makes {@link CountingBodyPublisher}'s rule of reading {@code remaining()} before
     * forwarding load-bearing: counting after the forward instead would report 0 bytes here.
     */
    private static final class DrainingSubscriber implements Subscriber<ByteBuffer> {

        private final CountDownLatch completionLatch = new CountDownLatch(1);
        private long drainedBytes;

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer item) {
            while (item.hasRemaining()) {
                item.get();
                drainedBytes++;
            }
        }

        @Override
        public void onError(Throwable throwable) {
            completionLatch.countDown();
        }

        @Override
        public void onComplete() {
            completionLatch.countDown();
        }

        /**
         * Awaiting a latch rather than assuming the current JDK's synchronous delivery keeps this test correct either
         * way. The assertion is "completed", not "completed within N", so it is not time sensitive.
         */
        public boolean awaitCompletion(Duration timeout) throws InterruptedException {
            return completionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public long getDrainedBytes() {
            return drainedBytes;
        }

    }

}
