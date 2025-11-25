package ai.timefold.solver.core.impl.bavet.common.index;

record CompositeKeyRetriever<Key_>(int id) implements KeyRetriever<Key_> {

    @Override
    public Key_ apply(Object compositeKey) {
        return ((CompositeKey) compositeKey).get(id);
    }

}
