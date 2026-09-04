package ai.timefold.solver.service.worker.impl.testutil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.reactive.messaging.MutinyEmitter;

/**
 * Test utility mutiny emitter that records payloads sent through {@link #send(Object)}
 * and {@link #sendAndAwait(Object)}.
 */
public final class RecordingMutinyEmitter<T> implements MutinyEmitter<T> {

    private final List<T> messages = new ArrayList<>();

    @Override
    public Uni<Void> send(T payload) {
        messages.add(payload);
        return Uni.createFrom().voidItem();
    }

    @Override
    public void sendAndAwait(T payload) {
        messages.add(payload);
    }

    @Override
    public Cancellable sendAndForget(T payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Message<? extends T>> void send(M msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Message<? extends T>> Uni<Void> sendMessage(M msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Message<? extends T>> void sendMessageAndAwait(M msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M extends Message<? extends T>> Cancellable sendMessageAndForget(M msg) {
        throw new UnsupportedOperationException();
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