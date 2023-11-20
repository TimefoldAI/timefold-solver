package ai.timefold.solver.constraint.streams.bavet.common.index;

final class NoneIndexProperties implements IndexProperties {

    static final NoneIndexProperties INSTANCE = new NoneIndexProperties();

    private NoneIndexProperties() {
    }

    @Override
    public <Type_> Type_ toKey(int index) {
        throw new IllegalArgumentException("Impossible state: none index property requested");
    }

    @Override
    public <Type_> Type_ toKey(int from, int to) {
        throw new IllegalArgumentException("Impossible state: none index property requested");
    }

}
