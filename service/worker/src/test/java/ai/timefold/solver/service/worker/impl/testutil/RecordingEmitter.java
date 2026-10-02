package ai.timefold.solver.service.worker.impl.testutil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Test utility emitter that records payloads sent through {@link #send(Object)}.
 */
public final class RecordingEmitter<T> implements Emitter<T> {

    private final List<T> messages = new ArrayList<>();

    @Override
    public CompletionStage<Void> send(T msg) {
        messages.add(msg);
        return CompletableFuture.completedFuture(null);
    }

    public List<T> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public T getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public int size() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
    }

    @Override
    public <M extends Message<? extends T>> void send(M msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void complete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error(Exception e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasRequests() {
        throw new UnsupportedOperationException();
    }
}