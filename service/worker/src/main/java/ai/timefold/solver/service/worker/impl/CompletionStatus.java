package ai.timefold.solver.service.worker.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CompletionStatus {

    private final ConcurrentHashMap<String, CountDownLatch> latches = new ConcurrentHashMap<>();

    public void initiateCompletion(String id) {
        latches.put(id, new CountDownLatch(1));
    }

    public void completed(String id) {
        CountDownLatch latch = latches.remove(id);
        if (latch != null) {
            latch.countDown();
        }
    }

    public boolean waitForCompletion(String id, long timeout) throws InterruptedException {
        CountDownLatch latch = latches.get(id);
        if (latch == null) {
            return true;
        }
        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }

}
