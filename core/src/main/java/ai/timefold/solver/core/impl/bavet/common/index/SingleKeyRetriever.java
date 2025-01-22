package ai.timefold.solver.core.impl.bavet.common.index;

record SingleKeyRetriever<Key_>() implements KeyRetriever<Key_> {

    @SuppressWarnings("unchecked")
    @Override
    public Key_ apply(Object indexKeys) {
        return (Key_) indexKeys;
    }

}
