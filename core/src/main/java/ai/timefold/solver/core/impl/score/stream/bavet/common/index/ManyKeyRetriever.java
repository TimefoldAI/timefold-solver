package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

record ManyKeyRetriever<Key_>(int id) implements KeyRetriever<Key_> {

    @Override
    public Key_ apply(Object indexKeys) {
        return ((IndexKeys) indexKeys).get(id);
    }

}
