package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

/**
 * Only used for null properties; non-null properties are supplied directly without this wrapper.
 * (Null properties unwrapped would cause indexing to mistake them for no key, and thus not index them.)
 */
final class NullIndexProperties implements IndexProperties {

    static final NullIndexProperties INSTANCE = new NullIndexProperties();

    private NullIndexProperties() { // No external instances.
    }

    @Override
    public <Type_> Type_ toKey(int id) {
        if (id != 0) {
            throw new IllegalArgumentException("Impossible state: index (" + id + ") != 0");
        }
        return null;
    }

}
