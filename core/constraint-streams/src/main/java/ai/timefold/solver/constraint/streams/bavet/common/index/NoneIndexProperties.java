package ai.timefold.solver.constraint.streams.bavet.common.index;

final class NoneIndexProperties implements IndexProperties {

    static final NoneIndexProperties INSTANCE = new NoneIndexProperties();

    private NoneIndexProperties() {
    }

    @Override
    public <Type_> Type_ toKey(int id) {
        throw new IllegalArgumentException("Impossible state: none index property requested");
    }

}
