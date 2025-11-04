package ai.timefold.solver.core.preview.api.move.ruin;

import ai.timefold.solver.core.preview.api.move.Rebaser;

public interface RecreateMetadata {
    default RecreateMetadata rebase(Rebaser rebaser) {
        throw new UnsupportedOperationException();
    }

    default Iterable<Object> entities() {
        throw new UnsupportedOperationException();
    }

    default Iterable<Object> values() {
        throw new UnsupportedOperationException();
    }
}
