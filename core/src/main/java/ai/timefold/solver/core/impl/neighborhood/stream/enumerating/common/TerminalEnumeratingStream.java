package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TerminalEnumeratingStream<Solution_, Tuple_ extends AbstractTuple, Dataset_ extends AbstractDataset<Solution_, Tuple_>> {

    Dataset_ getDataset();

}
