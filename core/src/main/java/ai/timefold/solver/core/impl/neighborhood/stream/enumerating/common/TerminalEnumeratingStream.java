package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TerminalEnumeratingStream<Solution_, Dataset_ extends AbstractDataset<Solution_>> {

    Dataset_ getDataset();

}
