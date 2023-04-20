package ai.timefold.solver.examples.tennis.persistence;

import ai.timefold.solver.examples.common.persistence.AbstractJsonSolutionFileIO;
import ai.timefold.solver.examples.tennis.domain.TennisSolution;

public class TennisSolutionFileIO extends AbstractJsonSolutionFileIO<TennisSolution> {

    public TennisSolutionFileIO() {
        super(TennisSolution.class);
    }
}
