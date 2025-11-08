package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ai.timefold.solver.core.api.solver.event.EventProducerId;

final class BestSolutionContainingProblemChanges<Solution_> {
    private final Solution_ bestSolution;
    private final EventProducerId producerId;
    private final List<CompletableFuture<Void>> containedProblemChanges;

    public BestSolutionContainingProblemChanges(Solution_ bestSolution, EventProducerId producerId,
            List<CompletableFuture<Void>> containedProblemChanges) {
        this.bestSolution = bestSolution;
        this.producerId = producerId;
        this.containedProblemChanges = containedProblemChanges;
    }

    public Solution_ getBestSolution() {
        return bestSolution;
    }

    public EventProducerId getProducerId() {
        return producerId;
    }

    public void completeProblemChanges() {
        containedProblemChanges.forEach(futureProblemChange -> futureProblemChange.complete(null));
    }

    public void completeProblemChangesExceptionally(Throwable exception) {
        containedProblemChanges.forEach(futureProblemChange -> futureProblemChange.completeExceptionally(exception));
    }
}
