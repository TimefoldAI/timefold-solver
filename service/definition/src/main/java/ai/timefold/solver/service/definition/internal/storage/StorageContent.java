package ai.timefold.solver.service.definition.internal.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Content to be written into a {@link Storage}.
 * <p>
 * The content is exposed as a stream so that implementations do not have to keep it in memory. The length is provided
 * as well, as some data stores require the size of the content up front.
 * <p>
 * Content that was handed over as bytes can be read as often as needed, every call to {@link #stream()} opens a new
 * stream over them. Content that is being forwarded from a stream somebody else opened, such as the body of a request
 * the access service relays, can only be read once. {@link #repeatable()} tells the two apart, so that a failed
 * operation is only ever retried when the content can actually be sent again.
 *
 * @param source opens a stream with the content, called once per attempt to write it
 * @param length number of bytes available in the stream, negative when it is not known
 * @param attributes additional attributes (such as object metadata or tags) to be associated with the content,
 *        never null but possibly empty
 * @param repeatable whether the content can be read more than once
 */
public record StorageContent(Supplier<InputStream> source, long length, Map<String, String> attributes,
        boolean repeatable) {

    public StorageContent {
        Objects.requireNonNull(source, "source cannot be null");
        if (attributes == null) {
            attributes = Map.of();
        }
    }

    /**
     * Content of a stream somebody else opened, which can only be read once and therefore cannot be written again
     * after a failed attempt.
     *
     * @param stream stream with the content, it is the responsibility of the storage to consume it
     * @param length number of bytes available in the stream, negative when it is not known
     * @param attributes additional attributes to be associated with the content, can be null
     */
    public StorageContent(InputStream stream, long length, Map<String, String> attributes) {
        this(once(stream), length, attributes, false);
    }

    /**
     * Opens the content for reading. A {@link #repeatable()} content returns a new stream on every call, so that an
     * attempt that failed half way through does not affect the next one.
     *
     * @return stream with the content, never null
     * @throws IllegalStateException when a content that is not repeatable is opened a second time
     */
    public InputStream stream() {
        return source.get();
    }

    public static StorageContent of(byte[] content) {
        return of(content, Map.of());
    }

    public static StorageContent of(byte[] content, Map<String, String> attributes) {
        Objects.requireNonNull(content, "content cannot be null");
        return new StorageContent(() -> new ByteArrayInputStream(content), content.length, attributes, true);
    }

    private static Supplier<InputStream> once(InputStream stream) {
        Objects.requireNonNull(stream, "stream cannot be null");
        AtomicBoolean handedOut = new AtomicBoolean();
        return () -> {
            if (handedOut.getAndSet(true)) {
                throw new IllegalStateException("The content is not repeatable and its stream was already handed out;"
                        + " content that has to be read more than once has to be read into memory first.");
            }
            return stream;
        };
    }
}
