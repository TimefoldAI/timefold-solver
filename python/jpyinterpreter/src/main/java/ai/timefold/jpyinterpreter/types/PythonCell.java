package ai.timefold.jpyinterpreter.types;

import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.jpyinterpreter.PythonLikeObject;

/**
 * Holds a reference to a PythonLikeObject. Cannot use {@link AtomicReference},
 * since cells are stored in a tuple via BUILD_TUPLE and thus need to be a {@link PythonLikeObject}.
 */
public class PythonCell extends AbstractPythonLikeObject {
    public static final PythonLikeType CELL_TYPE = new PythonLikeType("cell", PythonCell.class),
            $TYPE = CELL_TYPE;

    /**
     * The value the cell stores.
     */
    public PythonLikeObject cellValue;

    public PythonCell() {
        super(CELL_TYPE);
    }
}
