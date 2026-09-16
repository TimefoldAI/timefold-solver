package ai.timefold.solver.core.preview.api.neighborhood.stream.picking;

import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.EnumeratingStream;

import org.jspecify.annotations.NullMarked;

/**
 * Unlike {@link EnumeratingStream},
 * this stream is not cached in-memory;
 * instead, it is lazily evaluated.
 */
@NullMarked
public interface PickingStream {

}
