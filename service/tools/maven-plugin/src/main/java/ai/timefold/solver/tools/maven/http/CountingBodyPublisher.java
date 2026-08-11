package ai.timefold.solver.tools.maven.http;

import java.net.http.HttpRequest.BodyPublisher;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps another {@link BodyPublisher} and counts the bytes handed over to the HTTP client's write pipeline, so that
 * the thread waiting for the response can tell whether the request body is still moving.
 * <p>
 * The count is <em>not</em> the number of bytes acknowledged by the peer, it is the number of bytes the client has
 * accepted for writing. The overshoot is bounded by the socket send buffer plus, for HTTP/2, the peer's flow control
 * window, because the client only requests more buffers from this publisher once the previous ones have been written
 * and the window allows more. On a slow connection that overshoot is a few hundred kilobytes at most and roughly
 * constant, which makes the count a usable approximation of upload progress - and a slow connection is the only
 * situation in which the count is ever reported.
 */
public final class CountingBodyPublisher implements BodyPublisher {

    private final BodyPublisher delegate;
    private final AtomicLong transferredBytes = new AtomicLong();

    public CountingBodyPublisher(BodyPublisher delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Delegated unchanged on purpose. Returning anything else (for example -1) makes the client fall back to chunked
     * transfer encoding instead of sending a Content-Length header, which changes the request on the wire.
     */
    @Override
    public long contentLength() {
        return delegate.contentLength();
    }

    /**
     * Safe to call from any thread, in particular from the thread waiting for the response.
     */
    public long getTransferredBytes() {
        return transferredBytes.get();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        // The client may subscribe more than once for a single logical request, for example when it resends the
        // request after a redirect. Every subscription restarts the body from the beginning, so the counter has to
        // restart too, otherwise it runs past contentLength() and progress exceeds 100%.
        transferredBytes.set(0);
        delegate.subscribe(new CountingSubscriber(subscriber));
    }

    private final class CountingSubscriber implements Subscriber<ByteBuffer> {

        private final Subscriber<? super ByteBuffer> downstream;

        private CountingSubscriber(Subscriber<? super ByteBuffer> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            downstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer item) {
            // remaining() has to be read before forwarding: the downstream subscriber consumes the buffer, which
            // advances its position, so remaining() is 0 by the time onNext returns. The counter is only updated
            // after the forward, so a buffer rejected by a throwing subscriber is not counted.
            int count = item.remaining();
            downstream.onNext(item);
            transferredBytes.addAndGet(count);
        }

        @Override
        public void onError(Throwable throwable) {
            downstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }

}
